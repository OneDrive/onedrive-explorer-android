package com.microsoft.onedriveaccess;

import android.annotation.SuppressLint;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Helper class for handling ISO 8601 strings of the following format: "2008-03-01T13:00:00".
 * @author wrygiel
 * @see http://stackoverflow.com/a/10621553/533057
 */
final class ISO8601 {

    /**
     * The ISO8601 date format string, see https://en.wikipedia.org/wiki/ISO_8601
     * Modified slightly to match the OData response from OneDrive
     */
    public static final String DATE_FORMAT_STRING = "yyyy-MM-dd'T'HH:mm:ss";

    /**
     * Default constructor
     */
    private ISO8601() {
    }

    /**
     * Transform Date to ISO 8601 string.
     * @param date to convert
     * @return the date as an ISO 8601 string
     */
    @SuppressLint("SimpleDateFormat")
    public static String fromDate(final Date date) {
        return new SimpleDateFormat(DATE_FORMAT_STRING).format(date);
    }

    /**
     * Transform ISO 8601 string to a Date.
     * @param iso8601string to convert
     * @return the date
     * @exception ParseException If the date could not be parsed
     */
    @SuppressLint("SimpleDateFormat")
    public static Date toCalendar(final String iso8601string)
            throws ParseException {
        String s = iso8601string;
        try {
            s = s.substring(0, 22) + s.substring(23);  // to get rid of the ":"
        } catch (final IndexOutOfBoundsException e) {
            throw new ParseException("Invalid length", 0);
        }
        return new SimpleDateFormat(DATE_FORMAT_STRING).parse(s);
    }
}
