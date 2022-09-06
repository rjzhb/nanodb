package edu.caltech.nanodb.expressions;


import edu.caltech.nanodb.relations.ColumnInfo;
import edu.caltech.nanodb.relations.Schema;
import edu.caltech.nanodb.relations.SchemaNameException;


/** This class implements unary negation. */
public class NegateOperator extends Expression {

    /** The expression being negated. */
    private Expression expr;


    public NegateOperator(Expression e) {
        if (e == null)
            throw new IllegalArgumentException("e cannot be null");

        expr = e;
    }


    @Override
    public ColumnInfo getColumnInfo(Schema schema) throws SchemaNameException {
        return expr.getColumnInfo(schema);
    }


    public Object evaluate(Environment env) throws ExpressionException {
        // Evaluate the subexpression.  If the value is NULL (represented by
        // Java null value) then return that.
        Object value = expr.evaluate(env);
        if (value == null)
            return null;

        // Now, flip its sign.
        value = ArithmeticOperator.evalObjects(
            ArithmeticOperator.Type.MULTIPLY, -1, value);

        return value;
    }


    @Override
    public Expression traverse(ExpressionProcessor p) {
        p.enter(this);
        expr = expr.traverse(p);
        return p.leave(this);
    }


    /**
     * Returns a string representation of this arithmetic expression and its
     * subexpressions, including parentheses where necessary to specify
     * precedence.
     */
    @Override
    public String toString() {
        // Convert the subexpression into a string representation.
        String str = expr.toString();

        // Figure out if I need parentheses around the subexpression.
        boolean parens = true;
        if (expr instanceof ColumnValue || expr instanceof LiteralValue ||
            expr instanceof FunctionCall) {
            parens = false;
        }

        if (parens)
            str = '(' + str + ')';

        return '-' + str;
    }


    /**
     * Simplifies an arithmetic expression, computing as much of the expression
     * as possible.
     */
    public Expression simplify() {
        expr = expr.simplify();
        if (!expr.hasSymbols())
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
        if (obj instanceof NegateOperator) {
            NegateOperator other = (NegateOperator) obj;
            return expr.equals(other.expr);
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

        hash = 31 * hash + expr.hashCode();

        return hash;
    }


    /** Creates a copy of expression. */
    @Override
    protected Object clone() throws CloneNotSupportedException {
        NegateOperator copy = (NegateOperator) super.clone();

        // Clone the subexpression
        copy.expr = (Expression) expr.clone();

        return copy;
    }
}
