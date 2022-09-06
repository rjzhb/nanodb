package edu.caltech.nanodb.relations;


import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import edu.caltech.nanodb.storage.TupleFile;


/**
 * Created by donnie on 7/16/17.
 */
public class TableUtils {


    public static ColumnRefs findIndexOnColumns(TableInfo tableInfo,
                                                int[] colIndexes) {
        Schema schema = tableInfo.getSchema();

        for (ColumnRefs index : schema.getIndexes()) {
            if (index.hasSameColumns(colIndexes))
                return index;
        }

        return null;
    }


    public static Map<Integer, Object> makeValueMap(int[] colIndexes,
        Tuple tup, int[] refColIndexes) {

        Map<Integer, Object> values = new HashMap<>();

        for (int i = 0; i < colIndexes.length; i++) {
            int key = refColIndexes == null ? colIndexes[i] : refColIndexes[i];

            values.put(key, tup.getColumnValue(colIndexes[i]));
        }

        return values;
    }


    public static Map<Integer, Object> makeValueMap(int[] colIndexes, Tuple tup) {
        return makeValueMap(colIndexes, tup, null);
    }


    public static boolean hasEqualValues(Tuple tup, Map<Integer, Object> values) {
        for (Map.Entry<Integer, Object> entry : values.entrySet()) {
            Object tupleValue = tup.getColumnValue(entry.getKey());
            if (!Objects.equals(tupleValue, entry.getValue()))
                return false;
        }
        return true;
    }


    public static Tuple findFirstTupleEquals(TupleFile tupleFile,
        Map<Integer, Object> values) throws IOException {

        Tuple tup = tupleFile.getFirstTuple();
        while (tup != null) {
            if (hasEqualValues(tup, values))
                return tup;

            tup = tupleFile.getNextTuple(tup);
        }
        return null;
    }


    public static Tuple findNextTupleEquals(TupleFile tupleFile, Tuple prevTup,
        Map<Integer, Object> values) throws IOException {

        Tuple tup = tupleFile.getNextTuple(prevTup);
        while (tup != null) {
            if (hasEqualValues(tup, values))
                return tup;

            tup = tupleFile.getNextTuple(tup);
        }
        return null;
    }
}
