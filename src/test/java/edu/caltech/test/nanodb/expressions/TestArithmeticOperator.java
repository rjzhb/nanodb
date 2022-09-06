package edu.caltech.test.nanodb.expressions;


import java.math.BigDecimal;
import java.math.BigInteger;

import org.testng.annotations.Test;

import edu.caltech.nanodb.expressions.ArithmeticOperator;
import edu.caltech.nanodb.expressions.ArithmeticOperator.Type;
import edu.caltech.nanodb.expressions.DivideByZeroException;
import edu.caltech.nanodb.expressions.LiteralValue;


/**
 * This test class exercises the functionality of the {@link ArithmeticOperator}
 * class.
 */
@Test(groups={"framework"})
public class TestArithmeticOperator {

    /**
     * A simple helper class to record an arithmetic test to perform,
     * and its expected result.
     */
    private static class TestOperation {
        Type op;

        Object arg1;

        Object arg2;

        Object result;

        TestOperation(ArithmeticOperator.Type op,
                             Object arg1, Object arg2, Object result) {
            this.op = op;
            this.arg1 = arg1;
            this.arg2 = arg2;
            this.result = result;
        }
    }


    private static TestOperation[] ADD_TESTS = {
        // Simple addition, no casting
        new TestOperation(Type.ADD, Integer.valueOf(3), Integer.valueOf(4), Integer.valueOf(7)),
        new TestOperation(Type.ADD, Float.valueOf(2.5f), Float.valueOf(3.25f), Float.valueOf(5.75f)),
        new TestOperation(Type.ADD, Long.valueOf(14), Long.valueOf(38), Long.valueOf(52)),
        new TestOperation(Type.ADD, Double.valueOf(-3.5), Double.valueOf(9.0), Double.valueOf(5.5)),
        new TestOperation(Type.ADD, new BigInteger("123"), new BigInteger("345"), new BigInteger("468")),
        new TestOperation(Type.ADD, new BigDecimal("3.1"), new BigDecimal("-2.2"), new BigDecimal("0.9")),

        // Addition with casting

        new TestOperation(Type.ADD, Integer.valueOf(3), Float.valueOf(4.5f), Float.valueOf(7.5f)),
        new TestOperation(Type.ADD, Float.valueOf(2.5f), Integer.valueOf(4), Float.valueOf(6.5f)),

        new TestOperation(Type.ADD, Long.valueOf(3), Float.valueOf(4.5f), Float.valueOf(7.5f)),
        new TestOperation(Type.ADD, Float.valueOf(2.5f), Long.valueOf(4), Float.valueOf(6.5f)),

        new TestOperation(Type.ADD, Integer.valueOf(3), Long.valueOf(15), Long.valueOf(18)),
        new TestOperation(Type.ADD, Long.valueOf(12), Integer.valueOf(4), Long.valueOf(16)),

        new TestOperation(Type.ADD, Integer.valueOf(3), Double.valueOf(4.5), Double.valueOf(7.5)),
        new TestOperation(Type.ADD, Double.valueOf(2.5), Integer.valueOf(4), Double.valueOf(6.5)),

        new TestOperation(Type.ADD, Long.valueOf(3), Double.valueOf(4.5), Double.valueOf(7.5)),
        new TestOperation(Type.ADD, Double.valueOf(2.5), Long.valueOf(4), Double.valueOf(6.5)),

        new TestOperation(Type.ADD, Double.valueOf(3.5), Float.valueOf(4.75f), Double.valueOf(8.25)),
        new TestOperation(Type.ADD, Float.valueOf(2.5f), Double.valueOf(1.25), Double.valueOf(3.75))
    };


