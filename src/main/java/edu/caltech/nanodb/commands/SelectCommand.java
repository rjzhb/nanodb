package edu.caltech.nanodb.commands;


import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import edu.caltech.nanodb.server.NanoDBServer;
import edu.caltech.nanodb.server.SessionState;
import edu.caltech.nanodb.queryast.SelectClause;
import edu.caltech.nanodb.queryeval.Planner;
import edu.caltech.nanodb.queryeval.PrettyTuplePrinter;
import edu.caltech.nanodb.queryeval.TupleProcessor;
import edu.caltech.nanodb.relations.Schema;
import edu.caltech.nanodb.server.EventDispatcher;
import edu.caltech.nanodb.storage.TableManager;
import edu.caltech.nanodb.storage.StorageManager;


/**
 * This command object represents a top-level <tt>SELECT</tt> command issued
 * against the database.  The query itself is represented by the
 * {@link SelectClause} class, particularly because a <tt>SELECT</tt> statement
 * can itself contain other <tt>SELECT</tt> statements.
 *
 * @see SelectClause
 */
public class SelectCommand extends QueryCommand {

    /** A logging object for reporting anything interesting that happens. */
    private static Logger logger = LogManager.getLogger(SelectCommand.class);


    /**
     * This object contains all the details of the top-level select clause,
     * including any subqueries, that is going to be evaluated.
     */
    private SelectClause selClause;


    private TupleProcessor tupleProcessor;


    public SelectCommand(SelectClause selClause) {
        super(QueryCommand.Type.SELECT);

        if (selClause == null)
            throw new NullPointerException("selClause cannot be null");

        this.selClause = selClause;
    }


    /**
     * Returns the root select-clause for this select command.
     *
     * @return the root select-clause for this select command
     */
    public SelectClause getSelectClause() {
        return selClause;
    }


    /**
     * Prepares the <tt>SELECT</tt> statement for evaluation by analyzing the
     * schema details of the statement, and then preparing an execution plan
     * for the statement.
     */
    @Override
    protected void prepareQueryPlan(NanoDBServer server) {
        StorageManager storageManager = server.getStorageManager();
        TableManager tableManager = storageManager.getTableManager();
        Schema resultSchema = selClause.computeSchema(tableManager, null);
        logger.debug("Prepared SelectClause:\n" + selClause);
        logger.debug("Result schema:  " + resultSchema);

        // Create a plan for executing the SQL query.
        Planner planner = server.getQueryPlanner();
        plan = planner.makePlan(selClause, null);
    }


    public void setTupleProcessor(TupleProcessor tupleProcessor) {
        this.tupleProcessor = tupleProcessor;
    }


    @Override
    protected TupleProcessor getTupleProcessor(EventDispatcher eventDispatcher) {
        if (tupleProcessor == null) {
            logger.info("No tuple processor; using a PrettyTuplePrinter");
            tupleProcessor =
                new PrettyTuplePrinter(SessionState.get().getOutputStream());

            //tupleProcessor = new TuplePrinter(SessionState.get().getOutputStream());
        }

        return tupleProcessor;
    }


    @Override
    public String toString() {
        return "SelectCommand[" + selClause + "]";
    }
}

