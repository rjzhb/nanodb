package edu.caltech.nanodb.expressions;


import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.time.format.DateTimeParseException;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAmount;
import java.util.HashMap;

import edu.caltech.nanodb.relations.ColumnType;
import edu.caltech.nanodb.relations.SQLDataType;
import edu.caltech.nanodb.util.Pair;


/**
 * This class provides a whole bunch of helper functions for performing type
 * conversions on values produced by various expressions.  All values are passed
 * around as <code>Object</code> references, and this means they may need to be
 * converted into specific value types at some point.  This class contains all
 * of the conversion logic so it's in one place.
 */
public class TypeConverter {

    /**
     * A mapping from the various Java types used for expression results, to their
     * corresponding SQL data types.  The mapping is populated by a static
     * initializer block.
     */
    private static final HashMap<Class, SQLDataType> sqlTypeMapping;

    static {
        sqlTypeMapping = new HashMap<>();

        sqlTypeMapping.put(Null.class, SQLDataType.NULL);

        sqlTypeMapping.put(Boolean.class, SQLDataType.TINYINT);

        sqlTypeMapping.put(Byte.class, SQLDataType.TINYINT);
        sqlTypeMapping.put(Short.class, SQLDataType.SMALLINT);
        sqlTypeMapping.put(Integer.class, SQLDataType.INTEGER);
        sqlTypeMapping.put(Long.class, SQLDataType.BIGINT);

        sqlTypeMapping.put(Float.class, SQLDataType.FLOAT);
        sqlTypeMapping.put(Double.class, SQLDataType.DOUBLE);

        // Could choose CHAR or VARCHAR, but we will use VARCHAR
        sqlTypeMapping.put(String.class, SQLDataType.VARCHAR);

        sqlTypeMapping.put(BigInteger.class, SQLDataType.NUMERIC);
        sqlTypeMapping.put(BigDecimal.class, SQLDataType.NUMERIC);

        sqlTypeMapping.put(LocalDate.class, SQLDataType.DATE);
        sqlTypeMapping.put(LocalTime.class, SQLDataType.TIME);

        // Could choose DATETIME or TIMESTAMP, but we will use DATETIME
        sqlTypeMapping.put(LocalDateTime.class, SQLDataType.DATETIME);

        sqlTypeMapping.put(Duration.class, SQLDataType.INTERVAL);
        sqlTypeMapping.put(Period.class, SQLDataType.INTERVAL);

        // TODO:  others, in time...
    }


    /**
     * This method attempts to convert the input value into a
     * {@link java.lang.Boolean} value.  If the input is a nonzero number then
     * the result is {@link java.lang.Boolean#TRUE}; if it is a zero number then
     * the result is {@link java.lang.Boolean#FALSE}.  Otherwise a
     * {@link TypeCastException} is thrown.
     *
     * @param obj the input value to cast
     *
     * @return the input value cast to a <tt>Boolean</tt>
     *
     * @throws TypeCastException if the input value cannot be cast to
     *         a Boolean value.
     */
    public static Boolean getBooleanValue(Object obj) {
        if (obj == null)
            return null;

        Boolean result;

        if (obj instanceof Boolean) {
            result = (Boolean) obj;
        }
        else if (obj instanceof Number) {
            // If it's a nonzero number, return TRUE.  Otherwise, FALSE.
            Number num = (Number) obj;
            result = (num.intValue() != 0);
        }
        else {
            throw new TypeCastException("Cannot convert type " +
                obj.getClass() + " to byte.");
        }

        return result;
    }


