package edu.caltech.test.nanodb.expressions;


import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.testng.annotations.*;

import edu.caltech.nanodb.expressions.TypeCastException;
import edu.caltech.nanodb.expressions.TypeConverter;
import edu.caltech.nanodb.relations.SQLDataType;


/**
 * This class exercises the type-converter class.
 */
@Test(groups={"framework"})
public class TestTypeConverter {

    // === BOOLEAN ===========================================================

    public void testGetBooleanValue() {
        assert Boolean.TRUE.equals(TypeConverter.getBooleanValue(Integer.valueOf(3)));
        assert Boolean.TRUE.equals(TypeConverter.getBooleanValue(Boolean.TRUE));

        assert Boolean.FALSE.equals(TypeConverter.getBooleanValue(Integer.valueOf(0)));
        assert Boolean.FALSE.equals(TypeConverter.getBooleanValue(Boolean.FALSE));

        assert null == TypeConverter.getBooleanValue(null);
    }

    @Test(expectedExceptions={TypeCastException.class})
    public void testGetBooleanValueError() {
        TypeConverter.getBooleanValue(new Object());
    }

    // === BYTE ==============================================================

    public void testGetByteValue() {
        assert TypeConverter.getByteValue(null) == null;

        assert Byte.valueOf((byte) 11).equals(TypeConverter.getByteValue(Byte.valueOf((byte) 11)));

        assert Byte.valueOf((byte) 3).equals(TypeConverter.getByteValue(Integer.valueOf(3)));
        assert Byte.valueOf((byte) 15).equals(TypeConverter.getByteValue("15"));
        assert Byte.valueOf((byte) 21).equals(TypeConverter.getByteValue(Float.valueOf(21.234f)));
    }

    @Test(expectedExceptions={TypeCastException.class})
    public void testGetByteFromBooleanError() {
        TypeConverter.getByteValue(Boolean.TRUE);
    }

    @Test(expectedExceptions={TypeCastException.class})
    public void testGetByteFromObjectError() {
        TypeConverter.getByteValue(new Object());
    }

    @Test(expectedExceptions={TypeCastException.class})
    public void testGetByteFromStringError() {
        TypeConverter.getByteValue("123a");
    }

    // === SHORT =============================================================

    public void testGetShortValue() {
        assert TypeConverter.getShortValue(null) == null;

        assert Short.valueOf((short) 11).equals(TypeConverter.getShortValue(Short.valueOf((short) 11)));

        assert Short.valueOf((short) 3).equals(TypeConverter.getShortValue(Integer.valueOf(3)));
        assert Short.valueOf((short) 15).equals(TypeConverter.getShortValue("15"));
        assert Short.valueOf((short) 21).equals(TypeConverter.getShortValue(Float.valueOf(21.234f)));
    }

    @Test(expectedExceptions={TypeCastException.class})
    public void testGetShortFromBooleanError() {
        TypeConverter.getShortValue(Boolean.TRUE);
    }

    @Test(expectedExceptions={TypeCastException.class})
    public void testGetShortFromObjectError() {
        TypeConverter.getShortValue(new Object());
    }

    @Test(expectedExceptions={TypeCastException.class})
    public void testGetShortFromStringError() {
        TypeConverter.getShortValue("123a");
    }

    // === INTEGER ===========================================================

    public void testGetIntegerValue() {
        assert TypeConverter.getIntegerValue(null) == null;

        assert Integer.valueOf(11).equals(TypeConverter.getIntegerValue(Integer.valueOf(11)));

        assert Integer.valueOf(3).equals(TypeConverter.getIntegerValue(Long.valueOf(3)));
        assert Integer.valueOf(15).equals(TypeConverter.getIntegerValue("15"));
        assert Integer.valueOf(21).equals(TypeConverter.getIntegerValue(Float.valueOf(21.234f)));
    }

    @Test(expectedExceptions={TypeCastException.class})
    public void testGetIntegerFromBooleanError() {
        TypeConverter.getIntegerValue(Boolean.TRUE);
    }

