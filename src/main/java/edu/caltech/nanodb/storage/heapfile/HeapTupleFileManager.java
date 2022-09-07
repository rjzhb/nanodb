package edu.caltech.nanodb.storage.heapfile;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.caltech.nanodb.queryeval.TableStats;
import edu.caltech.nanodb.relations.Schema;
import edu.caltech.nanodb.storage.DBFile;
import edu.caltech.nanodb.storage.DBFileType;
import edu.caltech.nanodb.storage.DBPage;
import edu.caltech.nanodb.storage.PageReader;
import edu.caltech.nanodb.storage.PageWriter;
import edu.caltech.nanodb.storage.SchemaWriter;
import edu.caltech.nanodb.storage.StatsWriter;
import edu.caltech.nanodb.storage.StorageManager;
import edu.caltech.nanodb.storage.TupleFile;
import edu.caltech.nanodb.storage.TupleFileManager;

import java.io.File;


/**
 * This class provides high-level operations on heap tuple files.
 */

/**
 * The class provides the ability to create,
 * open and delete tuple files that use the heap-file organization.
 * When a table is opened for scanning,
 * this class is used to open the table’s corresponding tuple file.HeapTupleFileManager
 */

public class HeapTupleFileManager implements TupleFileManager {
    /** A logging object for reporting anything interesting that happens. */
    private static Logger logger = LogManager.getLogger(HeapTupleFileManager.class);


    /** A reference to the storage manager. */
    private StorageManager storageManager;


    public HeapTupleFileManager(StorageManager storageManager) {
        if (storageManager == null)
            throw new IllegalArgumentException("storageManager cannot be null");

        this.storageManager = storageManager;
    }


    @Override
    public DBFileType getDBFileType() {
        return DBFileType.HEAP_TUPLE_FILE;
    }


    @Override
    public String getShortName() {
        return "heap";
    }


    @Override
    public TupleFile createTupleFile(DBFile dbFile, Schema schema) {

        //打日志
        logger.info(String.format(
            "Initializing new heap tuple file %s with %d columns",
            dbFile, schema.numColumns()));

        //新增一个tablestats
        TableStats stats = new TableStats(schema.numColumns());
        //新建HeapTupleFile对象
        HeapTupleFile tupleFile = new HeapTupleFile(storageManager, this,
            dbFile, schema, stats);
        //数据写到db的page里
        saveMetadata(tupleFile);
        return tupleFile;
    }


    @Override
    public TupleFile openTupleFile(DBFile dbFile) {

        logger.info("Opening existing heap tuple file " + dbFile);

        // Table schema is stored into the header page, so get it and prepare
        // to write out the schema information.
        DBPage headerPage = storageManager.loadDBPage(dbFile, 0);
        PageReader hpReader = new PageReader(headerPage);
        // Skip past the page-size value.
        hpReader.setPosition(HeaderPage.OFFSET_SCHEMA_START);

        // Read in the schema details.
        SchemaWriter schemaWriter = new SchemaWriter();
        Schema schema = schemaWriter.readSchema(hpReader);

        // Read in the statistics.
        TableStats stats = StatsWriter.readTableStats(hpReader, schema);

        return new HeapTupleFile(storageManager, this, dbFile, schema, stats);
    }


    @Override
    public void saveMetadata(TupleFile tupleFile) {

        if (tupleFile == null)
            throw new IllegalArgumentException("tupleFile cannot be null");

        // Curiously, we never cast the tupleFile reference to HeapTupleFile,
        // but still, it would be very awkward if we tried to update the
        // metadata of some different kind of tuple file...
        if (!(tupleFile instanceof HeapTupleFile)) {
            throw new IllegalArgumentException(
                "tupleFile must be an instance of HeapTupleFile");
        }

        DBFile dbFile = tupleFile.getDBFile();

        Schema schema = tupleFile.getSchema();
        TableStats stats = tupleFile.getStats();

        // Table schema is stored into the header page, so get it and prepare
        // to write out the schema information.
        //从db文件里面读出一页，变成headerpage对象，填充这个page页面
        DBPage headerPage = storageManager.loadDBPage(dbFile, 0);
        //这个写的是真正的数据
        PageWriter hpWriter = new PageWriter(headerPage);
        // Skip past the page-size value.
        //固定长度
        hpWriter.setPosition(HeaderPage.OFFSET_SCHEMA_START);

        // Write out the schema details now.
        //写入元数据schema
        SchemaWriter schemaWriter = new SchemaWriter();
        schemaWriter.writeSchema(schema, hpWriter);

        // Compute and store the schema's size.
        int schemaEndPos = hpWriter.getPosition();
        int schemaSize = schemaEndPos - HeaderPage.OFFSET_SCHEMA_START;
        HeaderPage.setSchemaSize(headerPage, schemaSize);

        // Write in empty statistics, so that the values are at least
        // initialized to something.
        //写入表数据
        StatsWriter.writeTableStats(schema, stats, hpWriter);
        int statsSize = hpWriter.getPosition() - schemaEndPos;
        HeaderPage.setStatsSize(headerPage, statsSize);
    }


    @Override
    public void deleteTupleFile(TupleFile tupleFile) {
        // TODO
        /**
         * 有2件事要做：
         * 1.删除tuplefile
         * 2.删除内存中的page
         */
        //1.拉出一个页面，并且清空，后面会写回内存
        DBFile dbFile = tupleFile.getDBFile();

        DBPage dbPage = storageManager.loadDBPage(dbFile, 0);

        dbPage.setDataRange(3, dbPage.getPageSize() - 3, (byte) 0);
        dbPage.setDirty(true);
        //删了tuplefile
        tupleFile = null;
    }
}
