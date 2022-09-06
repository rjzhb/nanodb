package edu.caltech.nanodb.relations;


import java.util.Arrays;
import java.util.HashSet;


/**
 * This class represents a set of columns in a schema by specifying the
 * indexes of the columns in the set.  This is used to represent primary keys,
 * candidate keys, foreign keys, and columns in indexes.  Because it can
 * represent constraints, an optional constraint-name and constraint-type
 * can be specified on the class.
 */
public class ColumnRefs {

    /** This array holds the indexes of the columns in the set. */
    private int[] colIndexes;

    /** This is the optional name of the constraint specified in the DDL. */
    private String constraintName;

    /**
     * This optional field can indicate if there is a particular constraint
     * on the indicated set of columns.
     */
    private TableConstraintType constraintType;


    protected ColumnRefs(int[] colIndexes, String constraintName,
                         TableConstraintType constraintType) {
        if (colIndexes == null)
            throw new IllegalArgumentException("colIndexes must be specified");

        if (colIndexes.length == 0) {
            throw new IllegalArgumentException(
                "colIndexes must have at least one element");
        }

        // Make sure that no column-index values are duplicated, and that none
        // are negative values.
        int[] tmp = colIndexes.clone();
        Arrays.sort(tmp);
        for (int i = 0; i < tmp.length; i++) {
            if (tmp[i] < 0) {
                throw new IllegalArgumentException(
                    "colIndexes cannot contain negative values; got " +
                    Arrays.toString(colIndexes));
            }

            if (i > 0 && tmp[i] == tmp[i - 1]) {
                throw new IllegalArgumentException(
                    "colIndexes cannot contain duplicate values; got " +
                    Arrays.toString(colIndexes));
            }
        }

        this.colIndexes = colIndexes;
        this.constraintName = constraintName;
        this.constraintType = constraintType;
    }


    public ColumnRefs(int[] colIndexes) {
        this(colIndexes, null, null);
    }


    public int size() {
        return colIndexes.length;
    }


    public int getCol(int i) {
        return colIndexes[i];
    }

    public int[] getCols() {
        return colIndexes;
    }


    /**
     * Returns true if the specified <tt>ColumnIndexes</tt> object has the
     * same columns as this object, in the exact same order.
     *
     * @param ci the <tt>ColumnIndexes</tt> object to compare to this object
     *
     * @return true if the two objects have the same column indexes, in the
     *         exact same order
     */
    public boolean equalsColumns(ColumnRefs ci) {
        return equalsColumns(ci.colIndexes);
    }


    /**
     * Returns true if the specified array of column-indexes has the
     * same columns as this object, in the exact same order.
     *
     * @param indexes the array of indexes to compare to this object
     *
     * @return true if the two objects have the same column indexes, in the
     *         exact same order
     */
    public boolean equalsColumns(int[] indexes) {
        return Arrays.equals(colIndexes, indexes);
    }


    /**
     * Returns true if the specified <tt>ColumnIndexes</tt> object has the same
     * columns as this object, independent of order.
     *
     * @param ci the <tt>ColumnIndexes</tt> object to compare to this object
     *
     * @return true if the two objects have the same column indexes,
     *         independent of order
     */
    public boolean hasSameColumns(ColumnRefs ci) {
        return hasSameColumns(ci.colIndexes);
    }


    /**
     * Returns true if the specified array of column-indexes has the same
     * columns as this object, independent of order.
     *
     * @param indexes the array of indexes to compare to this object
     *
     * @return true if the two objects have the same column indexes,
     *         independent of order
     */
    public boolean hasSameColumns(int[] indexes) {
        HashSet<Integer> tmp = new HashSet<>();
        for (int i : colIndexes)
            tmp.add(i);

        for (int i : indexes) {
            if (!tmp.contains(i))
                return false;

            tmp.remove(i);
        }

        return tmp.isEmpty();
    }


    public String getConstraintName() {
        return constraintName;
    }


    public void setConstraintName(String constraintName) {
        this.constraintName = constraintName;
    }


    public TableConstraintType getConstraintType() {
        return constraintType;
    }


    public void setConstraintType(TableConstraintType constraintType) {
        this.constraintType = constraintType;
    }


    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();

        buf.append('(');
        boolean first = true;
        for (int i : colIndexes) {
            if (first)
                first = false;
            else
                buf.append(", ");

            buf.append(i);
        }
        buf.append(')');

        return buf.toString();
    }
}