    /**
     * This method attempts to convert the input value into a
     * {@link java.lang.Byte} value.  If the input is a number then the result
     * is generated from the {@link java.lang.Number#byteValue} method, possibly
     * causing truncation or overflow to occur.  If the input is a
     * {@link java.lang.String} that can be parsed into a byte then the result
     * is the parsed value.  If none of these cases hold then a
     * {@link TypeCastException} is thrown.
     *
     * @param obj the input value to cast
     *
     * @return the input value cast to a <tt>Byte</tt>
     *
     * @throws TypeCastException if the input value cannot be cast to a byte.
     */
    public static Byte getByteValue(Object obj) {
        if (obj == null)
            return null;

        Byte result;

        if (obj instanceof Byte) {
            result = (Byte) obj;
        }
        else if (obj instanceof Number) {
            // TODO:  Would be nice to detect overflow or truncation issues and
            //        log warnings about them!

            Number num = (Number) obj;
            result = num.byteValue();
        }
        else if (obj instanceof String) {
            try {
                result = Byte.valueOf((String) obj);
            }
            catch (NumberFormatException nfe) {
                throw new TypeCastException("Cannot convert string to byte.", nfe);
            }
        }
        else {
            throw new TypeCastException("Cannot convert type \"" +
                obj.getClass() + "\" to byte.");
        }

        return result;
    }


    /**
     * This method attempts to convert the input value into a
     * {@link java.lang.Short} value.  If the input is a number then the result
     * is generated from the {@link java.lang.Number#shortValue} method,
     * possibly causing truncation or overflow to occur.  If the input is a
     * {@link java.lang.String} that can be parsed into a short then the result
     * is the parsed value.  If none of these cases hold then a
     * {@link TypeCastException} is thrown.
     *
     * @param obj the input value to cast
     *
     * @return the input value cast to a <tt>Short</tt>
     *
     * @throws TypeCastException if the input value cannot be cast to a short.
     */
    public static Short getShortValue(Object obj) {
        if (obj == null)
            return null;

        Short result;

        if (obj instanceof Short) {
            result = (Short) obj;
        }
        else if (obj instanceof Number) {
            // TODO:  Would be nice to detect overflow or truncation issues and log
            //        warnings about them!

            Number num = (Number) obj;
            result = num.shortValue();
        }
        else if (obj instanceof String) {
            try {
                result = Short.valueOf((String) obj);
            }
            catch (NumberFormatException nfe) {
                throw new TypeCastException("Cannot convert string to short.", nfe);
            }
        }
        else {
            throw new TypeCastException("Cannot convert type \"" +
                obj.getClass() + "\" to short.");
        }

        return result;
    }


    /**
     * This method attempts to convert the input value into an
     * {@link java.lang.Integer} value.  If the input is a number then the
     * result is generated from the {@link java.lang.Number#intValue} method,
     * possibly causing truncation or overflow to occur.  If the input is a
     * {@link java.lang.String} that can be parsed into an integer then the
     * result is the parsed value.  If none of these cases hold then a
     * {@link TypeCastException} is thrown.
     *
     * @param obj the input value to cast
     *
     * @return the input value cast to an <tt>Integer</tt>
     *
     * @throws TypeCastException if the input value cannot be cast to an integer.
     */
    public static Integer getIntegerValue(Object obj) {
        if (obj == null)
            return null;

        Integer result;

        if (obj instanceof Integer) {
            result = (Integer) obj;
        }
        else if (obj instanceof Number) {
            // TODO:  Would be nice to detect overflow or truncation issues and log
            //        warnings about them!

            Number num = (Number) obj;
            result = num.intValue();
        }
        else if (obj instanceof String) {
            try {
                result = Integer.valueOf((String) obj);
            }
            catch (NumberFormatException nfe) {
                throw new TypeCastException("Cannot convert string to integer.", nfe);
            }
        }
        else {
            throw new TypeCastException("Cannot convert type \"" +
                obj.getClass() + "\" to integer.");
        }

        return result;
    }


