package com.example.everythingbim.ui.admin;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public final class AdminNotificationHelper {
    public static final String COLLECTION_ADMIN_NOTIFICATIONS = "admin_notifications";

    public static final String TYPE_INFO_REQUEST = "info_request";
    public static final String TYPE_LOCATION_REQUEST = "location_request";
    public static final String TYPE_DATASET_SUBMISSION = "dataset_submission";

    public static final String TARGET_INFO_DETAIL = "admin_info_detail";
    public static final String TARGET_LOCATION_DETAIL = "admin_location_detail";
    public static final String TARGET_DATASET_DETAIL = "admin_dataset_detail";

    private AdminNotificationHelper() {
    }

    public static void createNotification(@NonNull FirebaseFirestore db,
                                          @NonNull String type,
                                          @NonNull String title,
                                          @NonNull String message,
                                          @NonNull String status,
                                          @Nullable String relatedDocId,
                                          @Nullable String relatedCollection,
                                          @Nullable String targetScreen,
                                          @Nullable String targetType,
                                          @Nullable String entityName,
                                          @Nullable String submittedBy) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", type);
        notification.put("title", title);
        notification.put("message", message);
        notification.put("status", status);
        notification.put("read", false);
        notification.put("createdAt", Timestamp.now());
        notification.put("relatedDocId", relatedDocId != null ? relatedDocId : "");
        notification.put("relatedCollection", relatedCollection != null ? relatedCollection : "");
        notification.put("targetScreen", targetScreen != null ? targetScreen : "");
        notification.put("targetType", targetType != null ? targetType : "");
        notification.put("entityName", entityName != null ? entityName : "");
        notification.put("submittedBy", submittedBy != null ? submittedBy : "");

        db.collection(COLLECTION_ADMIN_NOTIFICATIONS).add(notification);
    }
}
