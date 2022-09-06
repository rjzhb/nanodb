package edu.caltech.test.nanodb.indexes;


import java.util.Set;

import edu.caltech.nanodb.indexes.IndexManager;
import edu.caltech.nanodb.relations.TableInfo;
import edu.caltech.nanodb.storage.TableManager;

import org.testng.annotations.Test;

import edu.caltech.test.nanodb.sql.SqlTestCase;
import edu.caltech.nanodb.indexes.IndexInfo;
import edu.caltech.nanodb.relations.Schema;
import edu.caltech.nanodb.server.CommandResult;
import edu.caltech.nanodb.storage.StorageManager;


/**
 * This class exercises the database with some simple CREATE and DROP
 * index statements against a single table, to see if creation and deletion
 * of indexes behaves properly
 */

@Test
public class TestIndexOps extends SqlTestCase {

    public TestIndexOps() {
        super("setup_testIndexOps");
    }


    /**
     * This test simply checks if the CREATE INDEX command works properly.
     * It does so by running the command on a table, and checking various
     * signs that the index was made, such as making sure that the
     * index file exists and can be opened.
     */
    public void testCreateNormalIndex() {
        // Perform the result to create the index. Table is test_index_ops
        server.doCommand("CREATE INDEX idx_test1 ON test_index_ops (a)", false);

        // Get the IndexInfo corresponding to the created index

        StorageManager storageManager = server.getStorageManager();
	    TableManager tableManager = storageManager.getTableManager();
        IndexManager indexManager = storageManager.getIndexManager();

        TableInfo tableInfo = tableManager.openTable("TEST_INDEX_OPS");
        IndexInfo indexInfo = indexManager.openIndex(tableInfo, "IDX_TEST1");

        // Check that the index criteria are appropriate
        assert(indexInfo.getIndexName().equals("IDX_TEST1"));
        assert(indexInfo.getTableName().equals("TEST_INDEX_OPS"));

        // Check tblFileInfo has the schemas stored
        Schema schema = tableInfo.getSchema();

        Set<String> indexNames = schema.getIndexNames();
        assert(indexNames.contains("IDX_TEST1"));
    }


    /**
     * This test checks to make sure that nanodb throws an error if
     * the user tries to create an index on a column that already has
     * an index.
     */
    public void testCreateSameColIndex() {
        // Perform the result to create the index. Table is test_index_ops
        CommandResult result;
        result = server.doCommand(
                "CREATE INDEX idx_test2 ON test_index_ops (a, a)", false);
        assert result.failed();
    }


   	/**
     * This test checks to make sure that nanodb throws an error if
     * the user tries to create an index with the same name as
     * another index.
     */
    public void testCreateSameIndex() {
        // Perform the result to create the index. Table is test_index_ops
        CommandResult result;
        server.doCommand(
            "CREATE INDEX idx_test3 ON test_index_ops (b)", false);
        result = server.doCommand(
            "CREATE INDEX idx_test3 ON test_index_ops (b)", false);
        if (!result.failed()) {
            // Drop the original index
            StorageManager storageManager = server.getStorageManager();
	        TableManager tableManager = storageManager.getTableManager();
            IndexManager indexManager = storageManager.getIndexManager();

            TableInfo tableInfo = tableManager.openTable("TEST_INDEX_OPS");
            indexManager.dropIndex(tableInfo, "IDX_TEST3");
            tableManager.closeTable(tableInfo);

            assert false;
        }
    }


	 /**
     * This test makes sure that the DROP INDEX command works properly.
     * In particular it runs the create index command, and then the
     * drop index command, and checks to see that the table schema
     * no longer has that index.
     */
    public void testDropNormalIndex() {
        // Perform the result to create the index. Table is test_index_ops
        server.doCommand(
            "CREATE INDEX idx_test4 ON test_index_ops (c)", false);
        server.doCommand(
            "DROP INDEX idx_test4 ON test_index_ops", false);
        StorageManager storageManager = server.getStorageManager();
        TableManager tableManager = storageManager.getTableManager();
        TableInfo tableInfo = tableManager.openTable("TEST_INDEX_OPS");
        // Check tblFileInfo has the index deleted from it
        Schema schema = tableInfo.getSchema();

        // Should not be any indices in here
        assert schema.getIndexes().isEmpty();
    }


