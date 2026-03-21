package com.example.everythingbim.ui.registration;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import java.util.concurrent.atomic.AtomicBoolean;

public class GeneralRegViewModel extends ViewModel {

    // UI State
    private final MutableLiveData<Integer> currentPage = new MutableLiveData<>(0);
    private final SingleLiveEvent<Class<?>> navigationEvent = new SingleLiveEvent<>();

    // Getters
    public MutableLiveData<Integer> getCurrentPage() { return currentPage; }
    public SingleLiveEvent<Class<?>> getNavigationEvent() { return navigationEvent; }

    // Navigation methods
    public void nextPage() {
        Integer curr = currentPage.getValue();
        if (curr != null) {
            currentPage.setValue(curr + 1);
        }
    }

    public void prevPage() {
        Integer curr = currentPage.getValue();
        if (curr != null && curr > 0) {
            currentPage.setValue(curr - 1);
        }
    }

    public void navigateTo(Class<?> destination) {
        navigationEvent.setValue(destination);
    }

    /**
     * A LiveData class that only delivers the value once, preventing accidental
     * re‑delivery after configuration changes.
     */
    public static class SingleLiveEvent<T> extends MutableLiveData<T> {
        private final AtomicBoolean pending = new AtomicBoolean(false);

        @Override
        public void setValue(T value) {
            pending.set(true);
            super.setValue(value);
        }

        @Override
        public void observe(@NonNull LifecycleOwner owner, @NonNull Observer<? super T> observer) {
            super.observe(owner, t -> {
                if (pending.compareAndSet(true, false)) {
                    observer.onChanged(t);
                }
            });
        }
    }
}