package edu.caltech.nanodb.server;


/**
 * This is the base-class of all exceptions specific to NanoDB functionality.
 * Since NanoDB uses runtime exceptions to report most issues, it is helpful
 * to have one base-class that all other NanoDB-related exceptions derive
 * from, so that we can identify other runtime exceptions that are actually
 * errors.
 */
public class NanoDBException extends RuntimeException {
    public NanoDBException() {
        super();
    }


    public NanoDBException(String msg) {
        super(msg);
    }


    public NanoDBException(Throwable cause) {
        super(cause);
    }


    public NanoDBException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
