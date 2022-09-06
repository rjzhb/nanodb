package edu.caltech.test.nanodb.relations;


import java.util.Set;

import edu.caltech.nanodb.relations.ColumnInfo;
import edu.caltech.nanodb.relations.ColumnType;
import edu.caltech.nanodb.relations.Schema;
import org.testng.annotations.Test;



@Test(groups={"framework"})
public class TestSchema {
    public void testEmptySchema() {
        Schema s = new Schema();
        assert s.numColumns() == 0;
    }


    public void testBasicSchemaOps() {
        Schema s = new Schema(
            new ColumnInfo("A", ColumnType.INTEGER),
            new ColumnInfo("B", "T1", ColumnType.VARCHAR(20)),
            new ColumnInfo("C", "T2", ColumnType.BIGINT)
        );

        ColumnInfo ci;

        ci = s.getColumnInfo(0);
        assert ci.getName().equals("A");
        assert ci.getTableName() == null;
        assert ci.getType().equals(ColumnType.INTEGER);

        ci = s.getColumnInfo(1);
        assert ci.getName().equals("B");
        assert ci.getTableName().equals("T1");
        assert ci.getType().equals(ColumnType.VARCHAR(20));

        ci = s.getColumnInfo(2);
        assert ci.getName().equals("C");
        assert ci.getTableName().equals("T2");
        assert ci.getType().equals(ColumnType.BIGINT);
    }


    public void testSchemaConcatenation() {
        Schema s1 = new Schema(
            new ColumnInfo("A", ColumnType.INTEGER),
            new ColumnInfo("B", ColumnType.BIGINT)
        );

        Schema s2 = new Schema(
            new ColumnInfo("C", ColumnType.DATE),
            new ColumnInfo("D", ColumnType.TINYINT)
        );

        Schema s3 = new Schema(s1);

        // assert s3.equals(s1);
        assert s3.numColumns() == 2;
        assert s3.getColumnInfo(0).getName().equals("A");
        assert s3.getColumnInfo(1).getName().equals("B");

        Schema s4 = new Schema(s1, s2);

        // assert !s4.equals(s1);
        assert s4.numColumns() == 4;
        assert s4.getColumnInfo(0).getName().equals("A");
        assert s4.getColumnInfo(1).getName().equals("B");
        assert s4.getColumnInfo(2).getName().equals("C");
        assert s4.getColumnInfo(3).getName().equals("D");

        Schema s5 = new Schema(s2, s1);

        // assert !s5.equals(s2);
        assert s5.numColumns() == 4;
        assert s5.getColumnInfo(0).getName().equals("C");
        assert s5.getColumnInfo(1).getName().equals("D");
        assert s5.getColumnInfo(2).getName().equals("A");
        assert s5.getColumnInfo(3).getName().equals("B");
    }


    public void testCommonTableNames() {
        Schema s1 = new Schema(
            new ColumnInfo("A", "T1", ColumnType.INTEGER),
            new ColumnInfo("B", ColumnType.INTEGER),
            new ColumnInfo("C", "T2", ColumnType.INTEGER),
            new ColumnInfo("D", "T3", ColumnType.INTEGER),
            new ColumnInfo("E", ColumnType.INTEGER)
        );

        Schema s2 = new Schema(
            new ColumnInfo("A", ColumnType.INTEGER),
            new ColumnInfo("B", "T1", ColumnType.INTEGER),
            new ColumnInfo("C", ColumnType.INTEGER),
            new ColumnInfo("D", "T2", ColumnType.INTEGER),
            new ColumnInfo("E", "T4", ColumnType.INTEGER)
        );

        Schema s3 = new Schema(
            new ColumnInfo("A", ColumnType.INTEGER),
            new ColumnInfo("B", ColumnType.INTEGER),
            new ColumnInfo("C", ColumnType.INTEGER),
            new ColumnInfo("D", ColumnType.INTEGER),
            new ColumnInfo("E", ColumnType.INTEGER)
        );

        Set<String> common;

        common = s1.getCommonTableNames(s2);
        assert common.size() == 2 : "Expected 2 common table names; got " + common.size();
        assert common.contains("T1");
        assert common.contains("T2");

        common = s1.getCommonTableNames(s3);
        assert common.size() == 0;

        common = s2.getCommonTableNames(s1);
        assert common.size() == 2;
        assert common.contains("T1");
        assert common.contains("T2");

        common = s1.getCommonTableNames(s1);
        assert common.size() == 3;
        assert common.contains("T1");
        assert common.contains("T2");
        assert common.contains("T3");

        common = s3.getCommonTableNames(s3);
        assert common.size() == 0;
    }
}
