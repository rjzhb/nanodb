package edu.caltech.test.nanodb.expressions;


import java.util.ArrayList;

import org.testng.annotations.Test;

import edu.caltech.nanodb.expressions.ColumnName;
import edu.caltech.nanodb.expressions.ColumnValue;
import edu.caltech.nanodb.expressions.OrderByExpression;
import edu.caltech.nanodb.expressions.TupleComparator;
import edu.caltech.nanodb.expressions.TupleLiteral;
import edu.caltech.nanodb.relations.ColumnInfo;
import edu.caltech.nanodb.relations.ColumnType;
import edu.caltech.nanodb.relations.Schema;


@Test(groups={"framework"})
public class TestTupleComparator {
    public void testAreTuplesEqual() {
        // Simple scenarios without type coercion.

        TupleLiteral t1 = new TupleLiteral(1, "a");
        TupleLiteral t2 = new TupleLiteral(1, "b");
        TupleLiteral t3 = new TupleLiteral(2, "a");
        TupleLiteral t4 = new TupleLiteral(1, null);
        TupleLiteral t5 = new TupleLiteral(null, "a");
        TupleLiteral t6 = new TupleLiteral(null, null);

        TupleLiteral t7 = new TupleLiteral(1, "a");

        assert !TupleComparator.areTuplesEqual(t1, t2);
        assert !TupleComparator.areTuplesEqual(t1, t3);
        assert !TupleComparator.areTuplesEqual(t1, t4);
        assert !TupleComparator.areTuplesEqual(t1, t5);
        assert !TupleComparator.areTuplesEqual(t1, t6);
        assert !TupleComparator.areTuplesEqual(t1, t2);

        assert TupleComparator.areTuplesEqual(t1, t7);

        // Scenarios with type coercion

        TupleLiteral t8 = new TupleLiteral(1L, "b");
        assert !TupleComparator.areTuplesEqual(t1, t8);
        assert TupleComparator.areTuplesEqual(t2, t8);
        assert TupleComparator.areTuplesEqual(t8, t2);

        // Tuples of different lengths

        TupleLiteral tz = new TupleLiteral();
        TupleLiteral ts = new TupleLiteral(1);
        TupleLiteral tl = new TupleLiteral(1, "a", 3, 4, 5);

        assert !TupleComparator.areTuplesEqual(t1, tz);
        assert !TupleComparator.areTuplesEqual(tz, t1);

        assert !TupleComparator.areTuplesEqual(t1, ts);
        assert !TupleComparator.areTuplesEqual(ts, t1);

        assert !TupleComparator.areTuplesEqual(t1, tl);
        assert !TupleComparator.areTuplesEqual(tl, t1);
    }


    public void testAreTuplesEqualExceptions() {
        TupleLiteral t = new TupleLiteral(1, "a");

        try {
            TupleComparator.areTuplesEqual(t, null);
            assert false;
        } catch (IllegalArgumentException e) {
            // Pass!
        }

        try {
            TupleComparator.areTuplesEqual(null, t);
            assert false;
        } catch (IllegalArgumentException e) {
            // Pass!
        }

        try {
            TupleComparator.areTuplesEqual(null, null);
            assert false;
        } catch (IllegalArgumentException e) {
            // Pass!
        }
    }


