package com.example.everythingbim.data.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.example.everythingbim.data.local.AppDatabase;
import com.example.everythingbim.data.local.dao.CommentDao;
import com.example.everythingbim.data.local.dao.LocationDao;
import com.example.everythingbim.data.local.dao.PostDao;
import com.example.everythingbim.data.local.dao.ReportDao;
import com.example.everythingbim.data.local.entities.CommentEntity;
import com.example.everythingbim.data.local.entities.LocationEntity;
import com.example.everythingbim.data.local.entities.PostEntity;
import com.example.everythingbim.data.local.entities.ReportEntity;

import java.util.List;
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
    private final LocationDao locationDao;
    private final ReportDao reportDao;
    private final ExecutorService executorService;

    public PostRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        postDao = db.postDao();
        commentDao = db.commentDao();
        locationDao = db.locationDao();
        reportDao = db.reportDao();
        executorService = Executors.newFixedThreadPool(2);
    }

    public LiveData<List<PostEntity>> getRandomizedPosts() {
        return postDao.getRandomizedPosts();
    }

    public void insert(PostEntity post) {
        executorService.execute(() -> postDao.insert(post));
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

    public LiveData<List<CommentEntity>> getCommentsForPost(long postId) {
        return commentDao.getCommentsForPost(postId);
    }

    public void insertComment(CommentEntity comment) {
        executorService.execute(() -> commentDao.insert(comment));
    }

    public void insertReport(ReportEntity report) {
        executorService.execute(() -> reportDao.insert(report));
    }

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
                long postId = postDao.insert(new PostEntity(locId, 101, "TravelAddict", "Explored the beautiful Harrison's Cave today! Nature is amazing. #Barbados #BIM", LOCAL_HARRISONS_CAVE_IMAGE, System.currentTimeMillis() - 86400000));
                
                // Top-level comments
                long c1 = commentDao.insert(new CommentEntity(postId, null, "TravelAddict", null, "This place looks incredible! Is it easy to get there?", System.currentTimeMillis() - 70000000));
                long c2 = commentDao.insert(new CommentEntity(postId, null, "LocalGuide", null, "Best time to visit is early morning to avoid the crowds.", System.currentTimeMillis() - 60000000));
                
                // Replies to c1
                commentDao.insert(new CommentEntity(postId, c1, "User123", "TravelAddict", "Yes, there are tour buses that go right there.", System.currentTimeMillis() - 50000000));
                commentDao.insert(new CommentEntity(postId, c1, "NatureLover", "TravelAddict", "Make sure to bring a light jacket, it gets cool inside!", System.currentTimeMillis() - 40000000));
                
                // Replies to c2
                commentDao.insert(new CommentEntity(postId, c2, "Adventurer", "LocalGuide", "Agreed! Also, the tram tour is totally worth it.", System.currentTimeMillis() - 30000000));
                
                // Nested reply (reply to a reply)
                // Finding the ID of the "User123" comment might be tricky here without querying, 
                // but for seeding we can just add more top-level replies.
                
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
