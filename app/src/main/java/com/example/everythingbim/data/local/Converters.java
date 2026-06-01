package com.example.everythingbim.data.local;

import androidx.room.TypeConverter;
import com.example.everythingbim.data.models.RequestReportStatus;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class Converters {
    @TypeConverter
    public static String fromStatus(RequestReportStatus status) {
        return status == null ? null : status.name();
    }

    @TypeConverter
    public static RequestReportStatus toStatus(String status) {
        return status == null ? null : RequestReportStatus.valueOf(status);
    }

    @TypeConverter
    public static String fromStringList(List<String> list) {
        if (list == null) {
            return null;
        }
        return String.join(",", list);
    }

    @TypeConverter
    public static List<String> toStringList(String value) {
        if (value == null || value.isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.asList(value.split(","));
    }

    @TypeConverter
    public static String fromLongList(List<Long> list) {
        if (list == null) {
            return null;
        }
        return list.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    @TypeConverter
    public static List<Long> toLongList(String value) {
        if (value == null || value.isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.stream(value.split(","))
                .map(Long::valueOf)
                .collect(Collectors.toList());
    }
}
