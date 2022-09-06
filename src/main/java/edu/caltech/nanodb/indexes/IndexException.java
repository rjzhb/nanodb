package edu.caltech.nanodb.indexes;


import edu.caltech.nanodb.storage.StorageException;

/**
 * This class represents errors that occur while manipulating indexes.
 */
public class IndexException extends StorageException {

    /** Construct an index exception with no message. */
    public IndexException() {
        super();
    }

    /**
     * Construct an index exception with the specified message.
     */
    public IndexException(String msg) {
        super(msg);
    }


    /**
     * Construct an index exception with the specified cause and no message.
     */
    public IndexException(Throwable cause) {
        super(cause);
    }


    /**
     * Construct an index exception with the specified message and cause.
     */
    public IndexException(String msg, Throwable cause) {
        super(msg, cause);
    }
}

