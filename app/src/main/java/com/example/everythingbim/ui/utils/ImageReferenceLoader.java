package com.example.everythingbim.ui.utils;

import android.widget.ImageView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;

public final class ImageReferenceLoader {

    private ImageReferenceLoader() {
    }

    public static void loadInto(@NonNull ImageView target,
                                @Nullable String imageReference,
                                @DrawableRes int placeholderResId) {
        if (imageReference == null || imageReference.trim().isEmpty()) {
            Glide.with(target)
                    .load(placeholderResId)
                    .centerCrop()
                    .into(target);
            return;
        }

        Glide.with(target)
                .load(imageReference)
                .placeholder(placeholderResId)
                .error(placeholderResId)
                .centerCrop()
                .into(target);
    }
}
