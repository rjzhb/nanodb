package edu.caltech.test.nanodb.sqlparse;


import java.util.List;

import org.testng.annotations.Test;

import edu.caltech.nanodb.commands.SelectCommand;
import edu.caltech.nanodb.queryast.FromClause;
import edu.caltech.nanodb.queryast.SelectClause;
import edu.caltech.nanodb.queryast.SelectValue;
import edu.caltech.nanodb.sqlparse.ParseUtil;


/**
 * Verifies that parsing of the <tt>SELECT</tt> command works as expected.
 */
@Test(groups={"parser"})
public class TestParserSelect {

    public void testParseSimpleSelect() {
        SelectClause selClause;

        SelectCommand cmd =
            (SelectCommand) ParseUtil.parseCommand("SELECT 3, a, b < 5;");
        selClause = cmd.getSelectClause();

        assert !selClause.isDistinct();
        assert !selClause.isTrivialProject();
        assert selClause.getFromClause() == null;
        assert selClause.getWhereExpr() == null;
        assert selClause.getGroupByExprs().isEmpty();
        assert selClause.getHavingExpr() == null;
        assert selClause.getOrderByExprs().isEmpty();

        List<SelectValue> values = selClause.getSelectValues();
        assert values.size() == 3;

        // TODO:  More checking...
    }


    public void testParseSimpleSelectDistinct() {
        SelectClause selClause;

        SelectCommand cmd = (SelectCommand) ParseUtil.parseCommand(
            "SELECT DISTINCT 3, a, b < 5;");
        selClause = cmd.getSelectClause();

        assert  selClause.isDistinct();
        assert !selClause.isTrivialProject();
        assert selClause.getFromClause() == null;
        assert selClause.getWhereExpr() == null;
        assert selClause.getGroupByExprs().isEmpty();
        assert selClause.getHavingExpr() == null;
        assert selClause.getOrderByExprs().isEmpty();

        List<SelectValue> values = selClause.getSelectValues();
        assert values.size() == 3;

        // TODO:  More checking...
    }


    public void testParseSelectStarFrom() {
        SelectClause selClause;

        SelectCommand cmd =
            (SelectCommand) ParseUtil.parseCommand("SELECT * FROM foo;");
        selClause = cmd.getSelectClause();

        assert !selClause.isDistinct();
        assert  selClause.isTrivialProject();
        assert selClause.getWhereExpr() == null;
        assert selClause.getGroupByExprs().isEmpty();
        assert selClause.getHavingExpr() == null;
        assert selClause.getOrderByExprs().isEmpty();

        List<SelectValue> values = selClause.getSelectValues();
        assert values.size() == 1;
        assert values.get(0).isWildcard();

        FromClause fromClause = selClause.getFromClause();
        assert fromClause.isBaseTable();
        assert fromClause.getTableName().equals("foo") :
            "Expected table-name \"foo\", got \"" + fromClause.getTableName() +
            "\" instead";

        // TODO:  More checking...
    }


    public void testParseSelectFrom() {
        SelectClause selClause;

        SelectCommand cmd = (SelectCommand) ParseUtil.parseCommand(
            "SELECT a, b + c, d FROM foo;");
        selClause = cmd.getSelectClause();

        assert !selClause.isDistinct();
        assert !selClause.isTrivialProject();
        assert selClause.getWhereExpr() == null;
        assert selClause.getGroupByExprs().isEmpty();
        assert selClause.getHavingExpr() == null;
        assert selClause.getOrderByExprs().isEmpty();

        List<SelectValue> values = selClause.getSelectValues();
        assert values.size() == 3;

        FromClause fromClause = selClause.getFromClause();
        assert fromClause.isBaseTable();
        assert fromClause.getTableName().equals("foo");

        // TODO:  More checking...
    }


    public void testParseSelectDistinctStarFrom() {
        SelectClause selClause;

        SelectCommand cmd = (SelectCommand) ParseUtil.parseCommand(
            "SELECT DISTINCT * FROM bar;");
        selClause = cmd.getSelectClause();

        assert  selClause.isDistinct();
        assert  selClause.isTrivialProject();
        assert selClause.getWhereExpr() == null;
        assert selClause.getGroupByExprs().isEmpty();
        assert selClause.getHavingExpr() == null;
        assert selClause.getOrderByExprs().isEmpty();

        List<SelectValue> values = selClause.getSelectValues();
        assert values.size() == 1;
        assert values.get(0).isWildcard();

        FromClause fromClause = selClause.getFromClause();
        assert fromClause.isBaseTable();
        assert fromClause.getTableName().equals("bar");

        // TODO:  More checking...
    }

}
