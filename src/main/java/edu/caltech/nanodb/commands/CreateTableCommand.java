package edu.caltech.nanodb.commands;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import edu.caltech.nanodb.indexes.IndexManager;
import edu.caltech.nanodb.relations.ColumnInfo;
import edu.caltech.nanodb.relations.ForeignKeyColumnRefs;
import edu.caltech.nanodb.relations.IndexColumnRefs;
import edu.caltech.nanodb.relations.KeyColumnRefs;
import edu.caltech.nanodb.relations.Schema;
import edu.caltech.nanodb.relations.TableConstraintType;
import edu.caltech.nanodb.relations.TableInfo;
import edu.caltech.nanodb.server.NanoDBServer;
import edu.caltech.nanodb.server.properties.PropertyRegistry;
import edu.caltech.nanodb.storage.TableManager;
import edu.caltech.nanodb.storage.StorageManager;


/**
 * This command handles the <tt>CREATE TABLE</tt> DDL operation.
 */
public class CreateTableCommand extends Command {

    /** A logging object for reporting anything interesting that happens. */
    private static Logger logger = LogManager.getLogger(CreateTableCommand.class);


    /** Name of the table to be created. */
    private String tableName;


    /** If this flag is {@code true} then the table is a temporary table. */
    private boolean temporary;


    /**
     * If this flag is {@code true} then the create-table operation should
     * only be performed if the specified table doesn't already exist.
     */
    private boolean ifNotExists;


    /** List of column-declarations for the new table. */
    private List<ColumnInfo> columnInfos = new ArrayList<>();


    /** List of constraints for the new table. */
    private List<ConstraintDecl> constraints = new ArrayList<>();


    /** Any additional properties specified in the command. */
    private CommandProperties properties;


    /**
     * Create a new object representing a <tt>CREATE TABLE</tt> statement.
     *
     * @param tableName the name of the table to be created
     */
    public CreateTableCommand(String tableName) {
        super(Command.Type.DDL);

        if (tableName == null)
            throw new IllegalArgumentException("tableName cannot be null");

        this.tableName = tableName;
    }


    /**
     * Returns {@code true} if the table is a temporary table, {@code false}
     * otherwise.
     *
     * @return {@code true} if the table is a temporary table, {@code false}
     *         otherwise.
     */
    public boolean isTemporary() {
        return temporary;
    }


    /**
     * Specifies whether the table is a temporary table or not.
     *
     * @param b {@code true} if the table is a temporary table, {@code false}
     *        otherwise.
     */
    public void setTemporary(boolean b) {
        temporary = b;
    }


    /**
     * Returns {@code true} if table creation should only be attempted if it
     * doesn't already exist, {@code false} if table creation should always
     * be attempted.
     *
     * @return {@code true} if table creation should only be attempted if it
     *         doesn't already exist, {@code false} if table creation should
     *         always be attempted.
     */
    public boolean getIfNotExists() {
        return ifNotExists;
    }


    /**
     * Sets the flag indicating whether table creation should only be
     * attempted if it doesn't already exist.
     *
     * @param b the flag indicating whether table creation should only be
     *        attempted if it doesn't already exist.
     */
    public void setIfNotExists(boolean b) {
        ifNotExists = b;
    }


    /**
     * Sets any additional properties associated with the command.  The
     * value may be {@code null} to indicate no properties.
     *
     * @param properties any additional properties to associate with the
     *        command.
     */
    public void setProperties(CommandProperties properties) {
        this.properties = properties;
    }


    /**
     * Returns any additional properties specified for the command, or
     * {@code null} if no additional properties were specified.
     *
     * @return any additional properties specified for the command, or
     *         {@code null} if no additional properties were specified.
     */
    public CommandProperties getProperties() {
        return properties;
    }


    /**
     * Adds a column description to this create-table command.  This method is
     * primarily used by the SQL parser.
     *
     * @param colDecl the details of the column to add
     *
     * @throws NullPointerException if colDecl is null
     */
    public void addColumn(TableColumnDecl colDecl) {
        if (colDecl == null)
            throw new IllegalArgumentException("colDecl cannot be null");

        // Pull out the column-info value first, and add it to the table
        // specification.  If the table-name doesn't match, update this.

        ColumnInfo colInfo = colDecl.getColumnInfo();
        if (!tableName.equals(colInfo.getTableName())) {
            colInfo = new ColumnInfo(colInfo.getName(), tableName,
                colInfo.getType());
        }
        columnInfos.add(colInfo);

        // Next, if any column-level constraints are specified,
        // add them to the collection of constraints.
        for (ConstraintDecl constraint : colDecl.getConstraints())
            addConstraint(constraint);
    }


