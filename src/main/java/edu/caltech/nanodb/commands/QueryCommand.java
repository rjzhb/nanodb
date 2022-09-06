package edu.caltech.nanodb.commands;


import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import edu.caltech.nanodb.plannodes.PlanNode;
import edu.caltech.nanodb.queryeval.EvalStats;
import edu.caltech.nanodb.queryeval.PlanCost;
import edu.caltech.nanodb.queryeval.QueryEvaluator;
import edu.caltech.nanodb.queryeval.TupleProcessor;
import edu.caltech.nanodb.server.EventDispatcher;
import edu.caltech.nanodb.server.NanoDBServer;
import edu.caltech.nanodb.storage.StorageManager;


/**
 * This class represents all SQL query commands, including <tt>SELECT</tt>,
 * <tt>INSERT</tt>, <tt>UPDATE</tt>, and <tt>DELETE</tt>.  The main difference
 * between these commands is simply what happens with the tuples that are
 * retrieved from the database.
 */
public abstract class QueryCommand extends Command {

    /** Typesafe enumeration of query-command types. */
    public enum Type {
        /** A SELECT query, which simply retrieves rows of data. */
        SELECT,

        /** An INSERT query, which adds new rows of data to a table. */
        INSERT,

        /**
         * An UPDATE query, which retrieves and then modifies rows of data in
         * a table.
         */
        UPDATE,

        /**
         * A DELETE query, which retrieves and then deletes rows of data in
         * a table.
         */
        DELETE
    }


    /** A logging object for reporting anything interesting that happens. */
    private static Logger logger = LogManager.getLogger(QueryCommand.class);


    /** The type of this query command, from the {@link Type} enum. */
    private QueryCommand.Type queryType;


    protected PlanNode plan;


    /**
     * If this flag is true then the command is to be explained only.  Otherwise
     * the command is to be executed normally.
     */
    protected boolean explain = false;


    /**
     * Initializes a new query-command object.
     *
     * @param queryType the kind of query command that is being executed
     */
    protected QueryCommand(QueryCommand.Type queryType) {
        super(Command.Type.DML);
        this.queryType = queryType;
    }


    public void setExplain(boolean f) {
        explain = f;
    }


    @Override
    public void execute(NanoDBServer server) {
        prepareQueryPlan(server);

        if (!explain) {
            // Debug:  print out the plan and its costing details.

            logger.debug("Generated execution plan:\n" +
                PlanNode.printNodeTreeToString(plan, true));

            PlanCost cost = plan.getCost();
            if (cost != null) {
                logger.debug(String.format(
                    "Estimated %f tuples with average size %f bytes",
                    cost.numTuples, cost.tupleSize));
                logger.debug("Estimated number of block IOs: " +
                             cost.numBlockIOs);
                logger.debug("Estimated CPU cost:  " + cost.cpuCost);
            }

            // Execute the query plan, then print out the evaluation stats.

            TupleProcessor processor =
                getTupleProcessor(server.getEventDispatcher());

            EvalStats stats = QueryEvaluator.executePlan(plan, processor);

            // Print out the evaluation statistics.

            out.printf("%s took %f sec to evaluate.%n", queryType,
                stats.getElapsedTimeSecs());

            String desc;
            switch (queryType) {
            case SELECT:
                desc = "Selected ";
                break;

            case INSERT:
                desc = "Inserted ";
                break;

            case UPDATE:
                desc = "Updated ";
                break;

            case DELETE:
                desc = "Deleted ";
                break;

            default:
                desc = "(UNKNOWN) ";
            }
            out.println(desc + stats.getRowsProduced() + " rows.");
        }
        else {
            out.println("Explain Plan:");
            plan.printNodeTree(out, true, "    ");

            out.println();

            PlanCost cost = plan.getCost();
            if (cost != null) {
                out.printf("Estimated %f tuples with average size %f%n",
                    cost.numTuples, cost.tupleSize);
                out.println("Estimated number of block IOs:  " +
                    cost.numBlockIOs);
                logger.debug("Estimated CPU cost:  " + cost.cpuCost);
            }
            else {
                out.println("Plan cost is not available.");
            }
        }
    }


    /**
     * Prepares an execution plan for generating the tuples that this query
     * command will operate on.  Since the specific plan to generate depends
     * on the operation being performed, this is an abstract method to be
     * implemented by subclasses.  For example, <tt>SELECT</tt>s support very
     * sophisticated operations and thus require complex plans, but a
     * <tt>DELETE</tt> operation simply requires a scan over a tuple file,
     * perhaps with a predicate applied.
     *
     * @param server the server to use for planning, fetching table schemas,
     *        statistics, and other details relevant for planning
     */
    protected abstract void prepareQueryPlan(NanoDBServer server);


    /**
     * Creates a tuple-processor responsible for dealing with the tuples that
     * are generated by the query command.  Depending on the operation being
     * performed, different tuple-processors are appropriate.  For example,
     * the <tt>SELECT</tt> processor sends the tuples to the console or to a
     * remote client; the <tt>DELETE</tt> processor deletes the tuples from
     * the referenced table.
     *
     * @param eventDispatcher used for notifying other components in the
     *        database when rows are inserted/updated/deleted
     *
     * @return the tuple processor to use in processing tuples generated by
     *         the query
     */
    protected abstract TupleProcessor getTupleProcessor(
        EventDispatcher eventDispatcher);
}
