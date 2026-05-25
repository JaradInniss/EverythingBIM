# EverythingBIM Test Infrastructure

## Overview

This document describes the test infrastructure for the EverythingBIM Android application, including how to run tests, dependencies, and guidelines for writing new tests.

## Test Framework

The project uses:
- **JUnit 4** - Core testing framework
- **Robolectric 4.14** - Android framework emulation for unit tests
- **Mockito 5.14.2** - Dependency mocking
- **AndroidX Test** - Android testing utilities

## Running Tests  

### Run all unit tests:
```bash
.\gradlew :app:testDebugUnitTest --tests "com.example.everythingbim.*"
```

### Clean and run all tests:
```bash
.\gradlew clean :app:testDebugUnitTest --tests "com.example.everythingbim.*"
```

### Run a specific test class:
```bash
.\gradlew :app:testDebugUnitTest --tests "com.example.everythingbim.LoginViewModelTest"
```

### Build the project (includes test compilation):
```bash
.\gradlew :app:build
```

## Test Dependencies

The following dependencies are configured in `app/build.gradle`:

```gradle
testImplementation 'org.robolectric:robolectric:4.14'
testImplementation 'net.bytebuddy:byte-buddy:1.17.2'
testImplementation 'org.mockito:mockito-core:5.14.2'
testImplementation 'org.mockito:mockito-android:5.14.2'
testImplementation 'androidx.test:core:1.5.0'
testImplementation 'androidx.test:fragment:fragment-testing:1.8.3'
testImplementation 'androidx.test:runner:1.5.2'
testImplementation 'androidx.test:rules:1.5.0'
testImplementation 'junit:junit:4.13.2'
```

## Writing ViewModel Tests

### Basic Robolectric Test Structure

For ViewModels that extend Android `ViewModel` and need Android context:

```java
package com.example.everythingbim;

import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLooper;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowLooper.class})
public class MyViewModelTest {

    private MyViewModel viewModel;

    @Before
    public void setUp() {
        viewModel = new MyViewModel();
    }

    // Your tests here...
}
```

### ViewModels with FirebaseProvider DI

For ViewModels that use the `FirebaseProvider` interface for dependency injection:

```java
@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowLooper.class})
public class MyViewModelTest {

    private MyViewModel viewModel;

    @Before
    public void setUp() {
        viewModel = new MyViewModel(new MockFirebaseProvider());
    }

    // Your tests here...
}
```

### ViewModels Requiring Application Context

Some ViewModels require an Application context in their constructor:

```java
import androidx.test.core.app.ApplicationProvider;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowLooper.class})
public class MyViewModelTest {

    private MyViewModel viewModel;

    @Before
    public void setUp() {
        Application application = ApplicationProvider.getApplicationContext();
        viewModel = new MyViewModel(application);
    }
}
```

## FirebaseProvider Dependency Injection

### Interface

`FirebaseProvider.java` is the interface for Firebase operations:

```java
public interface FirebaseProvider {
    FirebaseAuth getAuth();
    FirebaseFirestore getFirestore();
    FirebaseStorage getStorage();
    // ... other Firebase methods
}
```

### Implementations

| Class | Purpose |
|-------|---------|
| `RealFirebaseProvider` | Production implementation returning real Firebase instances |
| `MockFirebaseProvider` | Test implementation returning null; used for ViewModels with Firebase dependencies that aren't called in tests |

### Usage in ViewModels

ViewModels that use Firebase should accept `FirebaseProvider` in their constructor:

```java
public class LoginViewModel extends ViewModel {

    private final FirebaseProvider firebaseProvider;

    public LoginViewModel(FirebaseProvider firebaseProvider) {
        this.firebaseProvider = firebaseProvider;
    }

    // Use firebaseProvider.getAuth(), etc.
}
```

## Test File Locations

| Type | Location |
|------|----------|
| Unit Tests | `app/src/test/java/com/example/everythingbim/` |
| Instrumented Tests | `app/src/androidTest/java/com/example/everythingbim/` |

### Existing Test Files

