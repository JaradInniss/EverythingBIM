package com.example.everythingbim.ui.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.everythingbim.R;

public final class KeyboardScrollHintHelper {
    private static final String UI_HINTS_PREFS = "ui_hints_prefs";
    public static final String PREF_LOGIN_SCROLL_HINT_SEEN = "login_scroll_hint_seen";
    public static final String PREF_GENERAL_REG_SCROLL_HINT_SEEN = "general_registration_scroll_hint_seen";
    public static final String PREF_BUSINESS_REG_SCROLL_HINT_SEEN = "business_registration_scroll_hint_seen";
    public static final String PREF_VIEW_POST_SCROLL_HINT_SEEN = "view_post_scroll_hint_seen";
    public static final String PREF_CREATE_POST_SCROLL_HINT_SEEN = "create_post_scroll_hint_seen";
    public static final String PREF_ADD_LOCATION_SCROLL_HINT_SEEN = "add_location_scroll_hint_seen";
    public static final String PREF_ADD_INFORMATION_SCROLL_HINT_SEEN = "add_information_scroll_hint_seen";
    public static final String PREF_MAP_SCROLL_HINT_SEEN = "map_scroll_hint_seen";
    private static final int SCROLL_HINT_TRANSLATION_DP = 24;
    private static final int SCROLL_HINT_HAND_SWIPE_DP = 14;

    private KeyboardScrollHintHelper() {
    }

    public interface InsetUpdateCallback {
        void onKeyboardInsetChanged(int keyboardExtraBottom);
    }

    public interface ShowCondition {
        boolean canShow();
    }

    public static void resetAllHints(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(UI_HINTS_PREFS, Context.MODE_PRIVATE);
        preferences.edit()
                .remove(PREF_LOGIN_SCROLL_HINT_SEEN)
                .remove(PREF_GENERAL_REG_SCROLL_HINT_SEEN)
                .remove(PREF_BUSINESS_REG_SCROLL_HINT_SEEN)
                .remove(PREF_VIEW_POST_SCROLL_HINT_SEEN)
                .remove(PREF_CREATE_POST_SCROLL_HINT_SEEN)
                .remove(PREF_ADD_LOCATION_SCROLL_HINT_SEEN)
                .remove(PREF_ADD_INFORMATION_SCROLL_HINT_SEEN)
                .remove(PREF_MAP_SCROLL_HINT_SEEN)
                .apply();
    }

    public static void showPreview(View root) {
        HintController controller = new HintController(root, root, "preview_only", null);
        controller.currentKeyboardExtraBottom = 0;
        controller.showScrollHintOverlay(false);
    }

    public static void attach(
            View root,
            View triggerView,
            View scrollContainer,
            String preferenceKey,
            InsetUpdateCallback insetUpdateCallback
    ) {
        attach(root, triggerView, scrollContainer, preferenceKey, insetUpdateCallback, null);
    }

    public static void attach(
            View root,
            View triggerView,
            View scrollContainer,
            String preferenceKey,
            InsetUpdateCallback insetUpdateCallback,
            @Nullable ShowCondition showCondition
    ) {
        HintController controller = new HintController(root, scrollContainer, preferenceKey, showCondition);

        ViewCompat.setOnApplyWindowInsetsListener(triggerView, (view, insets) -> {
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            int keyboardExtraBottom = Math.max(ime.bottom - systemBars.bottom, 0);
            controller.currentKeyboardExtraBottom = keyboardExtraBottom;
            insetUpdateCallback.onKeyboardInsetChanged(keyboardExtraBottom);

            View overlay = controller.findOverlayBubble();
            if (overlay != null) {
                overlay.setTranslationY(-keyboardExtraBottom);
            }

            controller.maybeShow();
            return insets;
        });
        ViewCompat.requestApplyInsets(triggerView);
    }

    private static final class HintController {
        private final View root;
        private final View scrollContainer;
        private final String preferenceKey;
        @Nullable
        private final ShowCondition showCondition;
        private boolean hasShownScrollHintThisSession;
        private int currentKeyboardExtraBottom;

        private HintController(View root, View scrollContainer, String preferenceKey, @Nullable ShowCondition showCondition) {
            this.root = root;
            this.scrollContainer = scrollContainer;
            this.preferenceKey = preferenceKey;
            this.showCondition = showCondition;
        }

