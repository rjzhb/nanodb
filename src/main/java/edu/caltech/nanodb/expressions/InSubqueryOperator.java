package edu.caltech.nanodb.expressions;


import java.util.ArrayList;
import java.util.List;

import edu.caltech.nanodb.queryast.SelectClause;
import edu.caltech.nanodb.relations.ColumnInfo;
import edu.caltech.nanodb.relations.ColumnType;
import edu.caltech.nanodb.relations.Schema;
import edu.caltech.nanodb.relations.SchemaNameException;
import edu.caltech.nanodb.relations.Tuple;


/**
 * This class implements the <tt>expr IN (subquery)</tt> operator.  This
 * operation may be optimized out of a query, but if it is not, it can still
 * be evaluated although it will be slow.
 */
public class InSubqueryOperator extends SubqueryOperator {
    /**
     * The list of expressions to check against the set on the righthand side of the
     * <tt>IN</tt> operator.
     */
    private ArrayList<Expression> exprList = new ArrayList<>();


    /**
     * If this is false, the operator computes <tt>expr IN (subquery)</tt>;
     * if true, the operator computes <tt>expr NOT IN (subquery)</tt>.
     */
    private boolean invert = false;


    public InSubqueryOperator(Expression expr, SelectClause subquery) {
        if (expr == null)
            throw new IllegalArgumentException("expr cannot be null");

        if (subquery == null)
            throw new IllegalArgumentException("subquery cannot be null");

        exprList.add(expr);
        this.subquery = subquery;
    }


    public InSubqueryOperator(List<Expression> exprList, SelectClause subquery) {
        if (exprList == null)
            throw new IllegalArgumentException("exprList cannot be null");

        if (exprList.isEmpty())
            throw new IllegalArgumentException("exprList cannot be empty");

        if (subquery == null)
            throw new IllegalArgumentException("subquery cannot be null");

        this.exprList.addAll(exprList);
        this.subquery = subquery;
    }


    public void setInvert(boolean invert) {
        this.invert = invert;
    }


    public ColumnInfo getColumnInfo(Schema schema) throws SchemaNameException {
        // Comparisons always return Boolean values, so just pass a Boolean
        // value in to the TypeConverter to get out the corresponding SQL type.
        ColumnType colType =
            new ColumnType(TypeConverter.getSQLType(Boolean.FALSE));
        return new ColumnInfo(colType);
    }


    /**
     * Evaluates this comparison expression and returns either
     * {@link java.lang.Boolean#TRUE} or {@link java.lang.Boolean#FALSE}.  If
     * either the left-hand or right-hand expression evaluates to
     * <code>null</code> (representing the SQL <tt>NULL</tt> value), then the
     * expression's result is always <code>FALSE</code>.
     *
     * @design (Donnie) We have to suppress "unchecked operation" warnings on
     *         this code, since {@link Comparable} is a generic (and thus allows
     *         us to specify the type of object being compared), but we want to
     *         use it without specifying any types.
     */
    @SuppressWarnings("unchecked")
    public Object evaluate(Environment env) throws ExpressionException {
        if (subqueryPlan == null)
            throw new IllegalStateException("No execution plan for subquery");

        // Compute the values that we need to compare to, into a tuple.  Then
        // we can use the tuple-comparator to see if the subquery produces the
        // same tuple-values.
        TupleLiteral valueTup = new TupleLiteral();
        for (Expression expr : exprList) {
            // TODO:  Return NULL if one of the values is NULL?
            valueTup.addValue(expr.evaluate(env));
        }

        subqueryPlan.initialize();
        while (true) {
            Tuple subqueryTup = subqueryPlan.getNextTuple();
            if (subqueryTup == null)
                break;

            if (TupleComparator.areTuplesEqual(valueTup, subqueryTup))
                return invert ? false : true;
        }

        return invert ? true : false;
    }


    @Override
    public Expression traverse(ExpressionProcessor p) {
        p.enter(this);

        for (int i = 0; i < exprList.size(); i++)
            exprList.set(i, exprList.get(i).traverse(p));

        // We do not traverse the subquery; it is treated as a "black box"
        // by the expression-traversal mechanism.

        return p.leave(this);
    }


    /**
     * Returns a string representation of this comparison expression and its
     * subexpressions.
     */
    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();

        // Convert all of the components into string representations.

        if (exprList.size() == 1) {
            buf.append(exprList.get(0).toString());
        }
        else {
            char ch = '(';
            for (Expression expr : exprList) {
                buf.append(ch);
                ch = ',';
                buf.append(expr.toString());
            }
            buf.append(')');
        }

        if (invert)
            buf.append(" NOT");

        buf.append(" IN (");
        buf.append(subquery.toString());
        buf.append(')');

        return buf.toString();
    }


    /**
     * Checks if the argument is an expression with the same structure, but not
     * necessarily the same references.
     *
     * @param obj the object to which we are comparing
     */
    @Override
    public boolean equals(Object obj) {

        if (obj instanceof InSubqueryOperator) {
            InSubqueryOperator other = (InSubqueryOperator) obj;
            return exprList.equals(other.exprList) &&
                   subquery.equals(other.subquery);
        }

        return false;
    }


    @Override
    public int hashCode() {
        int hash = 7;

        for (Expression expr : exprList)
            hash = 31 * hash + expr.hashCode();

        hash = 31 * hash + subquery.hashCode();
        return hash;
    }


    /**
     * Creates a copy of expression.  This method is used by the
     * {@link Expression#duplicate} method to make a deep copy of an expression
     * tree.
     */
    @Override
    @SuppressWarnings("unchecked")
    protected Object clone() throws CloneNotSupportedException {
        InSubqueryOperator op = (InSubqueryOperator) super.clone();

        // Clone the subexpressions.
        op.exprList = (ArrayList<Expression>) exprList.clone();

        // Don't clone the subquery, since subqueries currently aren't cloneable.

        return op;
    }
}
