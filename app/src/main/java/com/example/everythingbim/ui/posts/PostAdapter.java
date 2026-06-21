package com.example.everythingbim.ui.posts;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.everythingbim.R;
import com.example.everythingbim.data.local.entities.PostEntity;
import com.example.everythingbim.ui.utils.ImageReferenceLoader;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.text.SimpleDateFormat;



/**
 * Post Adapter - Adapter for displaying a list of posts in a RecyclerView in a grid.
 */


public class PostAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_POST = 1;

    private final List<PostListItem> items = new ArrayList<>();
    private OnPostClickListener listener;

     // Interface for handling click events on individual posts.
    public interface OnPostClickListener {
        void onPostClick(PostEntity post);
    }

     // Sets the listener for post click events.
    public void setOnPostClickListener(OnPostClickListener listener) {
        this.listener = listener;
    }


     // Updates the data set and refreshes the RecyclerView.
     public void setPosts(List<PostEntity> posts) {
        items.clear();
        items.addAll(buildGroupedItems(posts));
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_HEADER) {
            View view = inflater.inflate(R.layout.item_post_group_header, parent, false);
            return new HeaderViewHolder(view);
        }
        View view = inflater.inflate(R.layout.item_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        PostListItem item = items.get(position);
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).bind(item.headerLabel);
            return;
        }

        PostEntity post = item.post;
        PostViewHolder postHolder = (PostViewHolder) holder;

        ImageReferenceLoader.loadInto(postHolder.imageView, post.imageUrl, R.drawable.bg_main);
        postHolder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPostClick(post);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).isHeader() ? VIEW_TYPE_HEADER : VIEW_TYPE_POST;
    }

    public boolean isHeaderPosition(int position) {
        return position >= 0
                && position < items.size()
                && items.get(position).isHeader();
    }

    private List<PostListItem> buildGroupedItems(List<PostEntity> posts) {
        List<PostListItem> groupedItems = new ArrayList<>();
        if (posts == null || posts.isEmpty()) {
            return groupedItems;
        }

        LinkedHashMap<String, List<PostEntity>> grouped = new LinkedHashMap<>();
        for (PostEntity post : posts) {
            String label = formatGroupLabel(post.createdAt);
            grouped.computeIfAbsent(label, ignored -> new ArrayList<>()).add(post);
        }

        for (java.util.Map.Entry<String, List<PostEntity>> entry : grouped.entrySet()) {
            groupedItems.add(PostListItem.header(entry.getKey()));
            for (PostEntity post : entry.getValue()) {
                groupedItems.add(PostListItem.post(post));
            }
        }
        return groupedItems;
    }

    @NonNull
    private String formatGroupLabel(long createdAtMillis) {
        if (createdAtMillis <= 0L) {
            return "Earlier";
        }

        Calendar itemCal = Calendar.getInstance();
        itemCal.setTimeInMillis(createdAtMillis);

        Calendar today = Calendar.getInstance();
        if (isSameDay(itemCal, today)) {
            return "Today";
        }

        today.add(Calendar.DAY_OF_YEAR, -1);
        if (isSameDay(itemCal, today)) {
            return "Yesterday";
        }

        return new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
                .format(createdAtMillis);
    }

    private boolean isSameDay(@NonNull Calendar one, @NonNull Calendar two) {
        return one.get(Calendar.YEAR) == two.get(Calendar.YEAR)
                && one.get(Calendar.DAY_OF_YEAR) == two.get(Calendar.DAY_OF_YEAR);
    }

    /**
     * ViewHolder for post items, holding references to the UI components.
     */
    static class PostViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.post_image);
        }
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        private final TextView titleView;

        HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            titleView = itemView.findViewById(R.id.post_group_title);
        }

        void bind(@NonNull String title) {
            titleView.setText(title);
        }
    }

    private static class PostListItem {
        final String headerLabel;
        final PostEntity post;

        private PostListItem(String headerLabel, PostEntity post) {
            this.headerLabel = headerLabel;
            this.post = post;
        }

        static PostListItem header(@NonNull String label) {
            return new PostListItem(label, null);
        }

        static PostListItem post(@NonNull PostEntity post) {
            return new PostListItem(null, post);
        }

        boolean isHeader() {
            return headerLabel != null;
        }
    }
}