    @Test(expectedExceptions={TypeCastException.class})
    public void testGetIntegerFromObjectError() {
        TypeConverter.getIntegerValue(new Object());
    }

    @Test(expectedExceptions={TypeCastException.class})
    public void testGetIntegerFromStringError() {
        TypeConverter.getIntegerValue("123a");
    }

    // === LONG ==============================================================

    public void testGetLongValue() {
        assert TypeConverter.getLongValue(null) == null;

        assert Long.valueOf(11).equals(TypeConverter.getLongValue(Long.valueOf(11)));

        assert Long.valueOf(3).equals(TypeConverter.getLongValue(Integer.valueOf(3)));
        assert Long.valueOf(15).equals(TypeConverter.getLongValue("15"));
        assert Long.valueOf(21).equals(TypeConverter.getLongValue(Float.valueOf(21.234f)));
    }

    @Test(expectedExceptions={TypeCastException.class})
    public void testGetLongFromBooleanError() {
        TypeConverter.getLongValue(Boolean.TRUE);
    }

    @Test(expectedExceptions={TypeCastException.class})
    public void testGetLongFromObjectError() {
        TypeConverter.getLongValue(new Object());
    }

    @Test(expectedExceptions={TypeCastException.class})
    public void testGetLongFromStringError() {
        TypeConverter.getLongValue("123a");
    }

    // === FLOAT =============================================================

    public void testGetFloatValue() {
        assert TypeConverter.getFloatValue(null) == null;

        assert Float.valueOf(11.125f).equals(TypeConverter.getFloatValue(Float.valueOf(11.125f)));

        assert Float.valueOf(3).equals(TypeConverter.getFloatValue(Integer.valueOf(3)));
        assert Float.valueOf(15.2579f).equals(TypeConverter.getFloatValue("15.2579"));
        assert Float.valueOf(21.234f).equals(TypeConverter.getFloatValue(Float.valueOf(21.234f)));
    }

    @Test(expectedExceptions={TypeCastException.class})
    public void testGetFloatFromBooleanError() {
        TypeConverter.getFloatValue(Boolean.TRUE);
    }

    @Test(expectedExceptions={TypeCastException.class})
    public void testGetFloatFromObjectError() {
        TypeConverter.getFloatValue(new Object());
    }

    @Test(expectedExceptions={TypeCastException.class})
    public void testGetFloatFromStringError() {
        TypeConverter.getFloatValue("123a");
    }

    // === DOUBLE ============================================================

    public void testGetDoubleValue() {
        assert TypeConverter.getDoubleValue(null) == null;

        assert Double.valueOf(11.125).equals(TypeConverter.getDoubleValue(Double.valueOf(11.125)));

        assert Double.valueOf(3).equals(TypeConverter.getDoubleValue(Integer.valueOf(3)));
        assert Double.valueOf(15.2579).equals(TypeConverter.getDoubleValue("15.2579"));
        assert Double.valueOf(21.5).equals(TypeConverter.getDoubleValue(Float.valueOf(21.5f)));
    }

    @Test(expectedExceptions={TypeCastException.class})
    public void testGetDoubleFromBooleanError() {
        TypeConverter.getDoubleValue(Boolean.TRUE);
    }

    @Test(expectedExceptions={TypeCastException.class})
    public void testGetDoubleFromObjectError() {
        TypeConverter.getDoubleValue(new Object());
    }

    @Test(expectedExceptions={TypeCastException.class})
    public void testGetDoubleFromStringError() {
        TypeConverter.getDoubleValue("123a");
    }

    // === BigInteger ========================================================

