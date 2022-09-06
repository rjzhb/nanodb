package edu.caltech.nanodb.relations;


import java.io.IOException;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import edu.caltech.nanodb.commands.DeleteCommand;
import edu.caltech.nanodb.commands.UpdateCommand;

import edu.caltech.nanodb.expressions.BooleanOperator;
import edu.caltech.nanodb.expressions.ColumnName;
import edu.caltech.nanodb.expressions.ColumnValue;
import edu.caltech.nanodb.expressions.CompareOperator;
import edu.caltech.nanodb.expressions.ExistsOperator;
import edu.caltech.nanodb.expressions.Expression;
import edu.caltech.nanodb.expressions.LiteralValue;
import edu.caltech.nanodb.expressions.TupleLiteral;

import edu.caltech.nanodb.plannodes.PlanNode;

import edu.caltech.nanodb.queryast.QueryUtils;
import edu.caltech.nanodb.queryast.SelectClause;

import edu.caltech.nanodb.queryeval.Planner;

import edu.caltech.nanodb.server.EventDispatchException;
import edu.caltech.nanodb.server.NanoDBServer;
import edu.caltech.nanodb.server.RowEventListener;

import edu.caltech.nanodb.server.properties.ServerProperties;
import edu.caltech.nanodb.storage.TableManager;
import edu.caltech.nanodb.storage.StorageManager;


/**
 * <p>
 * This class enforces all database constraints on the database schema,
 * including <tt>NOT NULL</tt> constraints, primary key/unique constraints,
 * and foreign key constraints.  It also performs the appropriate updates
 * when the target of a foreign-key reference is updated or deleted.
 * </p>
 * <p>
 * This class has some interesting design challenges to work around.  It must
 * sometimes update rows in other tables, and there may or may not be indexes
 * on the columns being searched and/or updated.  The easiest way to do this
 * is to issue nested DML operations against the database.  This has multiple
 * benefits.  First, the planner can take advantage of indexes if they are
 * present.  Second, the nested DML operations will invoke the row-event
 * processing code, which ensures that any constraints on the modified tables
 * will also have their constraints enforced.
 * </p>
 */
public class DatabaseConstraintEnforcer implements RowEventListener {
    /** A logging object for reporting anything interesting that happens. */
    private static Logger logger = LogManager.getLogger(DatabaseConstraintEnforcer.class);


    private NanoDBServer server;


    private TableManager tableManager;


    public DatabaseConstraintEnforcer(NanoDBServer server) {
        this.server = server;

        // Pull out the table manager and index manager, which are both
        // very important for this component!
        StorageManager storageManager = server.getStorageManager();
        this.tableManager = storageManager.getTableManager();
    }


    /**
     * A simple helper method to include the constraint name if we actually
     * know it, or just use the error message if we don't have a named
     * constraint.
     */
    private String makeErrorMessage(String msg, String constraintName) {
        if (constraintName != null)
            return msg + " " + constraintName;

        return msg;
    }


    /**
     * Perform processing before a row is inserted into a table.
     *
     * @param tableInfo the table that the tuple will be inserted into.
     * @param newTuple the new tuple that will be inserted into the table.
     */
    @Override
    public void beforeRowInserted(TableInfo tableInfo, Tuple newTuple) {

        Schema schema = tableInfo.getTupleFile().getSchema();

        // Check NOT NULL constraints first - they are cheapest.
        checkNotNullConstraints(tableInfo, newTuple);

        // All other constraints are key constraints; if we aren't supposed to
        // enforce them, skip the rest.
        if (!server.getPropertyRegistry().getBooleanProperty(ServerProperties.PROP_ENFORCE_KEY_CONSTRAINTS))
            return;

        // Iterate through all candidate keys (PRIMARY KEY and UNIQUE
        // constraints) and verify that the new tuple doesn't violate
        // any of these constraints.
        List<KeyColumnRefs> candKeyList = schema.getCandidateKeys();
        for (KeyColumnRefs candKey : candKeyList) {
            if (hasCandidateKeyValue(tableInfo, candKey, newTuple)) {
                String msg = makeErrorMessage("Cannot insert tuple " +
                    newTuple + " due to unique constraint",
                    candKey.getConstraintName());

                throw new ConstraintViolationException(msg);
            }
        }

        // Check all foreign keys from this table to other tables.  These are
        // simple; the new values must appear in the referenced table.
        List<ForeignKeyColumnRefs> foreignKeys = schema.getForeignKeys();
        for (ForeignKeyColumnRefs foreignKey : foreignKeys) {
            if (!referencedTableHasValue(foreignKey, newTuple)) {
                String msg = makeErrorMessage("Cannot insert tuple " +
                    newTuple + " due to foreign key constraint",
                    foreignKey.getConstraintName());

                throw new ConstraintViolationException(msg);
            }
        }
    }


