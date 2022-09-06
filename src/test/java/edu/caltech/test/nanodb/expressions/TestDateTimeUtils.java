package edu.caltech.test.nanodb.expressions;


import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.time.temporal.TemporalAmount;

import edu.caltech.nanodb.expressions.DateTimeUtils;
import org.testng.annotations.Test;


/**
 * Exercise the string-parsing functionality of the {@link DateTimeUtils}
 * class.
 */
@Test(groups={"framework"})
public class TestDateTimeUtils {
    /** Exercise the {@link DateTimeUtils#parseDate} function. */
    public void testParseDate() {
        LocalDate v = LocalDate.of(2015, 10, 22);  // Oct 22, 2015
        LocalDate t;

        t = DateTimeUtils.parseDate("2015-10-22");
        assert v.equals(t);

        t = DateTimeUtils.parseDate("22 Oct 2015");
        assert v.equals(t);

        t = DateTimeUtils.parseDate("Oct 22 2015");
        assert v.equals(t);
    }


    /** Exercise the {@link DateTimeUtils#parseTime} function. */
    public void testParseTime() {
        LocalTime v = LocalTime.of(17, 43, 21);
        LocalTime t;

        t = DateTimeUtils.parseTime("17:43:21");
        assert v.equals(t);

        t = DateTimeUtils.parseTime("5:43:21 PM");
        assert v.equals(t);

        t = DateTimeUtils.parseTime("5:43:21PM");
        assert v.equals(t);

        v = LocalTime.of(17, 43);

        t = DateTimeUtils.parseTime("17:43");
        assert v.equals(t);

        t = DateTimeUtils.parseTime("5:43 PM");
        assert v.equals(t);

        t = DateTimeUtils.parseTime("5:43PM");
        assert v.equals(t);

        v = LocalTime.of(5, 43);

        t = DateTimeUtils.parseTime("05:43");
        assert v.equals(t);

        t = DateTimeUtils.parseTime("5:43 AM");
        assert v.equals(t);

        t = DateTimeUtils.parseTime("5:43AM");
        assert v.equals(t);
    }


    /** Exercise the {@link DateTimeUtils#parseDateTime} function. */
    public void testParseDateTime() {
        LocalDateTime t;

        t = DateTimeUtils.parseDateTime("2015-10-22T17:43:21");
        assert t.equals(LocalDateTime.of(2015, 10, 22, 17, 43, 21));

        t = DateTimeUtils.parseDateTime("2015-10-22T17:43");
        assert t.equals(LocalDateTime.of(2015, 10, 22, 17, 43, 0));
    }


    /** Exercise the {@link DateTimeUtils#parseInterval} function. */
    public void testParseInterval() {
        TemporalAmount t;

        t = DateTimeUtils.parseInterval("3 year");
        assert t.equals(Period.ofYears(3));

        t = DateTimeUtils.parseInterval("5 years");
        assert t.equals(Period.ofYears(5));

        t = DateTimeUtils.parseInterval("-2 years");
        assert t.equals(Period.ofYears(-2));

        t = DateTimeUtils.parseInterval("9 month");
        assert t.equals(Period.ofMonths(9));

        t = DateTimeUtils.parseInterval("4 months");
        assert t.equals(Period.ofMonths(4));

        t = DateTimeUtils.parseInterval("-3 months");
        assert t.equals(Period.ofMonths(-3));

        t = DateTimeUtils.parseInterval("1 week");
        assert t.equals(Period.ofWeeks(1));

        t = DateTimeUtils.parseInterval("3 weeks");
        assert t.equals(Period.ofWeeks(3));

        t = DateTimeUtils.parseInterval("-1 week");
        assert t.equals(Period.ofWeeks(-1));

        t = DateTimeUtils.parseInterval("4 day");
        assert t.equals(Period.ofDays(4));

        t = DateTimeUtils.parseInterval("18 days");
        assert t.equals(Period.ofDays(18));

        t = DateTimeUtils.parseInterval("-14 days");
        assert t.equals(Period.ofDays(-14));

        t = DateTimeUtils.parseInterval("16 hour");
        assert t.equals(Duration.ofHours(16));

        t = DateTimeUtils.parseInterval("8 hours");
        assert t.equals(Duration.ofHours(8));

        t = DateTimeUtils.parseInterval("-2 hour");
        assert t.equals(Duration.ofHours(-2));

        t = DateTimeUtils.parseInterval("5 minute");
        assert t.equals(Duration.ofMinutes(5));

        t = DateTimeUtils.parseInterval("15 minutes");
        assert t.equals(Duration.ofMinutes(15));

        t = DateTimeUtils.parseInterval("-10 minutes");
        assert t.equals(Duration.ofMinutes(-10));

        t = DateTimeUtils.parseInterval("45 second");
        assert t.equals(Duration.ofSeconds(45));

        t = DateTimeUtils.parseInterval("3 seconds");
        assert t.equals(Duration.ofSeconds(3));

        t = DateTimeUtils.parseInterval("-1 second");
        assert t.equals(Duration.ofSeconds(-1));
    }
}
