package edu.caltech.test.nanodb.sqlparse;


import java.util.ArrayList;

import edu.caltech.nanodb.commands.AnalyzeCommand;
import edu.caltech.nanodb.commands.CrashCommand;
import edu.caltech.nanodb.commands.ExitCommand;
import edu.caltech.nanodb.commands.FlushCommand;
import edu.caltech.nanodb.commands.OptimizeCommand;
import edu.caltech.nanodb.commands.VerifyCommand;
import org.testng.annotations.Test;

import edu.caltech.nanodb.commands.BeginTransactionCommand;
import edu.caltech.nanodb.commands.CommitTransactionCommand;
import edu.caltech.nanodb.commands.RollbackTransactionCommand;
import edu.caltech.nanodb.sqlparse.ParseUtil;


/**
 * Exercise the parser's support of various simple utility commands.
 */
@Test(groups={"parser"})
public class TestParserUtility {

    /** Verifies that the CRASH command is parsed correctly. */
    public void testParseCrash() {
        CrashCommand cmd;

        // Not much else to check here.
        cmd = (CrashCommand) ParseUtil.parseCommand("CRASH;");
        assert cmd != null;
        assert cmd.getSecondsToCrash() == 0;

        cmd = (CrashCommand) ParseUtil.parseCommand("CRASH 17;");
        assert cmd != null;
        assert cmd.getSecondsToCrash() == 17;
    }


    /** Verifies that the EXIT/QUIT command is parsed correctly. */
    public void testParseExit() {
        ExitCommand cmd;

        // Not much else to check here.
        cmd = (ExitCommand) ParseUtil.parseCommand("EXIT;");
        assert cmd != null;

        cmd = (ExitCommand) ParseUtil.parseCommand("QUIT;");
        assert cmd != null;
    }


    /** Verifies that the FLUSH command is parsed correctly. */
    public void testParseFlush() {
        FlushCommand cmd;

        // Not much else to check here.
        cmd = (FlushCommand) ParseUtil.parseCommand("FLUSH;");
        assert cmd != null;
    }


    /** Verifies that the ANALYZE command is parsed correctly. */
    public void testParseAnalyze() {
        AnalyzeCommand cmd;
        ArrayList<String> tableNames = new ArrayList<>();

        cmd = (AnalyzeCommand) ParseUtil.parseCommand("analyze t;");
        assert cmd != null;
        tableNames.clear();
        tableNames.addAll(cmd.getTableNames());
        assert tableNames.size() == 1;
        assert tableNames.get(0).equals("t");

        cmd = (AnalyzeCommand) ParseUtil.parseCommand("ANALYZE T;");
        assert cmd != null;
        tableNames.clear();
        tableNames.addAll(cmd.getTableNames());
        assert tableNames.size() == 1;
        assert tableNames.get(0).equals("t");

        cmd = (AnalyzeCommand) ParseUtil.parseCommand("ANALYZE t5, t2, t4, t6, t3;");
        assert cmd != null;
        tableNames.clear();
        tableNames.addAll(cmd.getTableNames());
        assert tableNames.size() == 5;
        assert tableNames.get(0).equals("t5");
        assert tableNames.get(1).equals("t2");
        assert tableNames.get(2).equals("t4");
        assert tableNames.get(3).equals("t6");
        assert tableNames.get(4).equals("t3");

        cmd = (AnalyzeCommand) ParseUtil.parseCommand("Analyze T6, t2, T5, T1, t4;");
        assert cmd != null;
        tableNames.clear();
        tableNames.addAll(cmd.getTableNames());
        assert tableNames.size() == 5;
        assert tableNames.get(0).equals("t6");
        assert tableNames.get(1).equals("t2");
        assert tableNames.get(2).equals("t5");
        assert tableNames.get(3).equals("t1");
        assert tableNames.get(4).equals("t4");
    }


    /** Verifies that the OPTIMIZE command is parsed correctly. */
    public void testParseOptimize() {
        OptimizeCommand cmd;
        ArrayList<String> tableNames = new ArrayList<>();

        cmd = (OptimizeCommand) ParseUtil.parseCommand("optimize t;");
        assert cmd != null;
        tableNames.clear();
        tableNames.addAll(cmd.getTableNames());
        assert tableNames.size() == 1;
        assert tableNames.get(0).equals("t");

        cmd = (OptimizeCommand) ParseUtil.parseCommand("OPTIMIZE T;");
        assert cmd != null;
        tableNames.clear();
        tableNames.addAll(cmd.getTableNames());
        assert tableNames.size() == 1;
        assert tableNames.get(0).equals("t");

        cmd = (OptimizeCommand) ParseUtil.parseCommand("OPTIMIZE t5, t2, t4, t6, t3;");
        assert cmd != null;
        tableNames.clear();
        tableNames.addAll(cmd.getTableNames());
        assert tableNames.size() == 5;
        assert tableNames.get(0).equals("t5");
        assert tableNames.get(1).equals("t2");
        assert tableNames.get(2).equals("t4");
        assert tableNames.get(3).equals("t6");
        assert tableNames.get(4).equals("t3");

        cmd = (OptimizeCommand) ParseUtil.parseCommand("Optimize T6, t2, T5, T1, t4;");
        assert cmd != null;
        tableNames.clear();
        tableNames.addAll(cmd.getTableNames());
        assert tableNames.size() == 5;
        assert tableNames.get(0).equals("t6");
        assert tableNames.get(1).equals("t2");
        assert tableNames.get(2).equals("t5");
        assert tableNames.get(3).equals("t1");
        assert tableNames.get(4).equals("t4");
    }


    /** Verifies that the VERIFY command is parsed correctly. */
    public void testParseVerify() {
        VerifyCommand cmd;
        ArrayList<String> tableNames = new ArrayList<>();

        cmd = (VerifyCommand) ParseUtil.parseCommand("verify t;");
        assert cmd != null;
        tableNames.clear();
        tableNames.addAll(cmd.getTableNames());
        assert tableNames.size() == 1;
        assert tableNames.get(0).equals("t");

        cmd = (VerifyCommand) ParseUtil.parseCommand("VERIFY T;");
        assert cmd != null;
        tableNames.clear();
        tableNames.addAll(cmd.getTableNames());
        assert tableNames.size() == 1;
        assert tableNames.get(0).equals("t");

        cmd = (VerifyCommand) ParseUtil.parseCommand("VERIFY t5, t2, t4, t6, t3;");
        assert cmd != null;
        tableNames.clear();
        tableNames.addAll(cmd.getTableNames());
        assert tableNames.size() == 5;
        assert tableNames.get(0).equals("t5");
        assert tableNames.get(1).equals("t2");
        assert tableNames.get(2).equals("t4");
        assert tableNames.get(3).equals("t6");
        assert tableNames.get(4).equals("t3");

        cmd = (VerifyCommand) ParseUtil.parseCommand("Verify T6, t2, T5, T1, t4;");
        assert cmd != null;
        tableNames.clear();
        tableNames.addAll(cmd.getTableNames());
        assert tableNames.size() == 5;
        assert tableNames.get(0).equals("t6");
        assert tableNames.get(1).equals("t2");
        assert tableNames.get(2).equals("t5");
        assert tableNames.get(3).equals("t1");
        assert tableNames.get(4).equals("t4");
    }
}
