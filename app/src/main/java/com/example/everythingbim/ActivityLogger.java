package com.example.everythingbim;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Utility class for logging admin activities to Firestore.
 * Tracks admin actions on reports and requests.
 */
public class ActivityLogger {

    private static final String TAG = "ActivityLogger";
    private static final String COLLECTION_NAME = "admin_activities";

    // Request/Activity types
    public static final String TYPE_REPORT = "Report";
    public static final String TYPE_BUSINESS = "Business";
    public static final String TYPE_LOCATION = "Location";
    public static final String TYPE_INFO = "Info";

    // Action types
    public static final String ACTION_VIEWED = "Viewed";
    public static final String ACTION_APPROVED = "Approved";
    public static final String ACTION_REJECTED = "Rejected";
    public static final String ACTION_COMPLETED = "Completed";

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    public ActivityLogger() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    /**
     * Logs an admin activity to Firestore.
     *
     * @param requestType  Type of request (Report, Business, Location, Info)
     * @param action       Action taken (Viewed, Approved, Rejected, Completed)
     * @param requestTitle Title or description of the request
     * @param requestId    Firestore document ID of the request
     */
    public void logActivity(String requestType, String action, String requestTitle, String requestId) {
        String adminUsername = getAdminUsername();

        Map<String, Object> activityData = new HashMap<>();
        activityData.put("requestType", requestType);
        activityData.put("action", action);
        activityData.put("requestTitle", requestTitle);
        activityData.put("requestId", requestId);
        activityData.put("adminUsername", adminUsername);
        activityData.put("timestamp", com.google.firebase.Timestamp.now());
        activityData.put("displayTime", getCurrentTimeString());

        db.collection(COLLECTION_NAME)
                .add(activityData)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Activity logged: " + requestType + " - " + action);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to log activity: " + e.getMessage());
                });
    }

    /**
     * Convenience method to log viewing a request/report.
     */
    public void logView(String requestType, String requestTitle, String requestId) {
        logActivity(requestType, ACTION_VIEWED, requestTitle, requestId);
    }

    /**
     * Convenience method to log approving a request.
     */
    public void logApproval(String requestType, String requestTitle, String requestId) {
        logActivity(requestType, ACTION_APPROVED, requestTitle, requestId);
    }

    /**
     * Convenience method to log rejecting a request.
     */
    public void logRejection(String requestType, String requestTitle, String requestId) {
        logActivity(requestType, ACTION_REJECTED, requestTitle, requestId);
    }

    /**
     * Convenience method to log completing a report action.
     */
    public void logCompletion(String requestType, String requestTitle, String requestId) {
        logActivity(requestType, ACTION_COMPLETED, requestTitle, requestId);
    }

    /**
     * Gets the current admin's username or email.
     */
    private String getAdminUsername() {
        if (auth.getCurrentUser() != null) {
            String email = auth.getCurrentUser().getEmail();
            if (email != null) {
                // Extract username part before @
                int atIndex = email.indexOf('@');
                return atIndex > 0 ? email.substring(0, atIndex) : email;
            }
        }
        return "admin";
    }

    /**
     * Gets current time as a formatted string for display.
     */
    private String getCurrentTimeString() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
        return sdf.format(new Date());
    }

    /**
     * Calculates "time ago" string from a Firebase Timestamp.
     * Can be used on the UI side to display relative time.
     *
     * @param timestamp Firebase timestamp
     * @return String like "2hr ago", "1d ago", etc.
     */
    public static String getTimeAgo(com.google.firebase.Timestamp timestamp) {
        if (timestamp == null) return "";

        long millis = timestamp.toDate().getTime();
        long now = System.currentTimeMillis();
        long diff = now - millis;

        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return days + "d ago";
        } else if (hours > 0) {
            return hours + "hr ago";
        } else if (minutes > 0) {
            return minutes + "min ago";
        } else {
            return "Just now";
        }
    }

    /**
     * Fetches the N most recent activities.
     *
     * @param limit    Number of activities to fetch
     * @param listener Callback with query results
     */
    public void getRecentActivities(int limit, OnCompleteListener<QuerySnapshot> listener) {
        db.collection(COLLECTION_NAME)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .addOnCompleteListener(listener);
    }
}
