package edu.caltech.nanodb.client;


import java.io.IOException;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import edu.caltech.nanodb.commands.Command;
import edu.caltech.nanodb.server.CommandResult;
import edu.caltech.nanodb.server.NanoDBException;
import edu.caltech.nanodb.server.NanoDBServer;
import org.antlr.v4.runtime.misc.ParseCancellationException;


/**
 * <p>
 * This class is used for starting the NanoDB database in "exclusive mode,"
 * where only a single client interacts directly with the database system.
 * In fact, the exclusive client embeds the {@link NanoDBServer} instance in
 * the client object, since there is no need to interact with the server over
 * a network socket.
 * </p>
 * <p>
 * <b>Note that it is <u>wrong</u> to start multiple exclusive clients against
 * the same data directory!!!</b>  Exclusive-mode operation expects that only
 * this server is interacting with the data files.  For concurrent access from
 * multiple clients, see the {@link SharedServerClient} class.
 * </p>
 */
public class ExclusiveClient extends InteractiveClient {
    private static Logger logger = LogManager.getLogger(ExclusiveClient.class);


    /** The server that this exclusive client is using. */
    private NanoDBServer server;


    @Override
    public void startup() {
        // Start up the various database subsystems that require initialization.
        server = new NanoDBServer();
        try {
            server.startup();
        }
        catch (NanoDBException e) {
            System.out.println("DATABASE STARTUP FAILED:");
            e.printStackTrace(System.out);
            System.exit(1);
        }
    }


    @Override
    public CommandResult handleCommand(String command) {
        return server.doCommand(command, false);
    }


    @Override
    public void shutdown() {
        // Shut down the various database subsystems that require cleanup.
        if (!server.shutdown())
            System.out.println("DATABASE SHUTDOWN FAILED.");
    }


    public static void main(String args[]) {
        ExclusiveClient client = new ExclusiveClient();

        client.startup();
        client.mainloop();
        client.shutdown();
    }
}

