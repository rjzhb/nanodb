package edu.caltech.nanodb.server;


import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import edu.caltech.nanodb.commands.ExitCommand;
import edu.caltech.nanodb.functions.FunctionDirectory;
import edu.caltech.nanodb.queryeval.Planner;
import edu.caltech.nanodb.server.properties.BooleanFlagValidator;
import edu.caltech.nanodb.server.properties.IntegerValueValidator;
import edu.caltech.nanodb.server.properties.PlannerClassValidator;
import edu.caltech.nanodb.server.properties.ServerProperties;
import edu.caltech.nanodb.server.properties.StringEnumValidator;
import edu.caltech.nanodb.server.properties.StringValueValidator;
import edu.caltech.nanodb.sqlparse.ParseUtil;
import edu.caltech.nanodb.storage.DBFile;

import edu.caltech.nanodb.commands.Command;
import edu.caltech.nanodb.commands.SelectCommand;
import edu.caltech.nanodb.server.properties.PropertyRegistry;
import edu.caltech.nanodb.storage.StorageManager;


/**
 * <p>
 * This class provides the entry-point operations for managing the database
 * server, and executing commands against it.  While it is certainly possible
 * to implement these operations separately, these implementations are
 * strongly recommended since they include all necessary supporting steps,
 * such as firing before- and after-command events, acquiring and releasing
 * locks, and other critical resource-management tasks.
 * </p>
 * <p>
 * This class also includes operations to parse strings into command objects,
 * so that code can construct SQL statements as strings and then issue them
 * against the database.  Sometimes it is easier to create a SQL string than
 * it is to construct the corresponding command objects.
 * </p>
 */
public class NanoDBServer implements ServerProperties {
    /** A logging object for reporting anything interesting that happens. */
    private static Logger logger = LogManager.getLogger(NanoDBServer.class);


    /** The property registry for this database server. */
    private PropertyRegistry propertyRegistry;


    /** The function directory for this database server. */
    private FunctionDirectory functionDirectory;


    /** The event dispatcher for this database server. */
    private EventDispatcher eventDispatcher;


    /** The storage manager for this database server. */
    private StorageManager storageManager;


    /**
     * A read-write lock to force DDL operations to occur serially, and
     * without any overlap from DML operations.
     */
    private ReentrantReadWriteLock schemaLock;


    /**
     * This static method encapsulates all of the operations necessary for
     * cleanly starting the NanoDB server.  Database server properties are
     * initialized from the JVM system properties, and/or defaults are used.
     */
    public void startup() {
        startup(null);
    }


    /**
     * This static method encapsulates all of the operations necessary for
     * cleanly starting the NanoDB server.  Database server properties are
     * initialized from the JVM system properties, and/or defaults are used,
     * but these values may optionally be overridden by the initial properties
     * specified as an argument.
     *
     * @param initialProperties an optional set of database configuration
     *        properties which will override system properties and default
     *        values, or {@code null} if no initial properties should be
     *        provided.
     */
    public void startup(Properties initialProperties) {
        // Start up the database by doing the appropriate startup processing.

        schemaLock = new ReentrantReadWriteLock();

        // Start with objects that a lot of database components need.

        // Everything needs configuration.
        propertyRegistry = new PropertyRegistry();

        // Apply system properties first (populated from e.g. command line),
        // then override with any arguments to this function.
        propertyRegistry.setProperties(System.getProperties());
        if (initialProperties != null)
            propertyRegistry.setProperties(initialProperties);

        propertyRegistry.setupCompleted();

        // Components for query evaluation, index updating, etc.
        functionDirectory = new FunctionDirectory();
        eventDispatcher = new EventDispatcher();

        // The storage manager is a big one!

        logger.info("Initializing storage manager.");
        storageManager = new StorageManager();
        storageManager.initialize(this);
    }


    /**
     * Returns the event-dispatcher for this database server.
     *
     * @return the event-dispatcher for this database server.
     */
    public EventDispatcher getEventDispatcher() {
        return eventDispatcher;
    }


    /**
     * Returns the property registry for this database server.
     *
     * @return the property registry for this database server.
     */
    public PropertyRegistry getPropertyRegistry() {
        return propertyRegistry;
    }


    /**
     * Returns the function directory for this database server.
     *
     * @return the function directory for this database server.
     */
    public FunctionDirectory getFunctionDirectory() {
        return functionDirectory;
    }


    /**
     * Returns the storage manager for this database server.
     *
     * @return the storage manager for this database server.
     */
    public StorageManager getStorageManager() {
        return storageManager;
    }


    public Command parseCommand(String command) {
        return ParseUtil.parseCommand(command, functionDirectory);
    }


