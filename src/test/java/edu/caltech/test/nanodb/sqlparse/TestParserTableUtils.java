package edu.caltech.test.nanodb.sqlparse;


import edu.caltech.nanodb.commands.DropIndexCommand;
import edu.caltech.nanodb.commands.DropTableCommand;
import edu.caltech.nanodb.commands.DumpIndexCommand;
import edu.caltech.nanodb.commands.DumpTableCommand;
import edu.caltech.nanodb.sqlparse.ParseUtil;
import org.testng.annotations.Test;


/**
 * This class verifies the parsing of a number of table-related utility
 * operations, such as "<tt>DROP TABLE ...</tt>", "<tt>DUMP TABLE ...</tt>",
 * and so forth.  Since the <tt>CREATE TABLE</tt> command is so involved,
 * it is exercised in a separate test class.
 */
@Test(groups={"parser"})
public class TestParserTableUtils {
    public void testParseDropTable() {
        DropTableCommand cmd;

        cmd = (DropTableCommand) ParseUtil.parseCommand("DROP TABLE foo;");

        assert "foo".equals(cmd.getTableName()) :
            "Expected table \"foo\", got \"" + cmd.getTableName() + "\" instead";
        assert !cmd.getIfExists();

        cmd = (DropTableCommand) ParseUtil.parseCommand("Drop table BAR;");

        assert "bar".equals(cmd.getTableName()) :
            "Expected table \"bar\", got \"" + cmd.getTableName() + "\" instead";
        assert !cmd.getIfExists();

        cmd = (DropTableCommand) ParseUtil.parseCommand("drop table if exists foo;");

        assert "foo".equals(cmd.getTableName()) :
            "Expected table \"foo\", got \"" + cmd.getTableName() + "\" instead";
        assert cmd.getIfExists();

        cmd = (DropTableCommand) ParseUtil.parseCommand("Drop table IF EXISTS BaR;");

        assert "bar".equals(cmd.getTableName()) :
            "Expected table \"bar\", got \"" + cmd.getTableName() + "\" instead";
        assert cmd.getIfExists();
    }


    public void testParseDropIndex() {
        DropIndexCommand cmd;

        cmd = (DropIndexCommand) ParseUtil.parseCommand("DROP INDEX abc ON foo;");

        assert "foo".equals(cmd.getTableName());
        assert "abc".equals(cmd.getIndexName());
        assert !cmd.getIfExists();

        cmd = (DropIndexCommand) ParseUtil.parseCommand("Drop index xyz on BAR;");

        assert "bar".equals(cmd.getTableName());
        assert "xyz".equals(cmd.getIndexName());
        assert !cmd.getIfExists();

        cmd = (DropIndexCommand) ParseUtil.parseCommand("drop index if exists ABC on foo;");

        assert "foo".equals(cmd.getTableName());
        assert "abc".equals(cmd.getIndexName());
        assert cmd.getIfExists();

        cmd = (DropIndexCommand) ParseUtil.parseCommand("Drop Index IF EXISTS XyZ oN BaR;");

        assert "bar".equals(cmd.getTableName());
        assert "xyz".equals(cmd.getIndexName());
        assert cmd.getIfExists();
    }


