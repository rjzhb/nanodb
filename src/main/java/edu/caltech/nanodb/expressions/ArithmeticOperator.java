package edu.caltech.nanodb.expressions;


import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.time.temporal.TemporalAmount;

import edu.caltech.nanodb.relations.ColumnInfo;
import edu.caltech.nanodb.relations.ColumnType;
import edu.caltech.nanodb.relations.SQLDataType;
import edu.caltech.nanodb.relations.Schema;
import edu.caltech.nanodb.relations.SchemaNameException;
import edu.caltech.nanodb.util.Pair;


/**
 * This class implements simple binary arithmetic operations.  The supported
 * operations are:
 * <ul>
 *   <li>addition, <tt>+</tt></li>
 *   <li>subtraction, <tt>-</tt></li>
 *   <li>multiplication, <tt>*</tt></li>
 *   <li>division, <tt>/</tt></li>
 *   <li>remainder, <tt>%</tt></li>
 *   <li>exponentiation, <tt>^</tt></li>
 * </ul>
 */
public class ArithmeticOperator extends Expression {

    /**
     * This enum specifies the arithmetic operations that this class can provide.
     * Each arithmetic operation also holds its string representation, which is
     * used when converting an arithmetic expression into a string for display.
     */
    public enum Type {
        ADD("+"),
        SUBTRACT("-"),
        MULTIPLY("*"),
        DIVIDE("/"),
        REMAINDER("%"),
        POWER("^");


        /** The string representation for each operator.  Used for printing. */
        private final String stringRep;

        /**
         * Construct a Type enum with the specified string representation.
         *
         * @param rep the string representation of the arithmetic operation
         */
        Type(String rep) {
            stringRep = rep;
        }

        /**
         * Accessor for the operator type's string representation.
         *
         * @return the string representation of the arithmetic operation
         */
        public String stringRep() {
            return stringRep;
        }


        /**
         * Given a string representation of an arithmetic operator, this
         * method returns the corresponding {@code Type} value.
         *
         * @param stringRep the string representation of the arithmetic
         *        operator
         *
         * @return the operator's corresponding type, or {@code null} if the
         *         type cannot be found
         */
        public static Type find(String stringRep) {
            for (Type t : values()) {
                if (t.stringRep.equals(stringRep))
                    return t;
            }

            // Couldn't find the corresponding operator type.
            return null;
        }
    }


    /** The kind of comparison, such as "subtract" or "multiply." */
    private Type type;

    /** The left expression in the comparison. */
    private Expression leftExpr;

    /** The right expression in the comparison. */
    private Expression rightExpr;



    public ArithmeticOperator(Type type, Expression lhs, Expression rhs) {
        if (type == null || lhs == null || rhs == null)
            throw new NullPointerException();

        leftExpr = lhs;
        rightExpr = rhs;

        this.type = type;
    }


    @Override
    public ColumnInfo getColumnInfo(Schema schema) throws SchemaNameException {
        ColumnInfo ltColInfo = leftExpr.getColumnInfo(schema);
        ColumnInfo rtColInfo = rightExpr.getColumnInfo(schema);

        SQLDataType resultSQLType = getSQLResultType(
            ltColInfo.getType().getBaseType(), rtColInfo.getType().getBaseType());

        ColumnType colType = new ColumnType(resultSQLType);
        return new ColumnInfo(toString(), colType);
    }


    private SQLDataType getSQLResultType(SQLDataType lType, SQLDataType rType) {
        // This array specifies the type-conversion sequence.  If at least one of
        // the arguments is type typeOrder[i], then both arguments are coerced to
        // that type.  (This is not entirely accurate at the moment, but is
        // sufficient for our needs.)
        SQLDataType[] typeOrder = {
            SQLDataType.NUMERIC, SQLDataType.DOUBLE, SQLDataType.FLOAT,
            SQLDataType.BIGINT, SQLDataType.INTEGER, SQLDataType.SMALLINT,
            SQLDataType.TINYINT
        };

        for (SQLDataType aTypeOrder : typeOrder) {
            if (lType == aTypeOrder || rType == aTypeOrder)
                return aTypeOrder;
        }

        // Just guess INTEGER.  Works for C...
        return SQLDataType.INTEGER;
    }


