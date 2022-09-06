package edu.caltech.nanodb.storage;


/**
 * This class represents runtime issues with data files not being in the
 * proper format, or other such issues occurring.
 */
public class DataFormatException extends StorageException {

    /** Construct a data format exception with no message. */
    public DataFormatException() {
        super();
    }

    /**
     * Construct a data format exception with the specified message.
     */
    public DataFormatException(String msg) {
        super(msg);
    }


    /**
     * Construct a data format exception with the specified cause and no message.
     */
    public DataFormatException(Throwable cause) {
        super(cause);
    }


    /**
     * Construct a data format exception with the specified message and cause.
     */
    public DataFormatException(String msg, Throwable cause) {
        super(msg, cause);
    }
}