    /**
     * This method attempts to convert the input value into a
     * {@link java.lang.Long} value.  If the input is a number then the result
     * is generated from the {@link java.lang.Number#longValue} method, possibly
     * causing truncation or overflow to occur.  If the input is a
     * {@link java.lang.String} that can be parsed into a long then the result
     * is the parsed value.  If none of these cases hold then a
     * {@link TypeCastException} is thrown.
     *
     * @param obj the input value to cast
     *
     * @return the input value cast to a <tt>Long</tt>
     *
     * @throws TypeCastException if the input value cannot be cast to a long.
     */
    public static Long getLongValue(Object obj) {
        if (obj == null)
            return null;

        Long result;

        if (obj instanceof Long) {
            result = (Long) obj;
        }
        else if (obj instanceof Number) {
            // TODO:  Would be nice to detect overflow or truncation issues and log
            //        warnings about them!

            Number num = (Number) obj;
            result = num.longValue();
        }
        else if (obj instanceof String) {
            try {
                result = Long.valueOf((String) obj);
            }
            catch (NumberFormatException nfe) {
                throw new TypeCastException("Cannot convert string to long.", nfe);
            }
        }
        else {
            throw new TypeCastException("Cannot convert type \"" +
                obj.getClass() + "\" to long.");
        }

        return result;
    }


    /**
     * This method attempts to convert the input value into a
     * {@link java.lang.Float} value.  If the input is a number then the result
     * is generated from the {@link java.lang.Number#floatValue} method,
     * possibly causing truncation or overflow to occur.  If the input is a
     * {@link java.lang.String} that can be parsed into a float then the result
     * is the parsed value.  If none of these cases hold then a
     * {@link TypeCastException} is thrown.
     *
     * @param obj the input value to cast
     *
     * @return the input value cast to a <tt>Float</tt>
     *
     * @throws TypeCastException if the input value cannot be cast to a float.
     */
    public static Float getFloatValue(Object obj) {
        if (obj == null)
            return null;

        Float result;

        if (obj instanceof Float) {
            result = (Float) obj;
        }
        else if (obj instanceof Number) {
            // TODO:  Would be nice to detect overflow or truncation issues and log
            //        warnings about them!

            Number num = (Number) obj;
            result = num.floatValue();
        }
        else if (obj instanceof String) {
            try {
                result = Float.valueOf((String) obj);
            }
            catch (NumberFormatException nfe) {
                throw new TypeCastException("Cannot convert string to float.", nfe);
            }
        }
        else {
            throw new TypeCastException("Cannot convert type \"" +
                obj.getClass() + "\" to float.");
        }

        return result;
    }


    /**
     * This method attempts to convert the input value into a
     * {@link java.lang.Double} value.  If the input is a number then the result
     * is generated from the {@link java.lang.Number#doubleValue} method,
     * possibly causing truncation or overflow to occur.  If the input is a
     * {@link java.lang.String} that can be parsed into a double then the result
     * is the parsed value.  If none of these cases hold then a
     * {@link TypeCastException} is thrown.
     *
     * @param obj the input value to cast
     *
     * @return the input value cast to a <tt>Double</tt>
     *
     * @throws TypeCastException if the input value cannot be cast to a double.
     */
    public static Double getDoubleValue(Object obj) {
        if (obj == null)
            return null;

        Double result;

        if (obj instanceof Double) {
            result = (Double) obj;
        }
        else if (obj instanceof Number) {
            // This is the only conversion that doesn't have the chance of
            // truncating or overflowing, as long as primitive types are
            // involved.  The conversion may lose precision when converting
            // from long to double.

            // We would also lose precision or truncate or overflow if
            // converting from BigInteger or BigDouble to decimal; that could
            // happen if storing a BigDecimal value into a double column.

            Number num = (Number) obj;
            result = num.doubleValue();
        }
        else if (obj instanceof String) {
            try {
                result = Double.valueOf((String) obj);
            }
            catch (NumberFormatException nfe) {
                throw new TypeCastException("Cannot convert string to double.", nfe);
            }
        }
        else {
            throw new TypeCastException("Cannot convert type \"" +
                obj.getClass() + "\" to double.");
        }

        return result;
    }


