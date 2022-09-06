package edu.caltech.test.nanodb.sqlparse;


import java.util.List;

import edu.caltech.nanodb.commands.CommandProperties;
import edu.caltech.nanodb.relations.ColumnInfo;
import edu.caltech.nanodb.relations.ColumnType;
import edu.caltech.nanodb.relations.SQLDataType;
import edu.caltech.nanodb.sqlparse.ParseUtil;
import org.testng.annotations.Test;

import edu.caltech.nanodb.commands.CreateTableCommand;


@Test(groups={"parser"})
public class TestParserCreateTable {

    /**
     * A helper function to verify the details of a specific column.
     *
     * @param colInfo The column-info to verify
     * @param tableName The expected table name
     * @param columnName The expected column name
     * @param baseType The expected base-type of the column
     */
    private void checkColumnDecl(ColumnInfo colInfo, String tableName,
                                 String columnName, SQLDataType baseType) {
        assert tableName.equals(colInfo.getTableName());
        assert columnName.equals(colInfo.getName());
        assert baseType.equals(colInfo.getType().getBaseType());
    }


    public void testParseCreateTableColumnTypes() {
        CreateTableCommand cmd;

        cmd = (CreateTableCommand) ParseUtil.parseCommand("CREATE TABLE t (" +
            "A int, b integer, C BigInt, d decimal, e DECIMAL(15), " +
            "F decimal(12, 2), g numeric, h numeric(10), i numeric(6, 3), " +
            "j FLOAT, k double, L char (100), m character(30), " +
            "n varchar (350), o character VARYING(35), p date, q datetime, " +
            "r time, s timestamp" +
            ");"
            );

        assert !cmd.isTemporary();
        assert !cmd.getIfNotExists();
        assert cmd.getProperties() == null;
        assert cmd.getConstraints().size() == 0;

        List<ColumnInfo> columns = cmd.getColumns();
        assert columns.size() == 19;

        checkColumnDecl(columns.get(0), "t", "a", SQLDataType.INTEGER);

        checkColumnDecl(columns.get(1), "t", "b", SQLDataType.INTEGER);

        checkColumnDecl(columns.get(2), "t", "c", SQLDataType.BIGINT);

        checkColumnDecl(columns.get(3), "t", "d", SQLDataType.NUMERIC);
        assert columns.get(3).getType().getPrecision() == ColumnType.DEFAULT_PRECISION;
        assert columns.get(3).getType().getScale() == ColumnType.DEFAULT_SCALE;

        checkColumnDecl(columns.get(4), "t", "e", SQLDataType.NUMERIC);
        assert columns.get(4).getType().getPrecision() == 15;
        assert columns.get(4).getType().getScale() == ColumnType.DEFAULT_SCALE;

        checkColumnDecl(columns.get(5), "t", "f", SQLDataType.NUMERIC);
        assert columns.get(5).getType().getPrecision() == 12;
        assert columns.get(5).getType().getScale() == 2;

        checkColumnDecl(columns.get(6), "t", "g", SQLDataType.NUMERIC);
        assert columns.get(6).getType().getPrecision() == ColumnType.DEFAULT_PRECISION;
        assert columns.get(6).getType().getScale() == ColumnType.DEFAULT_SCALE;

        checkColumnDecl(columns.get(7), "t", "h", SQLDataType.NUMERIC);
        assert columns.get(7).getType().getPrecision() == 10;
        assert columns.get(7).getType().getScale() == ColumnType.DEFAULT_SCALE;

        checkColumnDecl(columns.get(8), "t", "i", SQLDataType.NUMERIC);
        assert columns.get(8).getType().getPrecision() == 6;
        assert columns.get(8).getType().getScale() == 3;

        checkColumnDecl(columns.get(9), "t", "j", SQLDataType.FLOAT);

        checkColumnDecl(columns.get(10), "t", "k", SQLDataType.DOUBLE);

        checkColumnDecl(columns.get(11), "t", "l", SQLDataType.CHAR);
        assert columns.get(11).getType().getLength() == 100;

        checkColumnDecl(columns.get(12), "t", "m", SQLDataType.CHAR);
        assert columns.get(12).getType().getLength() == 30;

        checkColumnDecl(columns.get(13), "t", "n", SQLDataType.VARCHAR);
        assert columns.get(13).getType().getLength() == 350;

        checkColumnDecl(columns.get(14), "t", "o", SQLDataType.VARCHAR);
        assert columns.get(14).getType().getLength() == 35;

        checkColumnDecl(columns.get(15), "t", "p", SQLDataType.DATE);

        checkColumnDecl(columns.get(16), "t", "q", SQLDataType.DATETIME);

        checkColumnDecl(columns.get(17), "t", "r", SQLDataType.TIME);

        checkColumnDecl(columns.get(18), "t", "s", SQLDataType.TIMESTAMP);
    }


