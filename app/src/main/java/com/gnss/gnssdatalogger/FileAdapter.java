package com.gnss.gnssdatalogger;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.afollestad.materialdialogs.MaterialDialog;

import java.io.File;
import java.util.List;

//import com.google.firebase.storage.BuildConfig;

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.FileHolder> {

    private static final String TAG = FileAdapter.class.getSimpleName();

    private List<File> fileList;

    public FileAdapter(List<File> fileList) {
        this.fileList = fileList;
    }

    @Override
    public FileHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new FileHolder(LayoutInflater
                .from(parent.getContext()).inflate(R.layout.adapter_file, parent, false));
    }

    @Override
    public void onBindViewHolder(final FileHolder holder, int position) {
        File file = fileList.get(position);
        holder.textViewName.setText(file.getName());
        holder.textViewDateTime.setText(fileDateTime(file.getName()));
        holder.textViewVer.setText(fileVersion(file.getName()));
        holder.textViewSize.setText(fileSize(file));
        holder.relativeLayout.setOnClickListener(v -> {
            Log.i(TAG, "Click -> File: " + file.getName());
            Context context = v.getContext();
            new MaterialDialog.Builder(context)
                    .title(file.getName())
                    .items(new String[]{
                            context.getString(R.string.open),
                            context.getString(R.string.upload),
                            context.getString(R.string.share),
                            context.getString(R.string.delete)
                    })
                    .itemsCallback((dialog, view, which, text) -> {
                        Log.i(TAG, "Dialog which: " + which);
                        if (which == 0) {
                            Log.i(TAG, "Open file" + file.getPath());
                            if (file.exists()) {
                                Uri uri = FileProvider.getUriForFile(context,
                                        BuildConfig.APPLICATION_ID + ".provider",
                                        file);
                                Intent intent = new Intent(Intent.ACTION_VIEW)
                                        .setDataAndType(uri, "text/plain").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                view.getContext().startActivity(Intent.createChooser(intent, "打开文件"));
                            }
                        }
                        else if (which == 2) {
                            Log.i(TAG, "Share file " + file.getPath());
                            if (file.exists()) {
                                Uri contentUri = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID+".provider",file);
                                Intent intent = new Intent(Intent.ACTION_SEND)
                                        .setType("text/*")
                                        .putExtra(Intent.EXTRA_STREAM, contentUri);
                                view.getContext().startActivity(Intent.createChooser(intent, "分享文件"));
                            }
                        } else if (which == 3) {
                            Log.i(TAG, "Delete file" + file.getPath());
                            if (file.delete()) {
                                Log.i(TAG, "Delete complete");
                                fileList.remove(holder.getAdapterPosition());
                                notifyItemRemoved(holder.getAdapterPosition());
                            }
                        }
                    })
                    .show();
        });
    }

    @Override
    public int getItemCount() {
        return fileList.size();
    }

    static class FileHolder extends RecyclerView.ViewHolder {

        TextView textViewName;
        TextView textViewDateTime;
        TextView textViewVer;
        RelativeLayout relativeLayout;
        TextView textViewSize;

        FileHolder(View itemView) {
            super(itemView);
            textViewName = itemView.findViewById(R.id.adapter_file_name);
            textViewDateTime = itemView.findViewById(R.id.adapter_file_dateTime);
            textViewVer = itemView.findViewById(R.id.adapter_file_ver);
            textViewSize=itemView.findViewById(R.id.adapter_file_size);
            relativeLayout = itemView.findViewById(R.id.adapter_file);
        }
    }

    private String fileDateTime(String name) {
        return name.substring(4, 6) + "/" +
                name.substring(6, 8) + "/" +
                name.substring(8, 10) + " " +
                name.substring(10, 12) + ":" +
                name.substring(12, 14);
    }
    private String fileVersion(String name) {
        return "0".equals(name.substring(16, 17)) ? "v2.11" : "v3.03";
    }

    @SuppressLint("DefaultLocale")
    private String fileSize(File file)
    {
        double size=file.length()/(1024*1024.0);

        return String.format("%.1f",size)+"M";
    }
}
