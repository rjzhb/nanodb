package edu.caltech.nanodb.client;


import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.net.SocketException;
import java.nio.channels.ClosedByInterruptException;
import java.util.concurrent.Semaphore;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import edu.caltech.nanodb.commands.Command;
import edu.caltech.nanodb.queryeval.PrettyTuplePrinter;
import edu.caltech.nanodb.relations.Schema;
import edu.caltech.nanodb.relations.Tuple;
import edu.caltech.nanodb.server.CommandResult;
import edu.caltech.nanodb.server.CommandState;
import edu.caltech.nanodb.server.SharedServer;


/**
 * This class implements a client the can connect to the NanoDB
 * {@link edu.caltech.nanodb.server.SharedServer shared server} and
 * send/receive commands and data.
 */
public class SharedServerClient extends InteractiveClient {
    private static Logger logger = LogManager.getLogger(SharedServerClient.class);


    /** The socket used to communicate with the shared server. */
    private Socket socket;


    /**
     * This stream is used to receive objects (tuples, messages, etc.) from
     * the server. */
    private ObjectInputStream objectInput;


    /**
     * This stream is used to send objects (commands, specifically) to the
     * server.
     */
    private ObjectOutputStream objectOutput;


    /**
     * This object receives data from the server asynchronously,
     * and prints out whatever it receives.  It is wrapped by the
     * {@link #receiverThread}.
     */
    private Receiver receiver;


    /**
     * This is the thread that the {@link #receiver} object runs within.
     */
    private Thread receiverThread;


    /**
     * This semaphore is used to coordinate when a command has been sent to
     * the server, and when the server is finished sending results back to
     * the client, so that another command cannot be sent until the current
     * one is finished.
     */
    private Semaphore semCommandDone;


    /**
     * This helper class prints out the results that come back from the
     * server.  It is intended to run within a separate thread.
     */
    private class Receiver implements Runnable {
        /** The print-stream to output server results on. */
        private PrintStream out;


        /** A flag indicating when the receiver thread should shut down. */
        private boolean done;


        public Receiver(PrintStream out) {
            this.out = out;
        }


        public void run() {
            PrettyTuplePrinter tuplePrinter = null;

            done = false;
            while (true) {
                try {
                    Object obj = objectInput.readObject();
                    if (obj instanceof String) {
                        // Just print strings to the console
                        System.out.print(obj);
                    }
                    else if (obj instanceof Schema) {
                        tuplePrinter = new PrettyTuplePrinter(out);
                        tuplePrinter.setSchema((Schema) obj);
                    }
                    else if (obj instanceof Tuple) {
                        tuplePrinter.process((Tuple) obj);
                    }
                    else if (obj instanceof Throwable) {
                        Throwable t = (Throwable) obj;
                        t.printStackTrace(System.out);
                    }
                    else if (obj instanceof CommandResult) {
                        CommandResult result = (CommandResult) obj;
                        if (result.isExit())
                            done = true;
                    }
                    else if (obj instanceof CommandState) {
                        CommandState state = (CommandState) obj;
                        if (state == CommandState.COMMAND_COMPLETED) {
                            if (tuplePrinter != null) {
                                tuplePrinter.finish();
                                tuplePrinter = null;
                            }

                            // Signal that the command is completed.
                            semCommandDone.release();
                        }
                    }
                    else {
                        // TODO:  Try to print whatever came across the wire.
                        System.out.println(obj);
                    }
                }
                catch (EOFException e) {
                    System.out.println("Connection was closed by the server.");
                    break;
                }
                catch (SocketException e) {
                    System.out.println("Socket communication error.");
                    break;
                }
                catch (ClosedByInterruptException e) {
                    System.out.println("Thread was interrupted during an IO operation.");
                    break;
                }
                catch (Exception e) {
                    System.out.println("Exception occurred:");
                    e.printStackTrace(System.out);
                }
            }
        }


        public void shutdown() {
            // TODO:  Probably need to interrupt the thread.  This is pretty
            //        insufficient, particularly for long-running queries.
            done = true;
        }
    }



    public SharedServerClient(String hostname, int port) throws IOException {
        // Try to establish a connection to the shared database server.
        socket = new Socket(hostname, port);
        objectOutput = new ObjectOutputStream(socket.getOutputStream());
        objectInput = new ObjectInputStream(socket.getInputStream());

        semCommandDone = new Semaphore(0);
    }


    public void startup() {
        // Start up the receiver thread that will print out whatever comes
        // across the wire.
        receiver = new Receiver(System.out);
        receiverThread = new Thread(receiver);
        receiverThread.start();
    }


    public CommandResult handleCommand(String command) {
        try {
            objectOutput.writeObject(command);
        }
        catch (IOException e) {
            throw new RuntimeException(
                "Unexpected error while transmitting command", e);
        }

        // Wait for the command to be completed.
        try {
            semCommandDone.acquire();
        }
        catch (InterruptedException e) {
            throw new RuntimeException(
                "Interrupted while waiting for command to finish", e);
        }

        return null;
    }


    public void shutdown() throws IOException {
        receiver.shutdown();
        receiverThread.interrupt();

        objectInput.close();
        objectOutput.close();
        socket.close();
    }
}
