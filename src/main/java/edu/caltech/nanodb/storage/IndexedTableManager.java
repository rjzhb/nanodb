package edu.caltech.nanodb.storage;


import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import edu.caltech.nanodb.commands.CommandProperties;
import edu.caltech.nanodb.indexes.IndexManager;
import edu.caltech.nanodb.relations.ForeignKeyColumnRefs;
import edu.caltech.nanodb.relations.Schema;
import edu.caltech.nanodb.relations.TableInfo;
import edu.caltech.nanodb.server.properties.ServerProperties;


/**
 * This class provides an implementation of the {@link TableManager} interface
 * for tables that can have indexes and constraints on them.
 */
public class IndexedTableManager implements TableManager {

    /** A logging object for reporting anything interesting that happens. */
    private static Logger logger = LogManager.getLogger(IndexedTableManager.class);


    private StorageManager storageManager;

    private HashMap<String, TableInfo> openTables;


    IndexedTableManager(StorageManager storageManager) {
        this.storageManager = storageManager;
        openTables = new HashMap<>();
    }

    /**
     * This method takes a table name and returns a filename string that
     * specifies where the table's data is stored.
     *
     * @param tableName the name of the table to get the filename of
     *
     * @return the name of the file that holds the table's data
     */
    private String getTableFileName(String tableName) {
        return tableName + ".tbl";
    }

    @Override
    public Set<String> getTables() {
        HashSet<String> tableNames = new HashSet<>();
        FileManager fileManager = storageManager.getFileManager();
        for(File dbFile : fileManager.getDBFiles()) {
            // Note that in getTableFileName all we do is add a .tbl at the end,
            // so that's all we have to account for.
            tableNames.add(dbFile.getName().replaceAll("\\.tbl$", ""));
        }
        return tableNames;
    }


    // Inherit interface docs.
    @Override
    public boolean tableExists(String tableName) {
        String tblFileName = getTableFileName(tableName);
        FileManager fileManager = storageManager.getFileManager();

        return fileManager.fileExists(tblFileName);
    }


    // Inherit interface docs.
    @Override
    public TableInfo createTable(String tableName, Schema schema,
        CommandProperties properties) {

        int pageSize = storageManager.getServer().getPropertyRegistry().getIntProperty(ServerProperties.PROP_PAGE_SIZE);
        String storageType = "heap";

        if (properties != null) {
            logger.info("Using command properties " + properties);

            pageSize = properties.getInt("pagesize", pageSize);
            storageType = properties.getString("storage", storageType);

            HashSet<String> names = new HashSet<>(properties.getNames());
            names.remove("pagesize");
            names.remove("storage");
            if (!names.isEmpty()) {
                throw new IllegalArgumentException("Unrecognized property " +
                    "name(s) specified:  " + names);
            }
        }

        String tblFileName = getTableFileName(tableName);

        DBFileType type;
        if ("heap".equals(storageType)) {
            type = DBFileType.HEAP_TUPLE_FILE;
        }
        else if ("btree".equals(storageType)) {
            type = DBFileType.BTREE_TUPLE_FILE;
        }
        else {
            throw new IllegalArgumentException("Unrecognized table file " +
                "type:  " + storageType);
        }
        TupleFileManager tupleFileManager = storageManager.getTupleFileManager(type);

        // First, create a new DBFile that the tuple file will go into.
        FileManager fileManager = storageManager.getFileManager();
        DBFile dbFile = fileManager.createDBFile(tblFileName, type, pageSize);
        logger.debug("Created new DBFile for table " + tableName +
            " at path " + dbFile.getDataFile());

        // Now, initialize it to be a tuple file with the specified type and
        // schema.
        TupleFile tupleFile = tupleFileManager.createTupleFile(dbFile, schema);

        // Cache this table since it's now considered "open".
        TableInfo tableInfo = new TableInfo(tableName, tupleFile);
        openTables.put(tableName, tableInfo);

        return tableInfo;
    }


    // Inherit interface docs.
    @Override
    public void saveTableInfo(TableInfo tableInfo) {
        TupleFile tupleFile = tableInfo.getTupleFile();
        TupleFileManager manager = tupleFile.getManager();
        manager.saveMetadata(tupleFile);
    }


    // Inherit interface docs.
    @Override
    public TableInfo openTable(String tableName) {
        TableInfo tableInfo;

        // If the table is already open, just return the cached information.
        tableInfo = openTables.get(tableName);
        if (tableInfo != null)
            return tableInfo;

        // Open the data file for the table; read out its type and page-size.
        String tblFileName = getTableFileName(tableName);
        TupleFile tupleFile = storageManager.openTupleFile(tblFileName);
        if (tupleFile == null)
            return null;

        // Cache this table since it's now considered "open".
        tableInfo = new TableInfo(tableName, tupleFile);
        openTables.put(tableName, tableInfo);
        return tableInfo;
    }


    // Inherit interface docs.
    @Override
    public void analyzeTable(TableInfo tableInfo) {
        // Analyze the table's tuple-file.
        tableInfo.getTupleFile().analyze();

        // TODO:  Probably want to analyze all the indexes associated with
        //        the table as well...
    }


    // Inherit interface docs.
    @Override
    public void closeTable(TableInfo tableInfo) {
        // Remove this table from the cache since it's about to be closed.
        openTables.remove(tableInfo.getTableName());

        DBFile dbFile = tableInfo.getTupleFile().getDBFile();

        // Flush all open pages for the table.
        storageManager.getBufferManager().flushDBFile(dbFile);
        storageManager.getFileManager().closeDBFile(dbFile);
    }


    // Inherit interface docs.
    @Override
    public void dropTable(String tableName) {
        TableInfo tableInfo = openTable(tableName);
        Schema schema = tableInfo.getSchema();

        // Check whether this table is referenced by any other tables via
        // foreign keys.  If it is, prevent the DROP TABLE operation.
        Set<String> refTables = schema.getReferencingTables();
        if (!refTables.isEmpty()) {
            throw new TableException(String.format("Cannot drop table %s:  " +
                "referenced by %s", tableInfo.getTableName(), refTables));
        }

        // For every table that this table references, update that table's
        // schema to record that this table no longer references it.
        for (ForeignKeyColumnRefs fk : schema.getForeignKeys()) {
            // Open the parent table, and remove this table from its
            // "referencing tables" collection.
            String referencedTableName = fk.getRefTable();
            TableInfo referencedTableInfo = openTable(referencedTableName);
            Schema referencedSchema = referencedTableInfo.getSchema();
            referencedSchema.removeReferencingTable(tableName);

            // Persist the changes we made to the schema
            saveTableInfo(referencedTableInfo);
        }

        if (storageManager.getServer().getPropertyRegistry().getBooleanProperty(ServerProperties.PROP_ENABLE_INDEXES)) {
            IndexManager indexManager = storageManager.getIndexManager();

            for (String indexName : schema.getIndexNames()) {
                indexManager.dropIndex(tableInfo, indexName);
            }
        }

        // Close the table.  This will purge out all dirty pages for the table
        // as well.
        closeTable(tableInfo);

        String tblFileName = getTableFileName(tableName);
        storageManager.getFileManager().deleteDBFile(tblFileName);
    }
}