    /**
     * Exercise the {@link TupleComparator#compare} method with various inputs.
     */
    public void testCompare() {
        Schema s = new Schema(
            new ColumnInfo("a", ColumnType.INTEGER),
            new ColumnInfo("b", ColumnType.INTEGER),
            new ColumnInfo("c", ColumnType.INTEGER)
        );

        ArrayList<OrderByExpression> order = new ArrayList<>();
        order.add(new OrderByExpression(new ColumnValue(new ColumnName("b")), true));
        order.add(new OrderByExpression(new ColumnValue(new ColumnName("a")), false));

        TupleComparator cmp = new TupleComparator(s, order);

        TupleLiteral t1 = new TupleLiteral(3, 5, 4);
        TupleLiteral t2 = new TupleLiteral(3, 5, 6);

        // Should compare equal, since these tuples are the same in the order-
        // by columns, even though they are different in the third column.
        assert cmp.compare(t1, t2) == 0;
        assert cmp.compare(t2, t1) == 0;

        TupleLiteral t3 = new TupleLiteral(3, 4, 7);  // t1 > t3 in col b
        TupleLiteral t4 = new TupleLiteral(3, 7, 1);  // t1 < t4 in col b

        assert cmp.compare(t1, t3) > 0;
        assert cmp.compare(t3, t1) < 0;

        assert cmp.compare(t1, t4) < 0;
        assert cmp.compare(t4, t1) > 0;

        TupleLiteral t3b = new TupleLiteral(8, 4, 7);  // t1 > t3b in col b
        TupleLiteral t4b = new TupleLiteral(1, 7, 1);  // t1 < t4b in col b

        assert cmp.compare(t1, t3b) > 0;
        assert cmp.compare(t3b, t1) < 0;

        assert cmp.compare(t1, t4b) < 0;
        assert cmp.compare(t4b, t1) > 0;

        TupleLiteral t5 = new TupleLiteral(8, 5, 11);  // t1 > t5 in col a
        TupleLiteral t6 = new TupleLiteral(-4, 5, 0);  // t1 < t6 in col a

        assert cmp.compare(t1, t5) > 0;
        assert cmp.compare(t5, t1) < 0;

        assert cmp.compare(t1, t6) < 0;
        assert cmp.compare(t6, t1) > 0;

        TupleLiteral t7 = new TupleLiteral(null, 5, 11);  // t1 < t7 in col a
        TupleLiteral t8 = new TupleLiteral(3, null, 0);   // t1 > t8 in col b

        assert cmp.compare(t1, t7) < 0;
        assert cmp.compare(t7, t1) > 0;

        assert cmp.compare(t1, t8) > 0;
        assert cmp.compare(t8, t1) < 0;

        TupleLiteral t9 = new TupleLiteral(null, 5, 6);  // t7 == t9

        assert cmp.compare(t7, t9) == 0;
        assert cmp.compare(t9, t7) == 0;
    }


    public void testCompareExceptions() {
        Schema s = new Schema();
        ArrayList<OrderByExpression> order = new ArrayList<>();

        try {
            new TupleComparator(null, order);
            assert false;
        } catch (IllegalArgumentException e) {
            // Pass!
        }

        try {
            new TupleComparator(s, null);
            assert false;
        } catch (IllegalArgumentException e) {
            // Pass!
        }
    }


    public void testCompareTuples() {
        TupleLiteral t1 = new TupleLiteral(1, 2, 3);
        TupleLiteral t2 = new TupleLiteral(2, 2, 3);
        TupleLiteral t3 = new TupleLiteral(1, 1, 3);
        TupleLiteral t4 = new TupleLiteral(1, 2, 2);
        TupleLiteral t5 = new TupleLiteral(1, 2, 4);
        TupleLiteral t6 = new TupleLiteral(null, 2, 3);
        TupleLiteral t7 = new TupleLiteral(1, null, 3);
        TupleLiteral t8 = new TupleLiteral(1, 2, null);

        assert TupleComparator.compareTuples(t1, t1) == 0;
        assert TupleComparator.compareTuples(t2, t2) == 0;

        assert TupleComparator.compareTuples(t1, t2) < 0;
        assert TupleComparator.compareTuples(t2, t1) > 0;

        assert TupleComparator.compareTuples(t1, t3) > 0;
        assert TupleComparator.compareTuples(t3, t1) < 0;

        assert TupleComparator.compareTuples(t1, t4) > 0;
        assert TupleComparator.compareTuples(t4, t1) < 0;

        assert TupleComparator.compareTuples(t1, t5) < 0;
        assert TupleComparator.compareTuples(t5, t1) > 0;

        assert TupleComparator.compareTuples(t1, t6) > 0;
        assert TupleComparator.compareTuples(t6, t1) < 0;

        assert TupleComparator.compareTuples(t1, t7) > 0;
        assert TupleComparator.compareTuples(t7, t1) < 0;

        assert TupleComparator.compareTuples(t1, t8) > 0;
        assert TupleComparator.compareTuples(t8, t1) < 0;

        assert TupleComparator.compareTuples(t6, t6) == 0;
        assert TupleComparator.compareTuples(t7, t7) == 0;
        assert TupleComparator.compareTuples(t8, t8) == 0;
    }


