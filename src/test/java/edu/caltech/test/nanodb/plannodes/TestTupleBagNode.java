package edu.caltech.test.nanodb.plannodes;


import org.testng.annotations.Test;

import edu.caltech.nanodb.expressions.TupleLiteral;
import edu.caltech.nanodb.plannodes.TupleBagNode;
import edu.caltech.nanodb.relations.ColumnInfo;
import edu.caltech.nanodb.relations.ColumnType;
import edu.caltech.nanodb.relations.Schema;


@Test(groups={"framework"})
public class TestTupleBagNode extends PlanNodeTestCase {

    /**
     * Ensure that the TupleBagNode constructor throws an exception if the
     * schema argument is unspecified.
     */
    @Test(expectedExceptions={IllegalArgumentException.class})
    public void testTupleBagNoSchemaError() {
        // Should throw an IllegalArgumentException.
        TupleBagNode n = new TupleBagNode(null);
    }


    /** Exercises basic behavior of an empty Tuple Bag plan node. */
    public void testTupleBagEmpty() {
        Schema s = new Schema(
            new ColumnInfo("A", ColumnType.INTEGER),
            new ColumnInfo("B", ColumnType.VARCHAR(10))
        );

        TupleBagNode node = new TupleBagNode(s);

        node.prepare();
        node.initialize();

        // Verify the tuple-bag properly reports its schema.
        assert node.getSchema() != null;
        assert node.getSchema().equals(s);

        // Verify that the reported tuples are correct.

        TupleLiteral[] expected = { };

        checkOrderedResults(expected, node);

        // Make sure the node continues to report null at the end of the
        // tuple sequence.
        assert node.getNextTuple() == null;
    }


    /** Exercises basic behavior of a non-empty Tuple Bag plan node. */
    public void testTupleBag() {
        Schema s = new Schema(
            new ColumnInfo("A", ColumnType.INTEGER),
            new ColumnInfo("B", ColumnType.VARCHAR(10))
        );

        TupleBagNode node = new TupleBagNode(s);

        node.addTuple(new TupleLiteral(10, "apple"));
        node.addTuple(new TupleLiteral(20, "banana"));
        node.addTuple(new TupleLiteral(30, "cherry"));
        node.addTuple(new TupleLiteral(40, "orange"));

        node.prepare();
        node.initialize();

        // Verify the tuple-bag properly reports its schema.
        assert node.getSchema() != null;
        assert node.getSchema().equals(s);

        // Verify that the reported tuples are correct.

        TupleLiteral[] expected = {
            new TupleLiteral(10, "apple"),
            new TupleLiteral(20, "banana"),
            new TupleLiteral(30, "cherry"),
            new TupleLiteral(40, "orange")
        };

        checkOrderedResults(expected, node);

        // Make sure the node continues to report null at the end of the
        // tuple sequence.
        assert node.getNextTuple() == null;
    }
}
