package edu.caltech.nanodb.plannodes;


import java.io.IOException;
import java.util.List;

import edu.caltech.nanodb.expressions.Expression;
import edu.caltech.nanodb.expressions.OrderByExpression;
import edu.caltech.nanodb.functions.TableFunction;
import edu.caltech.nanodb.relations.Tuple;


/**
 * A select plan-node that produces the output of a specific table-returning
 * function, checking the optional predicate against each tuple.  Note that
 * the planner is at the mercy of whatever statistics and other relevant
 * information are reported by the table-returning function.
 */
public class TableFunctionScanNode extends PlanNode {

    /**
     * The table-returning function whose tuples will be output by this
     * plan-node.
     */
    private TableFunction tableFunc;


    public TableFunctionScanNode(TableFunction tableFunc) {
        if (tableFunc == null)
            throw new IllegalArgumentException("tableFunc cannot be null");

        this.tableFunc = tableFunc;
    }


    @Override
    public Tuple getNextTuple() {
        return tableFunc.getNextTuple();
    }


    @Override
    public List<OrderByExpression> resultsOrderedBy() {
        return tableFunc.resultsOrderedBy();
    }


    @Override
    public void prepare() {
        tableFunc.prepare();

        schema = tableFunc.getSchema();
        stats = tableFunc.getStats();
        cost = tableFunc.getCost();
    }


    @Override
    public void cleanUp() {
        tableFunc.cleanUp();
    }

    @Override
    public String toString() {
        return "TableFunction[func:  " + tableFunc + "]";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TableFunctionScanNode) {
            TableFunctionScanNode other = (TableFunctionScanNode) obj;
            return tableFunc.equals(other.tableFunc);
        }

        return false;
    }

    @Override
    public int hashCode() {
        return tableFunc.hashCode();
    }
}
