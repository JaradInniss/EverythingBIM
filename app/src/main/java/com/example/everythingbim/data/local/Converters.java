package com.example.everythingbim.data.local;

import androidx.room.TypeConverter;
import com.example.everythingbim.data.models.RequestReportStatus;

public class Converters {
    @TypeConverter
    public static String fromStatus(RequestReportStatus status) {
        return status == null ? null : status.name();
    }

    @TypeConverter
    public static RequestReportStatus toStatus(String status) {
        return status == null ? null : RequestReportStatus.valueOf(status);
    }
}
