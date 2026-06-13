package com.example.everythingbim.ui.home;

import android.content.Context;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.everythingbim.R;
import com.example.everythingbim.ui.utils.ImageReferenceLoader;

final class NearbyLocationImageHelper {
    private NearbyLocationImageHelper() {
    }

    static void loadInto(@NonNull ImageView imageView, @Nullable String imageRef) {
        ImageReferenceLoader.loadInto(imageView, imageRef, R.drawable.ic_images);
    }
}
