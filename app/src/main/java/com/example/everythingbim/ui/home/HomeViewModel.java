package com.example.everythingbim.ui.home;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.everythingbim.data.models.SelectedImage;

import java.util.concurrent.atomic.AtomicBoolean;

public class HomeViewModel extends ViewModel {

    private final SingleLiveEvent<SelectedImage> navigationEvent = new SingleLiveEvent<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    @NonNull
    public LiveData<SelectedImage> getNavigationEvent() {
        return navigationEvent;
    }

    @NonNull
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public void onImageSelected(@NonNull SelectedImage selectedImage) {
        navigationEvent.setValue(selectedImage);
    }

    public void onSelectionError(@NonNull String message) {
        errorMessage.setValue(message);
    }

    /**
     * SingleLiveEvent - ensures the event is delivered only once.
     */
    public static class SingleLiveEvent<T> extends MutableLiveData<T> {
        private final AtomicBoolean pending = new AtomicBoolean(false);

        @Override
        public void setValue(T value) {
            pending.set(true);
            super.setValue(value);
        }

        @Override
        public void observe(@NonNull androidx.lifecycle.LifecycleOwner owner,
                            @NonNull androidx.lifecycle.Observer<? super T> observer) {
            super.observe(owner, t -> {
                if (pending.compareAndSet(true, false)) {
                    observer.onChanged(t);
                }
            });
        }
    }
}
