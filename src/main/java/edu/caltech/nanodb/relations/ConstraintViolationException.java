package edu.caltech.nanodb.relations;


import edu.caltech.nanodb.server.NanoDBException;


/**
 * This exception is used to signal when a database constraint is violated.
 */
public class ConstraintViolationException extends NanoDBException {
    public ConstraintViolationException() {
        super();
    }

    public ConstraintViolationException(String msg) {
        super(msg);
    }

    public ConstraintViolationException(Throwable cause) {
        super(cause);
    }

    public ConstraintViolationException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
