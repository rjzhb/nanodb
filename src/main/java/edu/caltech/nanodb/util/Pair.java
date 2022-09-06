package edu.caltech.nanodb.util;


/**
 * A simple read-only wrapper-class for holding two values.  This is used to
 * return results from the coercion functions, since they take two inputs and
 * must coerce them into being the same types.
 */
public class Pair {
    /** The first value of the pair. */
    public final Object value1;

    /** The second value of the pair. */
    public final Object value2;


    /**
     * Construct a new pair using the specified values.
     *
     * @param obj1 the first value to store
     * @param obj2 the second value to store
     */
    public Pair(Object obj1, Object obj2) {
        value1 = obj1;
        value2 = obj2;
    }


    /**
     * Returns a new {@code Pair} object with the values swapped.
     *
     * @return a new Pair object with the values swapped.
     */
    public Pair swap() {
        return new Pair(value2, value1);
    }
}
