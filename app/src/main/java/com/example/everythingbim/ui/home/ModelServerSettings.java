package com.example.everythingbim.ui.home;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import com.example.everythingbim.BuildConfig;

public final class ModelServerSettings {
    private static final String PREFS_NAME = "model_server_settings";
    private static final String KEY_BASE_URL = "base_url";

    private ModelServerSettings() {
    }

    @NonNull
    public static String getBaseUrl(@NonNull Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_BASE_URL, BuildConfig.ML_API_BASE_URL);
    }

    public static void setBaseUrl(@NonNull Context context, @NonNull String baseUrl) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_BASE_URL, baseUrl)
                .apply();
    }
}
