package edu.caltech.nanodb.expressions;

/**
 * This exception is thrown when an expression would cause a divide by zero.
 */
public class DivideByZeroException extends ExpressionException {
    /** Construct a divide-by-zero exception with no message. */
    public DivideByZeroException() {
        super();
    }


    /** Construct a divide-by-zero exception with the specified message. */
    public DivideByZeroException(String msg) {
        super(msg);
    }


    /**
     * Construct a divide-by-zero exception with the specified cause and no
     * message.
     */
    public DivideByZeroException(Throwable cause) {
        super(cause);
    }


    /**
     * Construct a divide-by-zero exception with the specified message and
     * cause.
     */
    public DivideByZeroException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
