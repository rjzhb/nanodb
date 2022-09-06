package edu.caltech.nanodb.commands;


import java.util.ArrayList;
import java.util.List;

import edu.caltech.nanodb.relations.ColumnInfo;
import edu.caltech.nanodb.relations.ColumnType;
import edu.caltech.nanodb.relations.ForeignKeyColumnRefs;
import edu.caltech.nanodb.relations.Schema;
import edu.caltech.nanodb.relations.SchemaNameException;
import edu.caltech.nanodb.relations.TableInfo;


/**
 * This helper class provides some useful functions for constructing keys and
 * indexes and other details for tables that are being initialized.
 */
public class DDLUtils {

    /**
     * This method constructs a {@link ForeignKeyColumnRefs} object that
     * includes the columns named in the input list, as well as the referenced
     * table and column names.  Note that this method <u>does not</u> update
     * the schema stored on disk, or create any other supporting files.
     *
     * @param tableSchema the schema of the table that the foreign key
     *        will be added to
     *
     * @param refTableInfo the table that the foreign key will reference
     *
     * @param constraintDecl the parsed constraint declaration describing
     *        the foreign key
     *
     * @return an object describing the foreign key
     *
     * @throws SchemaNameException if a column-name cannot be found, or if a
     *         column-name is ambiguous (unlikely), or if a column is
     *         specified multiple times in the input list.
     */
    public static ForeignKeyColumnRefs makeForeignKey(Schema tableSchema,
        TableInfo refTableInfo, ConstraintDecl constraintDecl) {

        // if (tableInfo == null)
        //     throw new IllegalArgumentException("tableInfo must be specified");

        if (refTableInfo == null)
            throw new IllegalArgumentException("refTableInfo must be specified");

        if (constraintDecl == null)
            throw new IllegalArgumentException("constraintDecl must be specified");

        Schema refTableSchema = refTableInfo.getSchema();

        int[] colIndexes = tableSchema.getColumnIndexes(
            constraintDecl.getColumnNames());

        List<String> refColumnNames = constraintDecl.getRefColumnNames();
        int[] refColIndexes;

        if (refColumnNames.isEmpty()) {
            // The constraint declaration doesn't specify column names because
            // it wants to use the primary key of the referenced table.
            refColIndexes = refTableSchema.getPrimaryKey().getCols();
        }
        else {
            // The constraint declaration specifies column names, so convert
            // those names into the corresponding column indexes.
            refColIndexes = refTableSchema.getColumnIndexes(refColumnNames);
        }

        if (!refTableSchema.hasKeyOnColumns(refColIndexes)) {
            throw new SchemaNameException(String.format(
                "Referenced columns %s in table %s are not a candidate key",
                refColumnNames, refTableInfo.getTableName()));
        }

        ArrayList<ColumnInfo> colInfos = tableSchema.getColumnInfos(colIndexes);
        ArrayList<ColumnInfo> refColInfos = refTableSchema.getColumnInfos(refColIndexes);

        // Check if the corresponding columns in the FK are the same types
        for (int i = 0; i < colInfos.size(); i++) {
            ColumnType type = colInfos.get(i).getType();
            ColumnType refType = refColInfos.get(i).getType();
            if (!type.equals(refType)) {
                throw new IllegalArgumentException("columns in " +
                    "child and parent tables of the foreign key must be " +
                    "of the same type!");
            }
        }

        // The onDelete and onUpdate values could be null if they are
        // unspecified in the constructor.  They are set to
        // ForeignKeyValueChangeOption.RESTRICT as a default in this case in
        // the constructor for ForeignKeyColumnIndexes.
        ForeignKeyColumnRefs fk = new ForeignKeyColumnRefs(colIndexes,
            refTableInfo.getTableName(), refColIndexes,
            constraintDecl.getOnDeleteOption(),
            constraintDecl.getOnUpdateOption());

        fk.setConstraintName(constraintDecl.getName());
        return fk;
    }
}
