package edu.caltech.test.nanodb.sqlparse;


import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.time.Period;

import edu.caltech.nanodb.expressions.FunctionCall;
import edu.caltech.nanodb.expressions.IsNullOperator;
import org.testng.annotations.Test;

import edu.caltech.nanodb.expressions.ColumnValue;
import edu.caltech.nanodb.expressions.LiteralValue;
import edu.caltech.nanodb.sqlparse.ParseUtil;


@Test(groups={"parser"})
public class TestParserExpressions {
    /**
     * Exercises the parsing of all kinds of literal values.
     */
    public void testParseExprLiteralValue() {
        LiteralValue e;

        e = (LiteralValue) ParseUtil.parseExpression("NULL");
        assert null == e.evaluate();

        e = (LiteralValue) ParseUtil.parseExpression("null");
        assert null == e.evaluate();

        e = (LiteralValue) ParseUtil.parseExpression("TRUE");
        assert Boolean.TRUE.equals(e.evaluate());

        e = (LiteralValue) ParseUtil.parseExpression("true");
        assert Boolean.TRUE.equals(e.evaluate());

        e = (LiteralValue) ParseUtil.parseExpression("FALSE");
        assert Boolean.FALSE.equals(e.evaluate());

        e = (LiteralValue) ParseUtil.parseExpression("false");
        assert Boolean.FALSE.equals(e.evaluate());

        e = (LiteralValue) ParseUtil.parseExpression("15723");
        assert Integer.valueOf(15723).equals(e.evaluate());

        e = (LiteralValue) ParseUtil.parseExpression("9876543210");
        assert Long.valueOf(9876543210L).equals(e.evaluate());

        e = (LiteralValue)
            ParseUtil.parseExpression("987654321098765432109876543210");
        assert new BigInteger("987654321098765432109876543210").equals(
            e.evaluate());

        e = (LiteralValue) ParseUtil.parseExpression("3.1415");
        assert new BigDecimal("3.1415").equals(e.evaluate());

        e = (LiteralValue) ParseUtil.parseExpression("519667.881");
        assert new BigDecimal("519667.881").equals(e.evaluate());

        e = (LiteralValue) ParseUtil.parseExpression(".243");
        assert new BigDecimal("0.243").equals(e.evaluate());

        e = (LiteralValue) ParseUtil.parseExpression("'hello'");
        assert "hello".equals(e.evaluate());

        e = (LiteralValue) ParseUtil.parseExpression("'HELLO'");
        assert "HELLO".equals(e.evaluate());

        e = (LiteralValue) ParseUtil.parseExpression("INTERVAL '2 week'");
        assert Period.ofWeeks(2).equals(e.evaluate());

        e = (LiteralValue) ParseUtil.parseExpression("interval '14 days'");
        assert Period.ofDays(14).equals(e.evaluate());

        e = (LiteralValue) ParseUtil.parseExpression("Interval '5 minute'");
        assert Duration.ofMinutes(5).equals(e.evaluate());
    }


    /**
     * Exercises the parsing of column references.
     */
    public void testParseExprColumnReference() {
        ColumnValue e;

        e = (ColumnValue) ParseUtil.parseExpression("abc");
        assert e.getColumnName().getTableName() == null;
        assert e.getColumnName().getColumnName().equals("abc");
        assert !e.getColumnName().isColumnWildcard();
        assert !e.getColumnName().isTableSpecified();

        e = (ColumnValue) ParseUtil.parseExpression("XYZ");
        assert e.getColumnName().getTableName() == null;
        assert e.getColumnName().getColumnName().equals("xyz");
        assert !e.getColumnName().isColumnWildcard();
        assert !e.getColumnName().isTableSpecified();

        e = (ColumnValue) ParseUtil.parseExpression("t1.abc");
        assert e.getColumnName().getTableName().equals("t1");
        assert e.getColumnName().getColumnName().equals("abc");
        assert !e.getColumnName().isColumnWildcard();
        assert e.getColumnName().isTableSpecified();

        e = (ColumnValue) ParseUtil.parseExpression("T2.XYZ");
        assert e.getColumnName().getTableName().equals("t2");
        assert e.getColumnName().getColumnName().equals("xyz");
        assert !e.getColumnName().isColumnWildcard();
        assert e.getColumnName().isTableSpecified();

        e = (ColumnValue) ParseUtil.parseExpression("*");
        assert e.getColumnName().getTableName() == null;
        assert e.getColumnName().getColumnName() == null;
        assert e.getColumnName().isColumnWildcard();
        assert !e.getColumnName().isTableSpecified();

        e = (ColumnValue) ParseUtil.parseExpression("t3.*");
        assert e.getColumnName().getTableName().equals("t3");
        assert e.getColumnName().getColumnName() == null;
        assert e.getColumnName().isColumnWildcard();
        assert e.getColumnName().isTableSpecified();
    }


    /**
     * Exercises the parsing of functions.
    public void testParseExprFunctionCall() {
        FunctionCall e;

        e = (FunctionCall) ParseUtil.parseExpression("fn1()");
        // TODO:  Verify

        e = (FunctionCall) ParseUtil.parseExpression("fn2(a)");
        // TODO:  Verify

        e = (FunctionCall) ParseUtil.parseExpression("fn3(distinct b)");
        // TODO:  Verify

        e = (FunctionCall) ParseUtil.parseExpression("fn4(c, d, e)");
        // TODO:  Verify

        e = (FunctionCall) ParseUtil.parseExpression("fn5(distinct f, g, h)");
        // TODO:  Verify
    }
     */


}
