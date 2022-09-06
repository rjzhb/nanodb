package edu.caltech.test.nanodb.sqlparse;


import java.util.List;

import edu.caltech.nanodb.expressions.Expression;
import edu.caltech.nanodb.queryast.SelectClause;
import edu.caltech.nanodb.queryast.SelectValue;
import edu.caltech.nanodb.sqlparse.ParseUtil;
import org.testng.annotations.Test;

import edu.caltech.nanodb.commands.InsertCommand;


/**
 * Verifies that parsing of the <tt>INSERT</tt> command works as expected.
 */
@Test(groups={"parser"})
public class TestParserInsert {
    /**
     * Verifies the <tt>INSERT INTO t VALUES ...</tt> statement, and the
     * <tt>INSERT INTO t (c1, c2, ...) VALUES ...</tt> statement.
     */
    public void testParseInsertValues() {
        InsertCommand cmd;
        List<String> colNames;
        List<Expression> values;

        cmd = (InsertCommand) ParseUtil.parseCommand(
            "INSERT INTO foo VALUES (1, 'hello', true);");

        assert "foo".equals(cmd.getTableName());

        colNames = cmd.getColNames();
        assert colNames.size() == 0;

        assert cmd.getSelectClause() == null;

        values = cmd.getValues();
        assert values.size() == 3;
        assert 1 == (Integer) values.get(0).evaluate();
        assert "hello".equals(values.get(1).evaluate());
        assert (Boolean) values.get(2).evaluate();

        cmd = (InsertCommand) ParseUtil.parseCommand(
            "INSERT INTO bar (a, b) VALUES ('goodbye', 500);");

        assert "bar".equals(cmd.getTableName());

        colNames = cmd.getColNames();
        assert colNames.size() == 2;
        assert "a".equals(colNames.get(0));
        assert "b".equals(colNames.get(1));

        assert cmd.getSelectClause() == null;

        values = cmd.getValues();
        assert values.size() == 2;
        assert "goodbye".equals(values.get(0).evaluate());
        assert 500 == (Integer) values.get(1).evaluate();
    }


    /**
     * Verifies the <tt>INSERT INTO t SELECT ...</tt> statement, and the
     * <tt>INSERT INTO t (c1, c2, ...) SELECT ...</tt> statement.
     */
    public void testParseInsertSelect() {
        InsertCommand cmd;
        List<String> colNames;
        SelectClause selClause;

        cmd = (InsertCommand) ParseUtil.parseCommand(
            "INSERT INTO foo SELECT * FROM bar;");

        assert "foo".equals(cmd.getTableName());

        colNames = cmd.getColNames();
        assert colNames.size() == 0;

        assert cmd.getValues() == null;

        selClause = cmd.getSelectClause();

        assert selClause != null;
        assert selClause.isTrivialProject();
        assert selClause.getFromClause().isBaseTable();
        assert "bar".equals(selClause.getFromClause().getTableName());

        cmd = (InsertCommand) ParseUtil.parseCommand(
            "INSERT INTO bar (a, b) SELECT c, d FROM abc;");

        assert "bar".equals(cmd.getTableName());

        colNames = cmd.getColNames();
        assert colNames.size() == 2;
        assert "a".equals(colNames.get(0));
        assert "b".equals(colNames.get(1));

        assert cmd.getValues() == null;

        selClause = cmd.getSelectClause();
        assert selClause != null;
        assert !selClause.isTrivialProject();
        assert selClause.getFromClause().isBaseTable();
        assert "abc".equals(selClause.getFromClause().getTableName());

        List<SelectValue> selVals = selClause.getSelectValues();
        assert selVals.size() == 2;
        assert selVals.get(0).isSimpleColumnValue();
        assert selVals.get(1).isSimpleColumnValue();
    }
}
