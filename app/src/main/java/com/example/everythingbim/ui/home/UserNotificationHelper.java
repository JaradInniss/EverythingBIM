package com.example.everythingbim.ui.home;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public final class UserNotificationHelper {
    public static final String COLLECTION_USER_NOTIFICATIONS = "user_notifications";
    public static final String TYPE_INFO_REQUEST = "info_request";
    public static final String TYPE_LOCATION_REQUEST = "location_request";
    public static final String TYPE_DATASET_SUBMISSION = "dataset_submission";
    public static final String TYPE_BUSINESS_VERIFICATION = "business_verification";
    public static final String TARGET_COMPLETED_INFO = "user_completed_info";
    public static final String TARGET_COMPLETED_LOCATION = "user_completed_location";
    public static final String TARGET_DATASET_DIALOG = "user_dataset_dialog";
    public static final String TARGET_COMPLETED_BUSINESS_VERIFICATION = "user_completed_business_verification";

    private UserNotificationHelper() {
    }

    public static void createNotification(@NonNull FirebaseFirestore db,
                                          @Nullable String recipientUserId,
                                          @NonNull String type,
                                          @NonNull String title,
                                          @NonNull String message,
                                          @Nullable String relatedDocId,
                                          @Nullable String relatedCollection) {
        createNotification(
                db,
                recipientUserId,
                type,
                title,
                message,
                "",
                relatedDocId,
                relatedCollection,
                "",
                type,
                ""
        );
    }

    public static void createNotification(@NonNull FirebaseFirestore db,
                                          @Nullable String recipientUserId,
                                          @NonNull String type,
                                          @NonNull String title,
                                          @NonNull String message,
                                          @Nullable String status,
                                          @Nullable String relatedDocId,
                                          @Nullable String relatedCollection,
                                          @Nullable String targetScreen,
                                          @Nullable String targetType,
                                          @Nullable String entityName) {
        if (recipientUserId == null || recipientUserId.trim().isEmpty() || "anonymous".equals(recipientUserId)) {
            return;
        }

        Map<String, Object> notification = new HashMap<>();
        notification.put("recipientUserId", recipientUserId);
        notification.put("type", type);
        notification.put("title", title);
        notification.put("message", message);
        notification.put("status", status != null ? status : "");
        notification.put("relatedDocId", relatedDocId != null ? relatedDocId : "");
        notification.put("relatedCollection", relatedCollection != null ? relatedCollection : "");
        notification.put("targetScreen", targetScreen != null ? targetScreen : "");
        notification.put("targetType", targetType != null ? targetType : "");
        notification.put("entityName", entityName != null ? entityName : "");
        notification.put("createdAt", Timestamp.now());
        notification.put("read", false);

        db.collection(COLLECTION_USER_NOTIFICATIONS).add(notification);
    }
}
