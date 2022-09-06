package edu.caltech.test.nanodb.storage;


import java.io.File;

import org.apache.commons.io.FileUtils;


/**
 */
public class StorageTestCase {
    protected File testBaseDir;


    public StorageTestCase() {
        testBaseDir = new File("test_datafiles");
        if (!testBaseDir.exists())
            testBaseDir.mkdirs();
    }
}
