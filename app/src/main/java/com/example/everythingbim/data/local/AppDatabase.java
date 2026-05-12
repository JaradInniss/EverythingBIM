package com.example.everythingbim.data.local;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.everythingbim.data.local.dao.LocationDao;
import com.example.everythingbim.data.local.dao.MarkerDao;
import com.example.everythingbim.data.local.dao.PostDao;
import com.example.everythingbim.data.local.dao.CommentDao;
import com.example.everythingbim.data.local.entities.LocationEntity;
import com.example.everythingbim.data.local.entities.MarkerEntity;
import com.example.everythingbim.data.local.entities.PostEntity;
import com.example.everythingbim.data.local.entities.ReviewEntity;
import com.example.everythingbim.data.local.entities.CommentEntity;
import com.example.everythingbim.data.local.entities.LikeEntity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {
        MarkerEntity.class,
        PostEntity.class,
        LocationEntity.class,
        ReviewEntity.class,
        CommentEntity.class,
        LikeEntity.class
}, version = 3)
public abstract class AppDatabase extends RoomDatabase {
    private static final ExecutorService DATABASE_EXECUTOR = Executors.newSingleThreadExecutor();

    public abstract MarkerDao markerDao();
    public abstract PostDao postDao();
    public abstract LocationDao locationDao();
    public abstract CommentDao commentDao();

    // Singleton instance
    private static volatile AppDatabase INSTANCE;

    private static void seedLocationsIfNeeded() {
        DATABASE_EXECUTOR.execute(() -> {
            if (INSTANCE == null) {
                return;
            }

            LocationDao locationDao = INSTANCE.locationDao();
            if (locationDao.getLocationCount() == 0) {
                locationDao.insertLocations(LocationSeedProvider.createSeedLocations());
            }
        });
    }

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "app_database")
                            .addCallback(new Callback() {
                                @Override
                                public void onCreate(@NonNull androidx.sqlite.db.SupportSQLiteDatabase db) {
                                    super.onCreate(db);
                                    seedLocationsIfNeeded();
                                }

                                @Override
                                public void onOpen(@NonNull androidx.sqlite.db.SupportSQLiteDatabase db) {
                                    super.onOpen(db);
                                    seedLocationsIfNeeded();
                                }
                            })
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
