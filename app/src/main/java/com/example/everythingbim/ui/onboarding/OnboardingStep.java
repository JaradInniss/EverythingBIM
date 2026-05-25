package com.example.everythingbim.ui.onboarding;

import androidx.annotation.Nullable;

public class OnboardingStep {
    public enum LayoutStyle {
        CENTER,
        BOTTOM,
        AUTO
    }

    private final String title;
    private final String message;
    private final LayoutStyle layoutStyle;
    @Nullable
    private final Integer navigationItemId;
    @Nullable
    private final Integer targetViewId;

    public OnboardingStep(String title, String message, LayoutStyle layoutStyle, @Nullable Integer navigationItemId) {
        this(title, message, layoutStyle, navigationItemId, null);
    }

    public OnboardingStep(String title, String message, LayoutStyle layoutStyle,
                          @Nullable Integer navigationItemId, @Nullable Integer targetViewId) {
        this.title = title;
        this.message = message;
        this.layoutStyle = layoutStyle;
        this.navigationItemId = navigationItemId;
        this.targetViewId = targetViewId;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public LayoutStyle getLayoutStyle() {
        return layoutStyle;
    }

    @Nullable
    public Integer getNavigationItemId() {
        return navigationItemId;
    }

    @Nullable
    public Integer getTargetViewId() {
        return targetViewId;
    }
}
