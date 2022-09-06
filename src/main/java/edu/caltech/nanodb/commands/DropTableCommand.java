package edu.caltech.nanodb.commands;


import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import edu.caltech.nanodb.server.NanoDBServer;
import edu.caltech.nanodb.storage.TableManager;
import edu.caltech.nanodb.storage.StorageManager;


/** This Command class represents the <tt>DROP TABLE</tt> SQL command. */
public class DropTableCommand extends Command {

    /** A logging object for reporting anything interesting that happens. **/
    private static Logger logger = LogManager.getLogger(DropTableCommand.class);


    /** The name of the table to drop from the database. */
    private String tableName;


    /**
     * This flag controls whether the drop-table command will fail if the
     * table already doesn't exist when the removal is attempted.
     */
    private boolean ifExists;


    /**
     * Construct a drop-table command for the named table.
     *
     * @param tableName the name of the table to drop.
     * @param ifExists a flag controlling whether the command should complain if
     *        the table already doesn't exist when the removal is attempted.
     */
    public DropTableCommand(String tableName, boolean ifExists) {
        super(Command.Type.DDL);
        this.tableName = tableName;
        this.ifExists = ifExists;
    }


    /**
     * Get the name of the table to be dropped.
     *
     * @return the name of the table to drop
     */
    public String getTableName() {
        return tableName;
    }


    /**
     * Returns the value of the "if exists" flag; true indicates that it is
     * not an error if the table doesn't exist when this command is issued.
     *
     * @return the value of the "if exists" flag
     */
    public boolean getIfExists() {
        return ifExists;
    }


    /**
     * This method executes the <tt>DROP TABLE</tt> command by calling the
     * {@link TableManager#dropTable} method with the specified table name.
     *
     * @throws ExecutionException if the table doesn't actually exist, or if
     *         the table cannot be deleted for some reason.
     */
    @Override
    public void execute(NanoDBServer server) throws ExecutionException {

        StorageManager storageManager = server.getStorageManager();
        TableManager tableManager = storageManager.getTableManager();

        // See if the table already doesn't exist.
        if (ifExists && !tableManager.tableExists(tableName)) {
            // Table already doesn't exist!  Skip the operation.
            out.printf("Table %s already doesn't exist; skipping drop-table.%n",
                tableName);
            return;
        }

        tableManager.dropTable(tableName);
    }


    @Override
    public String toString() {
        return "DropTable[" + tableName + "]";
    }
}
