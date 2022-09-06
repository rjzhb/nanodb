package edu.caltech.test.nanodb.sqlparse;


import java.util.List;

import edu.caltech.nanodb.commands.CommandProperties;
import org.testng.annotations.Test;

import edu.caltech.nanodb.commands.CreateIndexCommand;
import edu.caltech.nanodb.sqlparse.ParseUtil;


@Test(groups={"parser"})
public class TestParserCreateIndex {

    public void testParseCreateNonUniqueIndex() {
        CreateIndexCommand cmd;
        List<String> columns;

        cmd = (CreateIndexCommand) ParseUtil.parseCommand(
            "CREATE INDEX idx_foo ON foo (a);");

        assert !cmd.isUnique();
        assert !cmd.getIfNotExists();
        assert "idx_foo".equals(cmd.getIndexName());
        assert "foo".equals(cmd.getTableName());
        columns = cmd.getColumnNames();
        assert columns.size() == 1;
        assert "a".equals(columns.get(0));
        assert cmd.getProperties() == null;

        cmd = (CreateIndexCommand) ParseUtil.parseCommand(
            "create index if not exists IDX_foo ON FOO (A);");

        assert !cmd.isUnique();
        assert cmd.getIfNotExists();
        assert "idx_foo".equals(cmd.getIndexName());
        assert "foo".equals(cmd.getTableName());
        columns = cmd.getColumnNames();
        assert columns.size() == 1;
        assert "a".equals(columns.get(0));
        assert cmd.getProperties() == null;

        cmd = (CreateIndexCommand) ParseUtil.parseCommand(
            "create index idx_BAR ON Bar (x, Y, Zee);");

        assert !cmd.isUnique();
        assert !cmd.getIfNotExists();
        assert "idx_bar".equals(cmd.getIndexName());
        assert "bar".equals(cmd.getTableName());
        columns = cmd.getColumnNames();
        assert columns.size() == 3;
        assert "x".equals(columns.get(0));
        assert "y".equals(columns.get(1));
        assert "zee".equals(columns.get(2));
        assert cmd.getProperties() == null;
    }


    public void testParseCreateUniqueIndex() {
        CreateIndexCommand cmd;
        List<String> columns;

        cmd = (CreateIndexCommand) ParseUtil.parseCommand(
            "CREATE unique INDEX idx_foo ON foo (a);");

        assert cmd.isUnique();
        assert !cmd.getIfNotExists();
        assert "idx_foo".equals(cmd.getIndexName());
        assert "foo".equals(cmd.getTableName());
        columns = cmd.getColumnNames();
        assert columns.size() == 1;
        assert "a".equals(columns.get(0));
        assert cmd.getProperties() == null;

        cmd = (CreateIndexCommand) ParseUtil.parseCommand(
            "create UNIQUE index if not exists IDX_foo ON FOO (A);");

        assert cmd.isUnique();
        assert cmd.getIfNotExists();
        assert "idx_foo".equals(cmd.getIndexName());
        assert "foo".equals(cmd.getTableName());
        columns = cmd.getColumnNames();
        assert columns.size() == 1;
        assert "a".equals(columns.get(0));
        assert cmd.getProperties() == null;

        cmd = (CreateIndexCommand) ParseUtil.parseCommand(
            "create Unique index idx_BAR ON Bar (x, Y, Zee);");

        assert cmd.isUnique();
        assert !cmd.getIfNotExists();
        assert "idx_bar".equals(cmd.getIndexName());
        assert "bar".equals(cmd.getTableName());
        columns = cmd.getColumnNames();
        assert columns.size() == 3;
        assert "x".equals(columns.get(0));
        assert "y".equals(columns.get(1));
        assert "zee".equals(columns.get(2));
        assert cmd.getProperties() == null;
    }


    /**
     * Exercises the <tt>PROPERTIES ( ... )</tt> clause of the
     * <tt>CREATE INDEX</tt> command.
     */
    public void testParseCreateIndexProperties() {
        CreateIndexCommand cmd;
        List<String> columns;
        CommandProperties props;

        cmd = (CreateIndexCommand) ParseUtil.parseCommand(
            "create Unique index abc ON t (a, B) " +
            "properties (foo = 'bar');");

        assert cmd.isUnique();
        assert !cmd.getIfNotExists();
        assert "abc".equals(cmd.getIndexName());
        assert "t".equals(cmd.getTableName());
        columns = cmd.getColumnNames();
        assert columns.size() == 2;
        assert "a".equals(columns.get(0));
        assert "b".equals(columns.get(1));

        props = cmd.getProperties();
        assert props.getNames().size() == 1;
        assert "bar".equals(props.get("foo"));

        cmd = (CreateIndexCommand) ParseUtil.parseCommand(
            "create index ABC ON T (A, b) " +
            "properties (FOO = 'Bar', n=500);");

        assert !cmd.isUnique();
        assert !cmd.getIfNotExists();
        assert "abc".equals(cmd.getIndexName());
        assert "t".equals(cmd.getTableName());
        columns = cmd.getColumnNames();
        assert columns.size() == 2;
        assert "a".equals(columns.get(0));
        assert "b".equals(columns.get(1));

        props = cmd.getProperties();
        assert props.getNames().size() == 2;
        assert "Bar".equals(props.get("foo"));
        assert Integer.valueOf(500).equals(props.get("n"));
    }
}
