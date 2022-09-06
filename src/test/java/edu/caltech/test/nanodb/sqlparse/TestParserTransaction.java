package edu.caltech.test.nanodb.sqlparse;


import org.testng.annotations.Test;

import edu.caltech.nanodb.commands.BeginTransactionCommand;
import edu.caltech.nanodb.commands.CommitTransactionCommand;
import edu.caltech.nanodb.commands.RollbackTransactionCommand;
import edu.caltech.nanodb.sqlparse.ParseUtil;


/**
 * Exercise the parser's support of the transaction-demarcation commands.
 */
@Test(groups={"parser"})
public class TestParserTransaction {

    /**
     * Verifies that transaction-start commands are parsed correctly.
     */
    public void testParseTransactionBegin() {
        BeginTransactionCommand cmd;

        // Not much else to check here.
        cmd = (BeginTransactionCommand) ParseUtil.parseCommand("START TRANSACTION;");
        assert cmd != null;

        cmd = (BeginTransactionCommand) ParseUtil.parseCommand("BEGIN WORK;");
        assert cmd != null;

        cmd = (BeginTransactionCommand) ParseUtil.parseCommand("BEGIN;");
        assert cmd != null;
    }


    /**
     * Verifies that transaction-commit commands are parsed correctly.
     */
    public void testParseTransactionCommit() {
        CommitTransactionCommand cmd;

        // Not much else to check here.
        cmd = (CommitTransactionCommand) ParseUtil.parseCommand("COMMIT WORK;");
        assert cmd != null;

        cmd = (CommitTransactionCommand) ParseUtil.parseCommand("COMMIT;");
        assert cmd != null;
    }


    /**
     * Verifies that transaction-rollback commands are parsed correctly.
     */
    public void testParseTransactionRollback() {
        RollbackTransactionCommand cmd;

        // Not much else to check here.
        cmd = (RollbackTransactionCommand) ParseUtil.parseCommand("ROLLBACK WORK;");
        assert cmd != null;

        cmd = (RollbackTransactionCommand) ParseUtil.parseCommand("ROLLBACK;");
        assert cmd != null;
    }
}
