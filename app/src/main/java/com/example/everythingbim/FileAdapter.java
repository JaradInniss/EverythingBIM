package com.example.everythingbim;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.FileViewHolder> {

    private List<File> fileList;
    private Context context;
    private OnFileDeleteListener deleteListener;

    public FileAdapter(Context context, List<File> fileList, OnFileDeleteListener listener) {
        this.context = context;
        this.fileList = fileList;
        this.deleteListener = listener;
    }

    @NonNull
    @Override
    public FileAdapter.FileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.file_item, parent, false);
        return new FileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FileAdapter.FileViewHolder holder, int position) {
        File file = fileList.get(position);
        holder.fileName.setText(file.getName());
        holder.fileIcon.setImageResource(getFileIcon(file.getMimeType()));

        holder.deleteFileBttn.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onDeleteClick(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return fileList.size();
    }

    // Get Appropriate File Icon
    private int getFileIcon(String mimeType) {
        // If empty return generic icon
        if (mimeType == null) { return R.drawable.icon_file_generic; }
        // If PNG return png icon
        else if (mimeType.equals("image/png")) { return R.drawable.icon_file_png; }
        // If JPEG or JPG return jpg icon
        else if (mimeType.equals("image/jpeg") || mimeType.equals("image/jpg")) { return R.drawable.icon_file_jpg; }
        // If PDF return pdf icon
        else if (mimeType.equals("application/pdf")) { return R.drawable.icon_file_pdf; }

        return R.drawable.icon_file_generic;
    }

    // Interface for file deletion
    public interface OnFileDeleteListener {
        void onDeleteClick(int position);
    }

    static class FileViewHolder extends RecyclerView.ViewHolder {
        ImageView fileIcon, deleteFileBttn;
        TextView fileName;
         FileViewHolder(@NonNull View itemView) {
            super(itemView);
            fileIcon = itemView.findViewById(R.id.file_icon);
            fileName = itemView.findViewById(R.id.file_name);
            deleteFileBttn = itemView.findViewById(R.id.delete_file_bttn);
        }
    }
}
