package com.example.everythingbim.data.local;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import android.content.Context;

import com.example.everythingbim.data.local.dao.LocationDao;
import com.example.everythingbim.data.local.dao.MarkerDao;
import com.example.everythingbim.data.local.dao.PostDao;
import com.example.everythingbim.data.local.entities.LocationEntity;
import com.example.everythingbim.data.local.entities.MarkerEntity;
import com.example.everythingbim.data.local.entities.PostEntity;
import com.example.everythingbim.data.local.entities.ReviewEntity;
import com.example.everythingbim.data.local.entities.CommentEntity;
import com.example.everythingbim.data.local.entities.LikeEntity;

@Database(entities = {
        MarkerEntity.class,
        PostEntity.class,
        LocationEntity.class,
        ReviewEntity.class,
        CommentEntity.class,
        LikeEntity.class
}, version = 2)
public abstract class AppDatabase extends RoomDatabase {

    public abstract MarkerDao markerDao();
    public abstract PostDao postDao();
    public abstract LocationDao locationDao();

    // Singleton instance
    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "app_database")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}