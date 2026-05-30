package com.example.everythingbim;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Utility class for logging admin activities.
 * Tracks admin actions on reports and requests.
 *
 * <p>Activities are saved to local SharedPreferences as primary storage (works offline).
 * An attempt is also made to write to Firestore {@code admin_activities} collection, but
 * failure there does NOT prevent local persistence.
 */
public class ActivityLogger {

    private static final String TAG = "ActivityLogger";
    private static final String COLLECTION_NAME = "admin_activities";

    // SharedPreferences for local fallback
    private static final String PREFS_NAME = "activity_logger_prefs";
    private static final String KEY_RECENT_ACTIVITIES = "recent_activities";
    private static final int MAX_LOCAL_ACTIVITIES = 50;

    // Request/Activity types
    public static final String TYPE_REPORT = "Report";
    public static final String TYPE_BUSINESS = "Business";
    public static final String TYPE_LOCATION = "Location";
    public static final String TYPE_INFO = "Info";
    public static final String TYPE_DATASET = "Dataset";

    // Action types
    public static final String ACTION_VIEWED = "Viewed";
    public static final String ACTION_APPROVED = "Approved";
    public static final String ACTION_REJECTED = "Rejected";
    public static final String ACTION_COMPLETED = "Completed";

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private final Context appContext;

    public ActivityLogger(Context context) {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        this.appContext = context.getApplicationContext();
    }

    /**
     * Logs an admin activity to local SharedPreferences first,
     * then attempts Firestore write (Firestore failure does NOT affect local save).
     *
     * @param requestType  Type of request (Report, Business, Location, Info)
     * @param action       Action taken (Viewed, Approved, Rejected, Completed)
     * @param requestTitle Title or description of the request
     * @param requestId    Firestore document ID of the request
     */
    public void logActivity(String requestType, String action, String requestTitle, String requestId) {
        String adminUsername = getAdminUsername();
        long nowMillis = System.currentTimeMillis();

        Map<String, Object> activityData = new HashMap<>();
        activityData.put("requestType", requestType);
        activityData.put("action", action);
        activityData.put("requestTitle", requestTitle);
        activityData.put("requestId", requestId);
        activityData.put("adminUsername", adminUsername);
        activityData.put("timestamp", new Timestamp(new Date(nowMillis)));
        activityData.put("displayTime", getCurrentTimeString());
        activityData.put("_localMillis", nowMillis); // local timestamp for sorting

        // ALWAYS save locally first — this is the primary store
        saveLocalActivity(activityData);

        // Then attempt Firestore write (fails silently, local is already saved)
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
     * Saves an activity to SharedPreferences.
     * Maintains up to MAX_LOCAL_ACTIVITIES, sorted newest-first by _localMillis.
     */
    private void saveLocalActivity(Map<String, Object> activityData) {
        try {
            SharedPreferences prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            List<Map<String, Object>> activities = loadLocalActivityList(prefs);

            // Remove existing entry with same requestId to avoid duplicates
            String requestId = (String) activityData.get("requestId");
            if (requestId != null) {
                activities.removeIf(a -> requestId.equals(a.get("requestId")));
            }

            // Add new activity at front
            activities.add(0, activityData);

            // Trim to max size
            while (activities.size() > MAX_LOCAL_ACTIVITIES) {
                activities.remove(activities.size() - 1);
            }

            // Serialize to JSON
            JSONArray array = new JSONArray();
            for (Map<String, Object> act : activities) {
                array.put(mapToJson(act));
            }
            prefs.edit().putString(KEY_RECENT_ACTIVITIES, array.toString()).apply();

            Log.d(TAG, "Local activity saved: " + activityData.get("requestType") + " - " + activityData.get("action"));
        } catch (Exception e) {
            Log.e(TAG, "Failed to save local activity: " + e.getMessage());
        }
    }

    /**
     * Loads all locally-stored activities, sorted newest-first.
     * Called by AdminHomeFragment to display recent activities.
     *
     * @return List of activity maps (each map contains requestType, action, requestTitle,
     *         requestId, adminUsername, timestamp, displayTime, _localMillis)
     */
    public static List<Map<String, Object>> loadLocalActivities(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return loadLocalActivityList(prefs);
    }

    private static List<Map<String, Object>> loadLocalActivityList(SharedPreferences prefs) {
        List<Map<String, Object>> activities = new ArrayList<>();
        String json = prefs.getString(KEY_RECENT_ACTIVITIES, null);
        if (json == null || json.isEmpty()) return activities;

        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                Map<String, Object> act = jsonToMap(obj);
                activities.add(act);
            }
        } catch (JSONException e) {
            Log.e(TAG, "Failed to parse local activities JSON: " + e.getMessage());
        }
        return activities;
    }

    /**
     * Converts a Map to a JSONObject.
     */
    private static JSONObject mapToJson(Map<String, Object> map) throws JSONException {
        JSONObject obj = new JSONObject();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Object value = entry.getValue();
            if (value == null) {
                obj.put(entry.getKey(), JSONObject.NULL);
            } else if (value instanceof Timestamp) {
                obj.put(entry.getKey(), ((Timestamp) value).toDate().getTime());
            } else if (value instanceof Long) {
                obj.put(entry.getKey(), value);
            } else if (value instanceof Integer) {
                obj.put(entry.getKey(), value);
            } else if (value instanceof String) {
                obj.put(entry.getKey(), value);
            } else {
                obj.put(entry.getKey(), value.toString());
            }
        }
        return obj;
    }

    /**
     * Converts a JSONObject back to a Map.
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> jsonToMap(JSONObject obj) throws JSONException {
        Map<String, Object> map = new HashMap<>();
        java.util.Iterator<String> keys = obj.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            Object value = obj.get(key);
            if (value == JSONObject.NULL) {
                map.put(key, null);
            } else if (key.equals("timestamp") && value instanceof Long) {
                // Reconstruct Timestamp from millis
                map.put(key, new Timestamp(new Date((Long) value)));
            } else if (value instanceof Long) {
                map.put(key, value);
            } else if (value instanceof Integer) {
                map.put(key, value);
            } else {
                map.put(key, value);
            }
        }
        return map;
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
     *
     * @param timestamp Firebase timestamp
     * @return String like "2hr ago", "1d ago", etc.
     */
    public static String getTimeAgo(Timestamp timestamp) {
        if (timestamp == null) return "";

        long millis = timestamp.toDate().getTime();
        long now = System.currentTimeMillis();
        long diff = now - millis;

        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 7) {
            // Absolute date for items older than 7 days
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd", Locale.getDefault());
            return sdf.format(timestamp.toDate());
        } else if (days > 0) {
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
     * Fetches the N most recent activities from Firestore.
     * Note: This may fail due to PERMISSION_DENIED. Use loadLocalActivities() instead
     * for reliable local-only access.
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