    public Object evaluate(Environment env) throws ExpressionException {
        // Evaluate the left and right subexpressions.
        Object lhsValue = leftExpr.evaluate(env);
        Object rhsValue = rightExpr.evaluate(env);

        // If either the LHS value or RHS value is NULL (represented by Java
        // null value) then the entire expression evaluates to NULL.
        if (lhsValue == null || rhsValue == null)
            return null;

        return evalObjects(type, lhsValue, rhsValue);
    }


    /**
     * This static helper method can be used to compute basic arithmetic
     * operations between two arguments.  It is of course used to evaluate
     * <tt>ArithmeticOperator</tt> objects, but it can also be used to evaluate
     * specific arithmetic operations within other components of the database
     * system.
     *
     * @param type the arithmetic operation to perform
     * @param aObj the first operand value for the operation
     * @param bObj the second operand value for the operation
     *
     * @return the result of the arithmetic operation
     *
     * @throws ExpressionException if the operand type is unrecognized
     */
    public static Object evalObjects(Type type, Object aObj, Object bObj) {
        // Coerce the values to both have the same numeric type.

        Pair coerced = TypeConverter.coerceArithmetic(aObj, bObj);

        Object result;

        if (coerced.value1 instanceof BigInteger) {
            result = evalBigIntegers(type, (BigInteger) coerced.value1,
                                           (BigInteger) coerced.value2);
        }
        else if (coerced.value1 instanceof BigDecimal) {
            result = evalBigDecimals(type, (BigDecimal) coerced.value1,
                                           (BigDecimal) coerced.value2);
        }
        else if (coerced.value1 instanceof Double) {
            result = evalDoubles(type, (Double) coerced.value1,
                                       (Double) coerced.value2);
        }
        else if (coerced.value1 instanceof Float) {
            result = evalFloats(type, (Float) coerced.value1,
                                      (Float) coerced.value2);
        }
        else if (coerced.value1 instanceof Long) {
            result = evalLongs(type, (Long) coerced.value1,
                                     (Long) coerced.value2);
        }
        else if (coerced.value1 instanceof Integer) {
            result = evalIntegers(type, (Integer) coerced.value1,
                                        (Integer) coerced.value2);
        }
        else if (coerced.value1 instanceof LocalDate &&
                 coerced.value2 instanceof LocalDate) {
            result = evalDates(type, (LocalDate) coerced.value1,
                                     (LocalDate) coerced.value2);
        }
        else if (coerced.value1 instanceof LocalTime &&
                 coerced.value2 instanceof LocalTime) {
            result = evalTimes(type, (LocalTime) coerced.value1,
                                     (LocalTime) coerced.value2);
        }
        else if (coerced.value1 instanceof LocalDateTime &&
                 coerced.value2 instanceof LocalDateTime) {
            result = evalDateTimes(type, (LocalDateTime) coerced.value1,
                                         (LocalDateTime) coerced.value2);
        }
        else if (coerced.value1 instanceof LocalDate &&
            coerced.value2 instanceof TemporalAmount) {
            result = evalDateInterval(type, (LocalDate) coerced.value1,
                (TemporalAmount) coerced.value2);
        }
        else if (coerced.value1 instanceof LocalTime &&
            coerced.value2 instanceof TemporalAmount) {
            result = evalTimeInterval(type, (LocalTime) coerced.value1,
                (TemporalAmount) coerced.value2);
        }
        else if (coerced.value1 instanceof LocalDateTime &&
            coerced.value2 instanceof TemporalAmount) {
            result = evalDateTimeInterval(type, (LocalDateTime) coerced.value1,
                (TemporalAmount) coerced.value2);
        }
        else if (coerced.value1 instanceof TemporalAmount &&
            coerced.value2 instanceof LocalDate) {
            result = evalIntervalDate(type, (TemporalAmount) coerced.value1,
                (LocalDate) coerced.value2);
        }
        else if (coerced.value1 instanceof TemporalAmount &&
            coerced.value2 instanceof LocalTime) {
            result = evalIntervalTime(type, (TemporalAmount) coerced.value1,
                (LocalTime) coerced.value2);
        }
        else if (coerced.value1 instanceof TemporalAmount &&
            coerced.value2 instanceof LocalDateTime) {
            result = evalIntervalDateTime(type, (TemporalAmount) coerced.value1,
                (LocalDateTime) coerced.value2);
        }
        else {
            throw new IllegalArgumentException("Cannot perform arithmetic on " +
                coerced.value1.getClass() + " and " + coerced.value2.getClass());
        }

        return result;
    }


