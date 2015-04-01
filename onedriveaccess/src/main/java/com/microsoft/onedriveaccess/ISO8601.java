package com.microsoft.onedriveaccess;

import android.annotation.SuppressLint;


import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.joda.time.format.DateTimeFormat;


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
    public static final String DATE_FORMAT_STRING = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

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

    public static Date toCalendar(final String iso8601string)
            throws ParseException {
        String s = iso8601string;
       DateTimeFormatter formatter =  DateTimeFormat.forPattern(DATE_FORMAT_STRING); //This takes care of parsing issue posed
        DateTime newDate = formatter.parseDateTime(s); //converts it to Joda DateTime
        Date returnDate = newDate.toDate(); //then reverts it back to java.util.Date as requested by toCalendar
        return returnDate;

    }



}
//"2015-03-25T21:12:14.24"
