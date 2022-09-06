package edu.caltech.nanodb.relations;


/**
 * This class represents a primary key or other unique key, specifying the
 * indexes of the columns in the key.
 */
public class KeyColumnRefs extends ColumnRefs {

    public KeyColumnRefs(int[] colIndexes, String constraintName,
                         TableConstraintType constraintType) {
        super(colIndexes, constraintName, constraintType);

        if (constraintType != TableConstraintType.PRIMARY_KEY &&
            constraintType != TableConstraintType.UNIQUE) {
            throw new IllegalArgumentException("constraintType must be " +
                "PRIMARY_KEY or UNIQUE, got " + constraintType);
        }
    }


    /**
     * Builds a KeyColumnRefs object off of a unique index definition.
     *
     * @param idxColRefs the unique index used for the key definition.
     */
    public KeyColumnRefs(IndexColumnRefs idxColRefs) {
        super(idxColRefs.getCols(), null, TableConstraintType.UNIQUE);
    }
}
