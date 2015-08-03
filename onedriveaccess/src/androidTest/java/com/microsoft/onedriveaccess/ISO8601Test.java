package com.microsoft.onedriveaccess;

import android.test.AndroidTestCase;

import junit.framework.Assert;

import java.util.Date;
import java.util.TimeZone;

/**
 * Test cases for the {@see ISO8601} class
 */
public class ISO8601Test extends AndroidTestCase {

    /**
     * Make sure that dates with and without millis can be converted properly into strings
     * @throws Exception If there is an exception during the test
     */
    public void testFromDate() throws Exception {
        // I sure hope this works in other timezones...
        TimeZone.setDefault(TimeZone.getTimeZone("PST"));
        final Date date = new Date(123456789012345L);
        Assert.assertEquals("5882-03-11T00:30:12.345Z", ISO8601.fromDate(date));

        final Date dateNoMillis = new Date(123456789012000L);
        Assert.assertEquals("5882-03-11T00:30:12.000Z", ISO8601.fromDate(dateNoMillis));
    }

    /**
     * Make sure that dates in string format with and without millis can be converted properly into date objects
     * @throws Exception If there is an exception during the test
     */
    public void testToDate() throws Exception {
        TimeZone.setDefault(TimeZone.getTimeZone("PST"));
        final long toTheSecondDate = 123456789012000L;
        final Date dateToSecond = ISO8601.toDate("5882-03-11T00:30:12Z");
        Assert.assertEquals(toTheSecondDate, dateToSecond.getTime());

        final long toTheMillisecondDate = 123456789012345L;
        final Date dateToTheMillisecond = ISO8601.toDate("5882-03-11T00:30:12.345Z");
        Assert.assertEquals(toTheMillisecondDate, dateToTheMillisecond.getTime());

        final Date dateToTheExtremeMillisecond = ISO8601.toDate("5882-03-11T00:30:12.3456789Z");
        Assert.assertEquals(toTheMillisecondDate, dateToTheExtremeMillisecond.getTime());
    }
}