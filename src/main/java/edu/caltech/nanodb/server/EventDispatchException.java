package edu.caltech.nanodb.server;


/**
 */
public class EventDispatchException extends NanoDBException {
    public EventDispatchException() {
        super();
    }


    public EventDispatchException(String message) {
        super(message);
    }


    public EventDispatchException(Throwable cause) {
        super(cause);
    }


    public EventDispatchException(String message, Throwable cause) {
        super(message, cause);
    }
}