    /**
     * Perform processing after a row is inserted into a table.  For the
     * database constraint enforcer, all the work is done in the before-insert
     * handler.
     *
     * @param tblFileInfo the table that the tuple was inserted into.
     * @param newTuple    the new tuple that was inserted into the table.
     */
    @Override
    public void afterRowInserted(TableInfo tblFileInfo, Tuple newTuple)
            throws EventDispatchException {
        // Do nothing!
    }

    /**
     * Perform processing before a row is updated in a table.
     *
     * @param tableInfo the table that the tuple will be updated in.
     * @param oldTuple the old tuple in the table that is about to be updated.
     * @param newTuple the new version of the tuple.
     */
    @Override
    public void beforeRowUpdated(TableInfo tableInfo, Tuple oldTuple,
                                 Tuple newTuple) {

        Schema schema = tableInfo.getSchema();

        // Check NOT NULL constraints first - they are cheapest.
        checkNotNullConstraints(tableInfo, oldTuple);

        // All other constraints are key constraints; if we aren't supposed to
        // enforce them, skip the rest.
        if (!server.getPropertyRegistry().getBooleanProperty(ServerProperties.PROP_ENFORCE_KEY_CONSTRAINTS))
            return;

        // Iterate through all candidate keys (PRIMARY KEY and UNIQUE
        // constraints) and verify that the new tuple doesn't violate
        // any of these constraints.
        List<KeyColumnRefs> candKeyList = schema.getCandidateKeys();
        for (KeyColumnRefs candKey : candKeyList) {
            if (hasCandidateKeyValue(tableInfo, candKey, newTuple)) {
                String msg = makeErrorMessage(
                    "Cannot update tuple due to unique constraint",
                    candKey.getConstraintName());

                throw new ConstraintViolationException(msg);
            }
        }

        try {
            // Check all foreign keys from this table to other tables.  These are
            // simple; the new values must appear in the referenced table.
            List<ForeignKeyColumnRefs> foreignKeys = schema.getForeignKeys();
            for (ForeignKeyColumnRefs foreignKey : foreignKeys) {
                if (!referencedTableHasValue(foreignKey, newTuple)) {
                    String msg = makeErrorMessage(
                        "Cannot update tuple due to foreign key constraint",
                        foreignKey.getConstraintName());

                    throw new ConstraintViolationException(msg);
                }
            }

            // Check all foreign keys from other tables to this table.  These are
            // more complicated; depending on the configuration we may need to
            // propagate changes from this table to the referencing tables.
            for (String referencingTableName : schema.getReferencingTables()) {
                applyOnUpdateEffects(tableInfo, referencingTableName,
                    oldTuple, newTuple);
            }
        }
        catch (IOException e) {
            throw new ConstraintViolationException("Error during constraint enforcement", e);
        }
    }


    /**
     * Perform processing after a row is updated in a table.
     *
     * @param tableInfo the table that the tuple was updated in.
     * @param oldTuple the old version of the tuple before it was updated.
     * @param newTuple    the new tuple in the table that was updated.
     */
    @Override
    public void afterRowUpdated(TableInfo tableInfo,
                                Tuple oldTuple, Tuple newTuple) {
        // Do nothing!
    }

