package edu.caltech.test.nanodb.constraints;


import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import edu.caltech.test.nanodb.sql.SqlTestCase;
import edu.caltech.nanodb.server.CommandResult;


/**
 * This class exercises the database with some simple INSERT statements
 * on a table with NOT NULL constraints, to see if the NOT NULL constraint
 * works propertly.
 */
@Test(groups={"constraints"}, dependsOnGroups={"framework"})
public class TestNotNullOps extends SqlTestCase {

    @BeforeClass
    public void setup() throws Exception {
        tryDoCommand(
            "CREATE TABLE test_not_null_ops (" +
            "a INTEGER NOT NULL, " +
            "b VARCHAR(20) PRIMARY KEY, " +
            "c INTEGER );"
        );

        // Some initial values, to exercise the primary key column.
        tryDoCommand("INSERT INTO test_not_null_ops VALUES (1,    'red',   10 );");
        tryDoCommand("INSERT INTO test_not_null_ops VALUES (2, 'orange',   20 );");
    }


   	/**
     * This test checks that the not null constraint added to column a
     * and column b is enforced. It does this by trying to insert a
     * variety of tuples with null values in either column a or
     * column b, and that these insert commands should fail if the not null
     * constraint is indeed enforced.
     *
     * @throws Exception if any issues occur.
     */
    public void testNotNullConstraint() throws Throwable {
        // Try adding a null value to a. Table is test_not_null_ops
        CommandResult result;

        // Should fail since a has a NOT NULL constraint
        result = server.doCommand(
            "INSERT INTO test_not_null_ops VALUES (NULL, 'yellow', 30)", false);
        assert result.failed() : "Inserted a row with a forbidden NULL value";

        // Try adding a null value to b, which has a  NOT NULL and a
        // UNIQUE constraint on it (since it is a PRIMARY KEY)
        // to see if the two constraints somehow conflict.
        result = server.doCommand(
            "INSERT INTO test_not_null_ops VALUES (1, NULL, 40)", false);
        assert result.failed() : "Inserted a row with a forbidden NULL value";

        // Try adding a nonunique value to b, which has a  NOT NULL and a
        // UNIQUE constraint on it (since it is a PRIMARY KEY)
        // to see if the two constraints somehow conflict.
        result = server.doCommand(
            "INSERT INTO test_not_null_ops VALUES (1, 'red', 40)", false);
        assert result.failed() : "Inserted a row that violated a primary key";

        // Lastly, make sure adding null is possible to columns that don't
        // have the NOT NULL constraint. Particularly, we should be able
        // to insert a null into c.
        result = server.doCommand(
            "INSERT INTO test_not_null_ops VALUES (1, 'green', NULL)", false);
        assert !result.failed() : "Couldn't insert a row with allowed NULL value";
    }
}