    /**
     * This method attempts to convert the input value into a
     * {@link java.math.BigInteger} value.  If the input is a number then the
     * result is generated from the {@link java.lang.Number#longValue} method,
     * possibly causing truncation to occur.  If the input is a
     * {@link java.lang.String} that can be parsed into a BigInteger then the
     * result is the parsed value.  If none of these cases hold then a
     * {@link TypeCastException} is thrown.
     *
     * @param obj the input value to cast
     *
     * @return the input value cast to a <tt>BigInteger</tt>
     *
     * @throws TypeCastException if the input value cannot be cast to a
     *         BigInteger.
     */
    public static BigInteger getBigIntegerValue(Object obj) {
        if (obj == null)
            return null;

        BigInteger result;

        if (obj instanceof BigInteger) {
            result = (BigInteger) obj;
        }
        else if (obj instanceof Number) {
            // TODO:  This could go wrong in lots of ways, and we should
            //        probably think about exactly how to handle them.
            Number num = (Number) obj;
            result = BigInteger.valueOf(num.longValue());
        }
        else if (obj instanceof String) {
            try {
                result = new BigInteger((String) obj);
            }
            catch (NumberFormatException nfe) {
                throw new TypeCastException("Cannot convert string to BigInteger.", nfe);
            }
        }
        else {
            throw new TypeCastException("Cannot convert type \"" +
                obj.getClass() + "\" to BigInteger.");
        }

        return result;
    }


    /**
     * This method attempts to convert the input value into a
     * {@link java.math.BigDecimal} value.  If the input is a number then the
     * result is generated from the {@link java.lang.Number#longValue} method,
     * possibly causing truncation to occur.  If the input is a
     * {@link java.lang.String} that can be parsed into a BigInteger then the
     * result is the parsed value.  If none of these cases hold then a
     * {@link TypeCastException} is thrown.
     *
     * @param obj the input value to cast
     *
     * @return the input value cast to a <tt>BigInteger</tt>
     *
     * @throws TypeCastException if the input value cannot be cast to a
     *         BigInteger.
     */
    public static BigDecimal getBigDecimalValue(Object obj) {
        if (obj == null)
            return null;

        BigDecimal result;

        if (obj instanceof BigDecimal) {
            result = (BigDecimal) obj;
        }
        else if (obj instanceof BigInteger) {
            result = new BigDecimal((BigInteger) obj);
        }
        else if (obj instanceof Double || obj instanceof Float) {
            result = new BigDecimal(((Number) obj).doubleValue());
        }
        else if (obj instanceof Number) {
            result = new BigDecimal(((Number) obj).longValue());
        }
        else if (obj instanceof String) {
            try {
                result = new BigDecimal((String) obj);
            }
            catch (NumberFormatException nfe) {
                throw new TypeCastException("Cannot convert string to BigDecimal.", nfe);
            }
        }
        else {
            throw new TypeCastException("Cannot convert type \"" +
                obj.getClass() + "\" to BigDecimal.");
        }

        return result;
    }


    /**
     * This method converts the input value into a {@link java.lang.String}
     * value by calling {@link java.lang.Object#toString} on the input.
     *
     * @param obj the input value to cast
     *
     * @return the input value cast to a <tt>String</tt>
     */
    public static String getStringValue(Object obj) {
        // If obj is a String, well, String.toString() just returns itself!  :-)
        return (obj != null ? obj.toString() : null);
    }


    public static LocalDate getDateValue(Object obj) {
        if (obj == null)
            return null;

        LocalDate result;

        if (obj instanceof LocalDate) {
            result = (LocalDate) obj;
        }
        else if (obj instanceof LocalDateTime) {
            result = ((LocalDateTime) obj).toLocalDate();
        }
        else if (obj instanceof String) {
            // Only try to convert date strings.  If we convert date/time
            // strings and throw away the time, this is truncation.  Would be
            // better to provide a helper function to make a date from a
            // date/time, so that we know the user intended it.
            String s = (String) obj;
            try {
                result = DateTimeUtils.parseDate(s);
            }
            catch (DateTimeParseException e) {
                throw new TypeCastException("Cannot convert string \"" +
                    s + "\" to LocalDate.", e);
            }
        }
        else {
            throw new TypeCastException("Cannot convert type \"" +
                obj.getClass() + "\" to LocalDate.");
        }

        return result;
    }


