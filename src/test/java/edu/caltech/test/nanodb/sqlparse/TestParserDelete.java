package edu.caltech.test.nanodb.sqlparse;


import edu.caltech.nanodb.commands.DeleteCommand;
import edu.caltech.nanodb.expressions.Expression;
import edu.caltech.nanodb.sqlparse.ParseUtil;
import org.testng.annotations.Test;


/**
 * Verifies that parsing of the <tt>DELETE FROM t WHERE ...</tt> command
 * works as expected.
 */
@Test(groups={"parser"})
public class TestParserDelete {

    /**
     * Verifies that a <tt>DELETE</tt> statement with no predicate is parsed
     * correctly.
     */
    public void testParseDeleteNoPredicate() {
        DeleteCommand cmd = (DeleteCommand) ParseUtil.parseCommand("DELETE FROM foo;");

        assert "foo".equals(cmd.getTableName()) :
            "Expected table \"foo\", got \"" + cmd.getTableName() + "\" instead";
        assert cmd.getWhereExpr() == null;
    }


    /**
     * Verifies that a <tt>DELETE</tt> statement with a predicate is parsed
     * correctly.
     */
    public void testParseDeleteWithPredicate() {
        DeleteCommand cmd = (DeleteCommand) ParseUtil.parseCommand(
            "DELETE FROM bar WHERE a > 5;");

        assert "bar".equals(cmd.getTableName()) :
            "Expected table \"bar\", got \"" + cmd.getTableName() + "\" instead";

        Expression e = cmd.getWhereExpr();
        assert e != null;
        assert "a > 5".equals(e.toString()) :
            "Expected predicate \"a > 5\", got \"" + e.toString() + "\" instead";
    }
}
