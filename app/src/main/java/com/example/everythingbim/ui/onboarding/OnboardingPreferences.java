package com.example.everythingbim.ui.onboarding;

import android.content.Context;
import android.content.SharedPreferences;

public class OnboardingPreferences {
    private static final String PREFS_NAME = "app_prefs";
    private static final String KEY_GUEST_TOUR_SEEN = "tour_seen_guest_universal";

    private final SharedPreferences sharedPreferences;

    public OnboardingPreferences(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public boolean hasSeenGuestTour() {
        return sharedPreferences.getBoolean(KEY_GUEST_TOUR_SEEN, false);
    }

    public void setGuestTourSeen(boolean seen) {
        sharedPreferences.edit().putBoolean(KEY_GUEST_TOUR_SEEN, seen).apply();
    }
}