    public void testParseDumpTable() {
        DumpTableCommand cmd;

        cmd = (DumpTableCommand) ParseUtil.parseCommand("DUMP TABLE foo;");
        assert "foo".equals(cmd.getTableName());
        assert cmd.getFilename() == null;
        assert cmd.getFormat() == null;

        cmd = (DumpTableCommand) ParseUtil.parseCommand("Dump table BAR;");
        assert "bar".equals(cmd.getTableName());
        assert cmd.getFilename() == null;
        assert cmd.getFormat() == null;

        cmd = (DumpTableCommand)
            ParseUtil.parseCommand("dump table Foo To File 'foo.txt';");
        assert "foo".equals(cmd.getTableName());
        assert "foo.txt".equals(cmd.getFilename());
        assert cmd.getFormat() == null;

        cmd = (DumpTableCommand)
            ParseUtil.parseCommand("Dump table BAR tO filE 'bar.txt';");
        assert "bar".equals(cmd.getTableName());
        assert "bar.txt".equals(cmd.getFilename());
        assert cmd.getFormat() == null;

        cmd = (DumpTableCommand)
            ParseUtil.parseCommand("dump table FOO format 'json';");
        assert "foo".equals(cmd.getTableName());
        assert cmd.getFilename() == null;
        assert "json".equals(cmd.getFormat());

        cmd = (DumpTableCommand)
            ParseUtil.parseCommand("Dump table BAR Format 'CSV';");
        assert "bar".equals(cmd.getTableName());
        assert cmd.getFilename() == null;
        assert "csv".equals(cmd.getFormat());

        cmd = (DumpTableCommand) ParseUtil.parseCommand(
            "dump table Foo To File 'foo.txt' formaT 'JSON';");
        assert "foo".equals(cmd.getTableName());
        assert "foo.txt".equals(cmd.getFilename());
        assert "json".equals(cmd.getFormat());

        cmd = (DumpTableCommand) ParseUtil.parseCommand(
            "Dump table BAR tO filE 'bar.txt' forMat 'csv';");
        assert "bar".equals(cmd.getTableName());
        assert "bar.txt".equals(cmd.getFilename());
        assert "csv".equals(cmd.getFormat());
    }


    public void testParseDumpIndex() {
        DumpIndexCommand cmd;

        cmd = (DumpIndexCommand) ParseUtil.parseCommand("DUMP INDEX foo.ABC;");
        assert "foo".equals(cmd.getTableName());
        assert "abc".equals(cmd.getIndexName());
        assert cmd.getFilename() == null;
        assert cmd.getFormat() == null;

        cmd = (DumpIndexCommand) ParseUtil.parseCommand("Dump index BAR.xyz;");
        assert "bar".equals(cmd.getTableName());
        assert "xyz".equals(cmd.getIndexName());
        assert cmd.getFilename() == null;
        assert cmd.getFormat() == null;

        cmd = (DumpIndexCommand)
            ParseUtil.parseCommand("dump index Foo.Abc To File 'foo.txt';");
        assert "foo".equals(cmd.getTableName());
        assert "abc".equals(cmd.getIndexName());
        assert "foo.txt".equals(cmd.getFilename());
        assert cmd.getFormat() == null;

        cmd = (DumpIndexCommand)
            ParseUtil.parseCommand("Dump index BAR.xyZ tO filE 'bar.txt';");
        assert "bar".equals(cmd.getTableName());
        assert "xyz".equals(cmd.getIndexName());
        assert "bar.txt".equals(cmd.getFilename());
        assert cmd.getFormat() == null;

        cmd = (DumpIndexCommand)
            ParseUtil.parseCommand("dump index FOO.aBc format 'json';");
        assert "foo".equals(cmd.getTableName());
        assert "abc".equals(cmd.getIndexName());
        assert cmd.getFilename() == null;
        assert "json".equals(cmd.getFormat());

        cmd = (DumpIndexCommand)
            ParseUtil.parseCommand("Dump index BAR.XYZ Format 'CSV';");
        assert "bar".equals(cmd.getTableName());
        assert "xyz".equals(cmd.getIndexName());
        assert cmd.getFilename() == null;
        assert "csv".equals(cmd.getFormat());

        cmd = (DumpIndexCommand) ParseUtil.parseCommand(
            "dump index Foo.ABC To File 'foo.txt' formaT 'JSON';");
        assert "foo".equals(cmd.getTableName());
        assert "abc".equals(cmd.getIndexName());
        assert "foo.txt".equals(cmd.getFilename());
        assert "json".equals(cmd.getFormat());

        cmd = (DumpIndexCommand) ParseUtil.parseCommand(
            "Dump indeX BAR.XyZ tO filE 'bar.txt' forMat 'csv';");
        assert "bar".equals(cmd.getTableName());
        assert "xyz".equals(cmd.getIndexName());
        assert "bar.txt".equals(cmd.getFilename());
        assert "csv".equals(cmd.getFormat());
    }
}