    /**
     * Perform processing after a row has been deleted from a table.
     *
     * @param tableInfo the table that the tuple will be deleted from.
     * @param oldTuple the old tuple in the table that is about to be deleted.
     */
    @Override
    public void beforeRowDeleted(TableInfo tableInfo, Tuple oldTuple) {

        // All constraints are foreign-key constraints; if we aren't supposed
        // to enforce them, skip the operation.
        if (!server.getPropertyRegistry().getBooleanProperty(ServerProperties.PROP_ENFORCE_KEY_CONSTRAINTS))
            return;

        Schema schema = tableInfo.getSchema();

        try {
            // Check all foreign keys from other tables to this table.  These are
            // more complicated; depending on the configuration we may need to
            // propagate changes from this table to the referencing tables.
            for (String referencingTableName : schema.getReferencingTables())
                applyOnDeleteEffects(tableInfo, referencingTableName, oldTuple);
        }
        catch (IOException e) {
            throw new ConstraintViolationException("Error during constraint enforcement", e);
        }
    }

    /**
     * Perform processing after a row has been deleted from a table.
     *
     * @param tableInfo the table that the tuple was deleted from.
     * @param oldTuple the old values that were in the tuple before it was
     *                    deleted.
     */
    @Override
    public void afterRowDeleted(TableInfo tableInfo, Tuple oldTuple) {
        // Do nothing!
    }


    /**
     * This helper function verifies that a tuple being added to a table
     * satisfies all the NOT NULL constraints on the table.
     *
     * @param tableInfo the table that the tuple is being added to
     * @param tuple the tuple being added to the table
     * @throws ConstraintViolationException if the tuple has a NULL value for
     *         any not-null columns
     */
    private void checkNotNullConstraints(TableInfo tableInfo, Tuple tuple) {
        Schema schema = tableInfo.getSchema();
        for (int notNullCol : schema.getNotNull()) {
            logger.debug(String.format("Checking NOT NULL constraint on " +
                "column %d", notNullCol));

            if (tuple.isNullValue(notNullCol)) {
                throw new ConstraintViolationException(String.format(
                    "Cannot insert tuple %s into table %s; NOT NULL " +
                    "constraint on column %s would be violated", tuple,
                    tableInfo.getTableName(),
                    schema.getColumnInfo(notNullCol).toString()));
            }
        }
    }


    /**
     * Checks to see if a particular candidate-key value already appears in a
     * table.  The method tries to find an index on the specified columns of
     * the table; if there is one, the index is used to look up a matching
     * row.  If no index exists, the method scans through the table looking
     * for the specified row.
     *
     * @param tableInfo the table to check for the candidate-key value
     * @param candidateKey a description of the candidate key to examine
     * @param tuple the tuple containing the candidate-key values
     *
     * @return {@code true} if the candidate-key value appears in the table,
     *         {@code false} otherwise.
     */
    private boolean hasCandidateKeyValue(TableInfo tableInfo,
        KeyColumnRefs candidateKey, Tuple tuple) {

        // Build a subquery expression to evaluate against this tuple,
        // to see if the table has the specified key-value.
        Expression existsOp = makeExistsPredicate(tableInfo,
            candidateKey.getCols(), tuple, candidateKey.getCols());

        return existsOp.evaluatePredicate(null);
    }


    /**
     * This helper function enforces a foreign key constraint by checking the
     * referenced table to ensure that the tuple being added has values that
     * appear in the referenced table.  This constraint enforcement is
     * performed using an index on the referenced table; it is an error if
     * there is no index to perform the check.
     *
     * @param foreignKey the foreign key constraint, which specifies both the
     *        referencing table's columns and the referenced table's columns
     *
     * @param tuple the tuple being added to the referencing table
     */
    private boolean referencedTableHasValue(ForeignKeyColumnRefs foreignKey,
                                            Tuple tuple) {

        // Foreign keys are not enforced if any part of the FK is NULL.
        for (int colIndex : foreignKey.getCols()) {
            // If this column-value is NULL then just say "yes the referenced
            // table has the value."  Not good, but this is how SQL works.
            if (tuple.isNullValue(colIndex))
                return true;
        }

        String referencedTableName = foreignKey.getRefTable();
        TableInfo referencedTableInfo = tableManager.openTable(referencedTableName);

        // Build a subquery expression to evaluate against this tuple,
        // to see if the table has the specified key-value.
        Expression existsOp = makeExistsPredicate(referencedTableInfo,
            foreignKey.getRefCols(), tuple, foreignKey.getCols());

        return existsOp.evaluatePredicate(null);
    }


