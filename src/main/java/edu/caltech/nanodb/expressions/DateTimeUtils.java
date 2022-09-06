package edu.caltech.nanodb.expressions;


import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAmount;
import java.util.regex.Pattern;
import java.util.Locale;


/**
 * Provides a number of utility operations for working with dates, times and
 * timestamps.
 */
public class DateTimeUtils {
    /**
     * An array of date format specifications that NanoDB recognizes.  New
     * formats can be added to this array to expand what NanoDB is able to
     * parse.
     */
    private static final DateTimeFormatter DATE_FORMATS[] = {
        DateTimeFormatter.ISO_LOCAL_DATE,
        DateTimeFormatter.BASIC_ISO_DATE,
        DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("MMM dd yyyy", Locale.ENGLISH)
    };

    /**
     * An array of time format specifications that NanoDB recognizes.  New
     * formats can be added to this array to expand what NanoDB is able to
     * parse.
     */
    private static final DateTimeFormatter TIME_FORMATS[] = {
        DateTimeFormatter.ISO_LOCAL_TIME,
        DateTimeFormatter.ofPattern("h:mm[:ss[.SSS]][ ][a]") //,
//        DateTimeFormatter.ofPattern("h:mm[:ss[.SSS]][a]")
    };

    /**
     * An array of date/time format specifications that NanoDB recognizes.
     * New formats can be added to this array to expand what NanoDB is able to
     * parse.
     */
    private static final DateTimeFormatter DATETIME_FORMATS[] = {
        DateTimeFormatter.ISO_LOCAL_DATE_TIME
    };


    /*
    private static final Pattern INTERVAL_FORMATS[] = {
        Pattern.compile("\\d+(\\.\\d+)?\\h+\\D\\w*")
    };
    */


    /**
     * Attempts to parse a string into a date/time value using the formats
     * specified in {@link #DATETIME_FORMATS}.
     *
     * @param s the string to attempt to parse into a date/time
     *
     * @return an object holding the date/time value
     *
     * @throws DateTimeParseException if the string couldn't be parsed into a
     *         date/time value.
     */
    public static LocalDateTime parseDateTime(String s) {
        for (DateTimeFormatter fmt : DATETIME_FORMATS) {
            try {
                return fmt.parse(s, LocalDateTime::from);
            }
            catch (DateTimeParseException e) {
                // That didn't work.  Go on to the next one.
            }
        }

        throw new DateTimeParseException("Could not parse into LocalDateTime",
            s, 0);
    }


    /**
     * Attempts to parse a string into a date value using the formats
     * specified in {@link #DATE_FORMATS}.
     *
     * @param s the string to attempt to parse into a date
     *
     * @return an object holding the date value
     *
     * @throws DateTimeParseException if the string couldn't be parsed into a
     *         date value.
     */
    public static LocalDate parseDate(String s) {
        for (DateTimeFormatter fmt : DATE_FORMATS) {
            try {
                return fmt.parse(s, LocalDate::from);
            }
            catch (DateTimeParseException e) {
                // That didn't work.  Go on to the next one.
            }
        }

        throw new DateTimeParseException("Could not parse into LocalDate",
            s, 0);
    }


    /**
     * Attempts to parse a string into a time value using the formats
     * specified in {@link #TIME_FORMATS}.
     *
     * @param s the string to attempt to parse into a time
     *
     * @return an object holding the time value
     *
     * @throws DateTimeParseException if the string couldn't be parsed into a
     *         time value.
     */
    public static LocalTime parseTime(String s) {
        for (DateTimeFormatter fmt : TIME_FORMATS) {
            try {
                return fmt.parse(s, LocalTime::from);
            }
            catch (DateTimeParseException e) {
                // That didn't work.  Go on to the next one.
            }
        }

        throw new DateTimeParseException("Could not parse into LocalTime",
            s, 0);
    }


    /**
     * Attempts to parse a string into some kind of temporal value using the
     * formats specified in {@link #DATETIME_FORMATS}, {@link #DATE_FORMATS},
     * and {@link #TIME_FORMATS}.  Attempts are made in this order, so a
     * date/time will be produced if possible, then a date, and finally a
     * time.
     *
     * @param s the string to attempt to parse into a temporal value
     *
     * @return an object holding the temporal value
     *
     * @throws DateTimeParseException if the string couldn't be parsed into a
     *         temporal value.
     */
    public static Temporal parseTemporal(String s) {
        try {
            return parseDateTime(s);
        }
        catch (DateTimeParseException e1) {
            try {
                return parseDate(s);
            }
            catch (DateTimeParseException e2) {
                try {
                    return parseTime(s);
                }
                catch (DateTimeParseException e3) {
                    throw new DateTimeParseException("Could not parse into " +
                        "any kind of Temporal", s, 0);
                }
            }
        }
    }


    /**
     * Attempts to parse a string into some kind of "temporal amount" or
     * INTERVAL value.  Only one interval format is supported at this time:
     * "<tt><em>N</em> [YEAR|MONTH|WEEK|DAY|HOUR|MINUTE|SECOND]S?</tt>", where
     * <em>N</em> is an integer.  The units may be singular or plural; e.g.
     * both "<tt>YEAR</tt>" and "<tt>YEARS</tt>" is supported.
     *
     * @param s the string to attempt to parse into a temporal amount
     *
     * @return an object holding the temporal amount
     *
     * @throws DateTimeParseException if the string couldn't be parsed into a
     *         temporal amount.
     */
    public static TemporalAmount parseInterval(String s) {
        // Split the string on horizontal whitespace.
        s = s.trim();
        String parts[] = s.split("\\h");

        if (parts.length != 2) {
            throw new DateTimeParseException(
                "Could not parse into interval:  " +
                "must contain exactly two parts", s, 0);
        }

        TemporalAmount result;

        int amount;
        try {
            amount = Integer.parseInt(parts[0]);
        }
        catch (NumberFormatException e) {
            throw new DateTimeParseException(
                "Could not parse into interval:  " +
                    "first value must be a nonnegative integer", s, 0);
        }

        String units = parts[1].toLowerCase();

        if ("year".equals(units) || "years".equals(units)) {
            result = Period.ofYears(amount);
        }
        else if ("month".equals(units) || "months".equals(units)) {
            result = Period.ofMonths(amount);
        }
        else if ("week".equals(units) || "weeks".equals(units)) {
            result = Period.ofWeeks(amount);
        }
        else if ("day".equals(units) || "days".equals(units)) {
            result = Period.ofDays(amount);
        }
        else if ("hour".equals(units) || "hours".equals(units)) {
            result = Duration.ofHours(amount);
        }
        else if ("minute".equals(units) || "minutes".equals(units)) {
            result = Duration.ofMinutes(amount);
        }
        else if ("second".equals(units) || "seconds".equals(units)) {
            result = Duration.ofSeconds(amount);
        }
        else {
            throw new DateTimeParseException(
                "Could not parse into interval:  " +
                "second value specifies unrecognized units", s, 0);
        }

        return result;
    }
}
