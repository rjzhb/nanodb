package edu.caltech.nanodb.client;


import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import edu.caltech.nanodb.server.CommandResult;
import org.antlr.v4.runtime.misc.ParseCancellationException;


/**
 * This abstract class implements the basic functionality necessary for
 * providing an interactive SQL client.
 */
public abstract class InteractiveClient {

    private static Logger logger = LogManager.getLogger(InteractiveClient.class);


    /** A string constant specifying the "first-line" command-prompt. */
    private static final String CMDPROMPT_FIRST = "CMD> ";


    /** A string constant specifying the "subsequent-lines" command-prompt. */
    private static final String CMDPROMPT_NEXT = "   > ";

    /** The buffer that accumulates each command's text. */
    private StringBuilder enteredText;

    /** A flag that records if we are exiting the interactive client. */
    private boolean exiting;


    /**
     * Start up the interactive client.  The specific way the client
     * interacts with the server dictates how this startup mechanism
     * will work.
     *
     * @throws Exception if any error occurs during startup
     */
    public abstract void startup() throws Exception;


    /**
     * This is the interactive mainloop that handles input from the standard
     * input stream of the program.
     */
    protected void mainloop() {
        // We don't use the console directly, since we can't read/write it
        // if someone redirects a file onto the client's input-stream.
        boolean hasConsole = (System.console() != null);

        if (hasConsole) {
            System.out.println(
                "Welcome to NanoDB.  Exit with EXIT or QUIT command.\n");
        }

        exiting = false;
        BufferedReader bufReader =
            new BufferedReader(new InputStreamReader(System.in));
        while (!exiting) {
            enteredText = new StringBuilder();
            boolean firstLine = true;

            while (true) {
                try {
                    if (hasConsole) {
                        if (firstLine) {
                            System.out.print(CMDPROMPT_FIRST);
                            System.out.flush();
                            firstLine = false;
                        }
                        else {
                            System.out.print(CMDPROMPT_NEXT);
                            System.out.flush();
                        }
                    }

                    String line = bufReader.readLine();
                    if (line == null) {
                        // Hit EOF.
                        exiting = true;
                        break;
                    }

                    enteredText.append(line).append('\n');

                    processEnteredText();
                    if (exiting)
                        break;

                    if (enteredText.length() == 0)
                        firstLine = true;
                }
                catch (Throwable e) {
                    System.out.println("Unexpected error:  " + e.getClass() +
                        ":  " + e.getMessage());
                    logger.error("Unexpected error", e);
                }
            }
        }
    }


    /**
     * This helper function processes the contents of the {@link #enteredText}
     * field, consuming comments, handling client "shell commands" and
     * regular commands that are handled by the
     * {@link edu.caltech.nanodb.server.NanoDBServer}.  Whatever command or
     * comment is processed will also be removed from the {@code enteredText}
     * buffer by this function.  Note also that multiple commands will be
     * processed, if present.
     */
    private void processEnteredText() {
        // Process any commands in the entered text.
        while (true) {
            // Consume leading whitespace
            while (enteredText.length() > 0 &&
                Character.isWhitespace(enteredText.charAt(0))) {
                enteredText.deleteCharAt(0);
            }

            // Consume comments
            if (enteredText.length() >= 2) {
                if ("--".equals(enteredText.substring(0, 2))) {
                    // Consume single-line comment
                    int endIdx = 2;

                    // Look for the end of the line.
                    while (endIdx < enteredText.length() &&
                           enteredText.charAt(endIdx) != '\n') {
                        endIdx++;
                    }

                    if (endIdx == enteredText.length()) {
                        // Didn't find newline character.  Can't consume this
                        // comment yet.
                        return;
                    }

                    endIdx++;  // Skip the newline character as well.
                    enteredText.delete(0, endIdx);

                    // Go back and try to find more commands.
                    continue;
                }
                else if ("/*".equals(enteredText.substring(0, 2))) {
                    // Consume block comment

                    int endIdx = 2;

                    // Look for the end of the block comment.
                    while (endIdx + 1 < enteredText.length() &&
                           (enteredText.charAt(endIdx) != '*' ||
                            enteredText.charAt(endIdx + 1) != '/')) {
                        endIdx++;
                    }

                    if (endIdx + 1 == enteredText.length()) {
                        // Didn't find end of block comment.  Can't consume
                        // this comment yet.
                        return;
                    }

                    endIdx += 2;  // Skip the end of the block-comment.
                    enteredText.delete(0, endIdx);

                    // Go back and try to find more commands.
                    continue;
                }
            }

            // Look for shell commands
            if (enteredText.length() > 0 && enteredText.charAt(0) == '\\') {
                // This is a shell command, which continues to the
                // end of the current line.
                int endIdx = 0;
                while (endIdx < enteredText.length() &&
                    enteredText.charAt(endIdx) != '\n') {
                    endIdx++;
                }
                endIdx++;  // Include the newline character too

                String shellCommand = enteredText.substring(0, endIdx);
                enteredText.delete(0, endIdx);

                handleShellCommand(shellCommand);
                continue;
            }

            String command = getCommandString();
            if (command == null)
                break;  // Couldn't find a complete command.

            // if (logger.isDebugEnabled())
            //     logger.debug("Command string:\n" + command);

            CommandResult result = handleCommand(command);
            if (result.isExit()) {
                exiting = true;
                break;
            }
            else {
                outputCommandResult(result);
            }
        }
    }


