package com.example.everythingbim.data.repository;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.everythingbim.data.local.AppDatabase;
import com.example.everythingbim.data.local.dao.UserDao;
import com.example.everythingbim.data.local.entities.BusinessUserEntity;
import com.example.everythingbim.data.local.entities.GeneralUserEntity;
import com.example.everythingbim.data.local.entities.UserEntity;
import com.example.everythingbim.data.local.entities.UserWithProfile;
import com.example.everythingbim.data.models.BusinessProfile;
import com.example.everythingbim.data.models.RequestReportStatus;
import com.example.everythingbim.data.models.UserType;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


// UserRepository for User Entity

public class UserRepository {
    private static final String TAG = "UserRepository";
    private static final String COLLECTION_USERS = "users";
    private static final String COLLECTION_BUSINESSES = "businesses";

    private final UserDao userDao;
    private final ExecutorService executorService;
    private final FirebaseFirestore firestore;

    public UserRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        userDao = db.userDao();
        executorService = Executors.newFixedThreadPool(2);
        firestore = FirebaseFirestore.getInstance();
    }


    public void insert(UserEntity user) {
        executorService.execute(() -> userDao.insert(user));
    }

    public LiveData<List<UserEntity>> getAllUsers() {
        return userDao.getAllUsers();
    }

    public LiveData<UserEntity> getUserById(long userId) {
        return userDao.getUserById(userId);
    }

    public LiveData<UserWithProfile> getUserWithProfile(long userId) {
        return userDao.getUserWithProfile(userId);
    }

    public LiveData<List<UserWithProfile>> searchUsers(String query) {
        return userDao.searchUsers(query);
    }

    public LiveData<List<UserWithProfile>> searchUsersInFirestore(String query) {
        MutableLiveData<List<UserWithProfile>> results = new MutableLiveData<>();
        if (query == null || query.trim().isEmpty()) {
            results.setValue(new ArrayList<>());
            return results;
        }

        String upperBound = query + "\uf8ff";
        List<UserWithProfile> accumulator = new ArrayList<>();
        final boolean[] generalDone = {false};
        final boolean[] businessDone = {false};

        Runnable maybeEmit = () -> {
            if (generalDone[0] && businessDone[0]) {
                List<UserWithProfile> dedup = new ArrayList<>();
                java.util.Set<String> seenUids = new java.util.HashSet<>();
                for (UserWithProfile uwp : accumulator) {
                    if (uwp != null && uwp.user != null
                            && uwp.user.firebaseUid != null
                            && seenUids.add(uwp.user.firebaseUid)) {
                        dedup.add(uwp);
                    }
                }
                results.postValue(dedup);
            }
        };

        firestore.collection(COLLECTION_USERS)
                .whereGreaterThanOrEqualTo("username", query)
                .whereLessThanOrEqualTo("username", upperBound)
                .limit(20)
                .get()
                .addOnSuccessListener(snapshot -> {
                    accumulator.addAll(mapGeneralUsers(snapshot));
                    generalDone[0] = true;
                    maybeEmit.run();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "General user search failed", e);
                    generalDone[0] = true;
                    maybeEmit.run();
                });

        firestore.collection(COLLECTION_BUSINESSES)
                .whereGreaterThanOrEqualTo("businessName", query)
                .whereLessThanOrEqualTo("businessName", upperBound)
                .limit(20)
                .get()
                .addOnSuccessListener(snapshot -> {
                    accumulator.addAll(mapBusinessUsers(snapshot));
                    businessDone[0] = true;
                    maybeEmit.run();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Business user search failed", e);
                    businessDone[0] = true;
                    maybeEmit.run();
                });

        return results;
    }

    private List<UserWithProfile> mapGeneralUsers(QuerySnapshot snapshot) {
        List<UserWithProfile> result = new ArrayList<>();
        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            UserWithProfile mapped = mapGeneralUser(doc);
            if (mapped != null) {
                result.add(mapped);
            }
        }
        return result;
    }

    @androidx.annotation.Nullable
    private UserWithProfile mapGeneralUser(DocumentSnapshot doc) {
        try {
            String uid = doc.getId();
            String username = doc.getString("username");
            String email = doc.getString("email");
            if (username == null) return null;

            UserEntity user = new UserEntity(
                    username,
                    "",
                    email != null ? email : "",
                    UserType.GENERAL,
                    Boolean.TRUE.equals(doc.getBoolean("emailVerified")),
                    timestampToMillis(doc.getTimestamp("createdAt"))
            );
            user.firebaseUid = uid;
            user.setUserId(0L);

            GeneralUserEntity profile = new GeneralUserEntity(
                    doc.getString("profilePictureUrl"),
                    doc.getString("bio")
            );
            profile.userId = 0L;

            UserWithProfile uwp = new UserWithProfile();
            uwp.user = user;
            uwp.generalUser = profile;
            return uwp;
        } catch (Exception e) {
            Log.w(TAG, "Failed to map general user " + doc.getId(), e);
            return null;
        }
    }

    private List<UserWithProfile> mapBusinessUsers(QuerySnapshot snapshot) {
        List<UserWithProfile> result = new ArrayList<>();
        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            UserWithProfile mapped = mapBusinessUser(doc);
            if (mapped != null) {
                result.add(mapped);
            }
        }
        return result;
    }

    @androidx.annotation.Nullable
    private UserWithProfile mapBusinessUser(DocumentSnapshot doc) {
        try {
            String uid = doc.getId();
            String businessName = doc.getString("businessName");
            String email = doc.getString("businessEmail");
            if (businessName == null) return null;

            UserEntity user = new UserEntity(
                    businessName,
                    "",
                    email != null ? email : "",
                    UserType.BUSINESS,
                    Boolean.TRUE.equals(doc.getBoolean("emailVerified")),
                    timestampToMillis(doc.getTimestamp("createdAt"))
            );
            user.firebaseUid = uid;
            user.setUserId(0L);

            BusinessUserEntity profile = new BusinessUserEntity(
                    businessName,
                    doc.getString("phone"),
                    doc.getString("address"),
                    doc.getString("description"),
                    firstOrNull(doc.get("imageUrls")),
                    doc.getString("bio"),
                    null,
                    RequestReportStatus.ACCEPTED,
                    System.currentTimeMillis()
            );
            profile.userId = 0L;

            UserWithProfile uwp = new UserWithProfile();
            uwp.user = user;
            uwp.businessUser = profile;
            return uwp;
        } catch (Exception e) {
            Log.w(TAG, "Failed to map business user " + doc.getId(), e);
            return null;
        }
    }

    private static String firstOrNull(Object field) {
        if (field instanceof List) {
            List<?> list = (List<?>) field;
            if (!list.isEmpty() && list.get(0) != null) {
                return list.get(0).toString();
            }
        }
        return null;
    }

    private static long timestampToMillis(Timestamp ts) {
        return ts != null ? ts.toDate().getTime() : System.currentTimeMillis();
    }

    public LiveData<List<UserEntity>> getUsersByFirebaseUids(@androidx.annotation.Nullable List<String> uids) {
        MutableLiveData<List<UserEntity>> results = new MutableLiveData<>();
        if (uids == null || uids.isEmpty()) {
            results.setValue(new ArrayList<>());
            return results;
        }

        List<List<String>> chunks = new ArrayList<>();
        for (int i = 0; i < uids.size(); i += 30) {
            chunks.add(new ArrayList<>(uids.subList(i, Math.min(i + 30, uids.size()))));
        }

        List<UserEntity> accumulator = new ArrayList<>();
        java.util.Set<String> seenUids = new java.util.HashSet<>();
        int[] pending = {chunks.size() * 2};

        Runnable maybeEmit = () -> {
            if (pending[0] == 0) {
                results.postValue(accumulator);
            }
        };

        for (List<String> chunk : chunks) {
            firestore.collection(COLLECTION_USERS)
                    .whereIn(FieldPath.documentId(), chunk)
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        for (UserWithProfile uwp : mapGeneralUsers(snapshot)) {
                            if (uwp != null && uwp.user != null
                                    && uwp.user.firebaseUid != null
                                    && seenUids.add(uwp.user.firebaseUid)) {
                                accumulator.add(uwp.user);
                            }
                        }
                        pending[0]--;
                        maybeEmit.run();
                    })
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "General user batch lookup failed", e);
                        pending[0]--;
                        maybeEmit.run();
                    });

            firestore.collection(COLLECTION_BUSINESSES)
                    .whereIn(FieldPath.documentId(), chunk)
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        for (UserWithProfile uwp : mapBusinessUsers(snapshot)) {
                            if (uwp != null && uwp.user != null
                                    && uwp.user.firebaseUid != null
                                    && seenUids.add(uwp.user.firebaseUid)) {
                                accumulator.add(uwp.user);
                            }
                        }
                        pending[0]--;
                        maybeEmit.run();
                    })
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "Business user batch lookup failed", e);
                        pending[0]--;
                        maybeEmit.run();
                    });
        }

        return results;
    }

    public LiveData<UserWithProfile> getUserByFirebaseUid(@NonNull String firebaseUid) {
        MutableLiveData<UserWithProfile> result = new MutableLiveData<>();

        firestore.collection(COLLECTION_USERS)
                .document(firebaseUid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc != null && doc.exists()) {
                        UserWithProfile mapped = mapGeneralUser(doc);
                        if (mapped != null) {
                            result.postValue(mapped);
                            return;
                        }
                    }
                    fetchBusinessByUid(firebaseUid, result);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "User lookup by firebaseUid failed; trying businesses", e);
                    fetchBusinessByUid(firebaseUid, result);
                });

        return result;
    }

    private void fetchBusinessByUid(@NonNull String firebaseUid,
                                    MutableLiveData<UserWithProfile> result) {
        firestore.collection(COLLECTION_BUSINESSES)
                .document(firebaseUid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc == null || !doc.exists()) {
                        result.postValue(null);
                        return;
                    }
                    UserWithProfile mapped = mapBusinessUser(doc);
                    result.postValue(mapped);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Business lookup by firebaseUid failed", e);
                    result.postValue(null);
                });
    }

    public void seedUsersIfEmpty() {
        executorService.execute(() -> {
            if (userDao.getUserCount() == 0) {
                // 1. Create a General User
                UserEntity generalUser = new UserEntity(
                        "NatureLover",
                        "hashed_password",
                        "nature@example.com",
                        UserType.GENERAL,
                        true,
                        System.currentTimeMillis()
                );
                long genId = userDao.insert(generalUser);
                GeneralUserEntity genProfile = new GeneralUserEntity(
                        "https://images.unsplash.com/photo-1503023345310-bd7c1de61c7d",
                        "Passionate about the outdoors and Barbados' natural beauty! 🌴🌊"
                );
                genProfile.userId = genId;
                userDao.insertGeneralUser(genProfile);

                // 2. Create a Business User
                UserEntity businessUser = new UserEntity(
                        "Chefette",
                        "hashed_password",
                        "info@chefette.com",
                        UserType.BUSINESS,
                        true,
                        System.currentTimeMillis()
                );
                long bizId = userDao.insert(businessUser);

                BusinessUserEntity bizProfile = new BusinessUserEntity(
                        "Chefette Restaurant",
                        "+1(246) 430-3339",
                        "Broad Street, Bridgetown, Barbados",
                        "The legendary local favorite. Home of the famous Broasted Chicken and Roti!",
                        "https://www.chefette.com/images/chefette-logo.png",
                        "Quality Food, Quick Service, Great Value!",
                        null, // businessCertificates
                        RequestReportStatus.ACCEPTED,
                        System.currentTimeMillis()
                );
                bizProfile.userId = bizId;
                userDao.insertBusinessUser(bizProfile);
            }
        });
    }
}