    /*
     * This test checks that an index with a unique constraint was created
     * when creating the table test_unique_ops on column a. It does this by
     * attempting to create an index on a again, and that should fail if the
     * index exists.
     *
     * @throws Exception if any issues occur.
     * /

    TODO:  THIS TEST WAS DISABLED BECAUSE WE CAN'T JUST CHECK IF THE COLUMNS
    ARE IN ANOTHER INDEX; MUST ALSO SEE IF THE OTHER INDEX IS THE SAME
    TYPE.  SOOO, FOR NOW WE DISABLE THIS TEST.

    SEE BasicIndexManager.addIndexToTable() FOR MORE DETAILS.

    public void testUniqueIndexExists() throws Throwable {
    // Perform the result to create the index. Table is test_unique_ops
    CommandResult result;
    result = server.doCommand(
    "CREATE UNIQUE INDEX idx_test1 ON test_unique_ops (a)", false);
    if (!result.failed()) {
    result = server.doCommand(
    "DROP INDEX idx_test1 ON test_unique_ops", false);
    assert false;
    }
    }
     */




    /*
     * This test checks that dropping a unique index removes the unique
     * constraint.
     *
     * @throws Exception if any issues occur.
     *
    public void testDropUniqueIndex() throws Throwable {
    // Perform the result to create the index. Table is test_unique_ops
    CommandResult result;
    result = server.doCommand(
    "CREATE UNIQUE INDEX idx_test2 ON test_unique_ops (c)", false);
    // Check that unique constraint has been made
    result = server.doCommand(
    "INSERT INTO test_unique_ops VALUES (7, 'pink', 10)", false);
    assert result.failed() : "Inserted a row that violated a new UNIQUE constraint";

    result = server.doCommand(
    "DROP INDEX idx_test2 ON test_unique_ops", false);

    StorageManager storageManager = server.getStorageManager();
    TableManager tableManager = storageManager.getTableManager();
    TableInfo tableInfo = tableManager.openTable(
    "test_unique_ops");
    // Check tblFileInfo has the index deleted from it
    TableSchema schema = tableInfo.getSchema();
    for (IndexColumnRefs index : schema.getIndexes()) {
    if (index.getIndexName().equals("idx_test2"))
    assert false;
    }

    // Check that unique constraint has been dropped
    result = server.doCommand(
    "INSERT INTO test_unique_ops VALUES (8, 'mauve', 10)", false);
    if (result.failed()) {
    assert false;
    }
    // Restore table
    result = server.doCommand(
    "DELETE FROM test_unique_ops WHERE a=8 AND b='mauve' " +
    "AND c=10", false);
    }


    /**
     * This test checks to see if the CREATE UNIQUE INDEX command searches
     * the relevant columns to see if the populated values are unique. It
     * does this by inserting a nonunique value into c, and then attempts
     * to create a unique index on c afterwards, which should fail.
     *
     * @throws Exception if any issues occur.
     *
    public void testUniquePopulated() throws Throwable {
    // Perform the result to create the index. Table is test_unique_ops
    CommandResult result;
    result = server.doCommand(
    "INSERT INTO test_unique_ops VALUES (10, 'taupe', 10)", false);
    result = server.doCommand(
    "CREATE UNIQUE INDEX idx_test3 ON test_unique_ops (c)", false);
    // Index should not be able to be created because c is not unique
    if (!result.failed()) {
    result = server.doCommand(
    "DROP INDEX idx_test3 ON test_unique_ops", false);
    assert false;
    }
    result = server.doCommand(
    "DELETE FROM test_unique_ops WHERE a=10 AND b='taupe' " +
    "AND c=10", false);
    }
     */

}
