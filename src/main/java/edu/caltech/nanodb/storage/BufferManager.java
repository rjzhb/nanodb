package edu.caltech.nanodb.storage;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import edu.caltech.nanodb.server.SessionState;
import edu.caltech.nanodb.server.properties.PropertyObserver;
import edu.caltech.nanodb.server.properties.PropertyRegistry;
import edu.caltech.nanodb.server.properties.ServerProperties;


/**
 * The buffer manager reduces the number of disk IO operations by managing an
 * in-memory cache of data pages.  It also imposes a limit on the maximum
 * amount of space that can be used for data pages in the database.  Other
 * components can register with the Buffer Manager (as
 * {@link BufferManagerObserver}s) to be informed when pages are being
 * evicted.
 *
 * @todo Eventually add integrity checks, e.g. to make sure every cached
 *       page's file appears in the collection of cached files.
 */
public class BufferManager {

    /**
     * This helper class keeps track of a data page that is currently cached.
     */
    private static class CachedPageInfo {
        DBFile dbFile;

        int pageNo;

        CachedPageInfo(DBFile dbFile, int pageNo) {
            if (dbFile == null)
                throw new IllegalArgumentException("dbFile cannot be null");

            this.dbFile = dbFile;
            this.pageNo = pageNo;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof CachedPageInfo) {
                CachedPageInfo other = (CachedPageInfo) obj;
                return dbFile.equals(other.dbFile) && pageNo == other.pageNo;
            }
            return false;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 31 * hash + dbFile.hashCode();
            hash = 31 * hash + pageNo;
            return hash;
        }
    }


    /**
     * This helper class records the pin-count of a data page as imposed by a
     * given session, so that we can forcibly release the session's pins after
     * each command the session completes.
     */
    private static class SessionPinCount {
        /** The page that is pinned. */
        DBPage dbPage;

        /** The number of times the session has pinned the page. */
        int pinCount;

        SessionPinCount(DBPage dbPage) {
            this.dbPage = dbPage;
            this.pinCount = 0;
        }
    }


    /** A logging object for reporting anything interesting that happens. */
    private static Logger logger = LogManager.getLogger(BufferManager.class);


    /**
     * A simple object for synchronizing on, so that the buffer manager will
     * be thread-safe.
     */
    private final Object guard = new Object();


    private FileManager fileManager;


    private ArrayList<BufferManagerObserver> observers = new ArrayList<>();


    /**
     * This collection holds the {@link DBFile} objects corresponding to various
     * opened files the database is currently using.
     */
    private LinkedHashMap<String, DBFile> cachedFiles = new LinkedHashMap<>();


    /**
     * This collection holds database pages (not WAL pages) that the database
     * is currently working with, so that they don't continually need to be
     * reloaded.
     */
    private LinkedHashMap<CachedPageInfo, DBPage> cachedPages;


    /**
     * This collection maps session IDs to the files and pages that each
     * session has pinned, so that we can forcibly unpin pages used by a
     * given session when the session is done with the current command.
     */
    private HashMap<Integer, HashMap<DBPageID, SessionPinCount>> sessionPinCounts =
        new HashMap<>();


    /** This field records how many bytes are currently cached, in total. */
    private int totalBytesCached;


    /** This field records the maximum allowed cache size. */
    private int maxCacheSize;


    /**
     * A string indicating the buffer manager's page replacement policy.
     * Currently it can be "LRU" or "FIFO".
     */
    private String replacementPolicy;


    private class BufferPropertyObserver
        implements PropertyObserver, ServerProperties {
        public void propertyChanged(String propertyName, Object newValue) {
            // We only care about the pagecache-size value.
            if (PROP_PAGECACHE_SIZE.equals(propertyName)) {
                setMaxCacheSize((Integer) newValue);
            }
        }
    }



    public BufferManager(FileManager fileManager,
                         PropertyRegistry propertyRegistry) {
        this.fileManager = fileManager;
        propertyRegistry.addObserver(new BufferPropertyObserver());

        maxCacheSize = propertyRegistry.getIntProperty(
            ServerProperties.PROP_PAGECACHE_SIZE);

        // TODO:  Factor out the replacement policy implementation so that
        //        it's easier to replace/configure in the future.
        replacementPolicy = propertyRegistry.getStringProperty(
            ServerProperties.PROP_PAGECACHE_POLICY);
        cachedPages =
            new LinkedHashMap<>(16, 0.75f, "LRU".equals(replacementPolicy));

        totalBytesCached = 0;
    }


    /**
     * Sets the maximum buffer-cache size in bytes.  If in-use buffer
     * allocations exceed this limit then {@link #allocBuffer(int)} will
     * throw an exception.
     *
     * @param maxCacheSize the maximum size for the buffer cache.
     */
    public void setMaxCacheSize(int maxCacheSize) {
        if (maxCacheSize < ServerProperties.MIN_PAGECACHE_SIZE) {
            throw new IllegalArgumentException(
                "maxCacheSize must be at least " +
                ServerProperties.MIN_PAGECACHE_SIZE);
        }

        synchronized (guard) {
            this.maxCacheSize = maxCacheSize;

            if (maxCacheSize < totalBytesCached) {
                // Max cache size was reduced to below the current amount of
                // data cached.  Free up some space.
                ensureSpaceAvailable(0);
            }
        }
    }


    /**
     * Returns the current maximum buffer-cache size in bytes.
     *
     * @return the maximum size for the buffer cache.
     */
    public int getMaxCacheSize() {
        // Synchronize, just to be completely thread-safe...
        synchronized (guard) {
            return maxCacheSize;
        }
    }


    /**
     * Add another observer to the buffer manager.
     *
     * @param obs the observer to add to the buffer manager
     */
    public void addObserver(BufferManagerObserver obs) {
        if (obs == null)
            throw new IllegalArgumentException("obs cannot be null");

        synchronized (guard) {
            observers.add(obs);
        }
    }


    /**
     * This method attempts to allocate a buffer of the specified size,
     * possibly evicting some existing buffers in order to make space.
     *
     * @param size the size of the buffer to allocate
     *
     * @return an array of bytes, of the specified size
     */
    public byte[] allocBuffer(int size) {
        if (size <= 0)
            throw new IllegalArgumentException("size must be > 0, got " + size);

        synchronized (guard) {
            if (totalBytesCached < 0) {
                throw new IllegalStateException(
                    "totalBytesCached should never go below 0; saw " +
                    totalBytesCached);
            }

            ensureSpaceAvailable(size);

            if (totalBytesCached + size > maxCacheSize) {
                throw new IllegalStateException(
                    "Not enough room to allocate a buffer of " + size + " bytes!");
            }

            // Perform the allocation so that we know the JVM also has space...
            // Then update the total bytes in use by the buffer manager.
            byte[] buffer = new byte[size];
            totalBytesCached += size;

            // Record the identity of the buffer that we allocated, so that
            // releaseBuffer() can verify that it came from the buffer manager.
            // TODO:  System.identityHashCode() is not guaranteed to return a
            //        distinct value for every object, so we can have collisions
            //        on this value.  Come up with a different approach.
            // allocatedBuffers.add(System.identityHashCode(buffer));

            return buffer;
        }
    }


    public void releaseBuffer(byte[] buffer) {
        synchronized (guard) {
            // Verify that this was a buffer we allocated?
            // TODO:  System.identityHashCode() is not guaranteed to return a
            //        distinct value for every object, so we can have collisions
            //        on this value.  Come up with a different approach.
            /*
            if (!allocatedBuffers.remove(System.identityHashCode(buffer))) {
                throw new IllegalArgumentException("Received a buffer that " +
                    "wasn't allocated by the Buffer Manager");
            }
            */

            // Record that the buffer's space is now available.
            totalBytesCached -= buffer.length;
        }
    }


    /**
     * Retrieves the specified {@link DBFile} from the buffer manager, if it
     * has already been opened.
     *
     * @param filename The filename of the database file to retrieve.  This
     *        should be ONLY the database filename, no path.  The path is
     *        expected to be relative to the database's base directory.
     *
     * @return the {@link DBFile} corresponding to the filename, if it has
     *         already been opened, or <tt>null</tt> if the file isn't currently
     *         open.
     */
    public DBFile getFile(String filename) {
        synchronized (guard) {
            DBFile dbFile = cachedFiles.get(filename);
            if (dbFile == null) {
                if (logger.isDebugEnabled())
                    logger.debug("File %s isn't in the cache; opening.", filename);

                dbFile = fileManager.openDBFile(filename);
                if (dbFile != null)
                    cachedFiles.put(filename, dbFile);
            }

            return dbFile;
        }
    }


    public void addFile(DBFile dbFile) {
        if (dbFile == null)
            throw new IllegalArgumentException("dbFile cannot be null");

        synchronized (guard) {
            String filename = dbFile.getDataFile().getName();
            if (cachedFiles.containsKey(filename)) {
                throw new IllegalStateException(
                    "File cache already contains file " + filename);
            }

            // NOTE:  If we want to keep a cap on how many files are opened, we
            //        would do that here.

            logger.debug(String.format("Adding file %s to file-cache.", filename));

            cachedFiles.put(filename, dbFile);
        }
    }


    /**
     * Records that the page was pinned by the current session.  This method
     * does not actually pin the page; it is presumed that the page is already
     * pinned.
     *
     * @param dbPage the page that was pinned by the session
     */
    public void recordPagePinned(DBPage dbPage) {
        synchronized (guard) {
            int sessionID = SessionState.get().getSessionID();

            // Retrieve the set of pages pinned by the current session.
            HashMap<DBPageID, SessionPinCount> pinnedBySession =
                sessionPinCounts.computeIfAbsent(sessionID, k -> new HashMap<>());

            // Find the session-specific pin-count for the data page.
            SessionPinCount spc = pinnedBySession.computeIfAbsent(
                new DBPageID(dbPage), k -> new SessionPinCount(dbPage));

            // Finally, increment the session's pin-count on this page.
            spc.pinCount++;
        }
    }


    /**
     * Records that the page was unpinned by the current session.  This method
     * does not actually unpin the page; it is presumed that the page will be
     * unpinned after this call.
     *
     * @param dbPage the page that was unpinned
     */
    public void recordPageUnpinned(DBPage dbPage) {
        synchronized (guard) {
            int sessionID = SessionState.get().getSessionID();

            // Retrieve the set of pages pinned by the current session.
            HashMap<DBPageID, SessionPinCount> pinnedBySession =
                sessionPinCounts.get(sessionID);
            if (pinnedBySession == null) {
                logger.error(String.format("DBPage %d is being unpinned by " +
                        "session %d, but we have no record of the session!",
                    dbPage.getPageNo(), sessionID));
                return;
            }

            // Find the session-specific pin-count for the data page.
            DBPageID pageID = new DBPageID(dbPage);
            SessionPinCount spc = pinnedBySession.get(pageID);
            if (spc == null) {
                logger.error(String.format("DBPage %d is being unpinned by " +
                        "session %d, but we have no record of it having been pinned!",
                    dbPage.getPageNo(), sessionID));
                return;
            }

            // Record that the page was unpinned.
            spc.pinCount--;

            // If the pin-count went to zero, remove the SessionPinCount object.
            if (spc.pinCount == 0) {
                pinnedBySession.remove(pageID);

                // If the set of pages pinned by the current session is now empty,
                // remove the set of pages.
                if (pinnedBySession.isEmpty())
                    sessionPinCounts.remove(sessionID);
            }
        }
    }


    /**
     * This method unpins all pages pinned by the current session.  This is
     * generally done at the end of each transaction so that pages aren't
     * pinned forever, and can actually be evicted from the buffer manager.
     */
    public void unpinAllSessionPages() {
        synchronized (guard) {
            // Unpin all pages pinned by this session.
            int sessionID = SessionState.get().getSessionID();

            // Retrieve the set of pages pinned by the current session.
            HashMap<DBPageID, SessionPinCount> pinnedBySession =
                sessionPinCounts.get(sessionID);

            if (pinnedBySession == null) {
                // Nothing to release!  Nice -- the session is very clean.
                return;
            }

            // Duplicate the collection's values so that we don't get
            // concurrent modification exceptions.

            ArrayList<SessionPinCount> spcs =
                new ArrayList<>(pinnedBySession.values());

            int totalPins = 0;
            StringBuilder buf = new StringBuilder();

            for (SessionPinCount spc : spcs) {
                totalPins += spc.pinCount;

                buf.append(String.format("Session %d pinned %s %d times" +
                    " without a corresponding unpin call", sessionID,
                    spc.dbPage, spc.pinCount));

                while (spc.pinCount > 0)
                    spc.dbPage.unpin();
            }

            // TODO:  Make this cutoff a config property
            if (totalPins >= 20) {
                logger.warn("Session %d didn't clean up %d pins:\n%s",
                    totalPins, buf.toString());
            }

            // Since unpinning the pages calls back into the buffer manager,
            // we should automatically have all our "sessionPinCounts" state
            // cleaned up along the way.
        }
    }


    public void recordPageInvalidated(DBPage dbPage) {
        if (dbPage == null)
            throw new IllegalArgumentException("dbPage cannot be null");

        int pageNo = dbPage.getPageNo();
        DBPageID pageID = new DBPageID(dbPage);
        if (dbPage.getPinCount() > 0) {
            logger.warn(String.format("DBPage %d is being invalidated, but " +
                "it has a pin-count of %d", pageNo, dbPage.getPinCount()));
        }

        synchronized (guard) {
            for (int sessionID : sessionPinCounts.keySet()) {
                HashMap<DBPageID, SessionPinCount> pinnedBySession =
                    sessionPinCounts.get(sessionID);

                SessionPinCount spc = pinnedBySession.remove(pageID);
                if (spc != null) {
                    logger.warn(String.format("DBPage %d is being invalidated, " +
                        "but session %d has pinned it %d times", pageNo, sessionID,
                        spc.pinCount));
                }
            }
        }
    }


    /**
     * Retrieves the specified {@code DBPage} from the Buffer Manager if it's
     * currently buffered, or {@code null} if the page is not currently
     * buffered.  If a page is returned, it is pinned before it is returned.
     *
     * @param dbFile the file containing the page to retrieve
     * @param pageNo the page number in the {@code DBFile} to retrieve
     * @return the requested {@code DBPage}, or {@code null} if not found
     */
    public DBPage getPage(DBFile dbFile, int pageNo, boolean create) {
        DBPage dbPage;
        synchronized (guard) {
            dbPage = cachedPages.get(new CachedPageInfo(dbFile, pageNo));

            if (dbPage != null) {
                // Page is already in cache.  Pin it, then we're done.
                dbPage.pin();
            }
            else {
                // Need to load the page from disk!

                dbPage = new DBPage(this, dbFile, pageNo);

                // File Manager returns true if the page was actually loaded,
                // or false if the page doesn't exist in the file.
                boolean loaded = fileManager.loadPage(dbFile, pageNo,
                    dbPage.getPageData(), create);

                if (loaded) {
                    CachedPageInfo cpi = new CachedPageInfo(dbFile, pageNo);
                    if (cachedPages.containsKey(cpi)) {
                        throw new IllegalStateException(String.format(
                            "Page cache already contains page [%s,%d]", dbFile, pageNo));
                    }

                    logger.debug(String.format("Adding page [%s,%d] to page-cache.",
                        dbFile, pageNo));

                    // Make sure this page is pinned by the session so that we
                    // don't flush it until the session is done with it.  We
                    // do that before adding it to the cached-pages collection,
                    // so that another thread can't reclaim the page out from
                    // under us.
                    dbPage.pin();
                    cachedPages.put(cpi, dbPage);
                }
                else {
                    // Make sure to release the page's buffer, and return
                    // null to indicate that no page was loaded.
                    dbPage.invalidate();
                    dbPage = null;
                }
            }
        }

        return dbPage;
    }


    /**
     * <p>
     * This helper function ensures that the buffer manager has the specified
     * amount of space available.  This is done by looking for pages that may
     * be evicted from the cache, until the requested amount of space is
     * available.  Of course, if the requested amount of space is already
     * available, this function returns immediately.
     * </p>
     * <p>
     * Pinned pages are not considered to be eligible for eviction.
     * </p>
     *
     * @param bytesRequired the amount of space that should be made available
     *        in the cache, in bytes
     */
    private void ensureSpaceAvailable(int bytesRequired) {
        synchronized (guard) {
            // If we already have enough space, return without doing anything.
            if (bytesRequired + totalBytesCached <= maxCacheSize)
                return;

            // We don't currently have enough space in the cache.  Try to
            // solve this problem by evicting pages.  We collect together the
            // pages to evict, so that we can update the write-ahead log
            // before flushing the pages.

            ArrayList<DBPage> dirtyPages = new ArrayList<>();

            if (!cachedPages.isEmpty()) {
                // The cache will be too large after adding this page.

                Iterator<Map.Entry<CachedPageInfo, DBPage>> entries =
                    cachedPages.entrySet().iterator();

                while (entries.hasNext() &&
                    bytesRequired + totalBytesCached > maxCacheSize) {
                    Map.Entry<CachedPageInfo, DBPage> entry = entries.next();

                    DBPage oldPage = entry.getValue();

                    if (oldPage.isPinned())  // Can't flush pages that are in use.
                        continue;

                    logger.debug(String.format(
                        "    Evicting page [%s,%d] from page-cache to make room.",
                        oldPage.getDBFile(), oldPage.getPageNo()));

                    entries.remove();

                    // If the page is dirty, we need to write its data to disk before
                    // invalidating it.  Otherwise, just invalidate it.
                    if (oldPage.isDirty()) {
                        logger.debug("    Evicted page is dirty; must save to disk.");
                        dirtyPages.add(oldPage);
                    }
                    else {
                        oldPage.invalidate();
                    }
                }
            }

            // If we have any dirty data pages, they need to be flushed to disk.
            writeDirtyPages(dirtyPages, /* invalidate */ true);

            if (bytesRequired + totalBytesCached > maxCacheSize)
                logger.warn("Buffer manager is currently using too much space.");
        }
    }


    /**
     * This helper method writes out a list of dirty pages from the buffer
     * manager, ensuring that if transactions are enabled, the
     * write-ahead-logging rule is satisfied.
     *
     * @param dirtyPages the list of dirty pages to write
     * @param invalidate if true then the dirty pages are invalidated so they
     *        must be reloaded from disk
     */
    private void writeDirtyPages(List<DBPage> dirtyPages, boolean invalidate) {
        if (!dirtyPages.isEmpty()) {
            // Pass the observers a read-only version of the pages so they
            // can't change things.
            List<DBPage> readOnlyPages =
                Collections.unmodifiableList(dirtyPages);

            for (BufferManagerObserver obs : observers)
                obs.beforeWriteDirtyPages(readOnlyPages);

            // Finally, we can write out each dirty page.
            for (DBPage dbPage : dirtyPages) {
                fileManager.savePage(dbPage.getDBFile(), dbPage.getPageNo(),
                                     dbPage.getPageData());

                dbPage.setDirty(false);

                if (invalidate)
                    dbPage.invalidate();
            }
        }
    }


    /**
     * This method writes all dirty pages in the specified file, optionally
     * syncing the file after performing the write.  The pages are not removed
     * from the buffer manager after writing them; their dirty state is simply
     * cleared.
     *
     * @param dbFile the file whose dirty pages should be written to disk
     *
     * @param minPageNo dirty pages with a page-number less than this value
     *        will not be written to disk
     *
     * @param maxPageNo dirty pages with a page-number greater than this value
     *        will not be written to disk
     *
     * @param sync If true then the database file will be sync'd to disk;
     *        if false then no sync will occur.  The sync will always occur,
     *        in case dirty pages had previously been flushed to disk without
     *        syncing.
     */
    public void writeDBFile(DBFile dbFile, int minPageNo, int maxPageNo,
                            boolean sync) {
        logger.info(String.format("Writing all dirty pages for file %s to disk%s.",
            dbFile, (sync ? " (with sync)" : "")));

        synchronized (guard) {
            Iterator<Map.Entry<CachedPageInfo, DBPage>> entries =
                cachedPages.entrySet().iterator();

            ArrayList<DBPage> dirtyPages = new ArrayList<>();

            while (entries.hasNext()) {
                Map.Entry<CachedPageInfo, DBPage> entry = entries.next();

                CachedPageInfo info = entry.getKey();
                if (dbFile.equals(info.dbFile)) {
                    DBPage oldPage = entry.getValue();
                    if (!oldPage.isDirty())
                        continue;

                    int pageNo = oldPage.getPageNo();
                    if (pageNo < minPageNo || pageNo > maxPageNo)
                        continue;

                    logger.debug(String.format("    Saving page [%s,%d] to disk.",
                        oldPage.getDBFile(), oldPage.getPageNo()));

                    dirtyPages.add(oldPage);
                }
            }

            writeDirtyPages(dirtyPages, /* invalidate */ false);

            if (sync) {
                logger.debug("Syncing file " + dbFile);
                fileManager.syncDBFile(dbFile);
            }
        }
    }


    /**
     * This method writes all dirty pages in the specified file, optionally
     * syncing the file after performing the write.  The pages are not removed
     * from the buffer manager after writing them; their dirty state is simply
     * cleared.
     *
     * @param dbFile the file whose dirty pages should be written to disk
     *
     * @param sync If true then the database file will be sync'd to disk;
     *        if false then no sync will occur.  The sync will always occur,
     *        in case dirty pages had previously been flushed to disk without
     *        syncing.
     */
    public void writeDBFile(DBFile dbFile, boolean sync) {
        writeDBFile(dbFile, 0, Integer.MAX_VALUE, sync);
    }


    /**
     * This method writes all dirty pages in the buffer manager to disk.  The
     * pages are not removed from the buffer manager after writing them; their
     * dirty state is simply cleared.
     *
     * @param sync if true, this method will sync all files in which dirty pages
     *        were found, with the exception of WAL files and the
     *        transaction-state file.  If false, no file syncing will be
     *        performed.
     */
    public void writeAll(boolean sync) {
        logger.info("Writing ALL dirty pages in the Buffer Manager to disk.");

        synchronized (guard) {
            Iterator<Map.Entry<CachedPageInfo, DBPage>> entries =
                cachedPages.entrySet().iterator();

            ArrayList<DBPage> dirtyPages = new ArrayList<>();
            HashSet<DBFile> dirtyFiles = new HashSet<>();

            while (entries.hasNext()) {
                Map.Entry<CachedPageInfo, DBPage> entry = entries.next();

                DBPage oldPage = entry.getValue();
                if (!oldPage.isDirty())
                    continue;

                DBFile dbFile = oldPage.getDBFile();
                DBFileType type = dbFile.getType();
                if (type != DBFileType.WRITE_AHEAD_LOG_FILE &&
                    type != DBFileType.TXNSTATE_FILE) {
                    dirtyFiles.add(oldPage.getDBFile());
                }

                logger.debug(String.format("    Saving page [%s,%d] to disk.",
                    dbFile, oldPage.getPageNo()));

                dirtyPages.add(oldPage);
            }

            writeDirtyPages(dirtyPages, /* invalidate */ false);

            if (sync) {
                logger.debug("Synchronizing all files containing dirty pages to disk.");
                for (DBFile dbFile : dirtyFiles)
                    fileManager.syncDBFile(dbFile);
            }
        }
    }

    /**
     * This method removes all cached pages in the specified file from the
     * buffer manager, writing out any dirty pages in the process.  This method
     * is not generally recommended to be used, as it basically defeats the
     * purpose of the buffer manager in the first place; rather, the
     * {@link #writeDBFile} method should be used instead.  There is a specific
     * situation in which it is used, when a file is being removed from the
     * Buffer Manager by the Storage Manager.
     *
     * @param dbFile the file whose pages should be flushed from the cache
     */
    public void flushDBFile(DBFile dbFile) {
        logger.info("Flushing all pages for file " + dbFile +
            " from the Buffer Manager.");

        synchronized (guard) {
            Iterator<Map.Entry<CachedPageInfo, DBPage>> entries =
                cachedPages.entrySet().iterator();

            ArrayList<DBPage> dirtyPages = new ArrayList<>();

            while (entries.hasNext()) {
                Map.Entry<CachedPageInfo, DBPage> entry = entries.next();

                CachedPageInfo info = entry.getKey();
                if (dbFile.equals(info.dbFile)) {
                    DBPage oldPage = entry.getValue();

                    logger.debug(String.format(
                        "    Evicting page [%s,%d] from page-cache.",
                        oldPage.getDBFile(), oldPage.getPageNo()));

                    // Remove the page from the cache.
                    entries.remove();

                    // NOTE:  We don't modify totalBytesCached here, because
                    // that value is updated when the DBPage's page-buffers
                    // are returned back to the Buffer Manager.

                    // If the page is dirty, we need to write its data to disk before
                    // invalidating it.  Otherwise, just invalidate it.
                    if (oldPage.isDirty()) {
                        logger.debug("    Evicted page is dirty; must save to disk.");
                        dirtyPages.add(oldPage);
                    }
                    else {
                        oldPage.invalidate();
                    }
                }
            }

            writeDirtyPages(dirtyPages, /* invalidate */ true);
        }
    }


    /**
     * This method removes all cached pages from the buffer manager, writing
     * out any dirty pages in the process.  This method is not generally
     * recommended to be used, as it basically defeats the purpose of the
     * buffer manager in the first place; rather, the {@link #writeAll} method
     * should be used instead.  However, this method is useful to cause certain
     * performance issues to manifest with individual commands, and the Storage
     * Manager also uses it during shutdown processing to ensure all data is
     * saved to disk.
     */
    public void flushAll() {
        logger.info("Flushing ALL database pages from the Buffer Manager.");

        synchronized (guard) {
            Iterator<Map.Entry<CachedPageInfo, DBPage>> entries =
                cachedPages.entrySet().iterator();

            ArrayList<DBPage> dirtyPages = new ArrayList<>();

            while (entries.hasNext()) {
                Map.Entry<CachedPageInfo, DBPage> entry = entries.next();

                DBPage oldPage = entry.getValue();

                logger.debug(String.format(
                    "    Evicting page [%s,%d] from page-cache.",
                    oldPage.getDBFile(), oldPage.getPageNo()));

                // Remove the page from the cache.
                entries.remove();

                // NOTE:  We don't modify totalBytesCached here, because that
                // value is updated when the DBPage's page-buffers are
                // returned back to the Buffer Manager.

                // If the page is dirty, we need to write its data to disk before
                // invalidating it.  Otherwise, just invalidate it.
                if (oldPage.isDirty()) {
                    logger.debug("    Evicted page is dirty; must save to disk.");
                    dirtyPages.add(oldPage);
                }
                else {
                    oldPage.invalidate();
                }
            }

            writeDirtyPages(dirtyPages, /* invalidate */ true);
        }
    }


    /**
     * This method removes a file from the cache, first flushing all pages from
     * the file out of the cache.  This operation is used by the Storage Manager
     * to close a data file.
     *
     * @param dbFile the file to remove from the cache.
     */
    public void removeDBFile(DBFile dbFile) {
        logger.info("Removing DBFile " + dbFile + " from buffer manager");

        synchronized (guard) {
            flushDBFile(dbFile);
            cachedFiles.remove(dbFile.getDataFile().getName());
        }
    }


    /**
     * This method removes ALL files from the cache, first flushing all pages
     * from the cache so that any dirty pages will be saved to disk (possibly
     * updating the write-ahead log in the process).  This operation is used by
     * the Storage Manager during shutdown.
     *
     * @return a list of the files that were in the cache, so that they can be
     *         used by the caller if necessary (e.g. to sync and close each one)
     */
    public List<DBFile> removeAll() {
        logger.info("Removing ALL DBFiles from buffer manager");

        synchronized (guard) {
            // Flush all pages, ensuring that dirty pages will be written too.
            flushAll();

            // Get the list of DBFiles we had in the cache, then clear the cache.
            ArrayList<DBFile> dbFiles = new ArrayList<>(cachedFiles.values());
            cachedFiles.clear();
            return dbFiles;
        }
    }
}
