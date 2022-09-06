package edu.caltech.nanodb.queryeval;


import edu.caltech.nanodb.server.NanoDBException;


/**
 * This exception is used to signal when a SQL query contains a semantic error
 * that prevents its evaluation.  For example, an expression like
 * "<tt>MAX(AVG(a))</tt>" is invalid and cannot be evaluated.
 */
public class InvalidSQLException extends NanoDBException {
    public InvalidSQLException() {
        super();
    }

    public InvalidSQLException(String msg) {
        super(msg);
    }

    public InvalidSQLException(Throwable cause) {
        super(cause);
    }

    public InvalidSQLException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
