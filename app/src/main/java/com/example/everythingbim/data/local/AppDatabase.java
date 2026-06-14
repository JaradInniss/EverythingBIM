package com.example.everythingbim.data.local;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.everythingbim.data.local.dao.LocationDao;
import com.example.everythingbim.data.local.dao.MarkerDao;
import com.example.everythingbim.data.local.dao.PostDao;
import com.example.everythingbim.data.local.dao.CommentDao;
import com.example.everythingbim.data.local.dao.LikeDao;
import com.example.everythingbim.data.local.dao.ReportDao;
import com.example.everythingbim.data.local.dao.ReviewDao;
import com.example.everythingbim.data.local.dao.UserDao;
import com.example.everythingbim.data.local.entities.BusinessUserEntity;
import com.example.everythingbim.data.local.entities.LocationEntity;
import com.example.everythingbim.data.local.entities.MarkerEntity;
import com.example.everythingbim.data.local.entities.PostEntity;
import com.example.everythingbim.data.local.entities.ReviewEntity;
import com.example.everythingbim.data.local.entities.CommentEntity;
import com.example.everythingbim.data.local.entities.GeneralUserEntity;
import com.example.everythingbim.data.local.entities.LikeEntity;
import com.example.everythingbim.data.local.entities.ReportEntity;
import com.example.everythingbim.data.local.entities.UserEntity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {
        MarkerEntity.class,
        PostEntity.class,
        LocationEntity.class,
        ReviewEntity.class,
        CommentEntity.class,
        LikeEntity.class,
        ReportEntity.class,
        UserEntity.class,
        GeneralUserEntity.class,
        BusinessUserEntity.class
}, version = 12)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {
    private static final ExecutorService DATABASE_EXECUTOR = Executors.newSingleThreadExecutor();

    public abstract MarkerDao markerDao();
    public abstract PostDao postDao();
    public abstract LocationDao locationDao();
    public abstract CommentDao commentDao();
    public abstract LikeDao likeDao();
    public abstract ReportDao reportDao();
    public abstract ReviewDao reviewDao();
    public abstract UserDao userDao();

    // Singleton instance
    private static volatile AppDatabase INSTANCE;

    /**
     * Migration from schema v9 to v10. v10 added:
     * <ul>
     *   <li>{@code likeCount} + {@code commentCount} on {@code posts}</li>
     *   <li>{@code postId} + {@code firestoreId} on {@code likes}</li>
     *   <li>{@code authorUid} + {@code firestoreId} on {@code comments}
     *       (and renamed {@code timestamp} -> {@code createdAt})</li>
     * </ul>
     */
    private static final Migration MIGRATION_9_10 = new Migration(9, 10) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            // The new schema is structurally different enough (renamed
            // columns + new nullable columns) that an in-place ALTER
            // would be brittle. Drop the affected tables and let Room
            // recreate them with the new schema. Foreign-key cascades
            // take care of the rest.
            db.execSQL("DROP TABLE IF EXISTS `comments`");
            db.execSQL("DROP TABLE IF EXISTS `likes`");
            // posts is mostly compatible, but the schema's identity
            // hash still includes the new likeCount / commentCount
            // columns, so dropping + recreating is the safe path.
            db.execSQL("DROP TABLE IF EXISTS `posts`");
        }
    };

    private static final Migration MIGRATION_10_11 = new Migration(10, 11) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE `posts` ADD COLUMN `locationLatitude` REAL");
            db.execSQL("ALTER TABLE `posts` ADD COLUMN `locationLongitude` REAL");
            db.execSQL("ALTER TABLE `posts` ADD COLUMN `locationAddress` TEXT");
            db.execSQL("ALTER TABLE `posts` ADD COLUMN `locationPlaceId` TEXT");
        }
    };

    private static final Migration MIGRATION_11_12 = new Migration(11, 12) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE `locations` ADD COLUMN `locationFirestoreId` TEXT");
            db.execSQL("ALTER TABLE `locations` ADD COLUMN `isActive` INTEGER NOT NULL DEFAULT 1");
            db.execSQL("ALTER TABLE `locations` ADD COLUMN `sourceType` TEXT");
            db.execSQL("ALTER TABLE `locations` ADD COLUMN `placeId` TEXT");
            db.execSQL("ALTER TABLE `locations` ADD COLUMN `updatedAt` INTEGER NOT NULL DEFAULT 0");
        }
    };

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

    /**
     * One-time wipe of any pre-v10 on-disk database. Necessary because
     * Room's {@code checkIdentity} runs in {@code onOpen} <em>before</em>
     * the version-based migration path is consulted, so a leftover v9
     * database throws {@code IllegalStateException} on every open
     * regardless of {@code fallbackToDestructiveMigration()} or any
     * {@code Migration(9, 10)} we register. The version-bump +
     * migration combination works for the <em>next</em> launch (after
     * the migration rewrites the identity hash), but only if we get
     * past the first open. Deleting the file before the first open
     * achieves that.
     *
     * <p>This is gated by a SharedPreferences flag so it runs at most
     * once per install and so future schema bumps can opt-in by
     * changing the {@code WIPE_VERSION} sentinel.</p>
     */
    private static final String WIPE_PREFS = "room_wipe";
    private static final String WIPE_KEY = "wipe_v10_done";
    private static final int WIPE_VERSION = 10;

    private static void wipeLegacyDatabaseIfNeeded(@NonNull Context appContext) {
        android.content.SharedPreferences prefs = appContext
                .getSharedPreferences(WIPE_PREFS, Context.MODE_PRIVATE);
        int lastWiped = prefs.getInt(WIPE_KEY, 0);
        if (lastWiped >= WIPE_VERSION) {
            return; // already wiped for this version
        }
        // Delete the SQLite file outright. The -wal / -shm sidecar files
        // are removed too so a re-open starts cleanly.
        java.io.File db = appContext.getDatabasePath("app_database");
        if (db.exists()) {
            //noinspection ResultOfMethodCallIgnored
            db.delete();
        }
        java.io.File wal = new java.io.File(db.getParentFile(), "app_database-wal");
        if (wal.exists()) {
            //noinspection ResultOfMethodCallIgnored
            wal.delete();
        }
        java.io.File shm = new java.io.File(db.getParentFile(), "app_database-shm");
        if (shm.exists()) {
            //noinspection ResultOfMethodCallIgnored
            shm.delete();
        }
        prefs.edit().putInt(WIPE_KEY, WIPE_VERSION).apply();
    }

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    Context appContext = context.getApplicationContext();
                    wipeLegacyDatabaseIfNeeded(appContext);
                    INSTANCE = Room.databaseBuilder(appContext,
                                    AppDatabase.class, "app_database")
                            .addMigrations(MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12)
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
