package com.example.everythingbim.ui.admin;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

/**
 * Centralized SharedPreferences-backed read state tracking for all admin screens.
 *
 * <p>Consolidates all read-tracking (dot indicators, section timestamps) into a single
 * SharedPreferences file, replacing the previously fragmented approach across 4 fragments.
 *
 * <p>All methods are static — no instance state needed.
 */
public class ReadStateManager {

    // ─── Single SharedPreferences file for all admin read state ───
    private static final String PREFS_NAME = "admin_read_state_prefs";

    // ─────────────────────────────────────────────────────────────
    // ACTIVITY ID KEYS (for recent activity dot indicators)
    // ─────────────────────────────────────────────────────────────
    public static final String KEY_READ_ACTIVITY_IDS = "read_activity_ids";

    // ─────────────────────────────────────────────────────────────
    // SECTION TIMESTAMP KEYS (for dashboard card dot indicators)
    // ─────────────────────────────────────────────────────────────
    public static final String KEY_LAST_READ_BUSINESS = "last_read_business";
    public static final String KEY_LAST_READ_INFO = "last_read_info";
    public static final String KEY_LAST_READ_LOCATION = "last_read_location";
    public static final String KEY_LAST_READ_DATASET = "last_read_dataset";
    public static final String KEY_LAST_READ_REPORTS = "last_read_reports";

    // ─────────────────────────────────────────────────────────────
    // REQUEST ID KEYS (for request list item dot indicators)
    // ─────────────────────────────────────────────────────────────
    public static final String KEY_READ_REQUEST_IDS = "read_request_ids";   // Location/Info requests
    public static final String KEY_READ_BIZ_VER_IDS = "read_biz_ver_ids";    // Business verification
    public static final String KEY_READ_REPORT_IDS = "read_report_ids";      // Reports

    // ─────────────────────────────────────────────────────────────
    // SHARED PREFERENCES HELPERS
    // ─────────────────────────────────────────────────────────────

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // ─────────────────────────────────────────────────────────────
    // ACTIVITY ID OPERATIONS (recent activity dot indicators)
    // ─────────────────────────────────────────────────────────────

    /**
     * Returns the set of activity IDs marked as read.
     */
    public static Set<String> getReadActivityIds(Context context) {
        return new HashSet<>(getPrefs(context).getStringSet(KEY_READ_ACTIVITY_IDS, new HashSet<>()));
    }

    /**
     * Returns true if the activity ID is marked as read.
     */
    public static boolean isActivityRead(Context context, String activityId) {
        if (activityId == null) return false;
        return getReadActivityIds(context).contains(activityId);
    }

    /**
     * Marks an activity ID as read.
     */
    public static void markActivityRead(Context context, String activityId) {
        if (activityId == null || activityId.isEmpty()) return;
        Set<String> ids = new HashSet<>(getReadActivityIds(context));
        if (ids.add(activityId)) {
            getPrefs(context).edit().putStringSet(KEY_READ_ACTIVITY_IDS, ids).apply();
        }
    }

    // ─────────────────────────────────────────────────────────────
    // SECTION TIMESTAMP OPERATIONS (dashboard card dot indicators)
    // ─────────────────────────────────────────────────────────────

    /**
     * Marks a section (collection) as read by storing the current timestamp.
     * Sections: KEY_LAST_READ_BUSINESS, KEY_LAST_READ_INFO, KEY_LAST_READ_LOCATION, KEY_LAST_READ_REPORTS
     */
    public static void markSectionRead(Context context, String sectionKey) {
        getPrefs(context).edit().putLong(sectionKey, System.currentTimeMillis()).apply();
    }

    /**
     * Convenience method to mark business section as read.
     */
    public static void markBusinessSectionRead(Context context) {
        markSectionRead(context, KEY_LAST_READ_BUSINESS);
    }

    /**
     * Convenience method to mark info section as read.
     */
    public static void markInfoSectionRead(Context context) {
        markSectionRead(context, KEY_LAST_READ_INFO);
    }

    /**
     * Convenience method to mark location section as read.
     */
    public static void markLocationSectionRead(Context context) {
        markSectionRead(context, KEY_LAST_READ_LOCATION);
    }

    /**
     * Convenience method to mark dataset section as read.
     */
    public static void markDatasetSectionRead(Context context) {
        markSectionRead(context, KEY_LAST_READ_DATASET);
    }