    public static LocalTime getTimeValue(Object obj) {
        if (obj == null)
            return null;

        LocalTime result;

        if (obj instanceof LocalTime) {
            result = (LocalTime) obj;
        }
        else if (obj instanceof LocalDateTime) {
            result = ((LocalDateTime) obj).toLocalTime();
        }
        else if (obj instanceof String) {
            // Only try to convert time strings.  If we convert date/time
            // strings and throw away the date, this is truncation.  Would be
            // better to provide a helper function to make a time from a
            // date/time, so that we know the user intended it.
            String s = (String) obj;
            try {
                result = DateTimeUtils.parseTime(s);
            }
            catch (DateTimeParseException e) {
                throw new TypeCastException("Cannot convert string \"" +
                    s + "\" to LocalTime.", e);
            }
        }
        else {
            throw new TypeCastException("Cannot convert type \"" +
                obj.getClass() + "\" to LocalTime.");
        }

        return result;
    }


    public static LocalDateTime getDateTimeValue(Object obj) {
        if (obj == null)
            return null;

        LocalDateTime result;

        if (obj instanceof LocalDateTime) {
            result = (LocalDateTime) obj;
        }
        else if (obj instanceof LocalDate) {
            // Convert from LocalDate to LocalDateTime by assuming 12:00AM
            result = ((LocalDate) obj).atStartOfDay();
        }
        else if (obj instanceof String) {
            String s = (String) obj;
            try {
                // First try converting to LocalDateTime
                result = DateTimeUtils.parseDateTime(s);
            }
            catch (DateTimeParseException e1) {
                try {
                    // If that didn't work, try converting to LocalDate, then
                    // convert to LocalDateTime by assuming 12:00AM.
                    LocalDate d = DateTimeUtils.parseDate(s);
                    result = d.atStartOfDay();
                }
                catch (DateTimeParseException e2) {
                    throw new TypeCastException("Cannot convert string \"" +
                        s + "\" to LocalDateTime.");
                }
            }
        }
        else {
            throw new TypeCastException("Cannot convert type \"" +
                obj.getClass() + "\" to LocalDateTime.");
        }

        return result;

    }


    /**
     * This function takes two arguments and coerces them to be the same numeric
     * type, for use with arithmetic operations.
     * <p>
     * If either or both of the arguments are <tt>null</tt> then no coercion is
     * performed, and the results are returned in a pair-object.  The reason for
     * this is that the comparison will simply evaluate to <tt>UNKNOWN</tt>, so
     * no coercion is required.
     *
     * @param obj1 the first input value to cast
     * @param obj2 the second input value to cast
     *
     * @return an object holding the two input values, both converted to a type
     *         suitable for arithmetic
     *
     * @throws TypeCastException if both inputs are non-<tt>null</tt> and they
     *         cannot both be cast to types suitable for arithmetic.
     */
    public static Pair coerceArithmetic(Object obj1, Object obj2)
        throws TypeCastException {

        if (obj1 != null && obj2 != null) {
            if (obj1 instanceof Number || obj2 instanceof Number) {
                if (obj1 instanceof BigDecimal || obj2 instanceof BigDecimal) {
                    // At least one is a BigDecimal, so convert both to BigDecimals.
                    obj1 = getBigDecimalValue(obj1);
                    obj2 = getBigDecimalValue(obj2);
                }
                else if (obj1 instanceof BigInteger || obj2 instanceof BigInteger) {
                    // At least one is a BigInteger, so convert both to BigIntegers.
                    obj1 = getBigIntegerValue(obj1);
                    obj2 = getBigIntegerValue(obj2);
                }
                else if (obj1 instanceof Double || obj2 instanceof Double) {
                    // At least one is a Double, so convert both to Doubles.
                    obj1 = getDoubleValue(obj1);
                    obj2 = getDoubleValue(obj2);
                }
                else if (obj1 instanceof Float || obj2 instanceof Float) {
                    // At least one is a Float, so convert both to Floats.
                    obj1 = getFloatValue(obj1);
                    obj2 = getFloatValue(obj2);
                }
                else if (obj1 instanceof Long || obj2 instanceof Long) {
                    // At least one is a Long, so convert both to Longs.
                    obj1 = getLongValue(obj1);
                    obj2 = getLongValue(obj2);
                }
                else {
                    // Any other integer-type (e.g. shorts or bytes or chars)
                    // we will just coerce into being Integers.
                    obj1 = getIntegerValue(obj1);
                    obj2 = getIntegerValue(obj2);
                }

                assert obj1.getClass().equals(obj2.getClass());
            }
            else if (obj1 instanceof Temporal) {
                // LHS is a date, time or datetime.
                return coerceTemporalArithmetic((Temporal) obj1, obj2);
            }
            else if (obj2 instanceof Temporal) {
                Pair p = coerceTemporalArithmetic((Temporal) obj2, obj1);
                return p.swap();
            }
            else if (obj1 instanceof TemporalAmount) {
                // LHS is an interval.
                return coerceTemporalAmountArithmetic(
                    (TemporalAmount) obj1, obj2);
            }
            else if (obj2 instanceof TemporalAmount) {
                Pair p = coerceTemporalAmountArithmetic(
                    (TemporalAmount) obj2, obj1);
                return p.swap();
            }
            else {
                throw new TypeCastException(String.format(
                    "Cannot coerce types \"%s\" and \"%s\" for arithmetic.",
                    obj1.getClass(), obj2.getClass()));
            }
        }

        return new Pair(obj1, obj2);
    }