    /**
     * This helper method goes through the {@link #enteredText} buffer, trying
     * to identify the extent of the next command string.  This is done using
     * semicolons (that are not enclosed with single or double quotes).  If a
     * command is identified, it is removed from the internal buffer and
     * returned.  If no complete command is identified, {@code null} is
     * returned.
     *
     * @return the first semicolon-terminated command in the internal data
     *         buffer, or {@code null} if the buffer contains no complete
     *         commands.
     */
    private String getCommandString() {
        int i = 0;
        String command = null;

        while (i < enteredText.length()) {
            char ch = enteredText.charAt(i);
            if (ch == ';') {
                // Found the end of the command.  Extract the string, and
                // make sure the semicolon is also included.
                command = enteredText.substring(0, i + 1);
                enteredText.delete(0, i + 1);

                // Consume any leading whitespace at the start of the entered
                // text.
                while (enteredText.length() > 0 &&
                       Character.isWhitespace(enteredText.charAt(0))) {
                    enteredText.deleteCharAt(0);
                }

                break;
            }
            else if (ch == '\'' || ch == '"') {
                // Need to ignore all subsequent characters until we find
                // the end of this quoted string.
                i++;
                while (i < enteredText.length() &&
                       enteredText.charAt(i) != ch) {
                    i++;
                }
            }

            i++;  // Go on to the next character.
        }

        return command;
    }


    /**
     * Subclasses can implement this method to handle each command entered
     * by the user.  For example, a subclass may send the command over a
     * socket to the server, wait for a response, then output the response
     * to the console.
     *
     * @param command the command to handle.
     *
     * @return the command-result from executing the command
     */
    public abstract CommandResult handleCommand(String command);


    /**
     * Handle "shell commands," which are commands that the client itself
     * handles on behalf of the user.  Shell commands start with a backslash
     * "\" character.
     *
     * @param shellCommand the command to handle.
     */
    private void handleShellCommand(String shellCommand) {
        // Split the shell command into parts
        String[] parts = shellCommand.split("\\s+", 2);
        parts[0] = parts[0].toLowerCase();

        if ("\\source".equals(parts[0])) {
            // Source the requested SQL file.

            StringBuilder oldText = enteredText;
            enteredText = new StringBuilder();
            String filename = parts[1].strip();

            // Open the file with a try-with-resources so it will always be
            // closed when we are done with the file.
            try (BufferedReader reader =
                     new BufferedReader(new FileReader(filename))) {

                while (true) {
                    String line = reader.readLine();
                    if (line == null)
                        break;

                    // Inject the line of the file into the buffer.
                    enteredText.append(line).append('\n');

                    // Attempt to process any operations in the entered text.
                    // If there are no complete commands, this will be a
                    // no-op.  If there are multiple complete commands, they
                    // will all be processed.
                    processEnteredText();
                }
            }
            catch (FileNotFoundException e) {
                System.out.println("ERROR:  Could not open file \"" +
                    filename + "\"");
            }
            catch (IOException e) {
                System.out.println("ERROR:  Could not read file \"" +
                    filename + "\":  " + e.getMessage());
            }

            enteredText = oldText;
        }
        else if ("\\help".equals(parts[0])) {
            // Show help information.
            System.out.println("You can enter any NanoDB SQL command, or " +
                "the following built-in commands.");
            System.out.println("EXIT; or QUIT; will exit the NanoDB client.");
            System.out.println();
            System.out.println("\\help");
            System.out.println("\tDisplays this help information.");
            System.out.println();
            System.out.println("\\source filename.sql");
            System.out.println("\tLoads and executes the contents of \"filename.sql\".");
            System.out.println();
        }
        else {
            System.out.println("ERROR:  Unrecognized shell command \"" +
                parts[0] + "\"");
        }

    }


    /**
     * Outputs relevant information from the command-result object.
     *
     * @param result the command-result object to output
     */
    private void outputCommandResult(CommandResult result) {
        // TODO:  Right now we only print out error information.  In the
        //        future we will want to output other details.  This
        //        functionality also needs to be integrated with the tuple-
        //        output code from the server.  Need to think about this more.
        if (result.failed()) {
            Exception e = result.getFailure();
            if (e instanceof ParseCancellationException) {
                System.out.println("ERROR:  Could not parse command");
            }
            else {
                System.out.println("ERROR:  " + e.getMessage());
            }
        }
    }


    /**
     * Shut down the interactive client.  The specific way the client
     * interacts with the server dictates how this shutdown mechanism
     * will work.
     *
     * @throws Exception if any error occurs during shutdown
     */
    public abstract void shutdown() throws Exception;
}