    /**
     * Returns an immutable list of the column descriptions that are part of
     * this <tt>CREATE TABLE</tt> command.
     *
     * @return an immutable list of the column descriptions that are part of
     *         this <tt>CREATE TABLE</tt> command.
     */
    public List<ColumnInfo> getColumns() {
        return Collections.unmodifiableList(columnInfos);
    }


    /**
     * Adds a constraint to this create-table command.  This method is primarily
     * used by the SQL parser.
     *
     * @param con the details of the table constraint to add
     *
     * @throws NullPointerException if con is null
     */
    public void addConstraint(ConstraintDecl con) {
        if (con == null)
            throw new IllegalArgumentException("con cannot be null");

        constraints.add(con);
    }


    /**
     * Returns an immutable list of the constraint declarations that are part
     * of this <tt>CREATE TABLE</tt> command.
     *
     * @return an immutable list of the constraint declarations that are part
     *         of this <tt>CREATE TABLE</tt> command.
     */
    public List<ConstraintDecl> getConstraints() {
        return Collections.unmodifiableList(constraints);
    }


    @Override
    public void execute(NanoDBServer server) throws ExecutionException {

        StorageManager storageManager = server.getStorageManager();
        TableManager tableManager = storageManager.getTableManager();

        PropertyRegistry propReg = server.getPropertyRegistry();
        boolean createIndexesOnKeys = propReg.getBooleanProperty(
            NanoDBServer.PROP_CREATE_INDEXES_ON_KEYS);

        // See if the table already exists.
        if (ifNotExists && tableManager.tableExists(tableName)) {
            out.printf("Table %s already exists; skipping create-table.%n",
                tableName);
            return;
        }

        // Set up the table's schema based on the command details.

        logger.debug("Creating a TableSchema object for the new table " +
            tableName + ".");

        Schema schema = new Schema();
        for (ColumnInfo colInfo : columnInfos) {
            try {
                schema.addColumnInfo(colInfo);
            }
            catch (IllegalArgumentException iae) {
                throw new ExecutionException("Duplicate or invalid column \"" +
                    colInfo.getName() + "\".", iae);
            }
        }

        // Do some basic verification of the table constraints:
        //  * Verify that all named constraints are uniquely named.
        //  * Open all tables referenced by foreign-key constraints, to ensure
        //    they exist.  (More verification will occur later.)
        HashSet<String> constraintNames = new HashSet<>();
        HashMap<String, TableInfo> referencedTables = new HashMap<>();
        for (ConstraintDecl cd: constraints) {
            String name = cd.getName();
            if (name != null && !constraintNames.add(name)) {
                throw new ExecutionException("Constraint name " + name +
                    " appears multiple times.");
            }

            if (cd.getType() == TableConstraintType.FOREIGN_KEY) {
                String refTableName = cd.getRefTable();
                TableInfo refTblInfo = tableManager.openTable(refTableName);
                if (refTblInfo == null) {
                    throw new ExecutionException("Referenced table " +
                        refTableName + " doesn't exist.");
                }

                referencedTables.put(refTableName, refTblInfo);
            }
        }

        // Initialize all the constraints on the table.
        initTableConstraints(storageManager, schema, referencedTables);

        // Create the table.
        logger.debug("Creating the new table " + tableName + " on disk.");
        TableInfo tableInfo = tableManager.createTable(tableName, schema, properties);
        logger.debug("New table " + tableName + " was created.");

        if (createIndexesOnKeys) {
            logger.debug("Creating indexes on the new table, and any " +
                "referenced tables");
            initIndexes(storageManager, tableInfo);
        }

        out.println("Created table:  " + tableName);
    }