    public void testGetBigIntegerValue() {
        assert TypeConverter.getBigIntegerValue(null) == null;

        assert BigInteger.valueOf(11).equals(TypeConverter.getBigIntegerValue(Double.valueOf(11.125)));
        assert BigInteger.valueOf(3).equals(TypeConverter.getBigIntegerValue(Integer.valueOf(3)));
        assert BigInteger.valueOf(15).equals(TypeConverter.getBigIntegerValue("15"));
        assert BigInteger.valueOf(21).equals(TypeConverter.getBigIntegerValue(Float.valueOf(21.5f)));
        assert BigInteger.valueOf(45).equals(TypeConverter.getBigIntegerValue(Long.valueOf(45)));
    }

    @Test(expectedExceptions={TypeCastException.class})
    public void testGetBigIntegerFromBooleanError() {
        TypeConverter.getBigIntegerValue(Boolean.TRUE);
    }

    @Test(expectedExceptions={TypeCastException.class})
    public void testGetBigIntegerFromObjectError() {
        TypeConverter.getBigIntegerValue(new Object());
    }

    @Test(expectedExceptions={TypeCastException.class})
    public void testGetBigIntegerFromStringError() {
        TypeConverter.getBigIntegerValue("123a");
    }

    // === NUMERIC ===========================================================

    public void testGetBigDecimalValue() {
        assert TypeConverter.getBigDecimalValue(null) == null;

        // Specify decimal numbers as strings to ensure they are represented
        // precisely for the comparison.
        assert new BigDecimal("11.125").equals(TypeConverter.getBigDecimalValue(Double.valueOf(11.125)));
        assert new BigDecimal(3).equals(TypeConverter.getBigDecimalValue(Integer.valueOf(3)));
        assert new BigDecimal("15.2579").equals(TypeConverter.getBigDecimalValue("15.2579"));
        assert new BigDecimal("21.5").equals(TypeConverter.getBigDecimalValue(Float.valueOf(21.5f)));
        assert new BigDecimal(45).equals(TypeConverter.getBigDecimalValue(Long.valueOf(45)));
    }

    @Test(expectedExceptions={TypeCastException.class})
    public void testGetBigDecimalFromBooleanError() {
        TypeConverter.getBigDecimalValue(Boolean.TRUE);
    }

    @Test(expectedExceptions={TypeCastException.class})
    public void testGetBigDecimalFromObjectError() {
        TypeConverter.getBigDecimalValue(new Object());
    }

    @Test(expectedExceptions={TypeCastException.class})
    public void testGetBigDecimalFromStringError() {
        TypeConverter.getBigDecimalValue("123a");
    }

    // === MISC FUNCTIONS ====================================================

    public void testGetSQLType() {
        // Recognized types:

        assert TypeConverter.getSQLType(Boolean.TRUE) == SQLDataType.TINYINT;

        assert TypeConverter.getSQLType(Byte.valueOf((byte) 3)) == SQLDataType.TINYINT;
        assert TypeConverter.getSQLType(Short.valueOf((short) 3)) == SQLDataType.SMALLINT;
        assert TypeConverter.getSQLType(Integer.valueOf(3)) == SQLDataType.INTEGER;
        assert TypeConverter.getSQLType(Long.valueOf(3)) == SQLDataType.BIGINT;

        assert TypeConverter.getSQLType(Float.valueOf(3.0f)) == SQLDataType.FLOAT;
        assert TypeConverter.getSQLType(Double.valueOf(3.0)) == SQLDataType.DOUBLE;

        // Strings are treated as VARCHAR, not CHAR.
        assert TypeConverter.getSQLType("three") == SQLDataType.VARCHAR;

        assert TypeConverter.getSQLType(BigDecimal.ONE) == SQLDataType.NUMERIC;

        assert TypeConverter.getSQLType(LocalDateTime.now()) == SQLDataType.DATETIME;
        assert TypeConverter.getSQLType(LocalDate.now()) == SQLDataType.DATE;
        assert TypeConverter.getSQLType(LocalTime.now()) == SQLDataType.TIME;

        // Currently, BigInteger is not exposed in the SQL interface.
        // Currently, FilePointer is not exposed in the SQL interface.

        // Unrecognized types:

        assert TypeConverter.getSQLType(new Object()) == null;
    }
}
