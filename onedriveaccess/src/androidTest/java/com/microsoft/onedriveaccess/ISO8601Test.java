package com.microsoft.onedriveaccess;

import android.test.AndroidTestCase;

import junit.framework.Assert;

import java.util.Date;
import java.util.TimeZone;

public class ISO8601Test extends AndroidTestCase {

    /**
     * Make sure that dates with and without millis can be converted properly into strings
     */
    public void testFromDate() throws Exception {
        // I sure hope this works in other timezones...
        TimeZone.setDefault(TimeZone.getTimeZone("PST"));
        final Date date = new Date(123456789012345L);
        Assert.assertEquals("5882-03-11T00:30:12.345+0000", ISO8601.fromDate(date));

        final Date dateNoMillis = new Date(123456789012000L);
        Assert.assertEquals("5882-03-11T00:30:12.000+0000", ISO8601.fromDate(dateNoMillis));
    }

    /**
     * Make sure that dates in string format with and without millis can be converted properly into date objects
     */
    public void testToDate() throws Exception {
        TimeZone.setDefault(TimeZone.getTimeZone("PST"));
        final Date date = new Date(123456789012345L);
        Assert.assertEquals(date, ISO8601.toDate("5882-03-11T00:30:12.345+0000"));

        final Date dateNoMillis = new Date(123456789012000L);
        Assert.assertEquals(dateNoMillis, ISO8601.toDate("5882-03-11T00:30:12+0000"));
    }
}