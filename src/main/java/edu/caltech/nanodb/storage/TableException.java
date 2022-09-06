package edu.caltech.nanodb.storage;


/**
 * This class represents errors that occur while manipulating tables.
 */
public class TableException extends StorageException {

    /** Construct a table exception with no message. */
    public TableException() {
        super();
    }

    /**
     * Construct a table exception with the specified message.
     */
    public TableException(String msg) {
        super(msg);
    }


    /**
     * Construct a table exception with the specified cause and no message.
     */
    public TableException(Throwable cause) {
        super(cause);
    }


    /**
     * Construct a table exception with the specified message and cause.
     */
    public TableException(String msg, Throwable cause) {
        super(msg, cause);
    }
}

