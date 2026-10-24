package com.oldschoolnetwork.store;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.VideoView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import com.bumptech.glide.Glide;
import java.util.ArrayList;
import java.util.List;

public class MediaViewerActivity extends AppCompatActivity {
    private final List<MediaHolder> holders = new ArrayList<>();
    private ViewPager2 pager;
    private ArrayList<String> urls;
    private int videoCount;
    private boolean foreground;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); setContentView(R.layout.activity_media_viewer);
        urls = getIntent().getStringArrayListExtra("MEDIA_URLS");
        videoCount = getIntent().getIntExtra("VIDEO_COUNT", 0);
        if (urls == null || urls.isEmpty()) { finish(); return; }
        pager = findViewById(R.id.mediaViewPager);
        findViewById(R.id.btnBackMedia).setOnClickListener(v -> finish());
        pager.setAdapter(new RecyclerView.Adapter<MediaHolder>() {
            @NonNull @Override public MediaHolder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
                return new MediaHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_media_page, parent, false));
            }
            @Override public void onBindViewHolder(@NonNull MediaHolder holder, int position) {
                if (holder.download != null) { holder.download.cancel(true); holder.download = null; }
                holder.video.stopPlayback(); holder.position = position; holder.ready = false;
                if (!holders.contains(holder)) holders.add(holder);
                holder.error.setVisibility(View.GONE);
                holder.image.setVisibility(position < videoCount ? View.GONE : View.VISIBLE);
                holder.video.setVisibility(position < videoCount ? View.VISIBLE : View.GONE);
                if (position < videoCount) {
                    MediaController controls = new MediaController(MediaViewerActivity.this);
                    controls.setAnchorView(holder.video); holder.video.setMediaController(controls);
                    holder.video.setOnPreparedListener(player -> { holder.ready = true; updatePlayback(); });
                    holder.video.setOnErrorListener((player, what, extra) -> {
                        holder.error.setText("Could not play this video. Check the connection and MP4 format.");
                        holder.error.setVisibility(View.VISIBLE); return true;
                    });
                    String url = urls.get(position);
                    if (android.os.Build.VERSION.SDK_INT < 26 && url.startsWith("https://")) {
                        // The native player on old Android does not use Java's updated TLS trust.
                        holder.error.setText("Loading video…"); holder.error.setVisibility(View.VISIBLE);
                        com.bumptech.glide.request.FutureTarget<java.io.File> request = Glide.with(MediaViewerActivity.this).downloadOnly().load(url).submit();
                        holder.download = request;
                        new Thread(() -> {
                            try {
                                java.io.File file = request.get();
                                runOnUiThread(() -> { if (holder.download != request || isFinishing()) return; holder.error.setVisibility(View.GONE); holder.video.setVideoURI(Uri.fromFile(file)); });
                            } catch (Exception error) {
                                runOnUiThread(() -> { if (holder.download == request && !isFinishing()) { holder.error.setText("Could not load video. Check your connection."); holder.error.setVisibility(View.VISIBLE); } });
                            }
                        }, "store-video-cache").start();
                    } else holder.video.setVideoURI(Uri.parse(url));
                } else Glide.with(MediaViewerActivity.this).load(urls.get(position)).fitCenter().error(R.drawable.ic_screenshot_placeholder).into(holder.image);
            }
            @Override public int getItemCount() { return urls.size(); }
            @Override public void onViewRecycled(@NonNull MediaHolder holder) {
                if (holder.download != null) { holder.download.cancel(true); holder.download = null; }
                holder.video.stopPlayback(); holder.ready = false; Glide.with(MediaViewerActivity.this).clear(holder.image); holders.remove(holder);
            }
        });
        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override public void onPageSelected(int position) {
                ((TextView) findViewById(R.id.tvMediaCounter)).setText((position + 1) + " / " + urls.size()); updatePlayback();
            }
        });
        int position = state != null ? state.getInt("page", 0) : getIntent().getIntExtra("START_POSITION", 0);
        pager.setCurrentItem(Math.max(0, Math.min(position, urls.size() - 1)), false);
        ((TextView) findViewById(R.id.tvMediaCounter)).setText((pager.getCurrentItem() + 1) + " / " + urls.size());
    }
    private void updatePlayback() {
        if (pager == null) return;
        for (MediaHolder holder : holders) if (holder.ready) {
            if (foreground && holder.position == pager.getCurrentItem()) holder.video.start(); else holder.video.pause();
        }
    }
    @Override protected void onResume() { super.onResume(); foreground = true; updatePlayback(); }
    @Override protected void onPause() { foreground = false; updatePlayback(); super.onPause(); }
    @Override protected void onDestroy() { for (MediaHolder holder : holders) { holder.video.stopPlayback(); if (holder.download != null) holder.download.cancel(true); } holders.clear(); super.onDestroy(); }
    @Override protected void onSaveInstanceState(@NonNull Bundle state) { if (pager != null) state.putInt("page", pager.getCurrentItem()); super.onSaveInstanceState(state); }
    private static class MediaHolder extends RecyclerView.ViewHolder {
        final ImageView image; final VideoView video; final TextView error; int position; boolean ready; com.bumptech.glide.request.FutureTarget<java.io.File> download;
        MediaHolder(View view) { super(view); image = view.findViewById(R.id.mediaImage); video = view.findViewById(R.id.mediaVideo); error = view.findViewById(R.id.mediaError); }
    }
}
