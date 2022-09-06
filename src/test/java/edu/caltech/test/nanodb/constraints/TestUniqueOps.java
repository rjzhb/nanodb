package edu.caltech.test.nanodb.constraints;


import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import edu.caltech.test.nanodb.sql.SqlTestCase;
import edu.caltech.nanodb.server.CommandResult;


/**
 * This class exercises the database with some simple INSERT statements
 * on a table with UNIQUE constraints, to see if the UNIQUE constraint
 * works propertly.
 */
@Test(groups={"constraints"}, dependsOnGroups={"framework"})
public class TestUniqueOps extends SqlTestCase {

    @BeforeClass
    public void setup() throws Exception {
        tryDoCommand("CREATE TABLE test_unique_1 (" +
            "a INTEGER UNIQUE, b VARCHAR(20), c INTEGER);");

        tryDoCommand("INSERT INTO test_unique_1 VALUES (1,    'red',   10);");
        tryDoCommand("INSERT INTO test_unique_1 VALUES (2, 'orange',   20);");
        tryDoCommand("INSERT INTO test_unique_1 VALUES (3,     NULL,   30);");
        tryDoCommand("INSERT INTO test_unique_1 VALUES (4,  'green', NULL);");
        tryDoCommand("INSERT INTO test_unique_1 VALUES (5, 'yellow',   40);");
        tryDoCommand("INSERT INTO test_unique_1 VALUES (6,   'blue',   50);");

        tryDoCommand("CREATE TABLE test_unique_2 (" +
            "a INTEGER, b VARCHAR(20), c INTEGER," +
            "UNIQUE (a, b) );");

        tryDoCommand("INSERT INTO test_unique_2 VALUES (1,    'red',   10);");
        tryDoCommand("INSERT INTO test_unique_2 VALUES (1, 'orange',   20);");
        tryDoCommand("INSERT INTO test_unique_2 VALUES (2,   'gray',   30);");
        tryDoCommand("INSERT INTO test_unique_2 VALUES (3,     NULL,   40);");
    }


   	/**
     * This test checks that the unique constraint added to column a is
     * enforced. It does this by trying to insert a duplicate value in
     * column a, and that the insert command should fail if the unique
     * constraint is indeed enforced.
     */
    public void testUniqueConstraint1Col() {
        CommandResult result;

        // Try to violate the unique constraint; make sure it doesn't succeed
        result = server.doCommand(
            "INSERT INTO test_unique_1 VALUES (1, 'purple', 60)", false);
        assert result.failed() : "Inserted a row that violates a UNIQUE constraint";

        // Make sure we can insert a row that doesn't violate the constraint
        result = server.doCommand(
            "INSERT INTO test_unique_1 VALUES (7, NULL, 60);", false);
        assert !result.failed() : "Couldn't insert a row that satisfies constraint";
    }

    /**
     * This test checks that the unique constraint added to column a is
     * enforced. It does this by trying to insert a duplicate value in
     * column a, and that the insert command should fail if the unique
     * constraint is indeed enforced.
     */
    public void testUniqueConstraint2Col() {
        CommandResult result;

        // Try to violate the unique constraint; make sure it doesn't succeed
        result = server.doCommand(
            "INSERT INTO test_unique_2 VALUES (2, 'gray', 60)", false);
        assert result.failed() : "Inserted a row that violates a UNIQUE constraint";

        // Make sure we can insert a row that doesn't violate the constraint
        result = server.doCommand(
            "INSERT INTO test_unique_2 VALUES (2, 'blue', 80);", false);
        assert !result.failed() : "Couldn't insert a row that satisfies constraint";

        // NULL prevents a UNIQUE constraint from being enforced.  Therefore,
        // this insertion should succeed.
        result = server.doCommand(
            "INSERT INTO test_unique_2 VALUES (3, NULL, 50);", false);
        assert !result.failed() :
               "Couldn't insert a row with NULL in a UNIQUE constraint";
    }
}
