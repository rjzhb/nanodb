package edu.caltech.nanodb.queryeval;


import java.util.List;

import edu.caltech.nanodb.expressions.Null;
import edu.caltech.nanodb.plannodes.*;
import edu.caltech.nanodb.relations.JoinType;
import edu.caltech.nanodb.storage.heapfile.HeapTupleFile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.caltech.nanodb.queryast.FromClause;
import edu.caltech.nanodb.queryast.SelectClause;

import edu.caltech.nanodb.expressions.Expression;

import edu.caltech.nanodb.relations.TableInfo;
import edu.caltech.nanodb.storage.StorageManager;


/**
 * This class generates execution plans for very simple SQL
 * <tt>SELECT * FROM tbl [WHERE P]</tt> queries.  The primary responsibility
 * is to generate plans for SQL <tt>SELECT</tt> statements, but
 * <tt>UPDATE</tt> and <tt>DELETE</tt> expressions will also use this class
 * to generate simple plans to identify the tuples to update or delete.
 */
public class SimplePlanner implements Planner {

    /**
     * A logging object for reporting anything interesting that happens.
     */
    private static Logger logger = LogManager.getLogger(SimplestPlanner.class);


    /**
     * The storage manager used during query planning.
     */
    protected StorageManager storageManager;


    /**
     * Sets the server to be used during query planning.
     */
    public void setStorageManager(StorageManager storageManager) {
        if (storageManager == null)
            throw new IllegalArgumentException("storageManager cannot be null");

        this.storageManager = storageManager;
    }

    public PlanNode makePlan(FromClause fromClause) {
        PlanNode planNode = null;
        switch (fromClause.getClauseType()) {
            case JOIN_EXPR:
                planNode = makeNestedJoinNodePlan(fromClause, null);
                break;
            case SELECT_SUBQUERY:
                planNode = makeSimpleSelect(fromClause.getTableName(), fromClause.getSelectClause().getWhereExpr(), null);
                break;
            case BASE_TABLE:
                TableInfo tableInfo = storageManager.getTableManager().openTable(fromClause.getTableName());
                planNode = new FileScanNode(tableInfo, null);
                break;
        }
        if (planNode != null) {
            planNode.prepare();
        }
        return planNode;

    }


    public PlanNode makeNestedJoinNodePlan(FromClause fromClause, SelectClause selectClause) {
        assert (fromClause.isJoinExpr());
        FromClause leftChild = fromClause.getLeftChild();
        FromClause rightChild = fromClause.getRightChild();

        PlanNode leftNode = null;
        PlanNode rightNode = null;
        if (leftChild != null) {
            leftNode = makePlan(leftChild);
        }
        if (rightChild != null) {
            rightNode = makePlan(rightChild);
        }

        NestedLoopJoinNode nestedLoopJoinNode = new NestedLoopJoinNode(leftNode, rightNode, JoinType.CROSS, selectClause.getWhereExpr());

        return nestedLoopJoinNode;
    }

    /**
     * Returns the root of a plan tree suitable for executing the specified
     * query.
     *
     * @param selClause an object describing the query to be performed
     * @return a plan tree for executing the specified query
     */
    @Override
    public PlanNode makePlan(SelectClause selClause,
                             List<SelectClause> enclosingSelects) {

        // For HW1, we have a very simple implementation that defers to
        // makeSimpleSelect() to handle simple SELECT queries with one table,
        // and an optional WHERE clause.

        if (enclosingSelects != null && !enclosingSelects.isEmpty()) {
            throw new UnsupportedOperationException(
                    "Not implemented:  enclosing queries");
        }


        FromClause fromClause = selClause.getFromClause();
        if (!selClause.isTrivialProject()) {
            //常量折叠
            if (fromClause == null) {
                ProjectNode projectNode = new ProjectNode(null, selClause.getSelectValues());
                projectNode.prepare();

                return projectNode;
            }

            if (!fromClause.isBaseTable()) {
                if (fromClause.isJoinExpr()) {
                    PlanNode planNode = makeNestedJoinNodePlan(fromClause, selClause);
                    planNode.prepare();

                    return planNode;
                }
            }

            SelectNode leftChild = makeSimpleSelect(fromClause.getTableName(), selClause.getWhereExpr(), null);

            ProjectNode projectNode = new ProjectNode(leftChild, selClause.getSelectValues());
            projectNode.prepare();

            return projectNode;
        }

        if (!fromClause.isBaseTable()) {
            //先实现普通的join
            if (fromClause.isJoinExpr()) {
                PlanNode planNode = makeNestedJoinNodePlan(fromClause, selClause);
                planNode.prepare();

                return planNode;
            }
        }


        return makeSimpleSelect(fromClause.getTableName(),
                selClause.getWhereExpr(), null);
    }


    /**
     * Constructs a simple select plan that reads directly from a table, with
     * an optional predicate for selecting rows.
     * <p>
     * While this method can be used for building up larger <tt>SELECT</tt>
     * queries, the returned plan is also suitable for use in <tt>UPDATE</tt>
     * and <tt>DELETE</tt> command evaluation.  In these cases, the plan must
     * only generate tuples of type {@link edu.caltech.nanodb.storage.PageTuple},
     * so that the command can modify or delete the actual tuple in the file's
     * page data.
     *
     * @param tableName The name of the table that is being selected from.
     * @param predicate An optional selection predicate, or {@code null} if
     *                  no filtering is desired.
     * @return A new plan-node for evaluating the select operation.
     */
    public SelectNode makeSimpleSelect(String tableName, Expression predicate,
                                       List<SelectClause> enclosingSelects) {
        if (tableName == null)
            throw new IllegalArgumentException("tableName cannot be null");

        if (enclosingSelects != null) {
            // If there are enclosing selects, this subquery's predicate may
            // reference an outer query's value, but we don't detect that here.
            // Therefore we will probably fail with an unrecognized column
            // reference.
            logger.warn("Currently we are not clever enough to detect " +
                    "correlated subqueries, so expect things are about to break...");
        }

        // Open the table.
        TableInfo tableInfo =
                storageManager.getTableManager().openTable(tableName);

        // Make a SelectNode to read rows from the table, with the specified
        // predicate.
        SelectNode selectNode = new FileScanNode(tableInfo, predicate);
        selectNode.prepare();
        return selectNode;
    }

}

