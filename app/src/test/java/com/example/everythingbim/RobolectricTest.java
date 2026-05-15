package com.example.everythingbim;

import android.app.Application;

import org.junit.Before;

import androidx.test.core.app.ApplicationProvider;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;

/**
 * Base test class that provides Android Application context for Robolectric tests.
 * Used for testing ViewModels that require Application context.
 */
public abstract class RobolectricTest {

    protected Application application;

    @Before
    public void setUpApplication() {
        application = getApplicationContext();
    }
}