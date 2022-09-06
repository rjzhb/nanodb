package edu.caltech.nanodb.storage.btreefile;

import edu.caltech.nanodb.storage.TupleFileException;


/**
 * This class represents errors that occur while manipulating B<sup>+</sup>
 * tree tuple files.
 */
public class BTreeTupleFileException extends TupleFileException {

    /** Construct a tuple-file exception with no message. */
    public BTreeTupleFileException() {
        super();
    }

    /**
     * Construct a tuple-file exception with the specified message.
     */
    public BTreeTupleFileException(String msg) {
        super(msg);
    }


    /**
     * Construct a tuple-file exception with the specified cause and no message.
     */
    public BTreeTupleFileException(Throwable cause) {
        super(cause);
    }


    /**
     * Construct a tuple-file exception with the specified message and cause.
     */
    public BTreeTupleFileException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
