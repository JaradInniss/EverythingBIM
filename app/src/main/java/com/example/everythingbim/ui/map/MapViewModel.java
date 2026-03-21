package com.example.everythingbim.ui.map;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import com.example.everythingbim.data.local.AppDatabase;
import com.example.everythingbim.data.local.dao.MarkerDao;
import com.example.everythingbim.data.local.entities.MarkerEntity;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MapViewModel extends AndroidViewModel {

    private final MarkerDao markerDao;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final LiveData<List<MarkerEntity>> allMarkers;

    public MapViewModel(Application application) {
        super(application);
        markerDao = AppDatabase.getInstance(application).markerDao();
        allMarkers = markerDao.getAllMarkers();
    }

    public LiveData<List<MarkerEntity>> getAllMarkers() {
        return allMarkers;
    }

    public void insertMarker(MarkerEntity marker) {
        executorService.execute(() -> markerDao.insert(marker));
    }

    public void deleteMarker(MarkerEntity marker) {
        executorService.execute(() -> markerDao.delete(marker));
    }
}