package edu.caltech.test.nanodb.expressions;


import java.util.ArrayList;

import edu.caltech.nanodb.expressions.Expression;
import edu.caltech.nanodb.expressions.InValuesOperator;
import edu.caltech.nanodb.expressions.LiteralValue;
import org.testng.annotations.Test;


/**
 * Exercise the "<tt>IN (val1, val2, ...)</tt>" operator for correctness.
 */
@Test(groups={"framework"})
public class TestInValuesOperator {

    /**
     * Exercise the IN operator with string values.  There shouldn't be any
     * difference in behavior if we use strings or other types, so this should
     * be sufficient for exercising the operator's behavior.
     */
    public void testInValuesOperator() {
        InValuesOperator op;

        op = makeOperator("a", new String[]{"a", "b", "c"});
        assert Boolean.TRUE.equals(op.evaluate());

        op = makeOperator("a", new String[]{"b", "c", "d"});
        assert Boolean.FALSE.equals(op.evaluate());

        op = makeOperator(null, new String[]{"a", "b", "c"});
        assert op.evaluate() == null;

        op = makeOperator(null, new String[]{"a", "b", null, "c"});
        assert op.evaluate() == null;

        op = makeOperator(null, new String[]{null, "a", "b", "c"});
        assert op.evaluate() == null;

        op = makeOperator("a", new String[]{null, "a", "b", "c"});
        assert Boolean.TRUE.equals(op.evaluate());

        op = makeOperator("a", new String[]{"a", null, "b", "c"});
        assert Boolean.TRUE.equals(op.evaluate());

        op = makeOperator("a", new String[]{null, "b", "c", "d"});
        assert op.evaluate() == null;

        op = makeOperator("a", new String[]{"b", null, "c", "d"});
        assert op.evaluate() == null;
    }


    /**
     * Construct the {@link InValuesOperator} comparing the LHS value to the
     * collection of RHS values.
     *
     * @param lhs the value on the left of the IN operator
     * @param rhs the collection of values on the right of the IN operator
     *
     * @return the operator to evaluate the expression
     */
    private InValuesOperator makeOperator(Object lhs, Object[] rhs) {
        ArrayList<Expression> rhsValues = new ArrayList<>();
        for (Object v : rhs)
            rhsValues.add(new LiteralValue(v));

        return new InValuesOperator(new LiteralValue(lhs), rhsValues);
    }
}
