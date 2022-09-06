package edu.caltech.nanodb.sqlparse;


import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.caltech.nanodb.commands.Command;
import edu.caltech.nanodb.expressions.Expression;
import edu.caltech.nanodb.functions.FunctionDirectory;
import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.tree.ParseTree;


/**
 * This helper class parses a string into a SQL operation using the NanoSQL
 * parser.
 */
public class ParseUtil {
    /** A logging object for reporting anything interesting that happens. */
    private static Logger logger = LogManager.getLogger(ParseUtil.class);


    /**
     * This helper function conditionally appends a semicolon onto a command
     * string if one is not already present.  Note that leading and trailing
     * whitespace are also trimmed by this function.
     *
     * @param command the SQL command to optionally append a semicolon to.
     * @return a SQL command that is definitely terminated with a semicolon.
     */
    public static String appendSemicolon(String command) {
        command = command.trim();
        if (command.charAt(command.length() - 1) != ';')
            command = command + ';';

        return command;
    }


    /**
     * Parse an input string into some kind of {@link Command}.
     *
     * @param command the string representation of the command
     * @param functionDirectory a directory of functions to resolve
     *        function-calls against
     *
     * @return a {@code Command} object representing the command
     *
     * @throws RecognitionException if the string could not be parsed
     */
    public static Command parseCommand(String command,
                                       FunctionDirectory functionDirectory)
        throws RecognitionException {

        command = appendSemicolon(command);

        CharStream input = CharStreams.fromString(command);
        NanoSQLLexer lexer = new NanoSQLLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        NanoSQLParser parser = new NanoSQLParser(tokens);
        parser.setErrorHandler(new BailErrorStrategy());
        ParseTree tree = parser.command();
        if (logger.isDebugEnabled())
            logger.debug("Parse tree:  " + tree.toStringTree(parser));

        NanoSQLTranslator visitor = new NanoSQLTranslator(functionDirectory);
        return (Command) visitor.visit(tree);
    }


    /**
     * Parse an input string into some kind of {@link Command}.
     *
     * @param command the string representation of the command
     *
     * @return a {@code Command} object representing the command
     *
     * @throws RecognitionException if the string could not be parsed
     */
    public static Command parseCommand(String command)
        throws RecognitionException {
        return parseCommand(command, null);
    }


    @SuppressWarnings("unchecked")
    public static List<Command> parseCommands(String commands,
        FunctionDirectory functionDirectory) throws RecognitionException {

        appendSemicolon(commands);

        CharStream input = CharStreams.fromString(commands);
        NanoSQLLexer lexer = new NanoSQLLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        NanoSQLParser parser = new NanoSQLParser(tokens);
        ParseTree tree = parser.commands();
        if (logger.isDebugEnabled())
            logger.debug("Parse tree:  " + tree.toStringTree(parser));

        NanoSQLTranslator visitor = new NanoSQLTranslator(functionDirectory);
        return (List<Command>) visitor.visit(tree);
    }


    public static Expression parseExpression(String expression,
        FunctionDirectory functionDirectory) {

        CharStream input = CharStreams.fromString(expression);
        NanoSQLLexer lexer = new NanoSQLLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        NanoSQLParser parser = new NanoSQLParser(tokens);
        ParseTree tree = parser.expression();
        if (logger.isDebugEnabled())
            logger.debug("Parse tree:  " + tree.toStringTree(parser));

        NanoSQLTranslator visitor = new NanoSQLTranslator(functionDirectory);
        return (Expression) visitor.visit(tree);
    }


    public static Expression parseExpression(String expression) {
        return parseExpression(expression, null);
    }
}