    public void testCompareTuplesExceptions() {
        TupleLiteral t1 = new TupleLiteral(1, 2, 3);
        TupleLiteral t2 = new TupleLiteral(4, 5);

        try {
            TupleComparator.compareTuples(t1, null);
            assert false;
        } catch (IllegalArgumentException e) {
            // Pass!
        }

        try {
            TupleComparator.compareTuples(null, t1);
            assert false;
        }
        catch (IllegalArgumentException e) {
            // Pass!
        }

        try {
            // Different arity tuples should cause an exception.
            TupleComparator.compareTuples(t1, t2);
            assert false;
        }
        catch (IllegalArgumentException e) {
            // Pass!
        }
    }


    public void testComparePartialTuplesSameLengths() {
        TupleLiteral t1 = new TupleLiteral(1, 2, 3);
        TupleLiteral t2 = new TupleLiteral(2, 2, 3);
        TupleLiteral t3 = new TupleLiteral(1, 1, 3);
        TupleLiteral t4 = new TupleLiteral(1, 2, 2);
        TupleLiteral t5 = new TupleLiteral(1, 2, 4);
        TupleLiteral t6 = new TupleLiteral(null, 2, 3);
        TupleLiteral t7 = new TupleLiteral(1, null, 3);
        TupleLiteral t8 = new TupleLiteral(1, 2, null);

        assert TupleComparator.comparePartialTuples(t1, t1) == 0;
        assert TupleComparator.comparePartialTuples(t2, t2) == 0;

        assert TupleComparator.comparePartialTuples(t1, t2) < 0;
        assert TupleComparator.comparePartialTuples(t2, t1) > 0;

        assert TupleComparator.comparePartialTuples(t1, t3) > 0;
        assert TupleComparator.comparePartialTuples(t3, t1) < 0;

        assert TupleComparator.comparePartialTuples(t1, t4) > 0;
        assert TupleComparator.comparePartialTuples(t4, t1) < 0;

        assert TupleComparator.comparePartialTuples(t1, t5) < 0;
        assert TupleComparator.comparePartialTuples(t5, t1) > 0;

        assert TupleComparator.comparePartialTuples(t1, t6) > 0;
        assert TupleComparator.comparePartialTuples(t6, t1) < 0;

        assert TupleComparator.comparePartialTuples(t1, t7) > 0;
        assert TupleComparator.comparePartialTuples(t7, t1) < 0;

        assert TupleComparator.comparePartialTuples(t1, t8) > 0;
        assert TupleComparator.comparePartialTuples(t8, t1) < 0;

        assert TupleComparator.comparePartialTuples(t6, t6) == 0;
        assert TupleComparator.comparePartialTuples(t7, t7) == 0;
        assert TupleComparator.comparePartialTuples(t8, t8) == 0;
    }


