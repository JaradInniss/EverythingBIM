package com.example.everythingbim.data.repository;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.everythingbim.data.local.AppDatabase;
import com.example.everythingbim.data.local.dao.CommentDao;
import com.example.everythingbim.data.local.dao.LikeDao;
import com.example.everythingbim.data.local.dao.LocationDao;
import com.example.everythingbim.data.local.dao.PostDao;
import com.example.everythingbim.data.local.dao.ReportDao;
import com.example.everythingbim.data.local.entities.CommentEntity;
import com.example.everythingbim.data.local.entities.LikeEntity;
import com.example.everythingbim.data.local.entities.LocationEntity;
import com.example.everythingbim.data.local.entities.PostEntity;
import com.example.everythingbim.data.local.entities.ReportEntity;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Repository for managing {@link PostEntity} objects, their comments, and
 * their likes.
 *
 * Posts live in two places: locally in Room (for offline cache) and remotely in
 * Firestore (for cross-device sync). Firestore is the source of truth - the local
 * Room cache is updated by listening to the {@code posts} collection and reflecting
 * any changes there.
 *
 * <p>Comments are stored as a {@code posts/{postId}/comments/{commentId}}
 * subcollection and mirrored into the local {@code comments} Room table.</p>
 *
 * <p>Likes are stored in a flat {@code likes} collection (one doc per like,
 * keyed by a deterministic {@code like_{postId}_{userUid}} id) and a
 * denormalized counter ({@code posts/{postId}.likeCount}) is maintained
 * inside a Firestore transaction on every like/unlike. The local Room
 * {@code likes} table mirrors Firestore for offline reads.</p>
 */
public class PostRepository {
    private static final String TAG = "PostRepository";
    private static final String COLLECTION_POSTS = "posts";
    private static final String COLLECTION_COMMENTS = "comments";
    private static final String COLLECTION_LIKES = "likes";
    private static final String TARGET_TYPE_POST = "POST";
    private static final String LOCAL_HARRISONS_CAVE_IMAGE = "harrisons_cave.jpg";
    private static final String LOCAL_BATHSHEBA_IMAGE = "bathsheba.jpg";
    private static final String LEGACY_BATHSHEBA_LOCATION_IMAGE = "bathsheba_beach";
    private static final String LEGACY_HARRISONS_CAVE_IMAGE = "https://upload.wikimedia.org/wikipedia/commons/b/b5/Harrison%27s_Cave_Barbados_2.jpg";
    private static final String LEGACY_BATHSHEBA_IMAGE = "https://upload.wikimedia.org/wikipedia/commons/9/90/Bathsheba_Barbados.jpg";

    private final PostDao postDao;
    private final CommentDao commentDao;
    private final LikeDao likeDao;
    private final LocationDao locationDao;
    private final ReportDao reportDao;
    private final ExecutorService executorService;
    private final FirebaseFirestore firestore;

