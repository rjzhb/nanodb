package edu.caltech.nanodb.commands;


import edu.caltech.nanodb.server.NanoDBException;


/**
 * This exception is thrown when a fatal error occurs during command
 * execution.
 */
public class ExecutionException extends NanoDBException {

    public ExecutionException() {
        super();
    }


    public ExecutionException(String msg) {
        super(msg);
    }


    public ExecutionException(Throwable cause) {
        super(cause);
    }


    public ExecutionException(String msg, Throwable cause) {
        super(msg, cause);
    }
}


