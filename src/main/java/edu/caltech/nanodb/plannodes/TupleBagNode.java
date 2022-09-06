package edu.caltech.nanodb.plannodes;


import java.util.ArrayList;
import java.util.List;

import edu.caltech.nanodb.expressions.OrderByExpression;
import edu.caltech.nanodb.expressions.TupleLiteral;
import edu.caltech.nanodb.queryeval.PlanCost;
import edu.caltech.nanodb.relations.Schema;
import edu.caltech.nanodb.relations.Tuple;


/**
 * This plan-node returns tuples from a collection specified on the node
 * before it is prepared.  This can be used to inject a set of tuples into
 * an execution plan.
 */
public class TupleBagNode extends PlanNode {
    private boolean prepared = false;

    /** The collection of tuples that are returned by this plan node. */
    private ArrayList<Tuple> tuples = new ArrayList<>();


    private int currentTupleIndex;


    private int markedTupleIndex;


    public TupleBagNode(Schema schema) {
        if (schema == null)
            throw new IllegalArgumentException("schema cannot be null");

        this.schema = schema;
    }


    /**
     * Adds a tuple to the bag of tuples.  If the tuple is disk-backed, a
     * copy of the tuple is stored into this node.
     *
     * @param tup a tuple to add to the bag of tuples
     */
    public void addTuple(Tuple tup) {
        if (tup == null)
            throw new IllegalArgumentException("tup cannot be null");

        if (prepared)
            throw new IllegalStateException("Node was already prepared");

        tuples.add(tup.isDiskBacked() ? TupleLiteral.fromTuple(tup) : tup);
    }


    @Override
    public List<OrderByExpression> resultsOrderedBy() {
        return null;
    }

    @Override
    public boolean supportsMarking() {
        return true;
    }

    @Override
    public void prepare() {
        currentTupleIndex = -1;
        markedTupleIndex = -1;

        prepared = true;

        cost = new PlanCost(/* numTuples */ (float) tuples.size(),
            /* tupleSize */ 0f, /* cpuCost */ 0f, /* numBlockIOs */ 0,
            /* numLargeSeeks */ 0);

        stats = new ArrayList<>();
    }

    @Override
    public Tuple getNextTuple() {
        assert currentTupleIndex >= -1;
        assert currentTupleIndex <= tuples.size();

        if (currentTupleIndex == tuples.size())
            return null;

        Tuple tup = null;
        currentTupleIndex++;
        if (currentTupleIndex < tuples.size())
            tup = tuples.get(currentTupleIndex);

        return tup;
    }

    @Override
    public void markCurrentPosition() {
        markedTupleIndex = currentTupleIndex;
    }

    @Override
    public void resetToLastMark() {
        currentTupleIndex = markedTupleIndex;
    }

    @Override
    public void cleanUp() {
        // Do nothing.
    }

    @Override
    public String toString() {
        return "TupleBag[" + tuples.size() + " tuples]";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TupleBagNode) {
            TupleBagNode other = (TupleBagNode) obj;
            return tuples.equals(other.tuples);
        }

        return false;
    }

    @Override
    public int hashCode() {
        return tuples.hashCode();
    }
}