    private static Pair coerceTemporalArithmetic(Temporal obj1, Object obj2)
        throws TypeCastException {

        if (obj2 instanceof Temporal) {
            // Both sides are dates, times, or datetimes.

            if (obj1 instanceof LocalDateTime || obj2 instanceof LocalDateTime) {
                obj1 = getDateTimeValue(obj1);
                obj2 = getDateTimeValue(obj2);
            }
            else if (obj1 instanceof LocalDate || obj2 instanceof LocalDate) {
                obj1 = getDateValue(obj1);
                obj2 = getDateValue(obj2);
            }
            else if (obj1 instanceof LocalTime || obj2 instanceof LocalTime) {
                obj1 = getTimeValue(obj1);
                obj2 = getTimeValue(obj2);
            }
            else {
                throw new TypeCastException(String.format(
                    "Cannot coerce types \"%s\" and \"%s\" for arithmetic.",
                    obj1.getClass(), obj2.getClass()));
            }
        }
        else if (obj2 instanceof String) {
            // RHS is a String; try to convert it into a Temporal.  (We do
            // this instead of trying to convert it to a TemporalAmount,
            // because TemporalAmounts are already identified by the parser
            // with the "INTERVAL '...'" keyword.
            obj2 = DateTimeUtils.parseTemporal((String) obj2);
        }
        else if (!(obj2 instanceof TemporalAmount)) {
            throw new TypeCastException(String.format(
                "Cannot coerce types \"%s\" and \"%s\" for arithmetic.",
                obj1.getClass(), obj2.getClass()));
        }

        return new Pair(obj1, obj2);
    }


    private static Pair coerceTemporalAmountArithmetic(TemporalAmount obj1,
                                                       Object obj2)
        throws TypeCastException {

        if (obj2 instanceof String) {
            // RHS is a String; try to convert it into a Temporal.
            // (We do this instead of trying to convert it to a
            // TemporalAmount, because TemporalAmounts are already
            // identified by the parser with the "INTERVAL '...'"
            // keyword.
            obj2 = DateTimeUtils.parseTemporal((String) obj2);
        }
        else if (!(obj2 instanceof Temporal)) {
            throw new TypeCastException(String.format(
                "Cannot coerce types \"%s\" and \"%s\" for arithmetic.",
                obj1.getClass(), obj2.getClass()));
        }

        return new Pair(obj1, obj2);
    }