    public void testComparePartialTuplesIgnoreLength() {
        TupleLiteral t0 = new TupleLiteral();
        TupleLiteral t1 = new TupleLiteral(new Object[]{null});
        TupleLiteral t2 = new TupleLiteral(1);
        TupleLiteral t3 = new TupleLiteral(1, 2);
        TupleLiteral t5 = new TupleLiteral(1, 2, 2);
        TupleLiteral t7 = new TupleLiteral(1, null, 3);
        TupleLiteral t8 = new TupleLiteral(1, 2, null);

        assert TupleComparator.comparePartialTuples(t0, t0) == 0;
        assert TupleComparator.comparePartialTuples(t2, t2) == 0;

        // In "IGNORE_LENGTH" mode, empty tuple always compares equal to
        // all other tuples.

        assert TupleComparator.comparePartialTuples(t0, t2) == 0;
        assert TupleComparator.comparePartialTuples(t2, t0) == 0;

        assert TupleComparator.comparePartialTuples(t0, t1) == 0;
        assert TupleComparator.comparePartialTuples(t1, t0) == 0;

        assert TupleComparator.comparePartialTuples(t0, t5) == 0;
        assert TupleComparator.comparePartialTuples(t5, t0) == 0;

        // Compare tuples of different lengths - only the common columns
        // should be used.

        assert TupleComparator.comparePartialTuples(t2, t3) == 0;
        assert TupleComparator.comparePartialTuples(t3, t2) == 0;

        assert TupleComparator.comparePartialTuples(t3, t5) == 0;
        assert TupleComparator.comparePartialTuples(t5, t3) == 0;

        assert TupleComparator.comparePartialTuples(t2, t5) == 0;
        assert TupleComparator.comparePartialTuples(t5, t2) == 0;

        assert TupleComparator.comparePartialTuples(t2, t7) == 0;
        assert TupleComparator.comparePartialTuples(t7, t2) == 0;

        assert TupleComparator.comparePartialTuples(t3, t8) == 0;
        assert TupleComparator.comparePartialTuples(t8, t3) == 0;
    }


    public void testComparePartialTuplesShorterIsLess() {
        TupleLiteral t0 = new TupleLiteral();
        TupleLiteral t1 = new TupleLiteral(new Object[]{null});
        TupleLiteral t2 = new TupleLiteral(1);
        TupleLiteral t3 = new TupleLiteral(1, 2);
        TupleLiteral t5 = new TupleLiteral(1, 2, 2);
        TupleLiteral t7 = new TupleLiteral(1, null, 3);
        TupleLiteral t8 = new TupleLiteral(1, 2, null);

        // Just for less typing, oi!
        final TupleComparator.CompareMode mode =
            TupleComparator.CompareMode.SHORTER_IS_LESS;

        assert TupleComparator.comparePartialTuples(t0, t0, mode) == 0;
        assert TupleComparator.comparePartialTuples(t2, t2, mode) == 0;

        // Empty tuple should always compare less than all other tuples.

        assert TupleComparator.comparePartialTuples(t0, t2, mode) < 0;
        assert TupleComparator.comparePartialTuples(t2, t0, mode) > 0;

        assert TupleComparator.comparePartialTuples(t0, t1, mode) < 0;
        assert TupleComparator.comparePartialTuples(t1, t0, mode) > 0;

        assert TupleComparator.comparePartialTuples(t0, t5, mode) < 0;
        assert TupleComparator.comparePartialTuples(t5, t0, mode) > 0;

        // Compare tuples of different lengths - the common columns should be
        // used, but if all common columns are equal, the shorter tuple is
        // still considered to be "less than" the longer tuple.

        assert TupleComparator.comparePartialTuples(t2, t3, mode) < 0;
        assert TupleComparator.comparePartialTuples(t3, t2, mode) > 0;

        assert TupleComparator.comparePartialTuples(t3, t5, mode) < 0;
        assert TupleComparator.comparePartialTuples(t5, t3, mode) > 0;

        assert TupleComparator.comparePartialTuples(t2, t5, mode) < 0;
        assert TupleComparator.comparePartialTuples(t5, t2, mode) > 0;

        assert TupleComparator.comparePartialTuples(t2, t7, mode) < 0;
        assert TupleComparator.comparePartialTuples(t7, t2, mode) > 0;

        assert TupleComparator.comparePartialTuples(t3, t8, mode) < 0;
        assert TupleComparator.comparePartialTuples(t8, t3, mode) > 0;
    }


    public void testComparePartialTuplesExceptions() {
        TupleLiteral t = new TupleLiteral(1, 2, 3);

        try {
            TupleComparator.comparePartialTuples(t, null);
            assert false;
        } catch (IllegalArgumentException e) {
            // Pass!
        }

        try {
            TupleComparator.comparePartialTuples(null, t);
            assert false;
        }
        catch (IllegalArgumentException e) {
            // Pass!
        }
    }
}