        private void maybeShow() {
            if (currentKeyboardExtraBottom <= 0 || hasShownScrollHintThisSession || hasSeenScrollHint() || !canShow()) {
                return;
            }

            scrollContainer.post(() -> {
                if (hasShownScrollHintThisSession || hasSeenScrollHint() || !canShow()) {
                    return;
                }
                if (!(scrollContainer instanceof ViewGroup)) {
                    return;
                }

                View scrollChild = ((ViewGroup) scrollContainer).getChildAt(0);
                if (scrollChild == null) {
                    return;
                }

                int remainingScroll = scrollChild.getHeight()
                        + scrollContainer.getPaddingBottom()
                        - (scrollContainer.getHeight() + scrollContainer.getScrollY());
                boolean canScroll = scrollContainer.canScrollVertically(1)
                        || scrollContainer.canScrollVertically(-1)
                        || remainingScroll > dpToPx(root.getContext(), 16);
                if (!canScroll) {
                    return;
                }
                showScrollHintOverlay(true);
            });
        }

        private boolean canShow() {
            return showCondition == null || showCondition.canShow();
        }

        private boolean hasSeenScrollHint() {
            SharedPreferences preferences = root.getContext()
                    .getSharedPreferences(UI_HINTS_PREFS, Context.MODE_PRIVATE);
            return preferences.getBoolean(preferenceKey, false);
        }

        private void markScrollHintSeen() {
            SharedPreferences preferences = root.getContext()
                    .getSharedPreferences(UI_HINTS_PREFS, Context.MODE_PRIVATE);
            preferences.edit().putBoolean(preferenceKey, true).apply();
        }

        private void showScrollHintOverlay(boolean markSeen) {
            View overlay = findOverlayBubble();
            View hand = findOverlayChild(R.id.scroll_hint_hand);
            View arrows = findOverlayChild(R.id.scroll_hint_arrows);
            if (overlay == null || hand == null || arrows == null) {
                return;
            }

            hasShownScrollHintThisSession = true;
            float hiddenTranslationY = dpToPx(root.getContext(), SCROLL_HINT_TRANSLATION_DP) - currentKeyboardExtraBottom;
            float shownTranslationY = -currentKeyboardExtraBottom;

            overlay.bringToFront();
            overlay.setVisibility(View.VISIBLE);
            overlay.setAlpha(0f);
            overlay.setTranslationY(hiddenTranslationY);
            hand.animate().cancel();
            arrows.animate().cancel();
            hand.setTranslationY(0f);
            arrows.setTranslationY(0f);

            overlay.animate()
                    .alpha(1f)
                    .translationY(shownTranslationY)
                    .setDuration(220)
                    .withEndAction(() -> {
                        if (markSeen) {
                            markScrollHintSeen();
                        }
                        float handSwipeDistance = -dpToPx(root.getContext(), SCROLL_HINT_HAND_SWIPE_DP);
                        hand.animate()
                                .translationY(handSwipeDistance)
                                .setDuration(450)
                                .withEndAction(() -> hand.animate()
                                        .translationY(dpToPx(root.getContext(), 6))
                                        .setDuration(450)
                                        .withEndAction(() -> hand.animate()
                                                .translationY(0f)
                                                .setDuration(300)
                                                .start())
                                        .start())
                                .start();
                        arrows.animate()
                                .translationY(-8f)
                                .setDuration(450)
                                .withEndAction(() -> arrows.animate()
                                        .translationY(8f)
                                        .setDuration(450)
                                        .withEndAction(() -> arrows.animate()
                                                .translationY(0f)
                                                .setDuration(300)
                                                .start())
                                        .start())
                                .start();

                        overlay.postDelayed(() -> overlay.animate()
                                .alpha(0f)
                                .translationY(shownTranslationY - dpToPx(root.getContext(), 12))
                                .setDuration(220)
                                .withEndAction(() -> {
                                    hand.setTranslationY(0f);
                                    arrows.setTranslationY(0f);
                                    overlay.setVisibility(View.GONE);
                                })
                                .start(), 1800);
                    })
                    .start();
        }

        @Nullable
        private View findOverlayBubble() {
            View host = findOverlayHost();
            if (host != null) {
                View child = host.findViewById(R.id.scroll_hint_overlay);
                if (child != null) {
                    return child;
                }
            }
            return root.findViewById(R.id.scroll_hint_overlay);
        }

        @Nullable
        private View findOverlayHost() {
            View host = root.findViewById(R.id.scroll_hint_overlay_host);
            if (host != null) {
                return host;
            }
            return null;
        }

        @Nullable
        private View findOverlayChild(int id) {
            View host = findOverlayHost();
            if (host != null) {
                View child = host.findViewById(id);
                if (child != null) {
                    return child;
                }
            }
            return root.findViewById(id);
        }
    }

    private static int dpToPx(Context context, int dp) {
        return Math.round(dp * context.getResources().getDisplayMetrics().density);
    }
}
