package sizz.api.community.util;

import sizz.api.community.common.Constants;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DateUtils {

    private DateUtils() {
        throw new IllegalStateException("Utility class");
    }

    //LocalDateTime
    public static LocalDateTime now() {
        return LocalDateTime.now();
    }

    //LocalDateTime → String
    public static String format(LocalDateTime dateTime) {
        if (dateTime == null) return null;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(Constants.DEFAULT_DATE_FORMAT);
        return dateTime.format(formatter);
    }


    //String → LocalDateTime
    public static LocalDateTime parse(String dateTimeStr) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(Constants.DEFAULT_DATE_FORMAT);
        return LocalDateTime.parse(dateTimeStr, formatter);
    }
}
