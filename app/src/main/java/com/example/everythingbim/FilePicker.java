package com.example.everythingbim;

import android.app.Activity;
import android.content.Intent;

public class FilePicker {
    private static final int FILE_PICKER_IMAGE_REQUEST_CODE = 105;
    private static final int FILE_PICKER_FILE_REQUEST_CODE = 110;

    public void openFilePicker(Activity activity, int requestCode) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);

        if (requestCode == FILE_PICKER_IMAGE_REQUEST_CODE) {
            intent.setType("image/*");
            intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                    "image/jpeg",
                    "image/png"
            });
        }
        else if (requestCode == FILE_PICKER_FILE_REQUEST_CODE) {
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                    "application/pdf",
                    "image/jpeg",
                    "image/png"
            });
        }
        activity.startActivityForResult(Intent.createChooser(intent, "Select Files"), requestCode);
    }
}
