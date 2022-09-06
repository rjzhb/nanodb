package edu.caltech.nanodb.storage;


import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import edu.caltech.nanodb.server.performance.PerformanceCounters;


/**
 * The File Manager provides unbuffered, low-level operations for working with
 * paged data files.  It really doesn't know anything about the internal file
 * formats of the data files, except that the first two bytes of the first
 * page must specify the type and page size for the data file.  (This is a
 * requirement of {@link #openDBFile}.)
 *
 * @design Although it might make more sense to put per-file operations like
 *         "load page" and "store page" on the {@link DBFile} class, we
 *         provide higher-level operations on the Storage Manager so that we
 *         can provide global buffering capabilities in one place.
 *
 * @design This class only requires minimal synchronization for thread-safety.
 *         The only internal state maintained by the class is the performance
 *         information, so the {@link #updateFileIOPerfStats} method includes
 *         a synchronized block, but everything else is unprotected because
 *         the OS filesystem will be thread-safe.
 */
public class FileManagerImpl implements FileManager {

    /** A logging object for reporting anything interesting that happens. */
    private static Logger logger = LogManager.getLogger(FileManagerImpl.class);


    /**
     * The base directory that the file-manager should use for creating and
     * opening files.
     */
    private File baseDir;


    /**
     * The DBFile object of the file that was accessed most recently.  Used
     * for recording performance metrics regarding disk IOs.
     */
    private DBFile lastFileAccessed;


    /**
     * The page number of the file that was accessed most recently.  Used
     * for recording performance metrics regarding disk IOs.
     */
    private int lastPageNoAccessed;


    /**
     * Create a file-manager instance that uses the specified base directory.
     *
     * @param baseDir the base-directory that the file-manager should use
     */
    public FileManagerImpl(File baseDir) {
        if (baseDir == null)
            throw new IllegalArgumentException("baseDir cannot be null");

        if (!baseDir.isDirectory()) {
            throw new IllegalArgumentException("baseDir value " + baseDir +
               " is not a directory");
        }

        this.baseDir = baseDir;
    }

    @Override
    public File[] getDBFiles() {
        return this.baseDir.listFiles();
    }

    // Update our file-IO performance counters
    private void updateFileIOPerfStats(DBFile dbFile, int pageNo, boolean read,
                                       int bufSize) {
        synchronized (this) {
            if (lastFileAccessed == null || !dbFile.equals(lastFileAccessed)) {
                PerformanceCounters.inc(PerformanceCounters.STORAGE_FILE_CHANGES);
                lastPageNoAccessed = 0;
            }
            else {
                // Compute the "number of sectors difference" between the last
                // page we accessed and the current page we are accessing.  This
                // is obviously a guess, since we don't know the physical file
                // layout, or the physical sector size (it could be 4KiB too).
                long diff = dbFile.getPageSize() * (pageNo - lastPageNoAccessed);
                diff /= 512;

                PerformanceCounters.add(
                    PerformanceCounters.STORAGE_FILE_DISTANCE_TRAVELED,
                    Math.abs(diff));
            }

            PerformanceCounters.inc(read ?
                PerformanceCounters.STORAGE_PAGES_READ :
                PerformanceCounters.STORAGE_PAGES_WRITTEN);

            PerformanceCounters.add(read ?
                PerformanceCounters.STORAGE_BYTES_READ :
                PerformanceCounters.STORAGE_BYTES_WRITTEN, bufSize);

            lastFileAccessed = dbFile;
            lastPageNoAccessed = pageNo;
        }
    }


    /**
     * This helper function calculates the file-position of the specified page.
     * Obviously, this value is dependent on the page size.
     *
     * @param dbFile the database file to compute the page-start for
     * @param pageNo the page number to access
     *
     * @return the offset of the specified page from the start of the database
     *         file
     *
     * @throws IllegalArgumentException if the page number is negative
     */
    private long getPageStart(DBFile dbFile, int pageNo) {
        if (pageNo < 0)
            throw new IllegalArgumentException("pageNo must be >= 0, got " + pageNo);

        long pageStart = pageNo;
        pageStart *= (long) dbFile.getPageSize();

        return pageStart;
    }


    @Override
    public boolean fileExists(String filename) {
        File f = new File(baseDir, filename);
        return f.exists();
    }


    @Override
    public DBFile createDBFile(String filename, DBFileType type, int pageSize) {

        File f = new File(baseDir, filename);
        logger.debug("Creating new database file " + f + ".");
        try {
            if (!f.createNewFile())
                throw new FileSystemException("File " + f + " already exists!");
        }
        catch (IOException e) {
            throw new FileSystemException("Unexpected IO error while creating file", e);
        }

        DBFile dbFile;
        try {
            dbFile = new DBFile(f, type, pageSize);
        }
        catch (IOException e) {
            throw new FileSystemException("Couldn't create DB file " + f, e);
        }

        byte[] buffer = new byte[pageSize];
        buffer[0] = (byte) type.getID();
        buffer[1] = (byte) DBFile.encodePageSize(pageSize);

        savePage(dbFile, 0, buffer);

        return dbFile;
    }


    @Override
    public boolean renameDBFile(DBFile dbFile, String newFilename) {
        File dataFile = dbFile.getDataFile();
        File newDataFile = new File(baseDir, newFilename);
        if (dataFile.renameTo(newDataFile)) {
            // Rename succeeded!
            dbFile.setDataFile(newDataFile);
            return true;
        }

        // Rename failed.
        return false;
    }