    /**
     * Returns a query-planner object of the type specified in the current
     * server properties.  If the planner cannot be instantiated for some
     * reason, a {@code RuntimeException} will be thrown.
     *
     * @return a query-planner object of the type specified in the current
     *         server properties.
     *
     * @throws RuntimeException if the specified planner class cannot be
     *         instantiated for some reason.
     */
    public Planner getQueryPlanner() {
        String className =
            propertyRegistry.getStringProperty(PROP_PLANNER_CLASS);

        try {
            // Load and instantiate the specified planner class.
            Class<?> c = Class.forName(className);
            Planner p = (Planner) c.getDeclaredConstructor().newInstance();
            p.setStorageManager(storageManager);
            return p;
        }
        catch (Exception e) {
            throw new RuntimeException(
                "Couldn't instantiate Planner class " + className, e);
        }
    }


    /**
     * Parse and execute a single command, returning a {@link CommandResult}
     * object describing the results.  The tuples produced by the command may
     * optionally be included in the results as well.
     *
     * @param command the SQL operation to perform
     * @param includeTuples if {@code true}, the results will include all
     *        tuples produced by the command
     *
     * @return an object describing the outcome of the command execution
     */
    public CommandResult doCommand(String command, boolean includeTuples) {

        try {
            Command commandObject = parseCommand(command);
            return doCommand(commandObject, includeTuples);
        }
        catch (Exception e) {
            // If a parsing error or some other exception occurs, we won't
            // have a CommandResult to return.  So, make sure to return one
            // here.
            CommandResult result = new CommandResult();
            result.recordFailure(e);
            return result;
        }
    }


    /**
     * Parse and execute one or more commands, returning a list of
     * {@link CommandResult} object describing the results.  The tuples
     * produced by the commands may optionally be included in the results as
     * well.
     *
     * @param commands one or more SQL operations to perform
     * @param includeTuples if {@code true}, the results will include all
     *        tuples produced by the commands
     *
     * @return a list of objects describing the outcome of the command
     *         execution
     */
    public List<CommandResult> doCommands(String commands,
        boolean includeTuples) {

        ArrayList<CommandResult> results = new ArrayList<>();

        // Parse the string into however many commands there are.  If there is
        // a parsing error, no commands will run.
        List<Command> parsedCommands =
            ParseUtil.parseCommands(commands, functionDirectory);

        // Try to run each command in order.  Stop if a command fails.
        for (Command cmd : parsedCommands) {
            CommandResult result = doCommand(cmd, includeTuples);
            results.add(result);
            if (result.failed())
                break;
        }

        return results;
    }


    /**
     * Executes a single database command, generating a {@code CommandResult}
     * holding details about the operation, and optionally any tuples
     * generated by the operation.  This method also takes care of other
     * command-related details, such as firing "before-command" and
     * "after-command" events, and acquiring and releasing appropriate locks.
     *
     * @param command the command to execute
     * @param includeTuples a value of {@code true} causes the command's
     *        tuples to be stored into the command-result; {@code false}
     *        causes any tuples to be discarded.
     *
     * @return a command-result describing the results of the operation
     */
    public CommandResult doCommand(Command command, boolean includeTuples) {

        // DDL operations must be performed serially on the database; all
        // other operations are allowed to overlap with each other.
        Lock lock;
        if (command.getCommandType() == Command.Type.DDL)
            lock = schemaLock.writeLock();
        else
            lock = schemaLock.readLock();

        // The try-block is mainly here to ensure that the lock is released
        // at the end of command execution.
        lock.lock();  // TODO:  lockInterruptibly()?
        try {
            CommandResult result = new CommandResult();

            if (includeTuples && command instanceof SelectCommand)
                result.collectSelectResults((SelectCommand) command);

            result.startExecution();
            try {
                if (command instanceof ExitCommand) {
                    result.setExit();
                }
                else {
                    // Execute the command, but fire before- and after-command
                    // handlers when we execute it.
                    eventDispatcher.fireBeforeCommandExecuted(command);
                    command.execute(this);
                    eventDispatcher.fireAfterCommandExecuted(command);
                }
            }
            catch (Exception e) {
                logger.error("Command threw an exception!", e);
                result.recordFailure(e);

                if (!(e instanceof NanoDBException)) {
                    System.out.println("UNEXPECTED EXCEPTION:");
                    e.printStackTrace(System.out);
                }
            }
            result.endExecution();

            // Post-command cleanup:
            storageManager.getBufferManager().unpinAllSessionPages();

            if (propertyRegistry.getBooleanProperty(PROP_FLUSH_AFTER_CMD)) {
                try {
                    storageManager.flushAllData();
                }
                catch (Exception e) {
                    logger.error(
                        "Post-command flush of all data threw an exception!",
                        e);
                }
            }

            return result;
        }
        finally {
            lock.unlock();
        }
    }


    /**
     * This method encapsulates all of the operations necessary for cleanly
     * shutting down the NanoDB server.
     *
     * @return {@code true} if the database server was shutdown cleanly, or
     *         {@code false} if an error occurred during shutdown.
     */
    public boolean shutdown() {
        boolean success = true;

        try {
            storageManager.shutdown();
        }
        catch (Exception e) {
            logger.error("Couldn't cleanly shut down the Storage Manager!", e);
            success = false;
        }

        return success;
    }
}
