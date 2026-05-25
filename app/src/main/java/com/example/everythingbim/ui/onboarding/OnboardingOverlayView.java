package com.example.everythingbim.ui.onboarding;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.example.everythingbim.R;

public class OnboardingOverlayView extends FrameLayout {

    public interface Listener {
        void onNext();
        void onSkip();
    }

    private final FrameLayout cardContainer;
    private final TextView titleView;
    private final TextView messageView;
    private final Button nextButton;
    private final TextView skipView;
    private final Paint scrimPaint;
    private final Paint strokePaint;
    private final Path scrimPath;
    private final RectF spotlightRect;
    private boolean showSpotlight;

    public OnboardingOverlayView(@NonNull Context context) {
        super(context);
        setClickable(true);
        setFocusable(true);
        setWillNotDraw(false);

        scrimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        scrimPaint.setColor(Color.parseColor("#DE5A6673"));

        strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        strokePaint.setColor(context.getColor(R.color.bright_gold));
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(dp(2));

        scrimPath = new Path();
        spotlightRect = new RectF();

        cardContainer = new FrameLayout(context);
        LayoutParams containerParams = new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
        );
        int margin = dp(24);
        containerParams.setMargins(margin, margin, margin, margin);
        addView(cardContainer, containerParams);

        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(20), dp(20), dp(20), dp(20));

        GradientDrawable cardBackground = new GradientDrawable();
        cardBackground.setColor(context.getColor(R.color.white));
        cardBackground.setCornerRadius(dp(18));
        card.setBackground(cardBackground);

        titleView = new TextView(context);
        titleView.setTextColor(context.getColor(R.color.prussian_blue));
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        titleView.setTypeface(titleView.getTypeface(), android.graphics.Typeface.BOLD);
        card.addView(titleView);

        messageView = new TextView(context);
        LinearLayout.LayoutParams messageParams = new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
        );
        messageParams.topMargin = dp(10);
        messageView.setLayoutParams(messageParams);
        messageView.setTextColor(context.getColor(R.color.dim_grey));
        messageView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        card.addView(messageView);

        LinearLayout actionsRow = new LinearLayout(context);
        actionsRow.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams actionsParams = new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
        );
        actionsParams.topMargin = dp(20);
        actionsRow.setLayoutParams(actionsParams);

        skipView = new TextView(context);
        skipView.setText(context.getString(R.string.onboarding_skip));
        skipView.setTextColor(context.getColor(R.color.dim_grey));
        skipView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        actionsRow.addView(skipView);

        nextButton = new Button(context);
        LinearLayout.LayoutParams nextParams = new LinearLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
        );
        nextParams.leftMargin = dp(16);
        nextButton.setLayoutParams(nextParams);
        nextButton.setAllCaps(false);
        nextButton.setBackgroundResource(R.drawable.bg_rectangle_blue);
        nextButton.setTextColor(context.getColor(R.color.white));
        actionsRow.addView(nextButton);

        card.addView(actionsRow);
        cardContainer.addView(card);
    }

    public void render(@NonNull OnboardingStep step, boolean isLastStep, View targetView,
                       @NonNull Listener listener) {
        titleView.setText(step.getTitle());
        messageView.setText(step.getMessage());
        nextButton.setText(isLastStep
                ? getContext().getString(R.string.onboarding_done)
                : getContext().getString(R.string.onboarding_next));

        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) cardContainer.getLayoutParams();
        if (targetView != null && step.getLayoutStyle() != OnboardingStep.LayoutStyle.CENTER) {
            Rect targetRect = resolveTargetRect(targetView);
            int padding = dp(8);
            spotlightRect.set(
                    Math.max(0, targetRect.left - padding),
                    Math.max(0, targetRect.top - padding),
                    Math.min(getWidth(), targetRect.right + padding),
                    Math.min(getHeight(), targetRect.bottom + padding)
            );
            showSpotlight = true;
        } else {
            spotlightRect.setEmpty();
            showSpotlight = false;
        }

        if (step.getLayoutStyle() == OnboardingStep.LayoutStyle.CENTER) {
            params.gravity = Gravity.CENTER;
            params.bottomMargin = dp(24);
            params.topMargin = dp(24);
        } else if (step.getLayoutStyle() == OnboardingStep.LayoutStyle.AUTO && targetView != null) {
            if (spotlightRect.centerY() > getHeight() / 2f) {
                params.gravity = Gravity.TOP;
                params.topMargin = dp(48);
                params.bottomMargin = dp(24);
            } else {
                params.gravity = Gravity.BOTTOM;
                params.topMargin = dp(24);
                params.bottomMargin = dp(92);
            }
        } else {
            params.gravity = Gravity.BOTTOM;
            params.topMargin = dp(24);
            params.bottomMargin = dp(92);
        }
        cardContainer.setLayoutParams(params);
        invalidate();

        skipView.setOnClickListener(v -> listener.onSkip());
        nextButton.setOnClickListener(v -> listener.onNext());
    }

    @Override
    protected void onDraw(android.graphics.Canvas canvas) {
        super.onDraw(canvas);

        scrimPath.reset();
        scrimPath.setFillType(Path.FillType.EVEN_ODD);
        scrimPath.addRect(0, 0, getWidth(), getHeight(), Path.Direction.CW);
        if (showSpotlight && !spotlightRect.isEmpty()) {
            scrimPath.addRoundRect(spotlightRect, dp(18), dp(18), Path.Direction.CCW);
        }
        canvas.drawPath(scrimPath, scrimPaint);

        if (showSpotlight && !spotlightRect.isEmpty()) {
            canvas.drawRoundRect(spotlightRect, dp(18), dp(18), strokePaint);
        }
    }

    @NonNull
    private Rect resolveTargetRect(@NonNull View targetView) {
        int[] overlayLocation = new int[2];
        int[] targetLocation = new int[2];
        getLocationOnScreen(overlayLocation);
        targetView.getLocationOnScreen(targetLocation);
        return new Rect(
                targetLocation[0] - overlayLocation[0],
                targetLocation[1] - overlayLocation[1],
                targetLocation[0] - overlayLocation[0] + targetView.getWidth(),
                targetLocation[1] - overlayLocation[1] + targetView.getHeight()
        );
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                getResources().getDisplayMetrics()
        );
    }
}
