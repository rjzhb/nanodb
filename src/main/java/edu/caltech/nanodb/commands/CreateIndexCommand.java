package edu.caltech.nanodb.commands;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import edu.caltech.nanodb.indexes.IndexManager;

import edu.caltech.nanodb.relations.ColumnRefs;
import edu.caltech.nanodb.relations.IndexColumnRefs;
import edu.caltech.nanodb.relations.KeyColumnRefs;
import edu.caltech.nanodb.relations.TableConstraintType;
import edu.caltech.nanodb.relations.TableInfo;

import edu.caltech.nanodb.server.NanoDBServer;
import edu.caltech.nanodb.storage.TableManager;
import edu.caltech.nanodb.storage.StorageManager;


/** This command-class represents the <tt>CREATE INDEX</tt> DDL command. */
public class CreateIndexCommand extends Command {
    /** A logging object for reporting anything interesting that happens. **/
    private static Logger logger = LogManager.getLogger(CreateIndexCommand.class);


    /** The name of the index being created. */
    private String indexName;


    /**
     * This flag specifies whether the index is a unique index or not.  If the
     * value is true then no key-value may appear multiple times; if the value
     * is false then a key-value may appear multiple times.
     */
    private boolean unique;


    /**
     * If this flag is {@code true} then the create-index operation should
     * only be performed if the specified index doesn't already exist.
     */
    private boolean ifNotExists;


    /** The name of the table that the index is built against. */
    private String tableName;


    /**
     * The list of column-names that the index is built against.  The order of
     * these values is important; for ordered indexes, the index records must be
     * kept in the order specified by the sequence of column names.
     */
    private ArrayList<String> columnNames = new ArrayList<>();


    /** Any additional properties specified in the command. */
    private CommandProperties properties;


    public CreateIndexCommand(String indexName, String tableName,
                              boolean unique) {
        super(Type.DDL);

        if (tableName == null)
            throw new IllegalArgumentException("tableName cannot be null");

        this.indexName = indexName;
        this.tableName = tableName;
        this.unique = unique;
    }


    /**
     * Returns {@code true} if index creation should only be attempted if it
     * doesn't already exist, {@code false} if index creation should always
     * be attempted.
     *
     * @return {@code true} if index creation should only be attempted if it
     *         doesn't already exist, {@code false} if index creation should
     *         always be attempted.
     */
    public boolean getIfNotExists() {
        return ifNotExists;
    }


    /**
     * Sets the flag indicating whether index creation should only be
     * attempted if it doesn't already exist.
     *
     * @param b the flag indicating whether index creation should only be
     *        attempted if it doesn't already exist.
     */
    public void setIfNotExists(boolean b) {
        ifNotExists = b;
    }


    /**
     * Get the name of the table containing the index to be dropped.
     *
     * @return the name of the table containing the index to drop
     */
    public String getTableName() {
        return tableName;
    }


    /**
     * Get the name of the index to be dropped.
     *
     * @return the name of the index to drop
     */
    public String getIndexName() {
        return indexName;
    }


    /**
     * Returns the list of column names specified for the index as an
     * unmodifiable list.
     *
     * @return the list of column names specified for the index as an
     *         unmodifiable list.
     */
    public List<String> getColumnNames() {
        return Collections.unmodifiableList(columnNames);
    }


    /**
     * Returns true if the requested index is a unique index; false otherwise.
     *
     * @return true if the requested index is a unique index; false otherwise.
     */
    public boolean isUnique() {
        return unique;
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
     * Adds a column to the list of columns to be included in the index.
     *
     * @param columnName the name of the column to add to the columns to be
     *        included in the index.
     */
    public void addColumn(String columnName) {
        if (columnName == null)
            throw new IllegalArgumentException("columnName cannot be null");

        this.columnNames.add(columnName);
    }


    @Override
    public void execute(NanoDBServer server) throws ExecutionException {

        StorageManager storageManager = server.getStorageManager();
        TableManager tableManager = storageManager.getTableManager();
        IndexManager indexManager = storageManager.getIndexManager();

        // Open the table and get the schema for the table.
        logger.debug(String.format("Opening table %s to retrieve schema",
            tableName));
        TableInfo tableInfo = tableManager.openTable(tableName);

        int[] cols = tableInfo.getSchema().getColumnIndexes(columnNames);

        IndexColumnRefs index = new IndexColumnRefs(indexName, cols);
        if (unique) {
            // Also record a candidate key on the columns.
            KeyColumnRefs key = new KeyColumnRefs(cols, indexName, TableConstraintType.UNIQUE);
        }

        indexManager.addIndexToTable(tableInfo, index);

        logger.debug(String.format("New index %s on table %s is created!",
            indexName, tableName));

        out.printf("Created index %s on table %s.%n", indexName, tableName);
    }
}