    private static BigDecimal evalBigDecimals(Type type, BigDecimal a, BigDecimal b) {
        BigDecimal result;

        switch (type) {
        case ADD:
            result = a.add(b);
            break;

        case SUBTRACT:
            result = a.subtract(b);
            break;

        case MULTIPLY:
            result = a.multiply(b);
            break;

        case DIVIDE:
            if (b.equals(BigDecimal.ZERO))
                throw new DivideByZeroException();

            int scale = 10;  // TODO:  Scale?
            result = a.divide(b, scale, RoundingMode.HALF_UP);
            break;

        case REMAINDER:
            if (b.equals(BigDecimal.ZERO))
                throw new DivideByZeroException();

            result = a.remainder(b);
            break;

        case POWER:
            throw new ExpressionException("POWER is unsupported for BigDecimal");

        default:
            throw new ExpressionException("Unrecognized arithmetic type " + type);
        }

        return result;
    }


    private static BigInteger evalBigIntegers(Type type, BigInteger a, BigInteger b) {
        BigInteger result;

        switch (type) {
        case ADD:
            result = a.add(b);
            break;

        case SUBTRACT:
            result = a.subtract(b);
            break;

        case MULTIPLY:
            result = a.multiply(b);
            break;

        case DIVIDE:
            if (b.equals(BigInteger.ZERO))
                throw new DivideByZeroException();

            result = a.divide(b);
            break;

        case REMAINDER:
            if (b.equals(BigInteger.ZERO))
                throw new DivideByZeroException();

            result = a.remainder(b);
            break;

        case POWER:
            throw new ExpressionException("POWER is unsupported for BigInteger");

        default:
            throw new ExpressionException("Unrecognized arithmetic type " + type);
        }

        return result;
    }


    /**
     * This helper implements the arithmetic operations for <tt>Double</tt>
     * values.  Note that division of two <tt>Double</tt>s will produce a
     * <tt>Double</tt>.
     *
     * @param type the arithmetic operation to perform
     * @param a the first operand value for the operation
     * @param b the second operand value for the operation
     *
     * @return the result of the arithmetic operation
     *
     * @throws ExpressionException if the operand type is unrecognized
     */
    private static Double evalDoubles(Type type, Double a, Double b) {
        double result;

        switch (type) {
        case ADD:
            result = a + b;
            break;

        case SUBTRACT:
            result = a - b;
            break;

        case MULTIPLY:
            result = a * b;
            break;

        case DIVIDE:
            if (b == 0)
                throw new DivideByZeroException();

            result = a / b;
            break;

        case REMAINDER:
            if (b == 0)
                throw new DivideByZeroException();

            result = a % b;
            break;

        case POWER:
            if (a == 0 && b == 0)
                throw new ExpressionException("0**0 is undefined");

            result = Math.pow(a, b);
            break;

        default:
            throw new ExpressionException("Unrecognized arithmetic type " + type);
        }

        return result;
    }


    /**
     * This helper implements the arithmetic operations for <tt>Float</tt>
     * values.  Note that division of two <tt>Float</tt>s will produce a
     * <tt>Float</tt>.
     *
     * @param type the arithmetic operation to perform
     * @param a the first operand value for the operation
     * @param b the second operand value for the operation
     *
     * @return the result of the arithmetic operation
     *
     * @throws ExpressionException if the operand type is unrecognized
     */
    private static Float evalFloats(Type type, Float a, Float b) {
        float result;

        switch (type) {
        case ADD:
            result = a + b;
            break;

        case SUBTRACT:
            result = a - b;
            break;

        case MULTIPLY:
            result = a * b;
            break;

        case DIVIDE:
            if (b == 0)
                throw new DivideByZeroException();

            result = a / b;
            break;

        case REMAINDER:
            if (b == 0)
                throw new DivideByZeroException();

            result = a % b;
            break;

        case POWER:
            if (a == 0 && b == 0)
                throw new ExpressionException("0**0 is undefined");

            result = (float) Math.pow(a, b);
            break;

        default:
            throw new ExpressionException("Unrecognized arithmetic type " + type);
        }

        return result;  // Rely on boxing
    }


