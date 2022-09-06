package edu.caltech.nanodb.relations;


/**
 * This class represents a foreign key constraint between two tables in the
 * database.  The referencing table is not recorded in this data structure
 * because this key will be a member of that table's schema.
 */
public class ForeignKeyColumnRefs {
    /** This is the optional name of the constraint specified in the DDL. */
    private String constraintName;

    /**
     * This is the array of column indexes in the referencing table of the
     * foreign key.
     */
    private int[] colIndexes;

    /** This is the name of the referenced table in the foreign key. */
    private String referencedTable;

    /** These are the indexes of the columns in the referenced table. */
    private int[] referencedColIndexes;

    /** This is the ON DELETE behavior */
    private ForeignKeyValueChangeOption onDeleteOption;

    /** This is the ON UPDATE behavior */
    private ForeignKeyValueChangeOption onUpdateOption;


    public ForeignKeyColumnRefs(int[] colIndexes, String referencedTable,
        int[] referencedColIndexes, ForeignKeyValueChangeOption onDeleteOption,
        ForeignKeyValueChangeOption onUpdateOption) {

        if (colIndexes == null) {
            throw new IllegalArgumentException(
                "colIndexes must be specified");
        }

        if (referencedTable == null) {
            throw new IllegalArgumentException(
                "referencedTable must be specified");
        }

        if (referencedColIndexes == null) {
            throw new IllegalArgumentException(
                "referencedColIndexes must be specified");
        }

        if (colIndexes.length != referencedColIndexes.length) {
            throw new IllegalArgumentException(
                "colIndexes and referencedColIndexes must have the same length");
        }

        this.colIndexes = colIndexes;
        this.referencedTable = referencedTable;
        this.referencedColIndexes = referencedColIndexes;

        // If ON DELETE option isn't specified, default to RESTRICT.
        if (onDeleteOption != null)
            this.onDeleteOption = onDeleteOption;
        else
            this.onDeleteOption = ForeignKeyValueChangeOption.RESTRICT;

        // If ON UPDATE option isn't specified, default to RESTRICT.
        if (onUpdateOption != null)
            this.onUpdateOption = onUpdateOption;
        else
            this.onUpdateOption = ForeignKeyValueChangeOption.RESTRICT;
    }


    public String getConstraintName() {
        return constraintName;
    }


    public void setConstraintName(String constraintName) {
        this.constraintName = constraintName;
    }


    /** Returns the number of columns in the foreign key constraint. */
    public int size() {
        return colIndexes.length;
    }


    public int getCol(int i) {
        return colIndexes[i];
    }


    public int[] getCols() {
        return colIndexes;
    }

    public String getRefTable() {
        return referencedTable;
    }


    public int getRefCol(int i) {
        return referencedColIndexes[i];
    }

    public int[] getRefCols() {
        return referencedColIndexes;
    }


    public ForeignKeyValueChangeOption getOnDeleteOption() {
        return onDeleteOption;
    }


    public ForeignKeyValueChangeOption getOnUpdateOption() {
        return onUpdateOption;
    }


    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        boolean first;

        buf.append('(');
        first = true;
        for (int i : colIndexes) {
            if (first)
                first = false;
            else
                buf.append(", ");

            buf.append(i);
        }
        buf.append(") --> ");

        buf.append(referencedTable);

        buf.append('(');
        first = true;
        for (int i : referencedColIndexes) {
            if (first)
                first = false;
            else
                buf.append(", ");

            buf.append(i);
        }
        buf.append(')');
        buf.append(" ON DELETE: ");
        buf.append(onDeleteOption);
        buf.append(" ON UPDATE: ");
        buf.append(onUpdateOption);

        return buf.toString();
    }
}