| Test File | Tests | Coverage |
|-----------|-------|----------|
| `EntityValidationTest.java` | 17 | Room entity validation |
| `ValidationUtilsTest.java` | 33 | Validation utility patterns |
| `GeneralRegViewModelTest.java` | 36 | General registration ViewModel |
| `BusinessRegViewModelTest.java` | 46 | Business registration ViewModel, validation, file management, navigation |
| `LoginViewModelTest.java` | 8 | Login ViewModel |
| `CreatePostViewModelTest.java` | 26 | Create post ViewModel |
| `HomeViewModelTest.java` | 9 | Home ViewModel, SingleLiveEvent |
| `AdminViewModelTest.java` | 8 | Admin ViewModel, navbar state |
| `MapViewModelTest.java` | 14 | Map ViewModel, Barbados bounds |
| `NavigationCommandTest.java` | 5 | NavigationCommand model |
| `ExampleUnitTest.java` | 1 | JUnit example |

## Base Test Classes

### RobolectricTest

A base class providing common Robolectric setup:

```java
public abstract class RobolectricTest {
    protected Application application;

    @Before
    public void setUpApplication() {
        application = ApplicationProvider.getApplicationContext();
    }
}
```

### TestApplication

Minimal Application subclass for testing:

```java
public class TestApplication extends Application {
    // Minimal implementation for Robolectric
}
```

## Common Issues

### ShadowLooper Red Highlights in IDE

If you see red underlines on `ShadowLooper` in the IDE but tests compile and run:
1. The code is correct - this is an IDE caching issue
2. **Fix**: `File → Invalidate Caches → Invalidate and Restart`
3. Or: Close project → delete `.idea` folder → reopen project

### RobolectricTestRunner Not Found

Ensure the robolectric dependency is properly added:

```gradle
testImplementation 'org.robolectric:robolectric:4.14'
```

### LiveData setValue in Tests

LiveData's `setValue` is protected. Use Robolectric's `ShadowLooper` to handle the main looper:

```java
@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowLooper.class})
public class MyTest {
    // ShadowLooper handles LiveData.setValue() calls automatically
}
```

## Test Configuration Files

- `app/src/test/java/com/example/everythingbim/TestApplication.java` - Minimal Application subclass for testing
- `app/src/test/java/com/example/everythingbim/RobolectricTest.java` - Base class with common Robolectric setup
- `app/src/test/java/com/example/everythingbim/MockFirebaseProvider.java` - Returns null Firebase instances for tests

## Current Test Status

| Metric | Count |
|--------|-------|
| Total Tests | 203 |
| Passing | 203 |
| Failing | 0 |
| Errors | 0 |

### Test Summary by Category

| Category | Tests |
|----------|-------|
| ViewModel Tests | 133 |
| Validation Tests | 50 |
| Model/Utility Tests | 20 |

## Recent Changes

### Login Admin Collection Support (2026-05-23)

The `LoginViewModel.fetchUserTypeAndNavigate()` method now checks THREE collections in order:
1. `users` - General user accounts
2. `businesses` - Business user accounts
3. `admin` - Admin user accounts

This allows admin users to login and navigate to the AdminActivity screen.

```java
// Flow: users → businesses → admin
db.collection("users").document(userId).get()
    .addOnCompleteListener(task -> { ... });

db.collection("businesses").document(userId).get()
    .addOnCompleteListener(task2 -> { ... });

db.collection("admin").document(userId).get()
    .addOnCompleteListener(task3 -> { ... });
```

## Notes

- Firebase Test SDK mocks (`firebase-auth-testing`, `firebase-firestore-testing`) are **not available** on Maven Central. The project uses `MockFirebaseProvider` for DI-based testing instead.
- Robolectric provides a working Android emulator environment for unit tests without needing an actual device or emulator.
- ViewModels with Firebase static fields can still be tested by focusing on non-Firebase methods or using the DI pattern with `MockFirebaseProvider`.
- The `@Config(shadows = {ShadowLooper.class})` annotation handles `ShadowLooper` automatically - no manual setup in `setUp()` needed.