    /**
     * This helper implements the arithmetic operations for <tt>Long</tt>
     * values.  Note that division of two <tt>Long</tt>s will produce a
     * <tt>Double</tt>, not a <tt>Long</tt>.
     *
     * @param type the arithmetic operation to perform
     * @param a the first operand value for the operation
     * @param b the second operand value for the operation
     *
     * @return the result of the arithmetic operation
     *
     * @throws ExpressionException if the operand type is unrecognized
     */
    private static Object evalLongs(Type type, Long a, Long b) {
        long result;

        switch (type) {
        case ADD:
            result = a + b;
            break;

        case SUBTRACT:
            result = a - b;
            break;

        case MULTIPLY:
            result = a * b;
            break;

        case DIVIDE:
            if (b == 0)
                throw new DivideByZeroException();

            result = a / b;
            break;

        case REMAINDER:
            if (b == 0)
                throw new DivideByZeroException();

            result = a % b;
            break;

        case POWER:
            if (a == 0 && b == 0)
                throw new ExpressionException("0**0 is undefined");

            result = (long) Math.pow(a, b);
            break;

        default:
            throw new ExpressionException("Unrecognized arithmetic type " + type);
        }

        return result;  // Rely on boxing
    }


    /**
     * This helper implements the arithmetic operations for <tt>Integer</tt>
     * values.  Note that division of two <tt>Integer</tt>s will produce a
     * <tt>Double</tt>, not an <tt>Integer</tt>.
     *
     * @param type the arithmetic operation to perform
     * @param a the first operand value for the operation
     * @param b the second operand value for the operation
     *
     * @return the result of the arithmetic operation
     *
     * @throws ExpressionException if the operand type is unrecognized
     */
    private static Object evalIntegers(Type type, Integer a, Integer b) {
        int result;

        switch (type) {
        case ADD:
            result = a + b;
            break;

        case SUBTRACT:
            result = a - b;
            break;

        case MULTIPLY:
            result = a * b;
            break;

        case DIVIDE:
            if (b == 0)
                throw new DivideByZeroException();

            result = a / b;
            break;

        case REMAINDER:
            if (b == 0)
                throw new DivideByZeroException();

            result = a % b;
            break;

        case POWER:
            if (a == 0 && b == 0)
                throw new ExpressionException("0**0 is undefined");

            result = (int) Math.pow(a, b);
            break;

        default:
            throw new ExpressionException("Unrecognized arithmetic type " + type);
        }

        return result;  // Rely on boxing
    }


    private static Object evalDateTimes(Type type, LocalDateTime a, LocalDateTime b) {
        if (a == null || b == null)
            return null;

        if (type == Type.SUBTRACT) {
            return Duration.between(b, a);
        }
        else {
            throw new IllegalArgumentException(
                "Cannot perform requested operation " + type +
                " on LocalDateTime objects");
        }
    }


    private static Object evalDates(Type type, LocalDate a, LocalDate b) {
        if (a == null || b == null)
            return null;

        if (type == Type.SUBTRACT) {
            return Period.between(b, a);
        }
        else {
            throw new IllegalArgumentException(
                "Cannot perform requested operation " + type +
                    " on LocalDate objects");
        }
    }


    private static Object evalTimes(Type type, LocalTime a, LocalTime b) {
        if (a == null || b == null)
            return null;

        if (type == Type.SUBTRACT) {
            return Duration.between(b, a);
        }
        else {
            throw new IllegalArgumentException(
                "Cannot perform requested operation " + type +
                    " on LocalTime objects");
        }
    }


    private static Object evalDateInterval(Type type, LocalDate a, TemporalAmount b) {
        if (a == null || b == null)
            return null;

        if (type == Type.ADD) {
            return a.plus(b);
        }
        else if (type == Type.SUBTRACT) {
            return a.minus(b);
        }
        else {
            throw new IllegalArgumentException(
                "Cannot perform requested operation " + type +
                    " on LocalDate and TemporalAmount objects");
        }
    }


    private static Object evalTimeInterval(Type type, LocalTime a,
                                           TemporalAmount b) {
        if (a == null || b == null)
            return null;

        if (type == Type.ADD) {
            return a.plus(b);
        }
        else if (type == Type.SUBTRACT) {
            return a.minus(b);
        }
        else {
            throw new IllegalArgumentException(
                "Cannot perform requested operation " + type +
                    " on LocalTime and TemporalAmount objects");
        }
    }


    private static Object evalDateTimeInterval(Type type, LocalDateTime a,
                                               TemporalAmount b) {
        if (a == null || b == null)
            return null;

        if (type == Type.ADD) {
            return a.plus(b);
        }
        else if (type == Type.SUBTRACT) {
            return a.minus(b);
        }
        else {
            throw new IllegalArgumentException(
                "Cannot perform requested operation " + type +
                    " on LocalDateTime and TemporalAmount objects");
        }
    }


