package edu.caltech.nanodb.relations;


import edu.caltech.nanodb.expressions.TupleLiteral;
import edu.caltech.nanodb.expressions.TypeConverter;


public class TupleUtils {
    public static Tuple coerceToSchema(Tuple input, Schema schema) {
        TupleLiteral result = new TupleLiteral();

        for (int i = 0; i < input.getColumnCount(); i++) {
            Object value = input.getColumnValue(i);
            ColumnType colType =  schema.getColumnInfo(i).getType();
            Object coerced = TypeConverter.coerceTo(value, colType);
            result.addValue(coerced);
        }

        return result;
    }
}
