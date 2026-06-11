package com.example.everythingbim.data.repository;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PostRepository {
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

    public PostRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        postDao = db.postDao();
        commentDao = db.commentDao();
        likeDao = db.likeDao();
        locationDao = db.locationDao();
        reportDao = db.reportDao();
        executorService = Executors.newFixedThreadPool(2);
        firestore = FirebaseFirestore.getInstance();
    }

    public LiveData<List<PostEntity>> getRandomizedPosts() {
        MutableLiveData<List<PostEntity>> liveData = new MutableLiveData<>();
        firestore.collection("posts")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        executorService.execute(() -> liveData.postValue(postDao.getAllPostsSync()));
                        return;
                    }
                    if (value == null) {
                        liveData.postValue(new ArrayList<>());
                        return;
                    }

                    List<PostEntity> posts = new ArrayList<>();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        PostEntity post = mapPostFromFirestore(doc);
                        if (post == null) {
                            continue;
                        }
                        posts.add(post);
                    }

                    executorService.execute(() -> mirrorPostsIntoRoom(posts));
                    liveData.postValue(posts);
                });
        return liveData;
    }

    public void insert(PostEntity post) {
        executorService.execute(() -> postDao.upsert(post));
    }

    public LiveData<PostEntity> getPostById(long postId) {
        return postDao.getPostById(postId);
    }

    public LiveData<LocationEntity> getLocationById(long locationId) {
        return locationDao.getLocationById(locationId);
    }

    public LiveData<List<LocationEntity>> getAllLocations() {
        return locationDao.getAllLocations();
    }

    public LiveData<List<PostEntity>> getPostsByUserId(long userId) {
        return postDao.getPostsByUserId(userId);
    }

    public LiveData<List<CommentEntity>> getCommentsForPost(@Nullable String postFirestoreId) {
        MutableLiveData<List<CommentEntity>> liveData = new MutableLiveData<>();
        if (postFirestoreId == null || postFirestoreId.trim().isEmpty()) {
            liveData.setValue(new ArrayList<>());
            return liveData;
        }

        firestore.collection("posts")
                .document(postFirestoreId)
                .collection("comments")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    long localPostId = stableLongFromString(postFirestoreId);
                    if (error != null) {
                        executorService.execute(() -> {
                            LiveData<List<CommentEntity>> cachedSource = commentDao.getCommentsForPost(localPostId);
                            List<CommentEntity> cached = cachedSource.getValue();
                            liveData.postValue(cached != null ? cached : new ArrayList<>());
                        });
                        return;
                    }
                    if (value == null) {
                        liveData.postValue(new ArrayList<>());
                        return;
                    }

                    List<CommentEntity> comments = new ArrayList<>();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        CommentEntity comment = mapCommentFromFirestore(doc, localPostId);
                        if (comment != null) {
                            comments.add(comment);
                        }
                    }

                    executorService.execute(() -> {
                        try {
                            commentDao.deleteCommentsForPost(localPostId);
                            for (CommentEntity comment : comments) {
                                commentDao.insert(comment);
                            }
                        } catch (Exception ignored) {
                        }
                    });

                    liveData.postValue(comments);
                });
        return liveData;
    }

    public LiveData<CommentEntity> addCommentToFirestore(@NonNull String postFirestoreId,
                                                         @NonNull CommentEntity comment) {
        MutableLiveData<CommentEntity> result = new MutableLiveData<>();
        Map<String, Object> doc = new HashMap<>();
        doc.put("postId", comment.postId);
        doc.put("parentCommentId", comment.parentCommentId);
        doc.put("authorUid", comment.authorUid);
        doc.put("authorName", comment.authorName);
        doc.put("parentAuthorName", comment.parentAuthorName);
        doc.put("body", comment.body);
        doc.put("createdAt", FieldValue.serverTimestamp());

        com.google.firebase.firestore.DocumentReference postRef =
                firestore.collection("posts").document(postFirestoreId);
        com.google.firebase.firestore.DocumentReference commentRef =
                postRef.collection("comments").document();

        firestore.runBatch(batch -> {
            batch.set(commentRef, doc);
            batch.update(postRef, "commentCount", FieldValue.increment(1));
        }).addOnSuccessListener(unused -> {
                    comment.firestoreId = commentRef.getId();
                    comment.commentId = stableLongFromString(commentRef.getId());
                    executorService.execute(() -> {
                        try {
                            commentDao.insert(comment);
                        } catch (Exception ignored) {
                        }
                    });
                    result.postValue(comment);
                })
                .addOnFailureListener(e -> result.postValue(null));
        return result;
    }

    public void insertReport(ReportEntity report) {
        executorService.execute(() -> reportDao.insert(report));
    }

    public LiveData<Boolean> isLikedByCurrentUser(@NonNull String postFirestoreId,
                                                  long postLocalId,
                                                  @NonNull String userUid) {
        MutableLiveData<Boolean> liveData = new MutableLiveData<>();
        String likeId = likeDocumentId(postLocalId, userUid);
        firestore.collection("likes")
                .document(likeId)
                .addSnapshotListener((doc, error) -> {
                    if (error != null) {
                        executorService.execute(() -> {
                            LikeEntity cached = likeDao.getLikeForPostAndUser(postLocalId, userUid).getValue();
                            liveData.postValue(cached != null);
                        });
                        return;
                    }
                    boolean exists = doc != null && doc.exists();
                    if (exists) {
                        LikeEntity like = new LikeEntity(
                                userUid,
                                postFirestoreId,
                                "POST",
                                doc.getTimestamp("likedAt") != null
                                        ? doc.getTimestamp("likedAt").toDate().getTime()
                                        : System.currentTimeMillis()
                        );
                        like.postId = postLocalId;
                        like.firestoreId = likeId;
                        executorService.execute(() -> likeDao.upsert(like));
                    } else {
                        executorService.execute(() -> likeDao.deleteByPostAndUser(postLocalId, userUid));
                    }
                    liveData.postValue(exists);
                });
        return liveData;
    }

    public LiveData<Boolean> toggleLike(@NonNull String postFirestoreId,
                                        long postLocalId,
                                        @NonNull String userUid) {
        MutableLiveData<Boolean> result = new MutableLiveData<>();
        String likeId = likeDocumentId(postLocalId, userUid);
        com.google.firebase.firestore.DocumentReference postRef =
                firestore.collection("posts").document(postFirestoreId);
        com.google.firebase.firestore.DocumentReference likeRef =
                firestore.collection("likes").document(likeId);

        firestore.runTransaction(transaction -> {
            com.google.firebase.firestore.DocumentSnapshot likeDoc = transaction.get(likeRef);
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
            executorService.execute(() -> {
                try {
                    if (nowLiked) {
                        LikeEntity like = new LikeEntity(userUid, postFirestoreId, "POST", System.currentTimeMillis());
                        like.postId = postLocalId;
                        like.firestoreId = likeId;
                        likeDao.upsert(like);
                    } else {
                        likeDao.deleteByPostAndUser(postLocalId, userUid);
                    }
                } catch (Exception ignored) {
                }
            });
            result.postValue(nowLiked);
        }).addOnFailureListener(e -> result.postValue(null));
        return result;
    }

    public interface OnPostCreatedListener {
        void onResult(@Nullable PostEntity persisted, @Nullable Throwable error);
    }

    public void createPostInFirestoreAsync(@NonNull PostEntity post,
                                           @Nullable String authorUid,
                                           @Nullable String locationName,
                                           @NonNull OnPostCreatedListener listener) {
        Map<String, Object> doc = new HashMap<>();
        doc.put("authorUid", authorUid);
        doc.put("authorId", post.authorId);
        doc.put("authorName", post.authorName);
        doc.put("caption", post.caption);
        doc.put("imageUrl", post.imageUrl);
        doc.put("createdAt", Timestamp.now());
        doc.put("createdAtMillis", post.createdAt);
        doc.put("locationId", post.locationId);
        doc.put("locationName", locationName);
        doc.put("taggedUserUids", post.taggedUserUids != null ? post.taggedUserUids : new java.util.ArrayList<>());
        doc.put("likeCount", post.likeCount != null ? post.likeCount : 0);
        doc.put("commentCount", post.commentCount != null ? post.commentCount : 0);

        firestore.collection("posts")
                .add(doc)
                .addOnSuccessListener(ref -> {
                    post.firestoreId = ref.getId();
                    post.postId = stableLongFromString(ref.getId());
                    post.authorUid = authorUid;
                    post.locationName = locationName;
                    if (post.likeCount == null) {
                        post.likeCount = 0;
                    }
                    if (post.commentCount == null) {
                        post.commentCount = 0;
                    }
                    executorService.execute(() -> {
                        try {
                            ensureLocationExists(post.locationId, locationName);
                            postDao.upsert(post);
                            listener.onResult(post, null);
                        } catch (Exception e) {
                            listener.onResult(null, e);
                        }
                    });
                })
                .addOnFailureListener(e -> listener.onResult(null, e));
    }

    private void mirrorPostsIntoRoom(@NonNull List<PostEntity> posts) {
        for (PostEntity post : posts) {
            try {
                ensureLocationExists(post.locationId, post.locationName);
                postDao.upsert(post);
            } catch (Exception ignored) {
            }
        }
    }

    @Nullable
    private PostEntity mapPostFromFirestore(@NonNull DocumentSnapshot doc) {
        try {
            PostEntity post = new PostEntity();
            post.firestoreId = doc.getId();
            post.postId = stableLongFromString(doc.getId());
            post.locationId = numberToLong(doc.get("locationId"));
            post.locationName = doc.getString("locationName");
            post.authorId = numberToLong(doc.get("authorId"));
            post.authorName = doc.getString("authorName");
            post.authorUid = doc.getString("authorUid");
            post.caption = doc.getString("caption");
            post.imageUrl = doc.getString("imageUrl");
            post.createdAt = resolveCreatedAt(doc);
            post.likeCount = numberToInteger(doc.get("likeCount"));
            post.commentCount = numberToInteger(doc.get("commentCount"));

            Object tagged = doc.get("taggedUserUids");
            if (tagged instanceof List<?>) {
                List<String> taggedUserUids = new ArrayList<>();
                for (Object item : (List<?>) tagged) {
                    if (item instanceof String) {
                        taggedUserUids.add((String) item);
                    }
                }
                post.taggedUserUids = taggedUserUids;
            }
            return post;
        } catch (Exception e) {
            return null;
        }
    }

    private long resolveCreatedAt(@NonNull DocumentSnapshot doc) {
        Object createdAtMillis = doc.get("createdAtMillis");
        if (createdAtMillis instanceof Number) {
            return ((Number) createdAtMillis).longValue();
        }
        Timestamp createdAtTimestamp = doc.getTimestamp("createdAt");
        if (createdAtTimestamp != null) {
            return createdAtTimestamp.toDate().getTime();
        }
        Object createdAt = doc.get("createdAt");
        if (createdAt instanceof Number) {
            return ((Number) createdAt).longValue();
        }
        return 0L;
    }

    private long numberToLong(@Nullable Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return 0L;
    }

    @Nullable
    private Integer numberToInteger(@Nullable Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return null;
    }

    private void ensureLocationExists(long locationId, @Nullable String fallbackName) {
        if (locationId <= 0L) {
            return;
        }

        LocationEntity existing = null;
        for (LocationEntity location : locationDao.getAllLocationsSync()) {
            if (location != null && location.locationId == locationId) {
                existing = location;
                break;
            }
        }
        if (existing != null) {
            return;
        }

        LocationEntity placeholder = new LocationEntity(
                fallbackName != null && !fallbackName.trim().isEmpty() ? fallbackName : "Unknown location",
                0.0,
                0.0,
                0f,
                false,
                "firestore-mirror",
                "",
                "",
                "",
                ""
        );
        placeholder.locationId = locationId;
        locationDao.insert(placeholder);
    }

    private long stableLongFromString(@Nullable String value) {
        if (value == null) {
            return 0L;
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            long result = 0L;
            for (int i = 0; i < 8 && i < digest.length; i++) {
                result = (result << 8) | (digest[i] & 0xFFL);
            }
            return result;
        } catch (Exception e) {
            return value.hashCode();
        }
    }

    private String likeDocumentId(long postLocalId, @NonNull String userUid) {
        return "like_" + postLocalId + "_" + userUid;
    }

    @Nullable
    private CommentEntity mapCommentFromFirestore(@NonNull DocumentSnapshot doc, long fallbackPostId) {
        try {
            CommentEntity comment = new CommentEntity();
            comment.firestoreId = doc.getId();
            comment.commentId = stableLongFromString(doc.getId());
            comment.postId = numberToLong(doc.get("postId"));
            if (comment.postId <= 0L) {
                comment.postId = fallbackPostId;
            }
            comment.parentCommentId = doc.getLong("parentCommentId");
            comment.authorUid = doc.getString("authorUid");
            comment.authorName = doc.getString("authorName");
            comment.parentAuthorName = doc.getString("parentAuthorName");
            comment.body = doc.getString("body");
            comment.createdAt = resolveCommentCreatedAt(doc.get("createdAt"));
            return comment;
        } catch (Exception e) {
            return null;
        }
    }

    @Nullable
    private Long resolveCommentCreatedAt(@Nullable Object value) {
        if (value instanceof Timestamp) {
            return ((Timestamp) value).toDate().getTime();
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof java.util.Date) {
            return ((java.util.Date) value).getTime();
        }
        return null;
    }

    public void seedDataIfEmpty() {
        executorService.execute(() -> {
            if (postDao.getPostCount() == 0) {
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

                long postId = postDao.insert(new PostEntity(locId, 101, "TravelAddict", "Explored the beautiful Harrison's Cave today! Nature is amazing. #Barbados #BIM", LOCAL_HARRISONS_CAVE_IMAGE, System.currentTimeMillis() - 86400000));

                long c1 = commentDao.insert(new CommentEntity(postId, null, "TravelAddict", null, "This place looks incredible! Is it easy to get there?", System.currentTimeMillis() - 70000000));
                long c2 = commentDao.insert(new CommentEntity(postId, null, "LocalGuide", null, "Best time to visit is early morning to avoid the crowds.", System.currentTimeMillis() - 60000000));

                commentDao.insert(new CommentEntity(postId, c1, "User123", "TravelAddict", "Yes, there are tour buses that go right there.", System.currentTimeMillis() - 50000000));
                commentDao.insert(new CommentEntity(postId, c1, "NatureLover", "TravelAddict", "Make sure to bring a light jacket, it gets cool inside!", System.currentTimeMillis() - 40000000));
                commentDao.insert(new CommentEntity(postId, c2, "Adventurer", "LocalGuide", "Agreed! Also, the tram tour is totally worth it.", System.currentTimeMillis() - 30000000));

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
                postDao.insert(new PostEntity(locId2, 102, "IslandExplorer", "Sunset at Bathsheba. The rock formations are unlike anything else.", LOCAL_BATHSHEBA_IMAGE, System.currentTimeMillis() - 172800000));
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