    private static TestOperation[] SUB_TESTS = {
        // Simple subtraction, no casting
        new TestOperation(Type.SUBTRACT, Integer.valueOf(3), Integer.valueOf(4), Integer.valueOf(-1)),
        new TestOperation(Type.SUBTRACT, Float.valueOf(2.5f), Float.valueOf(3.25f), Float.valueOf(-0.75f)),
        new TestOperation(Type.SUBTRACT, Long.valueOf(14), Long.valueOf(38), Long.valueOf(-24)),
        new TestOperation(Type.SUBTRACT, Double.valueOf(-3.5), Double.valueOf(9.0), Double.valueOf(-12.5)),
        new TestOperation(Type.SUBTRACT, new BigInteger("123"), new BigInteger("345"), new BigInteger("-222")),
        new TestOperation(Type.SUBTRACT, new BigDecimal("3.1"), new BigDecimal("-2.2"), new BigDecimal("5.3")),

        // Subtraction with casting

        new TestOperation(Type.SUBTRACT, Integer.valueOf(3), Float.valueOf(4.5f), Float.valueOf(-1.5f)),
        new TestOperation(Type.SUBTRACT, Float.valueOf(2.5f), Integer.valueOf(4), Float.valueOf(-1.5f)),

        new TestOperation(Type.SUBTRACT, Long.valueOf(3), Float.valueOf(4.5f), Float.valueOf(-1.5f)),
        new TestOperation(Type.SUBTRACT, Float.valueOf(2.5f), Long.valueOf(4), Float.valueOf(-1.5f)),

        new TestOperation(Type.SUBTRACT, Integer.valueOf(3), Long.valueOf(15), Long.valueOf(-12)),
        new TestOperation(Type.SUBTRACT, Long.valueOf(12), Integer.valueOf(4), Long.valueOf(8)),

        new TestOperation(Type.SUBTRACT, Integer.valueOf(3), Double.valueOf(4.5), Double.valueOf(-1.5)),
        new TestOperation(Type.SUBTRACT, Double.valueOf(2.5), Integer.valueOf(4), Double.valueOf(-1.5)),

        new TestOperation(Type.SUBTRACT, Long.valueOf(3), Double.valueOf(4.5), Double.valueOf(-1.5)),
        new TestOperation(Type.SUBTRACT, Double.valueOf(2.5), Long.valueOf(4), Double.valueOf(-1.5)),

        new TestOperation(Type.SUBTRACT, Double.valueOf(3.5), Float.valueOf(4.75f), Double.valueOf(-1.25)),
        new TestOperation(Type.SUBTRACT, Float.valueOf(2.5f), Double.valueOf(1.25), Double.valueOf(1.25))
    };


    private static TestOperation[] MUL_TESTS = {
        // Simple multiplication, no casting
        new TestOperation(Type.MULTIPLY, Integer.valueOf(3), Integer.valueOf(4), Integer.valueOf(12)),
        new TestOperation(Type.MULTIPLY, Float.valueOf(2.5f), Float.valueOf(3.25f), Float.valueOf(8.125f)),
        new TestOperation(Type.MULTIPLY, Long.valueOf(14), Long.valueOf(38), Long.valueOf(532)),
        new TestOperation(Type.MULTIPLY, Double.valueOf(-3.5), Double.valueOf(9.0), Double.valueOf(-31.5)),
        new TestOperation(Type.MULTIPLY, new BigInteger("123"), new BigInteger("345"), new BigInteger("42435")),
        new TestOperation(Type.MULTIPLY, new BigDecimal("3.1"), new BigDecimal("-2.2"), new BigDecimal("-6.82")),

        // Multiplication with casting

        new TestOperation(Type.MULTIPLY, Integer.valueOf(3), Float.valueOf(4.5f), Float.valueOf(13.5f)),
        new TestOperation(Type.MULTIPLY, Float.valueOf(2.5f), Integer.valueOf(4), Float.valueOf(10)),

        new TestOperation(Type.MULTIPLY, Long.valueOf(3), Float.valueOf(4.5f), Float.valueOf(13.5f)),
        new TestOperation(Type.MULTIPLY, Float.valueOf(2.5f), Long.valueOf(4), Float.valueOf(10)),

        new TestOperation(Type.MULTIPLY, Integer.valueOf(3), Long.valueOf(15), Long.valueOf(45)),
        new TestOperation(Type.MULTIPLY, Long.valueOf(12), Integer.valueOf(4), Long.valueOf(48)),

        new TestOperation(Type.MULTIPLY, Integer.valueOf(3), Double.valueOf(4.5), Double.valueOf(13.5)),
        new TestOperation(Type.MULTIPLY, Double.valueOf(2.5), Integer.valueOf(4), Double.valueOf(10)),

        new TestOperation(Type.MULTIPLY, Long.valueOf(3), Double.valueOf(4.5), Double.valueOf(13.5)),
        new TestOperation(Type.MULTIPLY, Double.valueOf(2.5), Long.valueOf(4), Double.valueOf(10)),

        new TestOperation(Type.MULTIPLY, Double.valueOf(3.5), Float.valueOf(4.75f), Double.valueOf(16.625)),
        new TestOperation(Type.MULTIPLY, Float.valueOf(2.5f), Double.valueOf(1.25), Double.valueOf(3.125))
    };


