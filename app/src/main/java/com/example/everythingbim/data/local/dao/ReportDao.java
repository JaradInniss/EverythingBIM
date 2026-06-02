package com.example.everythingbim.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

import com.example.everythingbim.data.local.entities.ReportEntity;
import com.example.everythingbim.data.models.RequestReportStatus;

// Report Dao

@Dao
public interface ReportDao {

    @Insert
    long insert(ReportEntity reportEntity);

    @Query("SELECT * FROM reports")
    LiveData<List<ReportEntity>> getAllReports();

    @Query("SELECT * FROM reports WHERE reportId = :reportId")
    LiveData<List<ReportEntity>> getReportById(long reportId);

    @Query("SELECT * FROM reports WHERE postId = :postId")
    LiveData<List<ReportEntity>> getReportsForPost(long postId);

    @Query("SELECT * FROM reports WHERE reporterId = :reporterId")
    LiveData<List<ReportEntity>> getReportsByReporter(long reporterId);

    @Query("SELECT * FROM reports WHERE status = :status")
    LiveData<List<ReportEntity>> getReportsByStatus(RequestReportStatus status);
}
