package edu.caltech.test.nanodb.storage;


import java.io.File;
import java.io.IOException;

import edu.caltech.nanodb.server.properties.PropertyRegistry;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import edu.caltech.nanodb.storage.BufferManager;
import edu.caltech.nanodb.storage.DBFile;
import edu.caltech.nanodb.storage.DBFileType;
import edu.caltech.nanodb.storage.DBPage;
import edu.caltech.nanodb.storage.FileManager;
import edu.caltech.nanodb.storage.FileManagerImpl;


/** Tests to exercise the file manager implementation. */
@Test(groups={"storage", "framework"})
public class TestFileManager extends StorageTestCase {

    /** A reference to the File Manager implementation being exercised. */
    private FileManager fileMgr;

    /** Creating DBPages requires a Buffer Manager, and this is it. */
    private BufferManager bufMgr;


    @BeforeClass
    public void beforeClass() {
        fileMgr = new FileManagerImpl(testBaseDir);
        bufMgr = new BufferManager(fileMgr, new PropertyRegistry());
    }


    public void testCreateDeleteFile() throws IOException {
        String filename = "TestFileManager_testCreateDeleteFile";
        File f = new File(testBaseDir, filename);
        if (f.exists()) {
            if (!f.delete())
                throw new IOException("Couldn't delete file:  " + f);
        }

        DBFile dbf = fileMgr.createDBFile(filename, DBFileType.HEAP_TUPLE_FILE,
            DBFile.DEFAULT_PAGESIZE);

        f = dbf.getDataFile();
        assert f.getName().equals(filename);
        assert f.length() == DBFile.DEFAULT_PAGESIZE;
        assert f.canRead();

        DBPage page0 = new DBPage(bufMgr, dbf, 0);
        fileMgr.loadPage(dbf, 0, page0.getPageData());

        assert page0.readByte(0) == DBFileType.HEAP_TUPLE_FILE.getID();
        assert DBFile.decodePageSize(page0.readByte(1)) == DBFile.DEFAULT_PAGESIZE;

        fileMgr.closeDBFile(dbf);
        fileMgr.deleteDBFile(dbf);
        assert !f.exists();
    }


    public void testDoubleCreateFile() throws IOException {
        String filename = "TestFileManager_testDoubleCreateFile";
        File f = new File(testBaseDir, filename);
        if (f.exists()) {
            if (!f.delete())
                throw new IOException("Couldn't delete file " + f);
        }

        DBFile dbf = fileMgr.createDBFile(filename, DBFileType.HEAP_TUPLE_FILE,
            DBFile.DEFAULT_PAGESIZE);

        f = dbf.getDataFile();
        assert f.getName().equals(filename);
        assert f.length() == DBFile.DEFAULT_PAGESIZE;
        assert f.canRead();

        try {
            DBFile dbf2 = fileMgr.createDBFile(filename,
                DBFileType.HEAP_TUPLE_FILE, DBFile.DEFAULT_PAGESIZE);

            assert false : "Shouldn't be able to create a DBFile twice.";
        }
        catch (Exception e) {
            // Success.
        }

        fileMgr.closeDBFile(dbf);
        fileMgr.deleteDBFile(dbf);
        assert !f.exists();
    }
}
