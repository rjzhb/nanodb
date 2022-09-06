package edu.caltech.nanodb.indexes;


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import edu.caltech.nanodb.expressions.ColumnName;
import edu.caltech.nanodb.expressions.ColumnValue;
import edu.caltech.nanodb.expressions.CompareOperator;
import edu.caltech.nanodb.expressions.Expression;
import edu.caltech.nanodb.expressions.LiteralValue;
import edu.caltech.nanodb.expressions.PredicateUtils;
import edu.caltech.nanodb.relations.ColumnInfo;
import edu.caltech.nanodb.relations.IndexColumnRefs;
import edu.caltech.nanodb.relations.Schema;


/**
 * <p>
 * This class takes a predicate or a collection of conjuncts and analyzes it
 * into a "range" component and an "other" component.  The "range" component
 * consists of "column op value" comparisons (where op is =, !=, >, >=, < or
 * <=); these conjuncts specify ranges on specific columns, and can be used
 * to choose indexes for query planning.  The "other" component is all other
 * conjuncts, including "column op column" conjuncts, other functions and
 * operations (such as string-matching operations, <tt>IS NULL</tt> tests,
 * etc.), nested disjunctions, and so forth.
 * </p>
 * <p>
 * This class expects that expressions have been both normalized and
 * simplified.  It is designed to work with conjunctive selection predicates
 * (i.e. a series of conditions ANDed together), and will not be particularly
 * effective with other kinds of predicates.
 * </p>
 */
public class AnalyzedPredicate {

    /**
     * Records the starting and ending points of a range component of a query
     * predicate.  Only conjuncts of the form "column op value" are
     * represented, and it is possible that either the start or the end is
     * missing if there is no corresponding conjunct.
     */
    public static class RangeEndpoints {
        /**
         * A starting endpoint on an attribute.  The value will be a
         * comparison with a =, >= or > operator.
         */
        public CompareOperator startExpr;

        /**
         * An ending endpoint on an attribute.  The value will be a comparison
         * with a =, <= or < operator.
         */
        public CompareOperator endExpr;


        private void addCompare(CompareOperator cmp) {
            if (cmp == null)
                throw new IllegalArgumentException("cmp cannot be null");

            if (!(cmp.getLeftExpression() instanceof ColumnValue)) {
                throw new IllegalArgumentException(
                    "cmp LHS must be a ColumnValue");
            }

            if (!(cmp.getRightExpression() instanceof LiteralValue)) {
                throw new IllegalArgumentException(
                    "cmp RHS must be a LiteralValue");
            }

            CompareOperator.Type type = cmp.getType();

            // TODO(donnie):  What if the startExpr or endExpr is already set?
            //                Need to see if the condition is impossible to
            //                satisfy.

            switch (type) {
                case EQUALS:
                    startExpr = cmp;
                    endExpr = cmp;
                    break;

                case GREATER_OR_EQUAL:
                case GREATER_THAN:
                    startExpr = cmp;
                    break;

                case LESS_OR_EQUAL:
                case LESS_THAN:
                    endExpr = cmp;
                    break;

                default:
                    break;  // Do nothing.
            }
        }

        CompareOperator getStartCondition() {
            return startExpr;
        }

        CompareOperator getEndCondition() {
            return endExpr;
        }

        Object getStartValue() {
            Object result = null;
            if (startExpr != null)
                result = startExpr.getRightExpression().evaluate();

            return result;
        }

        Object getEndValue() {
            Object result = null;
            if (endExpr != null)
                result = endExpr.getRightExpression().evaluate();

            return result;
        }
    }


    /**
     * These conjuncts are specifically of the form "column op value", and
     * therefore may be useful .
     */
    HashMap<ColumnName, RangeEndpoints> rangeConjuncts = new HashMap<>();


    /**
     * Other conjuncts in the predicate that are not of the form
     * "column op value".
     */
    ArrayList<Expression> otherConjuncts = new ArrayList<>();


    public AnalyzedPredicate(Expression predicate) {
        addPredicate(predicate);
    }


    public AnalyzedPredicate(Collection<Expression> conjuncts) {
        addConjuncts(conjuncts);
    }


    private void addPredicate(Expression predicate) {
        HashSet<Expression> conjuncts = new HashSet<>();
        PredicateUtils.collectConjuncts(predicate, conjuncts);
        addConjuncts(conjuncts);
    }


    private void addConjuncts(Collection<Expression> conjuncts) {
        for (Expression e : conjuncts)
            addConjunct(e);
    }


    private void addConjunct(Expression conjunct) {
        if (conjunct instanceof CompareOperator) {
            CompareOperator cmp = (CompareOperator) conjunct;
            if (cmp.getLeftExpression() instanceof ColumnValue &&
                cmp.getRightExpression() instanceof LiteralValue) {

                ColumnValue lhs = (ColumnValue) cmp.getLeftExpression();
                RangeEndpoints range = rangeConjuncts.computeIfAbsent(
                    lhs.getColumnName(), k -> new RangeEndpoints());

                range.addCompare(cmp);
                return;
            }
        }
        otherConjuncts.add(conjunct);
    }


    public IndexScanEndpoints canUseIndex(Schema schema, IndexType indexType,
                                          IndexColumnRefs colRefs) {
        ArrayList<RangeEndpoints> ranges = new ArrayList<>();

        for (int iCol : colRefs.getCols()) {
            ColumnInfo colInfo = schema.getColumnInfo(iCol);
            ColumnName colName = colInfo.getColumnName();

            // Are there range endpoints for this column?
            RangeEndpoints colEndpoints = rangeConjuncts.get(colName);
            if (colEndpoints == null) {
                // No range-endpoints for this column.

                // Hash indexes require all columns to be present.
                if (indexType == IndexType.HASHED_INDEX)
                    return null;

                // If we are here then the index is a sequential index, and
                // we can function with some prefix of the index.  Make sure
                // we have found at least one range so far.
                assert indexType == IndexType.ORDERED_INDEX;
                if (ranges.isEmpty()) {
                    // We don't have any ranges!  Can't use this index.
                    return null;
                }

                // If we are here then the index is a sequential index and we
                // have some prefix of the index.
                break;
            }
            else {
                // We have range endpoints for this column.

                // Hash indexes require an equality test on all columns.  Note
                // that if the start-expression is EQUALS then the
                // end-expression will also be EQUALS, since an EQUALS test
                // reduces the range to a single value.
                if (indexType == IndexType.HASHED_INDEX) {
                    if (colEndpoints.startExpr == null ||
                        colEndpoints.startExpr.getType() !=
                            CompareOperator.Type.EQUALS) {
                        // This comparison is either absent or not EQUALS.
                        // Can't use the index.
                        return null;
                    }
                }

                // If we got here, we can use this range with the index.
                ranges.add(colEndpoints);
            }
        }

        // If we got here, we have a series of one or more range tests that
        // we can use against the index!  Pack it up and send it back to the
        // caller.
        assert !ranges.isEmpty();
        return new IndexScanEndpoints(ranges);
    }
}
