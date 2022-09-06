package edu.caltech.nanodb.server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.PrintStream;


/**
 * This class is a simple output-stream that can be used by a
 * {@link PrintStream} to buffer up printed output.  When this stream is
 * flushed, it causes the accumulated contents to be sent across an
 * {@link ObjectOutputStream}.  This allows the underlying
 * {@code ObjectOutputStream} to be used both for sending data objects, and
 * strings of printed output.
 */
public class ForwardingOutputStream extends ByteArrayOutputStream {

    /** The underlying output stream over which objects are serialized. */
    private ObjectOutputStream objectOutput;

    /**
     * Construct a forwarding output-stream that will send objects over the
     * specified object output-stream when flushed.
     */
    public ForwardingOutputStream(ObjectOutputStream objectOutput) {
        this.objectOutput = objectOutput;
    }


    /**
     * When called, this output stream will convert its accumulated data into
     * a {@code String} object, which will then be sent over the underlying
     * object output-stream.  Once this is completed, the buffered string data
     * is cleared.
     *
     * @throws IOException if an error occurs while sending the string data
     */
    public void flush() throws IOException {
        String contents = toString();
        objectOutput.writeObject(contents);
        objectOutput.flush();
        reset();
    }
}