    /**
     * This function performs <tt>ON UPDATE</tt> tasks for each referencing
     * table that is potentially affected by the update of a tuple in the
     * referenced table.
     */
    private void applyOnUpdateEffects(TableInfo tableInfo,
        String referencingTableName, Tuple oldTuple, Tuple newTuple)
        throws IOException {

        String tableName = tableInfo.getTableName();
        TableInfo referencingTableInfo =
            tableManager.openTable(referencingTableName);
        Schema referencingSchema = referencingTableInfo.getSchema();

        // Iterate through all foreign keys declared on the referencing table.
        for (ForeignKeyColumnRefs fk : referencingSchema.getForeignKeys()) {
            // We only care about the foreign keys that reference the table
            // being modified.
            if (!fk.getRefTable().equals(tableName))
                continue;

            // Now that we have the child table's foreign key info, we can
            // figure out how to resolve the UPDATE operation.

            switch (fk.getOnUpdateOption()) {
                case RESTRICT: {
                    // Check if this row's key-value appears in the referencing
                    // table.  If it does, throw an exception to prevent the
                    // update.

                    Expression existsOp = makeExistsPredicate(referencingTableInfo,
                        fk.getCols(), oldTuple, fk.getRefCols());

                    if (existsOp.evaluatePredicate(null)) {
                        throw new ConstraintViolationException(String.format(
                            "Cannot update tuple %s on table %s due to " +
                            "ON UPDATE RESTRICT constraint %s from " +
                            "referencing table %s", oldTuple, tableName,
                            fk.getConstraintName(), referencingTableName));
                    }
                }
                break;

                case CASCADE: {
                    // Check if this row's old key-value appears in the
                    // referencing table.  If it does, update the referencing
                    // table's rows to have the new version of the key-value.

                    UpdateCommand updateCmd =
                        makeUpdateCommand(referencingTableInfo, fk.getCols(),
                            oldTuple, newTuple, fk.getRefCols());

                    updateCmd.execute(server);
                }
                break;

                case SET_NULL: {
                    // Check if this row's old key-value appears in the
                    // referencing table.  If it does, update the referencing
                    // table's rows to have an all-NULLs version of the key-value.

                    Tuple nullTuple = TupleLiteral.ofSize(oldTuple.getColumnCount());
                    UpdateCommand updateCmd =
                        makeUpdateCommand(referencingTableInfo, fk.getCols(),
                            oldTuple, nullTuple, fk.getRefCols());

                    updateCmd.execute(server);
                }
                break;
            }
        }
    }


    /**
     * This function checks the <tt>ON DELETE</tt> option for each child table
     * affected by the deletion of tup due to a foreign key, and then
     * executes that option.
     */
    private void applyOnDeleteEffects(TableInfo tableInfo,
        String referencingTableName, Tuple oldTuple) throws IOException {

        String tableName = tableInfo.getTableName();
        TableInfo referencingTableInfo =
            tableManager.openTable(referencingTableName);
        Schema referencingSchema = referencingTableInfo.getSchema();

        // Iterate through all foreign keys declared on the referencing table.
        for (ForeignKeyColumnRefs fk : referencingSchema.getForeignKeys()) {
            // We only care about the foreign keys that reference the table
            // being modified.
            if (!fk.getRefTable().equals(tableName))
                continue;

            // Now that we have the child table's foreign key info, we can
            // figure out how to resolve the UPDATE operation.

            switch (fk.getOnUpdateOption()) {
                case RESTRICT: {
                    // Check if this row's key-value appears in the referencing
                    // table.  If it does, throw an exception to prevent the
                    // delete.

                    Expression existsOp = makeExistsPredicate(referencingTableInfo,
                        fk.getCols(), oldTuple, fk.getRefCols());

                    if (existsOp.evaluatePredicate(null)) {
                        throw new ConstraintViolationException(String.format(
                            "Cannot delete tuple %s on table %s due to " +
                            "ON DELETE RESTRICT constraint %s from " +
                            "referencing table %s", oldTuple, tableName,
                            fk.getConstraintName(), referencingTableName));
                    }
                }
                break;

                case CASCADE: {
                    // Check if this row's old key-value appears in the
                    // referencing table.  If it does, delete the rows in the
                    // referencing table that have the old key-value.

                    DeleteCommand deleteCmd =
                        makeDeleteCommand(referencingTableInfo, fk.getCols(),
                            oldTuple, fk.getRefCols());

                    deleteCmd.execute(server);
                }
                break;

                case SET_NULL: {
                    // Check if this row's old key-value appears in the
                    // referencing table.  If it does, update the rows in the
                    // referencing table to have an all-NULLs version of the
                    // key-value.

                    Tuple nullTuple = TupleLiteral.ofSize(oldTuple.getColumnCount());
                    UpdateCommand updateCmd =
                        makeUpdateCommand(referencingTableInfo, fk.getCols(),
                            oldTuple, nullTuple, fk.getRefCols());

                    updateCmd.execute(server);
                }
                break;
            }
        }
    }


