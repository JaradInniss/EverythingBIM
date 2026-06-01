package com.example.everythingbim.data.local.entities;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.example.everythingbim.data.models.RequestReportStatus;

@Entity(
        tableName = "reports",
        foreignKeys = {
                @ForeignKey(
                        entity = PostEntity.class,
                        parentColumns = "postId",
                        childColumns = "postId",
                        onDelete = ForeignKey.CASCADE
                )
        },
        indices = {@Index("postId")}
)
public class ReportEntity {

    @PrimaryKey(autoGenerate = true)
    public long reportId;

    public long postId;
    public long reporterId;
    public String reason;
    public String description;
    public RequestReportStatus status;
    public long timestamp;

    public ReportEntity(long postId, long reporterId, String reason, String description, RequestReportStatus status, long timestamp) {
        this.postId = postId;
        this.reporterId = reporterId;
        this.reason = reason;
        this.description = description;
        this.status = status;
        this.timestamp = timestamp;
    }

    @Ignore
    public ReportEntity(long reporterId, String reason, String description, RequestReportStatus status, long timestamp) {
        this.reporterId = reporterId;
        this.reason = reason;
        this.description = description;
        this.status = status;
        this.timestamp = timestamp;
    }
}
