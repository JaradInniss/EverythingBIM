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
//
// Two data sources are supported:
//  - Local Room (default) - used for everything that has already been
//    cached, including offline reads.
//  - Firestore - used for user search (see {@link #searchUsersInFirestore}),
//    because the user might not be in the local Room cache yet (e.g. they
//    registered on another device or haven't been written to Room since
//    signing in).
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

    /**
     * Local (Room) search - kept for backwards compatibility. Only finds users
     * that have already been inserted into the local cache.
     */
    public LiveData<List<UserWithProfile>> searchUsers(String query) {
        return userDao.searchUsers(query);
    }

    /**
     * Firestore-backed username search. Queries both the {@code users} and
     * {@code businesses} collections for documents whose {@code username} (or
     * {@code businessName} for businesses) starts with the given query string.
     *
     * <p>The search is case-sensitive because Firestore range queries are
     * case-sensitive. To get a more lenient search you can store a
     * lowercased shadow field (e.g. {@code usernameLower}) on each document
     * and query that instead.</p>
     *
     * <p>The result is returned as a {@link LiveData} that emits the combined,
     * de-duplicated list of matches. On error the LiveData emits an empty
     * list and the error is logged.</p>
     */
    public LiveData<List<UserWithProfile>> searchUsersInFirestore(String query) {
        MutableLiveData<List<UserWithProfile>> results = new MutableLiveData<>();
        if (query == null || query.trim().isEmpty()) {
            results.setValue(new ArrayList<>());
            return results;
        }

        // Firestore prefix-match pattern: x >= 'query' AND x <= 'query\uf8ff'
        // ('\uf8ff' is a private-use Unicode codepoint that sorts after all
        // regular characters, so it acts as a sentinel for "any continuation".)
        String upperBound = query + "\uf8ff";

        // Kick off both queries in parallel. Each one updates a local
        // accumulator; once both have arrived we merge the results and post.
        List<UserWithProfile> accumulator = new ArrayList<>();
        final boolean[] generalDone = {false};
        final boolean[] businessDone = {false};

        Runnable maybeEmit = () -> {
            if (generalDone[0] && businessDone[0]) {
                // De-duplicate (a user could theoretically be in both
                // collections if data was mis-migrated).
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

        // Business documents use `businessName` instead of `username`.
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

    /**
     * Maps Firestore general-user documents to {@link UserWithProfile}
     * instances. Username/email/userType are read from the same document
     * (the user profile fields like bio/profilePictureUrl live alongside
     * them in the {@code users} collection, not in a separate profile
     * subdocument).
     */
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

    /**
     * Maps a single general-user document to a {@link UserWithProfile}, or
     * returns {@code null} if the document doesn't carry the required fields.
     * Shared by the list-based and single-document lookup paths.
     */
    @androidx.annotation.Nullable
    private UserWithProfile mapGeneralUser(DocumentSnapshot doc) {
        try {
            String uid = doc.getId();
            String username = doc.getString("username");
            String email = doc.getString("email");
            if (username == null) return null;

            UserEntity user = new UserEntity(
                    username,
                    "",                                          // password not stored here
                    email != null ? email : "",
                    UserType.GENERAL,
                    Boolean.TRUE.equals(doc.getBoolean("emailVerified")),
                    timestampToMillis(doc.getTimestamp("createdAt"))
            );
            user.firebaseUid = uid;
            // Room's autoGenerate userId is irrelevant for a Firestore-
            // sourced user; leave it 0. The equals()/hashCode() we added
            // is on userId, but tag de-duplication uses
            // firebaseUid, which is unique, so collisions are not a
            // concern.
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

    /**
     * Maps Firestore business-user documents to {@link UserWithProfile}
     * instances. Business documents use {@code businessName} for display
     * (and search) and {@code businessEmail} for email.
     */
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

    /**
     * Maps a single business-user document to a {@link UserWithProfile}, or
     * returns {@code null} if the document doesn't carry the required fields.
     * Shared by the list-based and single-document lookup paths.
     */
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

    /**
     * Firestore-backed lookup of multiple users by their Firebase Auth UIDs.
     *
     * <p>The {@code users} and {@code businesses} collections both use the
     * Firebase UID as the document id, so we query each collection with
     * {@code whereIn(FieldPath.documentId(), uids)}. Results are merged,
     * de-duplicated by {@code firebaseUid}, and mapped to {@link UserEntity}
     * using the same helpers as {@link #searchUsersInFirestore(String)} -
     * i.e. each emitted entity has its {@code firebaseUid} populated and the
     * local Room {@code userId} left at {@code 0L}.</p>
     *
     * <p>If {@code uids} is null or empty the LiveData emits an empty list
     * immediately. On error the LiveData emits an empty list and the error
     * is logged.</p>
     */
    public LiveData<List<UserEntity>> getUsersByFirebaseUids(@androidx.annotation.Nullable List<String> uids) {
        MutableLiveData<List<UserEntity>> results = new MutableLiveData<>();
        if (uids == null || uids.isEmpty()) {
            results.setValue(new ArrayList<>());
            return results;
        }

        // Firestore `whereIn` accepts at most 30 values per query. We split
        // larger batches into chunks of 30 to be safe, but in practice tagged
        // lists are short.
        List<List<String>> chunks = new ArrayList<>();
        for (int i = 0; i < uids.size(); i += 30) {
            chunks.add(new ArrayList<>(uids.subList(i, Math.min(i + 30, uids.size()))));
        }

        // Accumulator shared by every chunk's success callback. We use a
        // List + a Set<firebaseUid> to keep the order of the first sighting
        // while still de-duplicating across chunks / collections.
        List<UserEntity> accumulator = new ArrayList<>();
        java.util.Set<String> seenUids = new java.util.HashSet<>();
        int[] pending = {chunks.size() * 2}; // 2 collections per chunk

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

    /**
     * Firestore-backed lookup of a single user by their Firebase Auth UID.
     *
     * <p>Used when navigating to a user profile from a context where we only
     * have a Firebase UID (e.g. a tagged-user chip on the View Post page)
     * and the user is not in the local Room cache. The result is wrapped in
     * a {@link UserWithProfile} so it can be consumed by
     * {@link com.example.everythingbim.ui.posts.ViewUserProfileViewModel}
     * without further adaptation.</p>
     *
     * <p>On error or if the document does not exist the LiveData emits
     * {@code null}.</p>
     */
    public LiveData<UserWithProfile> getUserByFirebaseUid(@NonNull String firebaseUid) {
        MutableLiveData<UserWithProfile> result = new MutableLiveData<>();

        // Try the general users collection first; if it doesn't exist, fall
        // back to the businesses collection. The two collections are
        // mutually exclusive in practice.
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
                    // Fall through to businesses.
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
