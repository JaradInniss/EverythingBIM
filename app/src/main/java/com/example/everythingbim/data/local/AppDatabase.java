package com.example.everythingbim.data.local;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import android.content.Context;

import com.example.everythingbim.data.local.dao.MarkerDao;
import com.example.everythingbim.data.local.entities.MarkerEntity;

@Database(entities = {MarkerEntity.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {

    public abstract MarkerDao markerDao();

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