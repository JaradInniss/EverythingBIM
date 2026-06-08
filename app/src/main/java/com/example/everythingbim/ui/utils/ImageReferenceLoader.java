package com.example.everythingbim.ui.utils;

import android.content.Context;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;

import java.util.Locale;

public final class ImageReferenceLoader {
    private ImageReferenceLoader() {
    }

    public static void loadInto(@NonNull ImageView imageView,
                                @Nullable String imageRef,
                                @DrawableRes int placeholderResId) {
        if (imageRef == null || imageRef.trim().isEmpty()) {
            imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            imageView.setImageResource(placeholderResId);
            return;
        }

        String normalizedRef = imageRef.trim();
        Object loadTarget = resolveLoadTarget(imageView.getContext(), normalizedRef);
        if (loadTarget == null) {
            imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            imageView.setImageResource(placeholderResId);
            return;
        }

        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        Glide.with(imageView)
                .load(loadTarget)
                .placeholder(placeholderResId)
                .error(placeholderResId)
                .centerCrop()
                .into(imageView);
    }

    @Nullable
    public static Object resolveLoadTarget(@NonNull Context context, @Nullable String imageRef) {
        if (imageRef == null || imageRef.trim().isEmpty()) {
            return null;
        }

        String normalizedRef = imageRef.trim();
        if (isDirectLoadableReference(normalizedRef)) {
            return normalizedRef;
        }

        int drawableResId = resolveDrawableResource(context, normalizedRef);
        return drawableResId != 0 ? drawableResId : null;
    }

    private static int resolveDrawableResource(@NonNull Context context, @NonNull String rawImageRef) {
        String candidate = rawImageRef;
        int slashIndex = candidate.lastIndexOf('/');
        if (slashIndex >= 0 && slashIndex < candidate.length() - 1) {
            candidate = candidate.substring(slashIndex + 1);
        }

        int extensionIndex = candidate.lastIndexOf('.');
        if (extensionIndex > 0) {
            candidate = candidate.substring(0, extensionIndex);
        }

        candidate = candidate
                .trim()
                .toLowerCase(Locale.US)
                .replace('-', '_')
                .replace(' ', '_');

        return context.getResources().getIdentifier(
                candidate,
                "drawable",
                context.getPackageName()
        );
    }

    private static boolean isDirectLoadableReference(@NonNull String imageRef) {
        String lower = imageRef.toLowerCase(Locale.US);
        return lower.startsWith("http://")
                || lower.startsWith("https://")
                || lower.startsWith("content://")
                || lower.startsWith("file://")
                || lower.startsWith("android.resource://");
    }
}