    /**
     * This function takes two arguments and coerces them to be the same numeric
     * type, for use with comparison operations.  It is up to the caller to
     * ensure that the types are actually comparable (although all recognized
     * types in this function are comparable).
     * <p>
     * If either or both of the arguments are <tt>null</tt> then no coercion is
     * performed, and the results are returned in a pair-object.  The reason for
     * this is that the comparison will simply evaluate to <tt>UNKNOWN</tt>, so
     * no coercion is required.
     *
     * @design When new types are added in the future (e.g. date and/or time
     *         values), this function will need to be updated with the new types.
     *
     * @param obj1 the first input value to cast
     * @param obj2 the second input value to cast
     *
     * @return an object holding the two input values, both converted to a type
     *         suitable for comparison
     *
     * @throws TypeCastException if both inputs are non-<tt>null</tt> and they
     *         cannot both be cast to types suitable for comparison.
     */
    public static Pair coerceComparison(Object obj1, Object obj2)
        throws TypeCastException {

        // We only need to do coercion if both inputs are non-NULL, and if
        // they aren't already the same type (as indicated by their class).
        if (obj1 != null && obj2 != null &&
            !obj1.getClass().equals(obj2.getClass())) {

            if (obj1 instanceof Number || obj2 instanceof Number) {
                // Reuse the same logic in the arithmetic coercion code.
                return coerceArithmetic(obj1, obj2);
            }
            else if (obj1 instanceof Boolean || obj2 instanceof Boolean) {
                obj1 = getBooleanValue(obj1);
                obj2 = getBooleanValue(obj2);
            }
            else if (obj1 instanceof String || obj2 instanceof String) {
                obj1 = getStringValue(obj1);
                obj2 = getStringValue(obj2);
            }
            else if (obj1 instanceof Temporal && obj2 instanceof Temporal) {
                // The temporal objects need to be the same type for
                // comparisons.  Since we know they are both Temporals then
                // coerceArithmetic() will make them into the same type.
                // (If one were a Temporal and the other were a TemporalAmount
                // then this might be left unchanged.)
                return coerceArithmetic(obj1, obj2);
            }
            else {
                // Inputs are different types, and we don't know how to make
                // them the same.
                throw new TypeCastException(String.format(
                    "Cannot coerce types \"%s\" and \"%s\" for comparison.",
                    obj1.getClass(), obj2.getClass()));
            }

            assert obj1.getClass().equals(obj2.getClass());
        }

        return new Pair(obj1, obj2);
    }


    /**
     * This method attempts to assign a SQL data type to a value produced by the
     * expression classes.  If a Java type is not recognized as a particular SQL
     * data type then <tt>null</tt> is returned.
     *
     * @param obj The object to determine a SQL data type for.
     *
     * @return The corresponding SQL data type, or <tt>null</tt> if the input
     *         value's type doesn't have an obvious corresponding SQL data type.
     */
    public static SQLDataType getSQLType(Object obj) {
        if (obj == null)
            return sqlTypeMapping.get(Null.class);

        return sqlTypeMapping.get(obj.getClass());
    }


    public static Object coerceTo(Object obj, ColumnType colType) {
        if (obj == null)
            return null;

        switch (colType.getBaseType()) {

        case INTEGER:
            return getIntegerValue(obj);

        case SMALLINT:
            return getShortValue(obj);

        case BIGINT:
            return getLongValue(obj);

        case TINYINT:
            return getByteValue(obj);

        case FLOAT:
            return getFloatValue(obj);

        case DOUBLE:
            return getDoubleValue(obj);

        case NUMERIC:
        {
            BigDecimal v = getBigDecimalValue(obj);

            // Truncate the value if it exceeds the column-type's specified
            // scale.
            if (v.scale() > colType.getScale())
                v = v.setScale(colType.getScale(), RoundingMode.HALF_UP);

            return v;
        }

        case CHAR:
        case VARCHAR:
        {
            // TODO:  Complain instead?
            String s = getStringValue(obj);
            if (s.length() > colType.getLength())
                s = s.substring(0, colType.getLength());

            return s;
        }

        case DATE:
            return getDateValue(obj);

        case TIME:
            return getTimeValue(obj);

        case DATETIME:
        case TIMESTAMP:
            return getDateTimeValue(obj);

        case FILE_POINTER: // TODO!
        default:
            throw new TypeCastException("Cannot coerce object " + obj +
                " to ColumnType " + colType);
        }
    }
}
