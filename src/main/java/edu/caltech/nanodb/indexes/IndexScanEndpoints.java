package edu.caltech.nanodb.indexes;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.caltech.nanodb.expressions.Expression;
import edu.caltech.nanodb.expressions.LiteralValue;
import edu.caltech.nanodb.expressions.PredicateUtils;


/**
 * <p>
 * This class represents endpoint information relative to a specific
 * index.  This includes a predicate for identifying the starting tuple
 * from the index's contents, and a predicate for identifying where tuples
 * from the index stop satisfying the predicate.
 * </p>
 * <p>
 * There are also two arrays of values for constructing search-key tuples
 * for the initial lookup against the index.  For a hash index the
 * search-key will include a value for every column in the index; for an
 * ordered index the search-key value may be any prefix of the index's
 * search-key.
 * </p>
 */
public class IndexScanEndpoints {
    /**
     * This hash-set holds all conjuncts used to generate the starting and
     * ending predicates.
     */
    private HashSet<Expression> conjunctsUsed = new HashSet<>();


    /**
     * A predicate that can be used to identify the first tuple in an
     * index's tuple-sequence that satisfies a query's predicate.
     */
    private Expression startPredicate;

    /**
     * A predicate that can be used to identify the remaining tuples in an
     * index's tuple-sequence that satisfies a query's predicate.
     */
    private Expression endPredicate;

    /**
     * An array of values that can be used for performing an index lookup
     * to find the starting point in the tuple sequence.
     */
    private ArrayList<Object> startValues;

    /**
     * An array of values that can be used for performing an index lookup
     * to find the ending point in the tuple sequence.
     */
    private ArrayList<Object> endValues;


    /**
     * Initialze a new index-endpoints object that can be used to find the
     * starting and ending points in an index for retrieving tuples that
     * satisfy a specific predicate.
     *
     * @param ranges a list of {@code RangeEndpoints} objects describing
     *        specific columns that are referenced by a predicate
     */
    public IndexScanEndpoints(List<AnalyzedPredicate.RangeEndpoints> ranges) {
        if (ranges == null)
            throw new IllegalArgumentException("ranges cannot be null");

        if (ranges.isEmpty()) {
            throw new IllegalArgumentException(
                "ranges must contain at least one range");
        }

        ArrayList<Expression> startExprs = new ArrayList<>();
        ArrayList<Expression> endExprs = new ArrayList<>();
        startValues = new ArrayList<>();
        endValues = new ArrayList<>();

        int i = 0;
        boolean startHitNull = false;
        boolean endHitNull = false;
        for (AnalyzedPredicate.RangeEndpoints range : ranges) {
            if (range.startExpr != null) {
                startExprs.add(range.startExpr);

                if (!startHitNull) {
                    LiteralValue litVal =
                        (LiteralValue) range.startExpr.getRightExpression();
                    Object value = litVal.evaluate();
                    if (value != null)
                        startValues.add(value);
                    else
                        startHitNull = true;
                }
            }

            if (range.endExpr != null) {
                endExprs.add(range.endExpr);

                if (!endHitNull) {
                    LiteralValue litVal =
                        (LiteralValue) range.endExpr.getRightExpression();
                    Object value = litVal.evaluate();
                    if (value != null)
                        endValues.add(value);
                    else
                        endHitNull = true;
                }
            }

            i++;
        }

        // If either the start or the end predicate has no conjuncts, the
        // corresponding predicate will be set to null.
        startPredicate = PredicateUtils.makePredicate(startExprs);
        endPredicate = PredicateUtils.makePredicate(endExprs);

        // Record what conjuncts were used, if any.
        conjunctsUsed.addAll(startExprs);
        conjunctsUsed.addAll(endExprs);
    }


    public Expression getStartPredicate() {
        return startPredicate;
    }


    public Expression getEndPredicate() {
        return endPredicate;
    }


    public Set<Expression> getConjunctsUsed() {
        return Collections.unmodifiableSet(conjunctsUsed);
    }


    public List<Object> getStartValues() {
        return Collections.unmodifiableList(startValues);
    }


    public List<Object> getEndValues() {
        return Collections.unmodifiableList(endValues);
    }


    @Override
    public String toString() {
        return "IndexScanEndpoints[start:  " + startPredicate + ", end:  " +
            endPredicate + ", startValues:  " + startValues + "]";
    }
}
