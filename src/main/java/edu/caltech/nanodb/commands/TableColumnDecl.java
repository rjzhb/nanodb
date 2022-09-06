package edu.caltech.nanodb.commands;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.caltech.nanodb.relations.ColumnInfo;


/**
 * This class represents a single column declaration within a <tt>CREATE
 * TABLE</tt> command.
 */
public class TableColumnDecl {
    /** Basic details about the column, including its name and type. */
    private ColumnInfo columnInfo;

    /** Any constraints specified on the column. */
    private ArrayList<ConstraintDecl> constraints = new ArrayList<>();


    public TableColumnDecl(ColumnInfo columnInfo) {
        if (columnInfo == null)
            throw new IllegalArgumentException("columnInfo cannot be null");

        this.columnInfo = columnInfo;
    }


    public void addConstraint(ConstraintDecl constraint) {
        if (constraint == null)
            throw new IllegalArgumentException("constraint cannot be null");

        constraints.add(constraint);
    }


    public ColumnInfo getColumnInfo() {
        return columnInfo;
    }


    public List<ConstraintDecl> getConstraints() {
        return Collections.unmodifiableList(constraints);
    }
}
