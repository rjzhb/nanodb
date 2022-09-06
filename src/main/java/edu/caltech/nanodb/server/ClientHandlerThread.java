package edu.caltech.nanodb.server;


import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;

import java.net.Socket;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import org.antlr.v4.runtime.RecognitionException;

import edu.caltech.nanodb.commands.Command;
import edu.caltech.nanodb.commands.ExitCommand;
import edu.caltech.nanodb.commands.SelectCommand;


/**
 * This class handles a connection from a single client to the database
 * server.
 */
public class ClientHandlerThread extends Thread {
    /** A logging object for reporting anything interesting that happens. */
    private static Logger logger = LogManager.getLogger(ClientHandlerThread.class);

    /** A reference to the NanoDB server object. */
    private NanoDBServer server;

    /** The unique ID assigned to this client. */
    private int id;

    /** The socket this client-handler uses to interact with its client. */
    private Socket sock;

    /** An input-stream for de-serializing objects sent from the client. */
    private ObjectInputStream objectInput;

    /** An output-stream for serializing objects to send to the client. */
    private ObjectOutputStream objectOutput;


    private ForwardingOutputStream commandOutput;

    /** A print-stream that will send printed output to the client. */
    private PrintStream printOutput;

    /** A tuple-processor for sending tuples to the client. */
    private TupleSender tupleSender;


    /**
     * Initialize a new client handler with the specified ID and socket.
     *
     * @param id The unique ID assigned to this client.
     * @param sock The socket used to communicate with the client.
     */
    public ClientHandlerThread(NanoDBServer server, int id, Socket sock) {
        if (server == null)
            throw new IllegalArgumentException("server cannot be null");

        if (sock == null)
            throw new IllegalArgumentException("sock cannot be null");

        this.server = server;
        this.id = id;
        this.sock = sock;
    }


    /**
     * This is the main loop that handles the commands from the client.
     */
    @Override
    public void run() {
        try {
            // Communicate over the socket using Java serialization, since
            // it's much easier than implementing our own write protocol.
            objectOutput = new ObjectOutputStream(sock.getOutputStream());
            objectInput = new ObjectInputStream(sock.getInputStream());

            // This tuple-processor is used to send tuples back to the client
            // over the ObjectOutputStream that uses the socket.
            tupleSender = new TupleSender(objectOutput);

            // Also, set up a PrintStream that queues up text written by
            // the currently executing command, and when the PrintStream
            // is flushed, forward the string text over the same
            // ObjectOutputStream that wraps the socket.
            commandOutput = new ForwardingOutputStream(objectOutput);
            printOutput = new PrintStream(commandOutput);
            SessionState.get().setOutputStream(printOutput);

            while (true) {
                // Receive a command from the client and execute it.

                CommandResult result = null;
                commandOutput.reset();  // (just in case)

                try {
                    String commandString = (String) objectInput.readObject();
                    Command command = server.parseCommand(commandString);

                    if (command instanceof SelectCommand) {
                        SelectCommand select = (SelectCommand) command;
                        select.setTupleProcessor(tupleSender);
                    }


                    // Try to execute the command, and send the response back to the
                    // client.
                    result = server.doCommand(command, true);

                    if (result.isExit()) {
                        logger.info(String.format("Client %d is exiting.", id));
                        break;
                    }
                }
                catch (EOFException e) {
                    logger.info(String.format("Client %d disconnected.%n", id));
                    break;
                }
                catch (RecognitionException e) {
                    System.out.println("Parser error:  " + e.getMessage());
                    logger.error("Parser error", e);

                    // Send error back to the client.
                    objectOutput.writeObject(e);
                    continue;
                }
                catch (Exception e) {
                    // This could be an IOException, a ClassNotFoundException,
                    // or a ClassCastException.
                    logger.error(String.format("Error communicating with " +
                        "client %d!  Disconnecting.%n", id), e);
                    break;
                }

                objectOutput.writeObject(commandOutput.toString());
                objectOutput.writeObject(CommandState.COMMAND_COMPLETED);
                commandOutput.reset();
            }
        }
        catch (IOException e) {
            logger.error(String.format(
                "Communication error interacting with client %d!%n", id), e);
        }
    }


    private void doCommand(Command cmd) throws IOException {
        if (cmd == null)
            throw new IllegalArgumentException("cmd cannot be null");

        if (cmd instanceof ExitCommand) {
            throw new IllegalArgumentException(
                "ExitCommands should be handled outside of this function");
        }

        logger.debug("Command to execute:  " + cmd);

        if (cmd instanceof SelectCommand) {
            // Set up the SELECT command to send the tuples back to the
            // client.
            SelectCommand selCmd = (SelectCommand) cmd;
            selCmd.setTupleProcessor(tupleSender);
        }

        try {
            cmd.execute(server);
        }
        catch (Exception e) {
            logger.error("Encountered error during command execution", e);
            objectOutput.writeObject(e);
        }

        objectOutput.flush();
    }


    public void shutdownClient() {
        // Interrupt the client-handler thread.
        interrupt();

        // TODO:  Wait for the client to completely shut down.
    }
}