    /**
     * Convenience method to mark reports section as read.
     */
    public static void markReportsSectionRead(Context context) {
        markSectionRead(context, KEY_LAST_READ_REPORTS);
    }

    /**
     * Returns the last-read timestamp for a section. Returns 0L if never read.
     */
    public static long getSectionLastRead(Context context, String sectionKey) {
        return getPrefs(context).getLong(sectionKey, 0L);
    }

    /**
     * Convenience method to get business section last-read timestamp.
     */
    public static long getBusinessLastRead(Context context) {
        return getSectionLastRead(context, KEY_LAST_READ_BUSINESS);
    }

    /**
     * Convenience method to get info section last-read timestamp.
     */
    public static long getInfoLastRead(Context context) {
        return getSectionLastRead(context, KEY_LAST_READ_INFO);
    }

    /**
     * Convenience method to get location section last-read timestamp.
     */
    public static long getLocationLastRead(Context context) {
        return getSectionLastRead(context, KEY_LAST_READ_LOCATION);
    }

    /**
     * Convenience method to get dataset section last-read timestamp.
     */
    public static long getDatasetLastRead(Context context) {
        return getSectionLastRead(context, KEY_LAST_READ_DATASET);
    }

    /**
     * Convenience method to get reports section last-read timestamp.
     */
    public static long getReportsLastRead(Context context) {
        return getSectionLastRead(context, KEY_LAST_READ_REPORTS);
    }

    // ─────────────────────────────────────────────────────────────
    // REQUEST ID OPERATIONS (request list item dot indicators)
    // ─────────────────────────────────────────────────────────────

    // ─── Location/Info request IDs ───

    /**
     * Returns the set of Location/Info request IDs marked as read.
     */
    public static Set<String> getReadRequestIds(Context context) {
        return new HashSet<>(getPrefs(context).getStringSet(KEY_READ_REQUEST_IDS, new HashSet<>()));
    }

    /**
     * Returns true if the request (Location/Info) ID is marked as read.
     */
    public static boolean isRequestRead(Context context, String requestId) {
        if (requestId == null) return false;
        return getReadRequestIds(context).contains(requestId);
    }

    /**
     * Marks a Location/Info request ID as read.
     */
    public static void markRequestRead(Context context, String requestId) {
        if (requestId == null || requestId.isEmpty()) return;
        Set<String> ids = new HashSet<>(getReadRequestIds(context));
        if (ids.add(requestId)) {
            getPrefs(context).edit().putStringSet(KEY_READ_REQUEST_IDS, ids).apply();
        }
    }

    // ─── Business verification IDs ───

    /**
     * Returns the set of business verification IDs marked as read.
     */
    public static Set<String> getReadBizVerIds(Context context) {
        return new HashSet<>(getPrefs(context).getStringSet(KEY_READ_BIZ_VER_IDS, new HashSet<>()));
    }

    /**
     * Returns true if the business verification ID is marked as read.
     */
    public static boolean isBizVerRead(Context context, String bizVerId) {
        if (bizVerId == null) return false;
        return getReadBizVerIds(context).contains(bizVerId);
    }

    /**
     * Marks a business verification ID as read.
     */
    public static void markBizVerRead(Context context, String bizVerId) {
        if (bizVerId == null || bizVerId.isEmpty()) return;
        Set<String> ids = new HashSet<>(getReadBizVerIds(context));
        if (ids.add(bizVerId)) {
            getPrefs(context).edit().putStringSet(KEY_READ_BIZ_VER_IDS, ids).apply();
        }
    }

    // ─── Report IDs ───

    /**
     * Returns the set of report IDs marked as read.
     */
    public static Set<String> getReadReportIds(Context context) {
        return new HashSet<>(getPrefs(context).getStringSet(KEY_READ_REPORT_IDS, new HashSet<>()));
    }

    /**
     * Returns true if the report ID is marked as read.
     */
    public static boolean isReportRead(Context context, String reportId) {
        if (reportId == null) return false;
        return getReadReportIds(context).contains(reportId);
    }

    /**
     * Marks a report ID as read.
     */
    public static void markReportRead(Context context, String reportId) {
        if (reportId == null || reportId.isEmpty()) return;
        Set<String> ids = new HashSet<>(getReadReportIds(context));
        if (ids.add(reportId)) {
            getPrefs(context).edit().putStringSet(KEY_READ_REPORT_IDS, ids).apply();
        }
    }
}
