package com.example.everythingbim.data.repository;

import android.app.Application;
import android.net.Uri;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.example.everythingbim.data.local.AppDatabase;
import com.example.everythingbim.data.local.dao.CommentDao;
import com.example.everythingbim.data.local.dao.LocationDao;
import com.example.everythingbim.data.local.dao.PostDao;
import com.example.everythingbim.data.local.dao.ReportDao;
import com.example.everythingbim.data.local.dao.UserDao;
import com.example.everythingbim.data.local.entities.CommentEntity;
import com.example.everythingbim.data.local.entities.LocationEntity;
import com.example.everythingbim.data.local.entities.PostEntity;
import com.example.everythingbim.data.local.entities.ReportEntity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PostRepository {
    private final Application application;
    private final PostDao postDao;
    private final ReportDao reportDao;
    private final CommentDao commentDao;
    private final LocationDao locationDao;
    private final UserDao userDao;
    private final ExecutorService executorService;

    public PostRepository(Application application) {
        this.application = application;
        AppDatabase db = AppDatabase.getInstance(application);
        postDao = db.postDao();
        commentDao = db.commentDao();
        locationDao = db.locationDao();
        reportDao = db.reportDao();
        userDao = db.userDao();
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

    public LiveData<List<CommentEntity>> getCommentsForPost(long postId) {
        return commentDao.getCommentsForPost(postId);
    }

    public void insertComment(CommentEntity comment) {
        executorService.execute(() -> commentDao.insert(comment));
    }

    public void insertReport(ReportEntity report) {
        executorService.execute(() -> reportDao.insert(report));
    }

    public LiveData<List<PostEntity>> getPostsByUserId(long userId) {
        return postDao.getPostsByUserId(userId);
    }

    public LiveData<LocationEntity> getLocationById(long locationId) {
        return locationDao.getLocationById(locationId);
    }

    public void repairLegacyPosts() {
        executorService.execute(() -> {
            List<PostEntity> allPosts = postDao.getAllPostsSync();
            if (allPosts == null) return;

            for (PostEntity post : allPosts) {
                // If the post uses a temporary content URI, try to persist it
                if (post.imageUrl != null && post.imageUrl.startsWith("content://")) {
                    Uri uri = Uri.parse(post.imageUrl);
                    String localPath = saveImageToInternalStorage(uri);
                    
                    if (localPath != null && localPath.startsWith("file://")) {
                        post.imageUrl = localPath;
                        postDao.update(post);
                        Log.d("PostRepository", "Successfully persisted legacy image for post " + post.postId);
                    }
                }
            }
        });
    }

    private String saveImageToInternalStorage(Uri uri) {
        if (uri == null) return null;
        if ("file".equals(uri.getScheme())) return uri.toString();
        
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
            String fileName = "POST_IMG_" + timeStamp + ".jpg";
            File file = new File(application.getFilesDir(), fileName);
            
            try (InputStream inputStream = application.getContentResolver().openInputStream(uri);
                 OutputStream outputStream = new FileOutputStream(file)) {
                
                if (inputStream == null) return uri.toString();
                
                byte[] buffer = new byte[4096];
                int read;
                while ((read = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, read);
                }
                return Uri.fromFile(file).toString();
            }
        } catch (SecurityException e) {
            // Permission already lost, nothing we can do for this specific image
            Log.w("PostRepository", "Permission already lost for legacy image: " + uri);
            return uri.toString();
        } catch (IOException e) {
            e.printStackTrace();
            return uri.toString();
        }
    }

    public void seedDataIfEmpty() {
        executorService.execute(() -> {
            if (postDao.getPostCount() == 0) {
                // Insert a sample location first
                long locId = locationDao.insert(new LocationEntity("Harrison's Cave", 13.1724, -59.5755, 4.8f, true, "Admin", null, null, null, null));
                
                // Insert a sample post
                long postId = postDao.insert(new PostEntity(locId, "Harrison's Cave", 101, "Harry","Explored the beautiful Harrison's Cave today! Nature is amazing. #Barbados #BIM", "https://upload.wikimedia.org/wikipedia/commons/b/b5/Harrison%27s_Cave_Barbados_2.jpg", System.currentTimeMillis() - 86400000, null));
                
                // Top-level comments
                long c1 = commentDao.insert(new CommentEntity(postId, null, "TravelAddict", null, "This place looks incredible! Is it easy to get there?", System.currentTimeMillis() - 70000000));
                long c2 = commentDao.insert(new CommentEntity(postId, null, "LocalGuide", null, "Best time to visit is early morning to avoid the crowds.", System.currentTimeMillis() - 60000000));
                
                // Replies to c1
                commentDao.insert(new CommentEntity(postId, c1, "User123", "TravelAddict", "Yes, there are tour buses that go right there.", System.currentTimeMillis() - 50000000));
                commentDao.insert(new CommentEntity(postId, c1, "NatureLover", "TravelAddict", "Make sure to bring a light jacket, it gets cool inside!", System.currentTimeMillis() - 40000000));
                
                // Replies to c2
                commentDao.insert(new CommentEntity(postId, c2, "Adventurer", "LocalGuide", "Agreed! Also, the tram tour is totally worth it.", System.currentTimeMillis() - 30000000));
                
                // Another post for variety
                long locId2 = locationDao.insert(new LocationEntity("Bathsheba Beach", 13.2101, -59.5218, 4.9f, true, "Admin", null, null, null, null));
                postDao.insert(new PostEntity(locId2, "Bathsheba Beach", 102, "Megan","Sunset at Bathsheba. The rock formations are unlike anything else.", "https://upload.wikimedia.org/wikipedia/commons/9/90/Bathsheba_Barbados.jpg", System.currentTimeMillis() - 172800000, null));
            }
        });
    }
}
