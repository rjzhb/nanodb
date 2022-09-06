package edu.caltech.nanodb.server.properties;


import edu.caltech.nanodb.server.NanoDBException;


/**
 * This exception is thrown when an attempt is made to write to a read-only
 * property.
 */
public class PropertyException extends NanoDBException {

    public PropertyException() {
        super();
    }


    public PropertyException(String msg) {
        super(msg);
    }


    public PropertyException(Throwable cause) {
        super(cause);
    }


    public PropertyException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
