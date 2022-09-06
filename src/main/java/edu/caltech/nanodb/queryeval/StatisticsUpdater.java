package edu.caltech.nanodb.queryeval;


import java.util.ArrayList;
import java.util.List;

import edu.caltech.nanodb.expressions.BooleanOperator;
import edu.caltech.nanodb.expressions.ColumnName;
import edu.caltech.nanodb.expressions.ColumnValue;
import edu.caltech.nanodb.expressions.CompareOperator;
import edu.caltech.nanodb.expressions.Expression;
import edu.caltech.nanodb.expressions.LiteralValue;
import edu.caltech.nanodb.expressions.TypeConverter;
import edu.caltech.nanodb.relations.ColumnInfo;
import edu.caltech.nanodb.relations.Schema;


/**
 * This helper class provides basic functionality for updating column
 * statistics based on a selection predicate.  The supported predicates are
 * very limited, because the problem becomes very grungy for arbitrary
 * predicates.  Supported predicates include:
 * <ul>
 *   <li>A single comparison between a column and a value</li>
 *   <li>
 *     A Boolean <tt>AND</tt> expression, although only the column-value
 *     comparisons within the expression will be used to update statistics
 *   </li>
 * </ul>
 */
public class StatisticsUpdater {
    /** This class should not be instantiated. */
    private StatisticsUpdater() {
        throw new IllegalArgumentException(
            "This class should not be instantiated.");
    }


    /**
     * <p>
     * This static helper takes a selection predicate, a schema the predicate
     * is evaluated against, and input column-statistics, and produces output
     * column-statistics that reflect the input arguments.  The output is a
     * deep-copy of the input, so that no accidental side-effects occur.
     * </p>
     * <p>
     * Only a very limited number of selection predicates are supported, all
     * centering around conjunctive selection based on "COLUMN op VALUE"
     * components.  Other kinds of predicates will not be used to update the
     * statistics.
     * </p>
     * <p>
     * If a predicate includes a column-reference that is not part of the
     * input schema, then that part of the predicate will be ignored.
     * </p>
     *
     * @param expr the selection predicate
     * @param schema the schema that the selection predicate is evaluated
     *        against
     * @param inputStats the column statistics of the input tuple-sequence
     *        that will be filtered by the selection predicate
     *
     * @return estimated column statistics
     */
    public static ArrayList<ColumnStats> updateStats(Expression expr,
        Schema schema, List<ColumnStats> inputStats) {
        // Make a deep copy of the incoming list so we can mutate it safely.
        ArrayList<ColumnStats> outputStats = new ArrayList<>();
        for (ColumnStats stat : inputStats)
            outputStats.add(new ColumnStats(stat));

        if (expr instanceof BooleanOperator) {
            // The predicate includes a Boolean operation.  If it's an AND
            // operation (conjunctive selection) then look at each conjunct
            // in sequence.
            BooleanOperator boolOp = (BooleanOperator) expr;
            if (boolOp.getType() == BooleanOperator.Type.AND_EXPR) {
                for (int i = 0; i < boolOp.getNumTerms(); i++) {
                    Expression e = boolOp.getTerm(i);
                    if (e instanceof CompareOperator) {
                        // This conjunct appears to be a comparison.  Unpack
                        // it and try to update the statistics based on the
                        // comparison.
                        updateCompareStats((CompareOperator) e, schema,
                            outputStats);
                    }
                }
            }
        }
        else if (expr instanceof CompareOperator) {
            // The predicate appears to be a comparison.  Unpack it and try to
            // update the statistics based on the comparison.
            updateCompareStats((CompareOperator) expr, schema, outputStats);
        }
        return outputStats;
    }


    /**
     * Try to update the column-statistics based on the passed-in comparison
     * operation.  Updates will only occur if the comparison is of the form
     * "COLUMN = VALUE".
     *
     * @param comp The comparison operation to consider
     * @param schema The schema that the operation is evaluated against
     * @param stats the statistics to update based on the comparison
     */
    private static void updateCompareStats(CompareOperator comp,
                                           Schema schema,
                                           List<ColumnStats> stats) {
        // Move the comparison into a normalized order so that it's easier to
        // write the logic for analysis.  Specifically, this will ensure that
        // if we are comparing a column and a value, the column will always be
        // on the left and the value will always be on the right.
        comp.normalize();

        Expression left = comp.getLeftExpression();
        Expression right = comp.getRightExpression();

        if (left instanceof ColumnValue && right instanceof LiteralValue) {
            // Resolve the column name against the schema, so we can look up
            // the corresponding statistics.  If the column name is unknown,
            // just return.
            ColumnName colName = ((ColumnValue) left).getColumnName();
            int colIdx = schema.getColumnIndex(colName);
            if (colIdx == -1)
                return;

            // Get the column's type from the schema, so we can coerce the
            // value in the comparison to the same type.
            ColumnInfo colInfo = schema.getColumnInfo(colIdx);
            Object value = right.evaluate();
            value = TypeConverter.coerceTo(value, colInfo.getType());

            ColumnStats stat = stats.get(colIdx);

            /* TODO:  IMPLEMENT THE REST!
             *
             * NOTE:  In Java, you can switch on an enumerated type, but you
             * do not specify the fully qualified name.  Thus, you will end up
             * with something like this:
             *
             * switch (comp.getType()) {
             *     case EQUALS:
             *         ...
             *         break;
             *
             *     case NOT_EQUALS:
             *         ...
             *         break;
             *
             *     ... // etc.
             * }
             *
             * If you need to declare local variables within a switch-block,
             * you can always declare a nested block, like this:
             *
             *     case SOMECASE: {
             *         int i = ...;
             *         ... // etc.
             *         break;
             *     }
             *
             * You may find the SelectivityEstimator.computeRatio() function
             * to be useful for some of the operations in this code.
             */
        }
    }
}
