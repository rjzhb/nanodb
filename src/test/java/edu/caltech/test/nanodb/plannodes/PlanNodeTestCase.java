package edu.caltech.test.nanodb.plannodes;


import edu.caltech.nanodb.expressions.TupleComparator;
import edu.caltech.nanodb.relations.Tuple;
import org.testng.annotations.Test;

import edu.caltech.nanodb.expressions.TupleLiteral;
import edu.caltech.nanodb.plannodes.PlanNode;


@Test(enabled=false)
public class PlanNodeTestCase {



    public void checkOrderedResults(TupleLiteral[] expected, PlanNode plan) {
        boolean success = true;
        StringBuilder buf = new StringBuilder();

        plan.prepare();
        plan.initialize();

        // Consume tuples produced by the plan, checking each one against the
        // expected results.
        int count = 0;
        while (true) {
            Tuple t = plan.getNextTuple();
            if (t == null)
                break;

            if (count < expected.length) {
                if (!TupleComparator.areTuplesEqual(expected[count], t)) {
                    success = false;
                    System.out.printf(
                        "Tuple %d:  Expected tuple %s, got %s instead",
                        count, expected[count], t);
                }
            }
            else {
                // Plan node produced an unexpected tuple.
                success = false;
                System.out.printf(
                    "Tuple %d:  Received unexpected tuple %s", count, t);
            }

            count++;
        }

        if (count != expected.length) {
            success = false;
            System.out.printf("Plan node produced %d tuples, but %d were expected",
                count, expected.length);
        }

        assert success;
    }
}
