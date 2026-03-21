package com.example.everythingbim.ui.utils;

import android.os.Bundle;

public class NavigationCommand {
    private final Class<?> destination;
    private final Bundle extras;

    public NavigationCommand(Class<?> destination) {
        this(destination, null);
    }

    public NavigationCommand(Class<?> destination, Bundle extras) {
        this.destination = destination;
        this.extras = extras;
    }

    public Class<?> getDestination() {
        return destination;
    }

    public Bundle getExtras() {
        return extras;
    }
}