    private Expression makeEqualityPredicate(Schema schema,
        int[] schemaIndexes, Tuple tuple, int[] tupleIndexes) {

        assert (schemaIndexes.length == tupleIndexes.length);

        if (schemaIndexes.length == 1) {
            return makeEqualityComparison(schema, schemaIndexes[0],
                tuple, tupleIndexes[0]);
        }
        else {
            BooleanOperator andOp = new BooleanOperator(BooleanOperator.Type.AND_EXPR);
            for (int i = 0; i < schemaIndexes.length; i++) {
                andOp.addTerm(makeEqualityComparison(schema, schemaIndexes[i],
                    tuple, tupleIndexes[i]));
            }
            return andOp;
        }
    }


    private CompareOperator makeEqualityComparison(Schema schema, int iSchema,
                                                   Tuple tuple, int iTuple) {
        ColumnName cn = schema.getColumnInfo(iSchema).getColumnName();
        ColumnValue colVal = new ColumnValue(cn);

        Object v = tuple.getColumnValue(iTuple);
        LiteralValue litVal = new LiteralValue(v);
        return new CompareOperator(CompareOperator.Type.EQUALS,
            colVal, litVal);
    }


    private Expression makeExistsPredicate(TableInfo tableInfo,
        int[] schemaIndexes, Tuple tuple, int[] tupleIndexes) {

        assert (schemaIndexes.length == tupleIndexes.length);

        Expression predicate = makeEqualityPredicate(tableInfo.getSchema(),
            schemaIndexes, tuple, tupleIndexes);

        SelectClause selClause =
            QueryUtils.makeSelectStar(tableInfo.getTableName(), predicate);

        ExistsOperator existsOp = new ExistsOperator(selClause);

        Planner planner = server.getQueryPlanner();
        PlanNode plan = planner.makePlan(selClause, null);
        existsOp.setSubqueryPlan(plan);

        return existsOp;
    }


    private UpdateCommand makeUpdateCommand(TableInfo tableInfo,
        int[] schemaIndexes, Tuple oldTuple, Tuple newTuple, int[] tupleIndexes) {

        assert (schemaIndexes.length == tupleIndexes.length);

        // "UPDATE table ..."
        UpdateCommand updateCmd = new UpdateCommand(tableInfo.getTableName());
        Schema schema = tableInfo.getSchema();

        // "... SET columns = new_values ..."
        for (int i = 0; i < schemaIndexes.length; i++) {
            updateCmd.addValue(schema.getColumnInfo(schemaIndexes[i]).getName(),
                new LiteralValue(newTuple.getColumnValue(tupleIndexes[i])));
        }

        // "... WHERE columns = old_values"
        Expression whereExpr = makeExistsPredicate(tableInfo, schemaIndexes,
            oldTuple, tupleIndexes);
        updateCmd.setWhereExpr(whereExpr);

        return updateCmd;
    }



    private DeleteCommand makeDeleteCommand(TableInfo tableInfo,
        int[] schemaIndexes, Tuple oldTuple, int[] tupleIndexes) {

        assert (schemaIndexes.length == tupleIndexes.length);

        Expression whereExpr = makeExistsPredicate(tableInfo, schemaIndexes,
            oldTuple, tupleIndexes);

        return new DeleteCommand(tableInfo.getTableName(), whereExpr);
    }
}