    public PostRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        postDao = db.postDao();
        commentDao = db.commentDao();
        likeDao = db.likeDao();
        locationDao = db.locationDao();
        reportDao = db.reportDao();
        executorService = Executors.newFixedThreadPool(2);
        firestore = FirebaseFirestore.getInstance();
    }

    /**
     * Streams the {@code posts} collection from Firestore, mapping each
     * document into a {@link PostEntity} (with the document id set on
     * {@code firestoreId}) and emitting the result through a {@link LiveData}.
     * The Room cache is also refreshed so that other queries (e.g.
     * {@link #getPostById(long)}) can return the data when offline.
     */
    public LiveData<List<PostEntity>> getRandomizedPosts() {
        MutableLiveData<List<PostEntity>> liveData = new MutableLiveData<>();
        firestore.collection(COLLECTION_POSTS)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.w(TAG, "Listen failed for posts collection", error);
                        // Fall back to whatever is already in Room. This call
                        // touches the DB so it MUST run on a background
                        // thread - Room refuses synchronous calls on the main
                        // thread (see https://issuetracker.google.com/issues/37130652).
                        executorService.execute(() -> {
                            List<PostEntity> cached = postDao.getAllPostsSync();
                            liveData.postValue(cached);
                        });
                        return;
                    }
                    if (value == null) {
                        liveData.postValue(new ArrayList<>());
                        return;
                    }

                    List<PostEntity> posts = new ArrayList<>();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        PostEntity post = mapPostFromFirestore(doc);
                        if (post == null) continue;
                        queuePortableLocationBackfill(post, doc.getReference());
                        posts.add(post);
                    }

                    // Persist to Room in the background for offline access.
                    executorService.execute(() -> mirrorPostsIntoRoom(posts));

                    liveData.postValue(posts);
                });
        return liveData;
    }

    /**
     * Mirrors a list of posts into the local Room cache, ensuring each
     * post's referenced {@code LocationEntity} exists first so the
     * foreign-key constraint on {@code posts.locationId} is satisfied.
     * If a post's location is not in the cache, a minimal placeholder
     * is created (the cached version of the location will be replaced
     * later by the proper Firestore mirror once the locations collection
     * is streamed).
     *
     * <p>This is the source of truth for the offline read path; the
     * previous implementation tried to insert posts directly and silently
     * dropped them whenever the location wasn't cached yet, leaving
     * {@link #getPostById(long)} returning {@code null} for any post
     * shown in the home feed.</p>
     */
    private void mirrorPostsIntoRoom(List<PostEntity> posts) {
        for (PostEntity post : posts) {
            try {
                // ensureLocationExists first so the FK on
                // posts.locationId -> locations.locationId is satisfied
                // even when this is the first time we're seeing this
                // location (e.g. a brand-new post referencing a
                // not-yet-cached location).
                ensureLocationExists(
                        post.locationId,
                        post.locationName,
                        post.locationLatitude,
                        post.locationLongitude,
                        post.locationAddress
                );
                // Use upsert (not insert) so a re-snapshot of the same
                // post doesn't fail with UNIQUE constraint. The previous
                // INSERT-OR-REPLACE could fail on the DELETE step of the
                // REPLACE when comments/likes still referenced the post,
                // and that DELETE-then-fail could leave orphan rows.
                postDao.upsert(post);
            } catch (Exception e) {
                Log.w(TAG, "Failed to cache post in Room", e);
            }
        }
    }

    /**
     * Inserts a placeholder {@link LocationEntity} into Room for the
     * given id if one doesn't already exist. No-op when the location is
     * already cached. The placeholder carries the id, the display name
     * (if known), and zeroed coordinates so the foreign key is satisfied
     * and the row can be replaced later with the real one.
     */
    private void ensureLocationExists(long locationId,
                                      @androidx.annotation.Nullable String fallbackName,
                                      @androidx.annotation.Nullable Double fallbackLatitude,
                                      @androidx.annotation.Nullable Double fallbackLongitude,
                                      @androidx.annotation.Nullable String fallbackAddress) {
        if (locationId <= 0L) return;
        if (locationDao.getLocationByIdSync(locationId) != null) return;
        LocationEntity placeholder = new LocationEntity(
                fallbackName != null && !fallbackName.isEmpty() ? fallbackName : "Unknown location",
                fallbackLatitude != null ? fallbackLatitude : 0.0,
                fallbackLongitude != null ? fallbackLongitude : 0.0,
                0f,
                false,
                "firestore-mirror",
                "",
                "",
                "",
                fallbackAddress != null ? fallbackAddress : ""
        );
        // Pre-set the id so it matches the post's locationId reference.
        placeholder.setLocationId(locationId);
        try {
            locationDao.insert(placeholder);
        } catch (Exception e) {
            // Best-effort: if even the placeholder insert fails (e.g.
            // another thread inserted in between), the post insert
            // below will throw the original FK error and be logged.
            Log.w(TAG, "Failed to insert placeholder location " + locationId, e);
        }
    }

    /**
     * Streams all posts authored by the given Firebase UID. Falls back to the
     * Room cache when Firestore is unreachable.
     */
    public LiveData<List<PostEntity>> getPostsByAuthorUid(@NonNull String authorUid) {
        MutableLiveData<List<PostEntity>> liveData = new MutableLiveData<>();
        firestore.collection(COLLECTION_POSTS)
                .whereEqualTo("authorUid", authorUid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.w(TAG, "Listen failed for posts by authorUid=" + authorUid, error);
                        // Touching the Room cache must happen on a background
                        // thread - same reason as getRandomizedPosts().
                        executorService.execute(() -> {
                            List<PostEntity> cached = postDao.getPostsByAuthorUidSync(authorUid);
                            liveData.postValue(cached);
                        });
                        return;
                    }
                    if (value == null) {
                        liveData.postValue(new ArrayList<>());
                        return;
                    }

                    List<PostEntity> posts = new ArrayList<>();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        PostEntity post = mapPostFromFirestore(doc);
                        if (post == null) continue;
                        queuePortableLocationBackfill(post, doc.getReference());
                        posts.add(post);
                    }
                    // Mirror into Room so offline reads (e.g. ViewPost's
                    // getPostById) can find these posts later. Same FK
                    // guard as getRandomizedPosts.
                    executorService.execute(() -> mirrorPostsIntoRoom(posts));
                    liveData.postValue(posts);
                });
        return liveData;
    }

    /**
     * Convenience overload that keeps backwards compatibility with callers that
     * still query by local Room user id.
     */
    public LiveData<List<PostEntity>> getPostsByUserId(long userId) {
        return postDao.getPostsByUserId(userId);
    }

    /**
     * Persists a new post to Firestore. On success, the generated document id is
     * written back to the entity ({@code firestoreId}) and the row is mirrored in
     * Room for offline reads.
     *
     * @return a {@link LiveData} that emits the persisted post (with its Firestore
     *         id) once the write completes, or {@code null} on failure.
     */
    public LiveData<PostEntity> createPostInFirestore(PostEntity post) {
        MutableLiveData<PostEntity> result = new MutableLiveData<>();
        Map<String, Object> doc = postToFirestoreMap(post);
        firestore.collection(COLLECTION_POSTS)
                .add(doc)
                .addOnSuccessListener(ref -> {
                    post.firestoreId = ref.getId();
                    post.postId = stableLongFromString(ref.getId());
                    executorService.execute(() -> {
                        try {
                            // Upsert instead of plain insert so re-runs of this
                            // method (e.g. on a retry) don't fail with a
                            // UNIQUE constraint violation, and so the FK to
                            // locations is satisfied via ensureLocationExists
                            // before the row is written.
                            ensureLocationExists(
                                    post.locationId,
                                    post.locationName,
                                    post.locationLatitude,
                                    post.locationLongitude,
                                    post.locationAddress
                            );
                            postDao.upsert(post);
                        } catch (Exception e) {
                            Log.w(TAG, "Failed to cache new post in Room", e);
                        }
                    });
                    result.postValue(post);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to create post in Firestore", e);
                    result.postValue(null);
                });
        return result;
    }

    /**
     * One-shot callback for {@link #createPostInFirestoreAsync(PostEntity, OnPostCreatedListener)}.
     * Exactly one of {@code persisted} and {@code error} is non-null on each
     * invocation: a successful write yields a non-null {@code persisted} and a
     * null {@code error}; a failed write yields a non-null {@code error}.
     */
    public interface OnPostCreatedListener {
        void onResult(@androidx.annotation.Nullable PostEntity persisted,
                      @androidx.annotation.Nullable Throwable error);
    }

    /**
     * Variant of {@link #createPostInFirestore(PostEntity)} that delivers the
     * result via a callback rather than a {@link LiveData}. This is the
     * preferred API for one-shot writes because it doesn't require the caller
     * to manage the observer lifecycle, and the callback fires on a
     * Firestore worker thread (callers must use {@code postValue} on any
     * LiveData they touch from inside the callback).
     */
    public void createPostInFirestoreAsync(@NonNull PostEntity post,
                                            @NonNull OnPostCreatedListener listener) {
        Map<String, Object> doc = postToFirestoreMap(post);
        firestore.collection(COLLECTION_POSTS)
                .add(doc)
                .addOnSuccessListener(ref -> {
                    post.firestoreId = ref.getId();
                    post.postId = stableLongFromString(ref.getId());
                    executorService.execute(() -> {
                        try {
                            // ensureLocationExists guards the FK on
                            // posts.locationId -> locations.locationId before
                            // the post is mirrored into Room. Without this,
                            // a brand-new post whose location hasn't been
                            // streamed into the cache yet would fail with
                            // "FOREIGN KEY constraint failed" the first
                            // time the createPost callback runs.
                            ensureLocationExists(
                                    post.locationId,
                                    post.locationName,
                                    post.locationLatitude,
                                    post.locationLongitude,
                                    post.locationAddress
                            );
                            postDao.upsert(post);
                        } catch (Exception e) {
                            Log.w(TAG, "Failed to cache new post in Room", e);
                        }
                    });
                    listener.onResult(post, null);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to create post in Firestore", e);
                    listener.onResult(null, e);
                });
    }

    /**
     * Builds a map of fields to write to Firestore. We use a {@code Map} rather
     * than writing the entity directly so that the {@code @Exclude}-annotated
     * fields (e.g. {@code firestoreId}, {@code postId}) are never persisted.
     */
    private Map<String, Object> postToFirestoreMap(PostEntity post) {
        Map<String, Object> map = new HashMap<>();
        map.put("authorUid", post.authorUid);
        map.put("authorId", post.authorId);
        map.put("authorName", post.authorName);
        map.put("caption", post.caption);
        map.put("imageUrl", post.imageUrl);
        map.put("createdAt", post.createdAt);
        map.put("locationId", post.locationId);
        map.put("locationName", post.locationName);
        map.put("locationLatitude", post.locationLatitude);
        map.put("locationLongitude", post.locationLongitude);
        map.put("locationAddress", post.locationAddress);
        map.put("locationPlaceId", post.locationPlaceId);
        map.put("locationFirestoreId", post.locationFirestoreId);
        map.put("taggedUserUids", post.taggedUserUids);
        // Initialise the denormalized like counter so the post has a
        // starting value of 0; subsequent increments/decrements are
        // handled transactionally in toggleLike().
        map.put("likeCount", post.likeCount != null ? post.likeCount : 0);
        return map;
    }

    private void queuePortableLocationBackfill(@NonNull PostEntity post,
                                               @NonNull com.google.firebase.firestore.DocumentReference postRef) {
        executorService.execute(() -> backfillPortableLocationFieldsIfMissing(post, postRef));
    }

    private void backfillPortableLocationFieldsIfMissing(@NonNull PostEntity post,
                                                         @NonNull com.google.firebase.firestore.DocumentReference postRef) {
        if (hasPortableCoordinates(post) && hasText(post.locationAddress)) {
            return;
        }

        LocationEntity candidate = null;
        if (post.locationId > 0L) {
            candidate = locationDao.getLocationByIdSync(post.locationId);
        }
        if (!hasValidCoordinates(candidate) && hasText(post.locationName)) {
            candidate = locationDao.getResolvedLocationByNameSync(post.locationName.trim());
        }
        if (!hasValidCoordinates(candidate) && hasText(post.locationName)) {
            candidate = locationDao.getResolvedLocationByNameLooseSync(post.locationName.trim());
        }
        if (!hasValidCoordinates(candidate)) {
            return;
        }

        boolean changed = false;
        if (!hasPortableCoordinates(post)) {
            post.locationLatitude = candidate.latitude;
            post.locationLongitude = candidate.longitude;
            changed = true;
        }
        if (!hasText(post.locationAddress) && hasText(candidate.address)) {
            post.locationAddress = candidate.address;
            changed = true;
        }
        if (!hasText(post.locationName) && hasText(candidate.name)) {
            post.locationName = candidate.name;
            changed = true;
        }
        if (!changed) {
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        if (post.locationLatitude != null) {
            updates.put("locationLatitude", post.locationLatitude);
        }
        if (post.locationLongitude != null) {
            updates.put("locationLongitude", post.locationLongitude);
        }
        if (hasText(post.locationAddress)) {
            updates.put("locationAddress", post.locationAddress);
        }
        if (hasText(post.locationName)) {
            updates.put("locationName", post.locationName);
        }
        if (updates.isEmpty()) {
            return;
        }

        postRef.update(updates)
                .addOnFailureListener(error -> Log.w(TAG,
                        "Failed to backfill portable location fields for post " + postRef.getId(),
                        error));
    }

    @androidx.annotation.Nullable
    private PostEntity mapPostFromFirestore(@NonNull DocumentSnapshot doc) {
        try {
            Long locationId = doc.getLong("locationId");
            Long authorId = doc.getLong("authorId");
            String locationName = doc.getString("locationName");
            Double locationLatitude = getNullableDouble(doc.get("locationLatitude"));
            Double locationLongitude = getNullableDouble(doc.get("locationLongitude"));
            String locationAddress = doc.getString("locationAddress");
            String locationPlaceId = doc.getString("locationPlaceId");
            String locationFirestoreId = doc.getString("locationFirestoreId");
            String authorName = doc.getString("authorName");
            String authorUid = doc.getString("authorUid");
            String caption = doc.getString("caption");
            String imageUrl = doc.getString("imageUrl");
            long createdAt = timestampFieldToMillis(doc.get("createdAt"));

            @SuppressWarnings("unchecked")
            List<String> taggedUserUids = (List<String>) doc.get("taggedUserUids");

            PostEntity post = new PostEntity(
                    locationId != null ? locationId : 0L,
                    locationName,
                    authorId != null ? authorId : 0L,
                    authorName,
                    authorUid,
                    caption,
                    imageUrl,
                    createdAt,
                    taggedUserUids != null ? taggedUserUids : new ArrayList<>()
            );
            post.firestoreId = doc.getId();
            post.postId = stableLongFromString(doc.getId());
            post.setPortableLocationSnapshot(
                    locationLatitude,
                    locationLongitude,
                    locationAddress,
                    locationPlaceId,
                    locationFirestoreId
            );
            Long likeCount = doc.getLong("likeCount");
            post.likeCount = likeCount != null ? likeCount.intValue() : 0;
            Long commentCount = doc.getLong("commentCount");
            post.commentCount = commentCount != null ? commentCount.intValue() : null;
            return post;
        } catch (Exception e) {
            Log.w(TAG, "Failed to map Firestore post " + doc.getId(), e);
            return null;
        }
    }

    public void insert(PostEntity post) {
        // Use upsert so a re-insert of the same post (e.g. after a failed
        // network call is retried by the caller) doesn't throw a UNIQUE
        // constraint failure. The plain insert() exists for callers that
        // know the row is brand-new; in practice all current callers go
        // through createPostInFirestoreAsync, which uses upsert directly.
        executorService.execute(() -> postDao.upsert(post));
    }

    public LiveData<PostEntity> getPostById(long postId) {
        return postDao.getPostById(postId);
    }

    /**
     * Refreshes a single post from Firestore and updates Room cache.
     * Call this before viewing a post to ensure fresh data.
     */
    public void refreshPostFromFirestore(long postId, PostCallback callback) {
        // First get the post to find its firestoreId
        executorService.execute(() -> {
            PostEntity cachedPost = postDao.getPostByIdSync(postId);
            if (cachedPost == null || cachedPost.firestoreId == null) {
                callback.onComplete(null);
                return;
            }

            firestore.collection(COLLECTION_POSTS)
                    .document(cachedPost.firestoreId)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                            DocumentSnapshot doc = task.getResult();
                            PostEntity post = mapPostFromFirestore(doc);
                            if (post != null) {
                                executorService.execute(() -> {
                                    postDao.upsert(post);
                                    callback.onComplete(post);
                                });
                            } else {
                                callback.onComplete(null);
                            }
                        } else {
                            callback.onComplete(null);
                        }
                    });
        });
    }

    public interface PostCallback {
        void onComplete(PostEntity post);
    }

    public LiveData<LocationEntity> getLocationById(long locationId) {
        return locationDao.getLocationById(locationId);
    }

    public LiveData<LocationEntity> getResolvedLocationByName(@androidx.annotation.Nullable String locationName) {
        if (locationName == null || locationName.trim().isEmpty()) {
            androidx.lifecycle.MutableLiveData<LocationEntity> liveData = new androidx.lifecycle.MutableLiveData<>();
            liveData.setValue(null);
            return liveData;
        }
        String normalizedName = locationName.trim();
        MediatorLiveData<LocationEntity> result = new MediatorLiveData<>();
        LiveData<LocationEntity> exact = locationDao.getResolvedLocationByName(normalizedName);
        LiveData<LocationEntity> loose = locationDao.getResolvedLocationByNameLoose(normalizedName);

        result.addSource(exact, location -> {
            if (hasValidCoordinates(location)) {
                result.setValue(location);
            } else {
                result.addSource(loose, looseLocation -> result.setValue(looseLocation));
            }
        });
        return result;
    }

    @androidx.annotation.Nullable
    private Double getNullableDouble(@androidx.annotation.Nullable Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return null;
    }

    private boolean hasPortableCoordinates(@NonNull PostEntity post) {
        return post.locationLatitude != null
                && post.locationLongitude != null
                && (post.locationLatitude != 0.0d || post.locationLongitude != 0.0d);
    }

    private boolean hasValidCoordinates(@androidx.annotation.Nullable LocationEntity location) {
        return location != null
                && (location.latitude != 0.0d || location.longitude != 0.0d);
    }

    private boolean hasText(@androidx.annotation.Nullable String value) {
        return value != null && !value.trim().isEmpty();
    }

    public LiveData<List<LocationEntity>> getAllLocations() {
        return locationDao.getAllLocations();
    }

    // ---------------------------------------------------------------------
    // Comments: Firestore subcollection + Room mirror
    // ---------------------------------------------------------------------

    /**
     * Streams all comments for a post from the
     * {@code posts/{postFirestoreId}/comments} Firestore subcollection.
     * Each snapshot is mirrored into the local {@code comments} Room table
     * (delete-then-insert for the post) so offline reads still work.
     *
     * <p>The returned entities have {@code firestoreId} and
     * {@code commentId} (a stable long derived from the firestoreId via
     * {@link #stableLongFromString}) populated so the existing
     * {@link CommentUIModel} hierarchy builder keeps working.</p>
     *
     * @param postFirestoreId Firestore document id of the post. Null or
     *                        empty results in an empty LiveData.
     */
    public LiveData<List<CommentEntity>> getCommentsForPost(@androidx.annotation.Nullable String postFirestoreId) {
        MutableLiveData<List<CommentEntity>> liveData = new MutableLiveData<>();
        if (postFirestoreId == null || postFirestoreId.isEmpty()) {
            liveData.setValue(new ArrayList<>());
            return liveData;
        }
        firestore.collection(COLLECTION_POSTS)
                .document(postFirestoreId)
                .collection(COLLECTION_COMMENTS)
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.w(TAG, "Listen failed for comments on post " + postFirestoreId, error);
                        // Fall back to the Room mirror so offline reads still work.
                        long localPostId = stableLongFromString(postFirestoreId);
                        executorService.execute(() -> {
                            List<CommentEntity> cached = commentDao.getCommentsForPost(localPostId).getValue();
                            liveData.postValue(cached != null ? cached : new ArrayList<>());
                        });
                        return;
                    }
                    if (value == null) {
                        liveData.postValue(new ArrayList<>());
                        return;
                    }
                    List<CommentEntity> comments = new ArrayList<>();
                    long localPostId = stableLongFromString(postFirestoreId);
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        CommentEntity comment = mapCommentFromFirestore(doc, localPostId);
                        if (comment == null) continue;
                        comments.add(comment);
                    }
                    // Mirror into Room. Touches the DB on a background thread.
                    executorService.execute(() -> {
                        try {
                            commentDao.deleteCommentsForPost(localPostId);
                            for (CommentEntity comment : comments) {
                                commentDao.insert(comment);
                            }
                        } catch (Exception e) {
                            Log.w(TAG, "Failed to mirror comments into Room", e);
                        }
                    });
                    liveData.postValue(comments);
                });
        return liveData;
    }

    /**
     * Persists a new comment to Firestore. On success, the generated
     * document id is written back to the entity ({@code firestoreId}) and
     * the row is mirrored in Room for offline reads.
     *
     * @return a {@link LiveData} that emits the persisted comment (with
     *         its firestoreId) once the write completes, or {@code null}
     *         on failure.
     */
    public LiveData<CommentEntity> addCommentToFirestore(@NonNull String postFirestoreId,
                                                         @NonNull CommentEntity comment) {
        MutableLiveData<CommentEntity> result = new MutableLiveData<>();
        Map<String, Object> doc = commentToFirestoreMap(comment);
        firestore.collection(COLLECTION_POSTS)
                .document(postFirestoreId)
                .collection(COLLECTION_COMMENTS)
                .add(doc)
                .addOnSuccessListener(ref -> {
                    comment.firestoreId = ref.getId();
                    comment.commentId = stableLongFromString(ref.getId());
                    executorService.execute(() -> {
                        try {
                            commentDao.insert(comment);
                        } catch (Exception e) {
                            Log.w(TAG, "Failed to mirror new comment into Room", e);
                        }
                    });
                    result.postValue(comment);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to create comment in Firestore", e);
                    result.postValue(null);
                });
        return result;
    }

    /**
     * Updates an existing comment in the local Room database.
     * Used to refresh author names after resolving from Firestore.
     */
    public void updateComment(CommentEntity comment) {
        executorService.execute(() -> {
            try {
                commentDao.update(comment);
            } catch (Exception e) {
                Log.w(TAG, "Failed to update comment in Room", e);
            }
        });
    }

    private CommentEntity mapCommentFromFirestore(@NonNull DocumentSnapshot doc, long fallbackPostId) {
        CommentEntity comment = new CommentEntity();
        comment.firestoreId = doc.getId();
        // Re-derive the local commentId so re-snapshots are idempotent.
        comment.commentId = stableLongFromString(doc.getId());

        Long postId = doc.getLong("postId");
        comment.postId = postId != null ? postId : fallbackPostId;
        comment.parentCommentId = doc.getLong("parentCommentId");
        comment.authorUid = doc.getString("authorUid");
        comment.authorName = doc.getString("authorName");
        comment.parentAuthorName = doc.getString("parentAuthorName");
        comment.parentAuthorUid = doc.getString("parentAuthorUid");
        comment.body = doc.getString("body");
        comment.createdAt = timestampFieldToMillis(doc.get("createdAt"));
        return comment;
    }

    private Map<String, Object> commentToFirestoreMap(CommentEntity comment) {
        Map<String, Object> map = new HashMap<>();
        // postId and parentCommentId are stored as their Room long form
        // so a future Firestore->Room import stays consistent.
        map.put("postId", comment.postId);
        map.put("parentCommentId", comment.parentCommentId);
        map.put("authorUid", comment.authorUid);
        map.put("authorName", comment.authorName);
        map.put("parentAuthorName", comment.parentAuthorName);
        map.put("parentAuthorUid", comment.parentAuthorUid);
        map.put("body", comment.body);
        // ServerTimestamp so ordering is monotonic across clients.
        map.put("createdAt", FieldValue.serverTimestamp());
        return map;
    }

    private long timestampFieldToMillis(Object value) {
        if (value instanceof Timestamp) {
            return ((Timestamp) value).toDate().getTime();
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof java.util.Date) {
            return ((java.util.Date) value).getTime();
        }
        return System.currentTimeMillis();
    }

    // ---------------------------------------------------------------------
    // Likes: Firestore collection + denormalized counter + Room mirror
    // ---------------------------------------------------------------------

    /**
     * Streams whether the given user has liked the given post. Backed
     * by a single-document listener on
     * {@code likes/like_{postLocalId}_{userUid}} (deterministic id so we
     * can avoid a query). Emits {@code true} when the document exists,
     * {@code false} otherwise.
     */
    public LiveData<Boolean> isLikedByCurrentUser(@NonNull String postFirestoreId,
                                                   long postLocalId,
                                                   @NonNull String userUid) {
        MutableLiveData<Boolean> liveData = new MutableLiveData<>();
        String likeId = likeDocumentId(postLocalId, userUid);
        firestore.collection(COLLECTION_LIKES)
                .document(likeId)
                .addSnapshotListener((doc, error) -> {
                    if (error != null) {
                        Log.w(TAG, "Listen failed for like " + likeId, error);
                        // Fall back to the Room cache.
                        executorService.execute(() -> {
                            LikeEntity cached = likeDao.getLikeForPostAndUser(postLocalId, userUid).getValue();
                            liveData.postValue(cached != null);
                        });
                        return;
                    }
                    boolean exists = doc != null && doc.exists();
                    if (exists) {
                        // Mirror to Room.
                        LikeEntity like = new LikeEntity(userUid, postFirestoreId, TARGET_TYPE_POST,
                                doc.getTimestamp("likedAt") != null
                                        ? doc.getTimestamp("likedAt").toDate().getTime()
                                        : System.currentTimeMillis());
                        like.setPostId(postLocalId);
                        like.setFirestoreId(likeId);
                        executorService.execute(() -> {
                            try {
                                likeDao.upsert(like);
                            } catch (Exception e) {
                                Log.w(TAG, "Failed to mirror like into Room", e);
                            }
                        });
                    } else {
                        executorService.execute(() -> {
                            try {
                                likeDao.deleteByPostAndUser(postLocalId, userUid);
                            } catch (Exception e) {
                                Log.w(TAG, "Failed to remove like from Room", e);
                            }
                        });
                    }
                    liveData.postValue(exists);
                });
        return liveData;
    }

    /**
     * Toggles the like state for the given user on the given post. Runs
     * a Firestore transaction that:
     * <ol>
     *   <li>Reads the current like document (existence only).</li>
     *   <li>Reads the current {@code likeCount} on the post.</li>
     *   <li>If liked: deletes the like doc and decrements the counter.</li>
     *   <li>If not liked: writes the like doc and increments the counter.</li>
     * </ol>
     * The whole sequence is atomic, so the counter can never drift from
     * the actual like rows.
     *
     * @return a {@link LiveData} that emits the new liked state
     *         ({@code true} = liked, {@code false} = unliked) once the
     *         transaction completes, or {@code null} on failure.
     */
    public LiveData<Boolean> toggleLike(@NonNull String postFirestoreId,
                                        long postLocalId,
                                        @NonNull String userUid) {
        MutableLiveData<Boolean> result = new MutableLiveData<>();
        String likeId = likeDocumentId(postLocalId, userUid);
        com.google.firebase.firestore.DocumentReference postRef =
                firestore.collection(COLLECTION_POSTS).document(postFirestoreId);
        com.google.firebase.firestore.DocumentReference likeRef =
                firestore.collection(COLLECTION_LIKES).document(likeId);

        firestore.runTransaction(transaction -> {
            com.google.firebase.firestore.DocumentSnapshot likeDoc = transaction.get(likeRef);
            com.google.firebase.firestore.DocumentSnapshot postDoc = transaction.get(postRef);
            long currentCount = postDoc.getLong("likeCount") != null ? postDoc.getLong("likeCount") : 0L;
            boolean wasLiked = likeDoc.exists();
            if (wasLiked) {
                transaction.delete(likeRef);
                transaction.update(postRef, "likeCount", FieldValue.increment(-1));
                return false;
            } else {
                Map<String, Object> likeMap = new HashMap<>();
                likeMap.put("postId", postLocalId);
                likeMap.put("userUid", userUid);
                likeMap.put("likedAt", FieldValue.serverTimestamp());
                transaction.set(likeRef, likeMap);
                transaction.update(postRef, "likeCount", FieldValue.increment(1));
                return true;
            }
        }).addOnSuccessListener(nowLiked -> {
            // Mirror the change into Room. The post listener will
            // also fire and update PostEntity.likeCount.
            executorService.execute(() -> {
                try {
                    if (nowLiked) {
                        LikeEntity like = new LikeEntity(userUid, postFirestoreId, TARGET_TYPE_POST,
                                System.currentTimeMillis());
                        like.setPostId(postLocalId);
                        like.setFirestoreId(likeId);
                        likeDao.upsert(like);
                    } else {
                        likeDao.deleteByPostAndUser(postLocalId, userUid);
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Failed to mirror toggleLike into Room", e);
                }
            });
            result.postValue(nowLiked);
        }).addOnFailureListener(e -> {
            Log.e(TAG, "toggleLike transaction failed", e);
            result.postValue(null);
        });
        return result;
    }

    /**
     * Deterministic like document id so "is this post liked by this
     * user" is a single-document read instead of a query.
     */
    private String likeDocumentId(long postLocalId, String userUid) {
        return "like_" + postLocalId + "_" + userUid;
    }

    public void insertReport(ReportEntity report) {
        executorService.execute(() -> reportDao.insert(report));
    }

    /**
     * Stable mapping from a Firestore document id to a {@code long} suitable for
     * the Room primary key column. Uses the first 8 bytes of the SHA-256 digest
     * of the document id, which gives a uniform distribution with negligible
     * collision probability for the small datasets we deal with here.
     */
    private long stableLongFromString(String value) {
        if (value == null) return 0L;
        try {
            byte[] digest = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            long result = 0L;
            for (int i = 0; i < 8 && i < digest.length; i++) {
                result = (result << 8) | (digest[i] & 0xFFL);
            }
            return result;
        } catch (Exception e) {
            return value.hashCode();
        }
    }

    /**
     * Seed placeholder data if the database is empty. This is invoked once on
     * app startup to give a fresh install something to display during testing.
     */
    public void seedDataIfEmpty() {
        executorService.execute(() -> {
            if (postDao.getPostCount() == 0) {
                // Insert a sample location first
                long locId = locationDao.insert(new LocationEntity(
                        "Harrison's Cave",
                        13.1724,
                        -59.5755,
                        4.8f,
                        true,
                        "Admin",
                        "Popular Barbados cave attraction known for guided tram tours and underground formations.",
                        "nature",
                        "harrisons_cave",
                        "Harrison's Cave, Allen View, St. Thomas, Barbados"
                ));

                // Insert a sample post
                long postId = postDao.insert(new PostEntity(
                        locId,
                        "Harrison's Cave",
                        101L,
                        "TravelAddict",
                        "Explored the beautiful Harrison's Cave today! Nature is amazing. #Barbados #BIM",
                        LOCAL_HARRISONS_CAVE_IMAGE,
                        System.currentTimeMillis() - 86400000,
                        new ArrayList<>()
                ));
                // Top-level comments
                long c1 = commentDao.insert(new CommentEntity(postId, null, "TravelAddict", null, "This place looks incredible! Is it easy to get there?", System.currentTimeMillis() - 70000000));
                long c2 = commentDao.insert(new CommentEntity(postId, null, "LocalGuide", null, "Best time to visit is early morning to avoid the crowds.", System.currentTimeMillis() - 60000000));

                // Replies to c1
                commentDao.insert(new CommentEntity(postId, c1, "User123", "TravelAddict", "Yes, there are tour buses that go right there.", System.currentTimeMillis() - 50000000));
                commentDao.insert(new CommentEntity(postId, c1, "NatureLover", "TravelAddict", "Make sure to bring a light jacket, it gets cool inside!", System.currentTimeMillis() - 40000000));

                // Replies to c2
                commentDao.insert(new CommentEntity(postId, c2, "Adventurer", "LocalGuide", "Agreed! Also, the tram tour is totally worth it.", System.currentTimeMillis() - 30000000));

                // Another post for variety
                long locId2 = locationDao.insert(new LocationEntity(
                        "Bathsheba Beach",
                        13.2101,
                        -59.5218,
                        4.9f,
                        true,
                        "Admin",
                        "Scenic east-coast beach famous for its rock formations and surf culture.",
                        "beach",
                        "bathsheba",
                        "Bathsheba, St. Joseph, Barbados"
                ));
                postDao.insert(new PostEntity(
                        locId2,
                        "Bathsheba Beach",
                        102L,
                        "IslandExplorer",
                        "Sunset at Bathsheba. The rock formations are unlike anything else.",
                        LOCAL_BATHSHEBA_IMAGE,
                        System.currentTimeMillis() - 172800000,
                        new ArrayList<>()
                ));
            }

            repairSeededPostImages();
            repairSampleLocationImages();
        });
    }

    private void repairSeededPostImages() {
        for (PostEntity post : postDao.getAllPostsSync()) {
            String correctedImageRef = getCorrectedImageRef(post);
            if (correctedImageRef == null || correctedImageRef.equals(post.imageUrl)) {
                continue;
            }
            postDao.updateImageUrl(post.postId, correctedImageRef);
        }
    }

    private void repairSampleLocationImages() {
        for (LocationEntity location : locationDao.getAllLocationsSync()) {
            String correctedImageRef = getCorrectedLocationImageRef(location);
            if (correctedImageRef == null || correctedImageRef.equals(location.imageUrl)) {
                continue;
            }
            locationDao.updateImageUrl(location.locationId, correctedImageRef);
        }
    }

    private String getCorrectedImageRef(PostEntity post) {
        if (post == null) {
            return null;
        }

        if (LEGACY_HARRISONS_CAVE_IMAGE.equals(post.imageUrl) || containsIgnoreCase(post.caption, "Harrison's Cave")) {
            return LOCAL_HARRISONS_CAVE_IMAGE;
        }

        if (LEGACY_BATHSHEBA_IMAGE.equals(post.imageUrl) || containsIgnoreCase(post.caption, "Bathsheba")) {
            return LOCAL_BATHSHEBA_IMAGE;
        }

        return null;
    }

    private String getCorrectedLocationImageRef(LocationEntity location) {
        if (location == null) {
            return null;
        }

        if (containsIgnoreCase(location.name, "Harrison's Cave")) {
            return LOCAL_HARRISONS_CAVE_IMAGE;
        }

        if (containsIgnoreCase(location.name, "Bathsheba")) {
            if (LEGACY_BATHSHEBA_LOCATION_IMAGE.equals(location.imageUrl)
                    || location.imageUrl == null
                    || location.imageUrl.trim().isEmpty()
                    || !LOCAL_BATHSHEBA_IMAGE.equals(location.imageUrl)) {
                return LOCAL_BATHSHEBA_IMAGE;
            }
        }

        return null;
    }

    private boolean containsIgnoreCase(String text, String query) {
        return text != null
                && query != null
                && text.toLowerCase(java.util.Locale.US).contains(query.toLowerCase(java.util.Locale.US));
    }
}
