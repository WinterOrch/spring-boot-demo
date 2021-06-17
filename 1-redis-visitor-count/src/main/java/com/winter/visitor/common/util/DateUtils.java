package com.winter.visitor.common.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DateUtils {
    private static DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    public static String getToday() {
        LocalDate date = LocalDate.now();
        return dateTimeFormatter.format(date);
    }
}
