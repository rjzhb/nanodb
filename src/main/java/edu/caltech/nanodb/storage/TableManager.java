package edu.caltech.nanodb.storage;


import java.util.Set;

import edu.caltech.nanodb.commands.CommandProperties;
import edu.caltech.nanodb.relations.Schema;
import edu.caltech.nanodb.relations.TableInfo;


/**
 * This interface specifies the operations performed specifically on table
 * files.
 */
public interface TableManager {

    /**
     * Returns a set of the names of the tables available.
     * @return a list of table names
     */
    Set<String> getTables();


    /**
     * Returns true if the specified table exists, or false otherwise.
     *
     * @param tableName the name of the table to check for existence
     *
     * @return {@code true} if the specified table exists, or {@code false}
     *         if it doesn't exist
     */
    boolean tableExists(String tableName);


    /**
     * Creates a new table file with the table-name and schema specified in
     * the passed-in {@link TableInfo} object.  Additional details such as
     * the data file and the table manager are stored into the passed-in
     * {@code TableInfo} object upon successful creation of the new table.
     */
    TableInfo createTable(String tableName, Schema schema,
        CommandProperties properties);


    /**
     * This method opens the data file corresponding to the specified table
     * name and reads in the table's schema.  If the table is already open
     * then the cached data is simply returned.  If no such table exists,
     * {@code null} is returned.
     *
     * @param tableName the name of the table to open.  This is generally
     *        whatever was specified in a SQL statement that references the
     *        table.
     *
     * @return an object that holds the details of the open table, or
     *         {@code null} if the table does not exist.
     */
    TableInfo openTable(String tableName);


    /**
     * This method saves the schema and other details of a table into the
     * backing table file, using the schema and other details specified in
     * the passed-in {@link TableInfo} object.  It is used to initialize
     * new tables, and also to update tables when their schema changes.
     *
     * @param tableInfo This object is an in/out parameter.  It is used to
     *        specify the name and schema of the new table being created.
     *        When the table is successfully created, the object is updated
     *        with the actual file that the table's schema and data are
     *        stored in.
     */
    void saveTableInfo(TableInfo tableInfo);


    /**
     * This function analyzes the specified table, and updates the table's
     * statistics to be the most up-to-date values.
     *
     * @param tableInfo the opened table to analyze.
     */
    void analyzeTable(TableInfo tableInfo);


    /**
     * This method closes a table file that is currently open, flushing any
     * dirty pages to the table's storage in the process.
     *
     * @param tableInfo the table to close
     */
    void closeTable(TableInfo tableInfo);


    /**
     * Drops the specified table from the database.
     *
     * @param tableName the name of the table to drop
     */
    void dropTable(String tableName);
}
