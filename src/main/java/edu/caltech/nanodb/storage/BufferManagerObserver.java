package edu.caltech.nanodb.storage;


import java.util.List;


/**
 * This interface allows other classes to respond to operations performed by
 * the {@link BufferManager}.  For example, the
 * {@link edu.caltech.nanodb.transactions.TransactionManager} observes the
 * {@code BufferManager} to update the write-ahead log files appropriately,
 * based on what pages are being written out to disk.
 */
public interface BufferManagerObserver {
    /**
     * This method is called before the buffer manager writes the specified
     * collection of pages.
     *
     * @param pages a collection of <tt>DBPage</tt> objects that are about to
     *        be written back to disk
     */
    void beforeWriteDirtyPages(List<DBPage> pages);
}
