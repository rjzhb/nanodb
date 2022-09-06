package edu.caltech.nanodb.expressions;


import java.util.ArrayList;
import java.util.List;

import edu.caltech.nanodb.relations.Tuple;


public class TupleHasher {
    /**
     * Collects tuple values according to colIndexes into an ArrayList
     * and returns the hash of the list. Order matters here, so
     * hash(tuple, [1,2,3]) != hash(tuple, [3,2,1]).
     * @param tuple the tuple to be hashed
     * @param colIndexes the indices of the columns to be hashed.
     * @return the hash value for the tuple given the passed column indices
     */
    public static int hashTuple(Tuple tuple, List<Integer> colIndexes) {
        if (tuple == null)
            throw new IllegalArgumentException("tuple cannot be null");
        if (colIndexes == null)
            throw new IllegalArgumentException("colIndexes cannot be null");

        // Collect tuple columns into an ArrayList
        ArrayList<Object> values = new ArrayList<>();
        for (int i : colIndexes)
            values.add(tuple.getColumnValue(i));

        return values.hashCode();
    }


    public static int hashTuple(Tuple tuple) {
        if (tuple == null)
            throw new IllegalArgumentException("tuple cannot be null");
        ArrayList<Object> values = new ArrayList<>();
        for (int i = 0; i < tuple.getColumnCount(); i++)
            values.add(tuple.getColumnValue(i));

        return values.hashCode();
    }
}