    private void initTableConstraints(StorageManager storageManager,
        Schema schema, HashMap<String, TableInfo> referencedTables) {

        if (constraints.isEmpty()) {
            logger.debug("No table constraints specified, our work is done.");
            return;
        }

        TableManager tableManager = storageManager.getTableManager();

        logger.debug("Adding " + constraints.size() +
            " constraints to the table.");

        HashSet<String> constraintNames = new HashSet<>();

        for (ConstraintDecl cd : constraints) {
            // Make sure that if constraint names are specified, every
            // constraint is actually uniquely named.
            if (cd.getName() != null) {
                if (!constraintNames.add(cd.getName())) {
                    throw new ExecutionException("Constraint name " +
                        cd.getName() + " appears multiple times.");
                }
            }

            TableConstraintType type = cd.getType();
            if (type == TableConstraintType.PRIMARY_KEY ||
                type == TableConstraintType.UNIQUE) {
                // Make a candidate-key constraint and put it on the schema.

                int[] cols = schema.getColumnIndexes(cd.getColumnNames());
                KeyColumnRefs ck = new KeyColumnRefs(cols, cd.getName(), type);

                if (type == TableConstraintType.PRIMARY_KEY) {
                    // Add NOT NULL constraints for all primary-key columns.
                    for (int iCol : cols)
                        schema.addNotNull(iCol);
                }

                schema.addCandidateKey(ck);
            }
            else if (type == TableConstraintType.FOREIGN_KEY) {
                // Make a foreign key constraint and put it on the schema.
                // This involves these steps:
                // 1)  Create the foreign key on this table's schema.
                // 2)  Update the referenced table's schema to record that
                //     this table references that table.

                // This should never be null since we already resolved all
                // foreign-key table references earlier.
                TableInfo refTableInfo = referencedTables.get(cd.getRefTable());
                Schema refSchema = refTableInfo.getSchema();

                // The makeForeignKey() method ensures that the referenced
                // columns are also a candidate key (or primary key) on the
                // referenced table.
                ForeignKeyColumnRefs fk = DDLUtils.makeForeignKey(schema,
                    refTableInfo, cd);

                schema.addForeignKey(fk);

                // Update the referenced table's schema to record that this
                // table has a foreign-key reference to the table.
                if (refSchema.addReferencingTable(tableName))
                    tableManager.saveTableInfo(refTableInfo);
            }
            else if (type == TableConstraintType.NOT_NULL) {
                int idx = schema.getColumnIndex(cd.getColumnNames().get(0));
                schema.addNotNull(idx);
            }
            else {
                throw new ExecutionException("Unexpected constraint type " +
                    cd.getType());
            }
        }
    }


    private void initIndexes(StorageManager storageManager,
                             TableInfo tableInfo) {
        IndexManager indexManager = storageManager.getIndexManager();
        Schema schema = tableInfo.getSchema();

        for (ConstraintDecl cd : constraints) {
            TableConstraintType type = cd.getType();
            if (type == TableConstraintType.PRIMARY_KEY ||
                type == TableConstraintType.UNIQUE) {

                int[] cols = schema.getColumnIndexes(cd.getColumnNames());
                IndexColumnRefs ck = new IndexColumnRefs(cd.getName(), cols);

                // Make the index.  This also updates the table schema with
                // the fact that there is another candidate key on the table.
                indexManager.addIndexToTable(tableInfo, ck);
            }
            else if (type == TableConstraintType.FOREIGN_KEY) {

                // Check if there is already an index on the foreign-key
                // columns.  If there is not, we will create a non-unique
                // index on those columns.

                int[] fkCols = schema.getColumnIndexes(cd.getColumnNames());
                IndexColumnRefs fkColRefs = null; // schema.getIndexOnColumns(new ColumnRefs(fkCols));
                if (fkColRefs == null) {
                    // Need to make a new index for this foreign-key reference
                    fkColRefs = new IndexColumnRefs(cd.getName(), fkCols);
                    fkColRefs.setConstraintType(TableConstraintType.FOREIGN_KEY);

                    // Make the index.
                    indexManager.addIndexToTable(tableInfo, fkColRefs);

                    logger.debug(String.format(
                        "Created index %s on table %s to enforce foreign key.",
                        fkColRefs.getIndexName(), tableInfo.getTableName()));
                }
            }
        }
    }


    @Override
    public String toString() {
        return "CreateTable[" + tableName + "]";
    }


    /**
     * Returns a verbose, multi-line string containing all of the details of
     * this table.
     *
     * @return a detailed description of the table described by this command
     */
    public String toVerboseString() {
        StringBuilder strBuf = new StringBuilder();

        strBuf.append(toString());
        strBuf.append('\n');

        for (ColumnInfo colInfo : columnInfos) {
            strBuf.append('\t');
            strBuf.append(colInfo.toString());
            strBuf.append('\n');
        }

        for (ConstraintDecl con : constraints) {
            strBuf.append('\t');
            strBuf.append(con.toString());
            strBuf.append('\n');
        }

        return strBuf.toString();
    }
}
