package edu.caltech.nanodb.storage;


/**
 * This class represents errors that occur while opening, closing and
 * manipulating files.
 */
public class FileSystemException extends StorageException {
    /** Construct a storage exception with no message. */
    public FileSystemException() {
        super();
    }

    /**
     * Construct a file exception with the specified message.
     */
    public FileSystemException(String msg) {
        super(msg);
    }


    /**
     * Construct a file exception with the specified cause and no message.
     */
    public FileSystemException(Throwable cause) {
        super(cause);
    }


    /**
     * Construct a file exception with the specified message and cause.
     */
    public FileSystemException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
