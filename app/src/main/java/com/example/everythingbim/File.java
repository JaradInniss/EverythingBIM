package com.example.everythingbim;

import android.net.Uri;

public class File {
    private String name;
    private String mimeType;
    private Uri uri;
    private long size;

    public File(String name, String mimeType, Uri uri, long size){
        this.name = name;
        this.mimeType = mimeType;
        this.uri = uri;
        this.size = size;
    }

    // Getters
    public String getName() { return name; }
    public String getMimeType() { return mimeType; }
    public Uri getUri() { return uri; }
    public long getSize() { return size; }
}