    private static Object evalIntervalDate(Type type, TemporalAmount a,
                                           LocalDate b) {
        if (a == null || b == null)
            return null;

        if (type == Type.ADD) {
            return b.plus(a);
        }
        else {
            throw new IllegalArgumentException(
                "Cannot perform requested operation " + type +
                    " on TemporalAmount and LocalDate objects");
        }
    }


    private static Object evalIntervalTime(Type type, TemporalAmount a,
                                           LocalTime b) {
        if (a == null || b == null)
            return null;

        if (type == Type.ADD) {
            return b.plus(a);
        }
        else {
            throw new IllegalArgumentException(
                "Cannot perform requested operation " + type +
                    " on TemporalAmount and LocalTime objects");
        }
    }


    private static Object evalIntervalDateTime(Type type, TemporalAmount a,
                                               LocalDateTime b) {
        if (a == null || b == null)
            return null;

        if (type == Type.ADD) {
            return b.plus(a);
        }
        else {
            throw new IllegalArgumentException(
                "Cannot perform requested operation " + type +
                    " on TemporalAmount and LocalDateTime objects");
        }
    }


    @Override
    public Expression traverse(ExpressionProcessor p) {
        p.enter(this);
        leftExpr = leftExpr.traverse(p);
        rightExpr = rightExpr.traverse(p);
        return p.leave(this);
    }


    /**
     * Returns a string representation of this arithmetic expression and its
     * subexpressions, including parentheses where necessary to specify
     * precedence.
     */
    @Override
    public String toString() {
        // Convert all of the components into string representations.
        String leftStr = leftExpr.toString();
        String rightStr = rightExpr.toString();
        String opStr = " " + type.stringRep() + " ";

        // Figure out if I need parentheses around the subexpressions.

        if (type == Type.MULTIPLY || type == Type.DIVIDE || type == Type.REMAINDER) {
            if (leftExpr instanceof ArithmeticOperator) {
                ArithmeticOperator leftOp = (ArithmeticOperator) leftExpr;
                if (leftOp.type == Type.ADD || leftOp.type == Type.SUBTRACT)
                    leftStr = "(" + leftStr + ")";
            }

            if (rightExpr instanceof ArithmeticOperator) {
                ArithmeticOperator rightOp = (ArithmeticOperator) rightExpr;
                if (rightOp.type == Type.ADD || rightOp.type == Type.SUBTRACT)
                    rightStr = "(" + rightStr + ")";
            }
        }

        return leftStr + opStr + rightStr;
    }


    /**
     * Simplifies an arithmetic expression, computing as much of the expression
     * as possible.
     */
    @Override
    public Expression simplify() {
        leftExpr = leftExpr.simplify();
        rightExpr = rightExpr.simplify();

        if (!leftExpr.hasSymbols())
            leftExpr = new LiteralValue(leftExpr.evaluate());

        if (!rightExpr.hasSymbols())
            rightExpr = new LiteralValue(rightExpr.evaluate());

        if (!hasSymbols())
            return new LiteralValue(evaluate());

        return this;
    }


    /**
     * Checks if the argument is an expression with the same structure, but not
     * necesarily the same references.
     *
     * @param obj the object to which we are comparing
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ArithmeticOperator) {
            ArithmeticOperator other = (ArithmeticOperator) obj;
            return (type.equals(other.type) &&
                    leftExpr.equals(other.leftExpr) &&
                    rightExpr.equals(other.rightExpr));
        }
        return false;
    }


    /**
     * Computes the hashcode of an Expression.  This method is used to see if
     * two expressions CAN be equal.
     */
    @Override
    public int hashCode() {
        int hash = 7;

        hash = 31 * hash + type.hashCode();

        hash = 31 * hash + leftExpr.hashCode();
        hash = 31 * hash + rightExpr.hashCode();

        return hash;
    }


    /** Creates a copy of expression. */
    @Override
    protected Object clone() throws CloneNotSupportedException {
        ArithmeticOperator expr = (ArithmeticOperator)super.clone();

        // Type is immutable, copy it.
        expr.type = this.type;

        // Clone the subexpressions
        expr.leftExpr = (Expression) leftExpr.clone();
        expr.rightExpr = (Expression) rightExpr.clone();

        return expr;
    }
}