    /* Note that integer division produces integer result types. */
    private static TestOperation[] DIV_TESTS = {
        // Simple division, no casting
        new TestOperation(Type.DIVIDE, Integer.valueOf(12), Integer.valueOf(3), Integer.valueOf(4)),
        new TestOperation(Type.DIVIDE, Float.valueOf(8.125f), Float.valueOf(2.5f), Float.valueOf(3.25f)),
        new TestOperation(Type.DIVIDE, Long.valueOf(532), Long.valueOf(14), Long.valueOf(38)),
        new TestOperation(Type.DIVIDE, Double.valueOf(-31.5), Double.valueOf(-3.5), Double.valueOf(9.0)),

        // Division with casting

        new TestOperation(Type.DIVIDE, Float.valueOf(13.5f), Integer.valueOf(3), Float.valueOf(4.5f)),
        new TestOperation(Type.DIVIDE, Integer.valueOf(10), Float.valueOf(2.5f), Float.valueOf(4)),

        new TestOperation(Type.DIVIDE, Float.valueOf(13.5f), Long.valueOf(3), Float.valueOf(4.5f)),
        new TestOperation(Type.DIVIDE, Long.valueOf(10), Float.valueOf(2.5f), Float.valueOf(4)),

        new TestOperation(Type.DIVIDE, Long.valueOf(45), Integer.valueOf(3), Long.valueOf(15)),
        new TestOperation(Type.DIVIDE, Integer.valueOf(48), Long.valueOf(12), Long.valueOf(4)),

        new TestOperation(Type.DIVIDE, Double.valueOf(13.5), Integer.valueOf(3), Double.valueOf(4.5)),
        new TestOperation(Type.DIVIDE, Integer.valueOf(10), Double.valueOf(2.5), Double.valueOf(4)),

        new TestOperation(Type.DIVIDE, Double.valueOf(13.5), Long.valueOf(3), Double.valueOf(4.5)),
        new TestOperation(Type.DIVIDE, Long.valueOf(10), Double.valueOf(2.5), Double.valueOf(4)),

        new TestOperation(Type.DIVIDE, Double.valueOf(16.625), Float.valueOf(3.5f), Double.valueOf(4.75)),
        new TestOperation(Type.DIVIDE, Float.valueOf(3.125f), Double.valueOf(2.5), Double.valueOf(1.25))
    };


    private static TestOperation[] DIV_BY_ZERO_TESTS = {
        new TestOperation(Type.DIVIDE, Integer.valueOf(12), Integer.valueOf(0), null),
        new TestOperation(Type.DIVIDE, Float.valueOf(12), Float.valueOf(0), null),
        new TestOperation(Type.DIVIDE, Long.valueOf(12), Long.valueOf(0), null),
        new TestOperation(Type.DIVIDE, Double.valueOf(12), Double.valueOf(0), null),
        new TestOperation(Type.DIVIDE, new BigInteger("12"), new BigInteger("0"), null),
        new TestOperation(Type.DIVIDE, new BigDecimal(12), new BigDecimal(0), null)
    };


    public void testAdd() {
        runTests(ADD_TESTS);
    }


    public void testSubtract() {
        runTests(SUB_TESTS);
    }


    public void testMultiply() {
        runTests(MUL_TESTS);
    }


    public void testDivide() {
        runTests(DIV_TESTS);
    }


    public void testDivideByZero() {
        for (TestOperation test : DIV_BY_ZERO_TESTS) {
            ArithmeticOperator op = prepareTest(test);
            try {
                op.evaluate();
                assert false : "Expected divide-by-zero exception";
            } catch (DivideByZeroException e) {
                // Pass!
            }
        }
    }


    private void runTests(TestOperation[] tests) {
        for (TestOperation test : tests) {
            ArithmeticOperator op = prepareTest(test);
            Object actual = op.evaluate();
            checkTestResult(test, actual);
        }
    }


    private ArithmeticOperator prepareTest(TestOperation test) {
        return new ArithmeticOperator(test.op, new LiteralValue(test.arg1),
            new LiteralValue(test.arg2));
    }

    private void checkTestResult(TestOperation test, Object actual) {
        assert actual.equals(test.result) : "Actual result " + actual +
            " doesn't match expected value " + test.result;
    }
}
