package edu.caltech.test.nanodb.storage;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import org.apache.commons.io.FileUtils;

import static org.mockito.Mockito.*;

import edu.caltech.nanodb.server.properties.PropertyRegistry;
import edu.caltech.test.nanodb.framework.Concurrent;
import org.testng.annotations.Test;

import edu.caltech.nanodb.storage.BufferManager;
import edu.caltech.nanodb.storage.DBFile;
import edu.caltech.nanodb.storage.DBFileType;
import edu.caltech.nanodb.storage.DBPage;
import edu.caltech.nanodb.storage.FileManager;
import edu.caltech.nanodb.storage.FileManagerImpl;


/** Tests to exercise the buffer manager implementation. */
@Test(groups={"storage", "framework"})
public class TestBufferManager extends StorageTestCase {

    /**
     * This simple test ensures that the Buffer Manager does some basic
     * buffering correctly.
     *
     * @throws IOException if any IO errors occur during the test.
     */
    public void testBuffering() throws IOException {
        FileUtils.cleanDirectory(testBaseDir);

        FileManager fileMgr = spy(new FileManagerImpl(testBaseDir));
        BufferManager bufMgr =
            new BufferManager(fileMgr, new PropertyRegistry());

        DBFile file = fileMgr.createDBFile("TestBufferManager_testBuffering",
            DBFileType.TEST_FILE, 4096);
        bufMgr.addFile(file);

        DBPage page = bufMgr.getPage(file, 5, /* create */ true);
        verify(fileMgr).loadPage(file, 5, page.getPageData(), true);

        // Should not result in a new call to the File Manager.
        DBPage page2 = bufMgr.getPage(file, 5, /* create */ true);

        assert page == page2;  // Should be the same object
        verify(fileMgr, times(1)).loadPage(file, 5, page.getPageData(), true);

        DBPage page3 = bufMgr.getPage(file, 3, /* create */ true);
        verify(fileMgr).loadPage(file, 3, page3.getPageData(), true);
        assert page != page3 && page2 != page3;

        verify(fileMgr, times(1)).loadPage(file, 3, page3.getPageData(), true);
        verify(fileMgr, times(1)).loadPage(file, 5, page.getPageData(), true);

        bufMgr.removeDBFile(file);
        fileMgr.closeDBFile(file);
    }


    /**
     * This test performs concurrent operations, but with each thread
     * operating against a separate file so that there are no data consistency
     * issues.  This should exercise the concurrency capabilities of the
     * Buffer Manager and File Manager without overcomplicating analysis of
     * the test results.
     *
     * @throws Exception if an IO error occurs, or if the concurrent execution
     *         code receives an Interrupted Exception.
     */
    public void testConcurrentSeparateFiles() throws Exception {
        FileUtils.cleanDirectory(testBaseDir);

        FileManager fileMgr = spy(new FileManagerImpl(testBaseDir));
        BufferManager bufMgr =
            new BufferManager(fileMgr, new PropertyRegistry());

        // Set the Buffer Manager's pool size to 4MiB.  This will force a
        // significant number of pool evictions during the test.
        bufMgr.setMaxCacheSize(4 * 1024 * 1024);

        // This test will create 20 threads.  Each thread will create its own
        // file with 1024 pages (page size = 4096 bytes, so 4MiB files), then
        // write to those pages in some randomized order (each file will be
        // written in its own order).  Each file is then verified in sequential
        // order from start to end.
        ArrayList<Runnable> tasks = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            String filename = String.format("TestBufferManager_testConcurrent_%d", i);
            tasks.add(() -> writeThenReadFile(fileMgr, bufMgr, filename, 4096, 1024));
        }

        // Give the test 60 seconds to complete.
        Concurrent.assertConcurrent("Concurrent reads and writes", tasks, 120);
    }


    /**
     * This helper function creates a test file, writes out pages to that file
     * in some randomized order, and then reads them back sequentially from
     * the start of the file until the end of the file, to verify the contents
     * of those pages.
     *
     * @param fileMgr the file manager to use
     * @param bufMgr the buffer manager to use
     * @param filename the name of the file to create for the test
     * @param pageSize the size of the pages in the test file
     * @param numPages the number of pages to create in the file
     */
    private void writeThenReadFile(FileManager fileMgr,
                                   BufferManager bufMgr, String filename,
                                   int pageSize, int numPages) {
        ArrayList<Integer> pages = new ArrayList<>();
        for (int i = 0; i < numPages; i++)
            pages.add(i);

        Collections.shuffle(pages);

        DBFile file =
            fileMgr.createDBFile(filename, DBFileType.TEST_FILE, pageSize);
        bufMgr.addFile(file);

        // Write each page in the file.
        for (int pageNum : pages) {
            // For each page, write a sequence of bytes based on the page
            // number and the byte position.
            DBPage page = bufMgr.getPage(file, pageNum, true);
            for (int i = 0; i < page.getPageSize(); i++)
                page.writeByte(i, (byte) (pageNum * i));

            page.unpin();
        }

        // Read each page in the file, and make sure the byte sequence is
        // correct.
        for (int pageNum : pages) {
            // For each page, verify the byte-sequence in the page.
            DBPage page = bufMgr.getPage(file, pageNum, true);
            for (int i = 0; i < page.getPageSize(); i++)
                assert page.readByte(i) == (byte) (pageNum * i);

            page.unpin();
        }

        // Close the data file.
        bufMgr.removeDBFile(file);
        fileMgr.closeDBFile(file);
    }
}
