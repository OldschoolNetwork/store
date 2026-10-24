package com.oldschoolnetwork.store;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class TopAppsFragment extends Fragment implements StoreRepository.CatalogListener {

    private TextView tabAll, tabApps, tabGames;
    private View topTabIndicator;
    private RecyclerView topRecyclerView;
    private int currentTabIndex = 0;

    private static class RankedApp implements Comparable<RankedApp> {
        AppItem app;
        boolean isGame;
        int rawDownloads;
        float rating;

        RankedApp(AppItem app) {
            this.app = app;
            this.isGame = app.isGame();
            this.rawDownloads = app.getDownloadsCount();
            this.rating = app.getRatingFloat();
        }

        @Override
        public int compareTo(RankedApp o) {
            int cmp = StoreRepository.compareNumbers(o.rawDownloads, this.rawDownloads);
            if (cmp != 0) return cmp;
            return Float.compare(o.rating, this.rating);
        }
    }

    private final List<RankedApp> allItems = new ArrayList<>();
    private TopAppAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_store_top, container, false);

        tabAll = view.findViewById(R.id.tabAll);
        tabApps = view.findViewById(R.id.tabApps);
        tabGames = view.findViewById(R.id.tabGames);
        topTabIndicator = view.findViewById(R.id.topTabIndicator);
        topRecyclerView = view.findViewById(R.id.topRecyclerView);

        tabAll.post(() -> {
            ViewGroup.LayoutParams params = topTabIndicator.getLayoutParams();
            params.width = tabAll.getWidth();
            topTabIndicator.setLayoutParams(params);
        });

        tabAll.setOnClickListener(v -> switchTab(0, tabAll));
        tabApps.setOnClickListener(v -> switchTab(1, tabApps));
        tabGames.setOnClickListener(v -> switchTab(2, tabGames));

        topRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new TopAppAdapter();
        topRecyclerView.setAdapter(adapter);

        StoreRepository.getInstance().addListener(this);
        StoreRepository.getInstance().fetchCatalog(false);

        switchTab(0, tabAll);
        return view;
    }

    private void switchTab(int index, TextView targetTab) {
        currentTabIndex = index;
        tabAll.setTextColor(Color.parseColor("#888888"));
        tabAll.setTypeface(null, Typeface.NORMAL);
        tabApps.setTextColor(Color.parseColor("#888888"));
        tabApps.setTypeface(null, Typeface.NORMAL);
        tabGames.setTextColor(Color.parseColor("#888888"));
        tabGames.setTypeface(null, Typeface.NORMAL);

        targetTab.setTextColor(Color.parseColor("#00BCD4"));
        targetTab.setTypeface(null, Typeface.BOLD);

        topTabIndicator.animate().translationX(targetTab.getX()).setDuration(250).start();
        ViewGroup.LayoutParams params = topTabIndicator.getLayoutParams();
        params.width = targetTab.getWidth();
        topTabIndicator.setLayoutParams(params);

        updateList(index);
    }

    private void updateList(int index) {
        String q = StoreRepository.getInstance().getSearchQuery().toLowerCase(Locale.US);
        List<RankedApp> filteredList = new ArrayList<>();
        for (RankedApp ra : allItems) {
            if (!q.isEmpty()) {
                boolean match = ra.app.getName().toLowerCase(Locale.US).contains(q)
                        || ra.app.getDeveloper().toLowerCase(Locale.US).contains(q)
                        || ra.app.getPackageName().toLowerCase(Locale.US).contains(q);
                if (!match) continue;
            }
            if (index == 0) filteredList.add(ra);
            else if (index == 1 && !ra.isGame) filteredList.add(ra);
            else if (index == 2 && ra.isGame) filteredList.add(ra);
        }
        if (adapter != null) {
            adapter.setList(filteredList);
        }
    }

    @Override
    public void onCatalogUpdated(List<AppItem> apps, List<BannerItem> banners) {
        allItems.clear();
        for (AppItem app : apps) {
            allItems.add(new RankedApp(app));
        }
        Collections.sort(allItems);
        updateList(currentTabIndex);
    }

    @Override
    public void onDestroyView() {
        StoreRepository.getInstance().removeListener(this);
        super.onDestroyView();
    }

    private class TopAppAdapter extends RecyclerView.Adapter<TopAppAdapter.ViewHolder> {
        private List<RankedApp> list = new ArrayList<>();

        public void setList(List<RankedApp> list) {
            this.list = list;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_top_app, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            RankedApp ra = list.get(position);
            AppItem app = ra.app;

            holder.tvTopRank.setText(String.valueOf(position + 1));
            holder.title.setText(app.getTitle());
            holder.developer.setText(app.getDeveloper());
            holder.rating.setText(app.getRating());
            holder.downloads.setText(app.getDownloads());
            holder.price.setText(app.getPrice());

            float density = holder.itemView.getContext().getResources().getDisplayMetrics().density;
            app.loadIconInto(holder.icon.getContext(), holder.icon, (int) (12 * density));

            holder.price.setTextColor(app.getPrice().equalsIgnoreCase("Free") ? Color.parseColor("#4CAF50") : Color.WHITE);

            holder.itemView.setOnClickListener(v -> {
                if (getActivity() instanceof StoreMainActivity) {
                    ((StoreMainActivity) getActivity()).openAppDetails(app);
                }
            });
        }

        @Override
        public void onViewRecycled(@NonNull ViewHolder holder) {
            super.onViewRecycled(holder);
            Glide.with(holder.itemView.getContext()).clear(holder.icon);
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTopRank, title, developer, rating, downloads, price;
            ImageView icon;
            ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTopRank = itemView.findViewById(R.id.tvTopRank);
                title = itemView.findViewById(R.id.itemTopAppTitle);
                developer = itemView.findViewById(R.id.itemTopAppUser);
                rating = itemView.findViewById(R.id.itemTopRating);
                downloads = itemView.findViewById(R.id.itemTopDownloads);
                price = itemView.findViewById(R.id.itemTopPrice);
                icon = itemView.findViewById(R.id.itemTopAppIcon);
            }
        }
    }
}