    @Override
    public DBFile openDBFile(String filename) {
        try {
            File f = new File(baseDir, filename);
            if (!f.isFile())
                return null;    // File doesn't exist; return null.

            RandomAccessFile fileContents = new RandomAccessFile(f, "rw");

            int typeID = fileContents.readUnsignedByte();
            int pageSize = DBFile.decodePageSize(fileContents.readUnsignedByte());

            DBFileType type = DBFileType.valueOf(typeID);
            if (type == null)
                throw new IOException("Unrecognized file type ID " + typeID);

            DBFile dbFile;
            try {
                dbFile = new DBFile(f, type, pageSize, fileContents);
            }
            catch (IllegalArgumentException iae) {
                // This would be highly unlikely to occur, given that we store
                // the page-size P encoded as 2^p, and we load it above.
                throw new DataFormatException("Invalid page size " + pageSize +
                    " specified for data file " + f, iae);
            }

            logger.debug(String.format("Opened existing database file %s; " +
                "type is %s, page size is %d.", f, type, pageSize));

            return dbFile;
        }
        catch (IOException e) {
            throw new FileSystemException("Unexpected IO error while " +
                "opening DB file " + filename, e);
        }
    }


    @Override
    public boolean loadPage(DBFile dbFile, int pageNo, byte[] buffer,
                         boolean create) {
        if (pageNo < 0) {
            throw new IllegalArgumentException("pageNo must be >= 0, got " +
                pageNo);
        }

        if (buffer.length != dbFile.getPageSize()) {
            throw new IllegalArgumentException("Buffer has a different size" +
                " from the specified DBFile page-size");
        }

        // Update our file-IO performance counters
        updateFileIOPerfStats(dbFile, pageNo, /* read */ true, buffer.length);

        long pageStart = getPageStart(dbFile, pageNo);

        RandomAccessFile fileContents = dbFile.getFileContents();
        try {
            fileContents.seek(pageStart);
            fileContents.readFully(buffer);
        }
        catch (EOFException e) {
            // The read went past the end of this file.  How does the caller
            // want us to handle this situation?
            if (create) {
                // Caller wants to create the page if it doesn't already exist
                // yet.  Don't let the exception propagate.

                logger.debug(String.format(
                    "Requested page %d doesn't yet exist in file %s; creating.",
                    pageNo, dbFile));

                // Figure out what the new file-length should be, so we can
                // extend the file.  Note that we use pageNo+1 since we need
                // to have space for the page itself to be stored.
                long newLength = (1L + (long) pageNo) * (long) dbFile.getPageSize();

                // If this check fails, it's because we have a concurrency-
                // control/isolation issue somewhere that is making us cry.
                try {
                    long oldLength = fileContents.length();
                    if (oldLength < newLength) {
                        fileContents.setLength(newLength);
                        logger.debug(String.format("Set file %s length to %d",
                            dbFile, newLength));
                    }
                    else {
                        String msg = "Expected DB file to be less than " +
                            newLength + " bytes long, but it's " + oldLength +
                            " bytes long!";

                        logger.error(msg);
                        throw new DataFormatException(msg);
                    }
                }
                catch (IOException e2) {
                    throw new FileSystemException("Unexpected IO error " +
                        "while reading/updating file " + dbFile + " length", e2);
                }
            }
            else {
                // No page to load, and the caller didn't request file
                // extension.  Indicate failure by returning false.
                return false;
            }
        }
        catch (IOException e) {
            throw new FileSystemException(String.format(
                "Unexpected IO error while loading page %d from file %s",
                pageNo, dbFile));
        }

        // If we got here, page-load (and possible file-extension) succeeded.
        return true;
    }


    @Override
    public boolean loadPage(DBFile dbFile, int pageNo, byte[] buffer) {
        return loadPage(dbFile, pageNo, buffer, false);
    }


    @Override
    public void savePage(DBFile dbFile, int pageNo, byte[] buffer) {

        if (pageNo < 0) {
            throw new IllegalArgumentException("pageNo must be >= 0, got " +
                pageNo);
        }

        if (buffer.length != dbFile.getPageSize()) {
            throw new IllegalArgumentException("Buffer has a different size" +
                " from the specified DBFile page-size");
        }

        // Update our file-IO performance counters
        updateFileIOPerfStats(dbFile, pageNo, /* read */ false, buffer.length);

        long pageStart = getPageStart(dbFile, pageNo);

        try {
            RandomAccessFile fileContents = dbFile.getFileContents();
            fileContents.seek(pageStart);
            fileContents.write(buffer);
        }
        catch (IOException e) {
            throw new FileSystemException("Unexpected IO error while saving page", e);
        }
    }


    @Override
    public void syncDBFile(DBFile dbFile) {
        logger.info("Synchronizing database file to disk:  " + dbFile);
        try {
            dbFile.getFileContents().getFD().sync();
        }
        catch (IOException e) {
            throw new FileSystemException(
                "Unexpected IO error while synchronizing file " + dbFile, e);
        }
    }


    @Override
    public void closeDBFile(DBFile dbFile) {
        // Sync the file before closing, so that we can have some confidence
        // that any modified data has reached the disk.
        syncDBFile(dbFile);

        logger.info("Closing database file:  " + dbFile);
        try {
            dbFile.getFileContents().close();
        }
        catch (IOException e) {
            throw new FileSystemException(
                "Unexpected IO error while closing file " + dbFile, e);
        }
    }


    @Override
    public void deleteDBFile(String filename) {
        File f = new File(baseDir, filename);
        deleteDBFile(f);
    }


    @Override
    public void deleteDBFile(File f) {
        if (!f.delete())
            throw new FileSystemException("Couldn't delete file \"" +
                f.getName() + "\".");
    }


    @Override
    public void deleteDBFile(DBFile dbFile) {
        deleteDBFile(dbFile.getDataFile());
    }
}
