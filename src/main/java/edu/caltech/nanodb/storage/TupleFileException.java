package edu.caltech.nanodb.storage;


/**
 * This class represents errors that occur while manipulating tuple files.
 */
public class TupleFileException extends StorageException {

    /** Construct a tuple-file exception with no message. */
    public TupleFileException() {
        super();
    }

    /**
     * Construct a tuple-file exception with the specified message.
     */
    public TupleFileException(String msg) {
        super(msg);
    }


    /**
     * Construct a tuple-file exception with the specified cause and no message.
     */
    public TupleFileException(Throwable cause) {
        super(cause);
    }


    /**
     * Construct a tuple-file exception with the specified message and cause.
     */
    public TupleFileException(String msg, Throwable cause) {
        super(msg, cause);
    }
}

