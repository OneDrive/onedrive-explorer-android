package com.microsoft.onedriveaccess;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;
import org.joda.time.format.ISODateTimeFormat;

import java.text.ParseException;
import java.util.Date;

/**
 * Helper class for handling ISO 8601 strings of the format: "2008-03-01T13:00:00.123Z".
 */
final class ISO8601 {
    /**
     * The datetime formatter which can deal with second/millisecond/extreme millisecond value dates
     */
    private static DateTimeFormatter sFormatter;

    /**
     * The min number of milliseconds to accept in the ISO8601 format
     */
    private static final int MILLISECONDS_MIN_DIGITS = 3;

    /**
     * The max number of milliseconds to accept in the ISO8601 format
     */
    private static final int MILLISECONDS_MAX_DIGITS = 9;

    /**
     * The min number of time zone elements to accept in the ISO8601 format
     */
    private static final int TIME_ZONE_MIN_FIELDS = 2;

    /**
     * The min number of time zone elements to accept in the ISO8601 format
     */
    private static final int TIME_ZONE_MAX_FIELDS = 4;

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
    public static String fromDate(final Date date) {
        final DateTimeFormatter formatter = ISODateTimeFormat.dateTime();
        final DateTime dateTime = new DateTime(date);
        return formatter.print(dateTime);
    }

    /**
     * Transform ISO 8601 string to a Date.
     * @param iso8601string to convert
     * @return the date
     * @exception ParseException If the date could not be parsed
     */
    public static Date toDate(final String iso8601string)
            throws ParseException {
        if (sFormatter == null) {
            sFormatter = new DateTimeFormatterBuilder()
                    .append(ISODateTimeFormat.date())
                    .appendLiteral('T')
                    .append(ISODateTimeFormat.hourMinuteSecond())
                    .appendOptional(new DateTimeFormatterBuilder()
                            .appendLiteral('.')
                            .appendFractionOfSecond(MILLISECONDS_MIN_DIGITS, MILLISECONDS_MAX_DIGITS)
                            .toParser())
                    .appendTimeZoneOffset("Z", true, TIME_ZONE_MIN_FIELDS, TIME_ZONE_MAX_FIELDS)
                    .toFormatter()
                    .withZoneUTC();
        }

        return sFormatter.parseDateTime(iso8601string).toDate();
    }
}
