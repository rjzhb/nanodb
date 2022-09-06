package edu.caltech.nanodb.transactions;


import edu.caltech.nanodb.server.NanoDBException;

/**
 * This class represents errors that occur as part of transaction processing.
 */
public class TransactionException extends NanoDBException {
    public TransactionException() {
        super();
    }


    public TransactionException(String msg) {
        super(msg);
    }


    public TransactionException(Throwable cause) {
        super(cause);
    }


    public TransactionException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