    /**
     * Exercises options to the <tt>CREATE TABLE</tt> statement, such as
     * <tt>IF NOT EXISTS</tt> and <tt>TEMPORARY</tt>.
     */
    public void testParseCreateTableOptions() {
        CreateTableCommand cmd;
        List<ColumnInfo> columns;

        cmd = (CreateTableCommand) ParseUtil.parseCommand(
            "CREATE TEMPORARY TABLE t1 (a INTEGER, b CHAR(100), c DATE);");

        assert cmd.isTemporary();
        assert !cmd.getIfNotExists();
        assert cmd.getProperties() == null;
        assert cmd.getConstraints().size() == 0;

        columns = cmd.getColumns();
        assert columns.size() == 3;

        checkColumnDecl(columns.get(0), "t1", "a", SQLDataType.INTEGER);
        checkColumnDecl(columns.get(1), "t1", "b", SQLDataType.CHAR);
        assert columns.get(1).getType().getLength() == 100;
        checkColumnDecl(columns.get(2), "t1", "c", SQLDataType.DATE);

        cmd = (CreateTableCommand) ParseUtil.parseCommand(
            "CREATE TABLE IF NOT EXISTS t2 (a INTEGER, b CHAR(100), c DATE);");

        assert !cmd.isTemporary();
        assert cmd.getIfNotExists();
        assert cmd.getProperties() == null;
        assert cmd.getConstraints().size() == 0;

        columns = cmd.getColumns();
        assert columns.size() == 3;

        checkColumnDecl(columns.get(0), "t2", "a", SQLDataType.INTEGER);
        checkColumnDecl(columns.get(1), "t2", "b", SQLDataType.CHAR);
        assert columns.get(1).getType().getLength() == 100;
        checkColumnDecl(columns.get(2), "t2", "c", SQLDataType.DATE);

        cmd = (CreateTableCommand) ParseUtil.parseCommand(
            "CREATE TEMPORARY TABLE IF NOT EXISTS t3 (a INTEGER, b CHAR(100), c DATE);");

        assert cmd.isTemporary();
        assert cmd.getIfNotExists();
        assert cmd.getProperties() == null;
        assert cmd.getConstraints().size() == 0;

        columns = cmd.getColumns();
        assert columns.size() == 3;

        checkColumnDecl(columns.get(0), "t3", "a", SQLDataType.INTEGER);
        checkColumnDecl(columns.get(1), "t3", "b", SQLDataType.CHAR);
        assert columns.get(1).getType().getLength() == 100;
        checkColumnDecl(columns.get(2), "t3", "c", SQLDataType.DATE);
    }


    /**
     * Exercises the <tt>PROPERTIES ( ... )</tt> clause of the
     * <tt>CREATE TABLE</tt> command.
     */
    public void testParseCreateTableProperties() {
        CreateTableCommand cmd;
        List<ColumnInfo> columns;
        CommandProperties props;

        cmd = (CreateTableCommand) ParseUtil.parseCommand(
            "CREATE TEMPORARY TABLE t1 (a INTEGER, b CHAR(100), c DATE) " +
            "properties (foo = 'bar');");

        assert cmd.isTemporary();
        assert !cmd.getIfNotExists();
        assert cmd.getProperties() != null;
        assert cmd.getConstraints().size() == 0;

        columns = cmd.getColumns();
        assert columns.size() == 3;

        checkColumnDecl(columns.get(0), "t1", "a", SQLDataType.INTEGER);
        checkColumnDecl(columns.get(1), "t1", "b", SQLDataType.CHAR);
        assert columns.get(1).getType().getLength() == 100;
        checkColumnDecl(columns.get(2), "t1", "c", SQLDataType.DATE);

        props = cmd.getProperties();
        assert props.getNames().size() == 1;
        assert "bar".equals(props.get("foo"));


        cmd = (CreateTableCommand) ParseUtil.parseCommand(
            "CREATE TABLE IF NOT EXISTS t2 (a INTEGER, b CHAR(100), c DATE) " +
            "properties (FOO = 'Bar', n=500);");

        assert !cmd.isTemporary();
        assert cmd.getIfNotExists();
        assert cmd.getProperties() != null;
        assert cmd.getConstraints().size() == 0;

        columns = cmd.getColumns();
        assert columns.size() == 3;

        checkColumnDecl(columns.get(0), "t2", "a", SQLDataType.INTEGER);
        checkColumnDecl(columns.get(1), "t2", "b", SQLDataType.CHAR);
        assert columns.get(1).getType().getLength() == 100;
        checkColumnDecl(columns.get(2), "t2", "c", SQLDataType.DATE);

        props = cmd.getProperties();
        assert props.getNames().size() == 2;
        assert "Bar".equals(props.get("foo"));
        assert Integer.valueOf(500).equals(props.get("n"));
    }
}
