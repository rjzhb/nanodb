package edu.caltech.test.nanodb.sqlparse;


import java.util.List;

import edu.caltech.nanodb.expressions.Expression;
import org.testng.annotations.Test;

import edu.caltech.nanodb.commands.UpdateCommand;
import edu.caltech.nanodb.commands.UpdateValue;
import edu.caltech.nanodb.sqlparse.ParseUtil;


/**
 * Verifies that parsing of the <tt>UPDATE</tt> command works as expected.
 */
@Test(groups={"parser"})
public class TestParserUpdate {
    /**
     * Verifies that an <tt>UPDATE</tt> statement with no predicate is parsed
     * correctly.
     */
    public void testParseUpdateNoPredicate() {
        List<UpdateValue> values;
        UpdateCommand cmd = (UpdateCommand) ParseUtil.parseCommand(
            "UPDATE foo SET x = 5, y = 'hello';");

        assert "foo".equals(cmd.getTableName()) :
            "Expected table \"foo\", got \"" + cmd.getTableName() + "\" instead";

        assert cmd.getWhereExpr() == null;

        values = cmd.getValues();
        assert values.size() == 2;

        assert "x".equals(values.get(0).getColumnName());
        assert (Integer) values.get(0).getExpression().evaluate() == 5;

        assert "y".equals(values.get(1).getColumnName());
        assert "hello".equals(values.get(1).getExpression().evaluate());
    }


    /**
     * Verifies that an <tt>UPDATE</tt> statement with a predicate is parsed
     * correctly.
     */
    public void testParseUpdateWithPredicate() {
        List<UpdateValue> values;
        UpdateCommand cmd = (UpdateCommand) ParseUtil.parseCommand(
            "UPDATE bar SET b = 'goodbye' WHERE z < 10;");

        assert "bar".equals(cmd.getTableName()) :
            "Expected table \"bar\", got \"" + cmd.getTableName() + "\" instead";

        Expression expr = cmd.getWhereExpr();
        assert expr != null;
        assert "z < 10".equals(expr.toString());

        values = cmd.getValues();
        assert values.size() == 1;

        assert "b".equals(values.get(0).getColumnName());
        assert "goodbye".equals(values.get(0).getExpression().evaluate());
    }
}
