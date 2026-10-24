package com.oldschoolnetwork.store;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.CompositePageTransformer;
import androidx.viewpager2.widget.MarginPageTransformer;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.ArrayList;
import java.util.List;

public class AppsFragment extends Fragment implements StoreRepository.CatalogListener {

    private RecyclerView appsRecyclerView;
    private AppAdapter appAdapter;
    private ViewPager2 bannerViewPager;
    private LinearLayout dotsLayout;
    private ImageView[] dots;
    private ImageView btnFilter;

    private final List<BannerItem> bannerList = new ArrayList<>();
    private RecyclerView.Adapter<BannerViewHolder> bannerAdapter;

    private final Handler autoScrollHandler = new Handler();
    private Runnable autoScrollRunnable;
    private boolean isBannerAutoScrollEnabled = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_store_apps, container, false);

        appsRecyclerView = view.findViewById(R.id.appsRecyclerView);
        bannerViewPager = view.findViewById(R.id.bannerViewPager);
        dotsLayout = view.findViewById(R.id.dotsLayout);
        btnFilter = view.findViewById(R.id.btnFilter);

        appsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        appsRecyclerView.setItemViewCacheSize(10);
        appsRecyclerView.setHasFixedSize(true);
        appsRecyclerView.setNestedScrollingEnabled(true);
        appAdapter = new AppAdapter(new ArrayList<>());
        appsRecyclerView.setAdapter(appAdapter);

        setupBanners();
        setupFilterButton();

        StoreRepository.getInstance().addListener(this);
        StoreRepository.getInstance().fetchCatalog(false);
        refreshUI();

        PullRefreshLayout pull=new PullRefreshLayout(requireContext());
        pull.setContent(view,()->StoreRepository.getInstance().fetchCatalog(true));
        return pull;
    }

    private void setupFilterButton() {
        if (btnFilter != null) {
            updateFilterIcon(StoreRepository.getInstance().getFilterState(false).isModified());
            btnFilter.setOnClickListener(v -> {
                FilterBottomSheetFragment filterSheet = FilterBottomSheetFragment.newInstance(false);
                filterSheet.setFilterListener(this::updateFilterIcon);
                filterSheet.show(requireActivity().getSupportFragmentManager(), "FilterBottomSheetApps");
            });
        }
    }

    private void updateFilterIcon(boolean isModified) {
        if (btnFilter == null) return;
        if (isModified) {
            btnFilter.setImageResource(R.drawable.ic_filter_selected);
            btnFilter.setColorFilter(Color.parseColor("#00BCD4"));
        } else {
            btnFilter.setImageResource(R.drawable.ic_filter_unselected);
            btnFilter.setColorFilter(Color.parseColor("#FFFFFF"));
        }
    }

    private void setupBanners() {
        bannerList.clear();
        bannerList.addAll(StoreRepository.getInstance().getBanners());

        final float density = getResources().getDisplayMetrics().density;
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int paddingPx = (int) (48 * density);

        final int bannerWidth = Math.max(320, screenWidth - paddingPx);
        final int bannerHeight = (int) (bannerWidth * 9.0f / 16.0f);

        ViewGroup.LayoutParams vpParams = bannerViewPager.getLayoutParams();
        vpParams.height = bannerHeight;
        bannerViewPager.setLayoutParams(vpParams);

        bannerAdapter = new RecyclerView.Adapter<BannerViewHolder>() {
            @NonNull
            @Override
            public BannerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_store_banner, parent, false);
                return new BannerViewHolder(itemView);
            }

            @Override
            public void onBindViewHolder(@NonNull BannerViewHolder holder, int position) {
                if (bannerList.isEmpty()) return;
                int realPosition = position % bannerList.size();
                final BannerItem banner = bannerList.get(realPosition);

                if (banner.getImageUrl() != null && !banner.getImageUrl().isEmpty()) {
                    Glide.with(holder.ivBanner.getContext())
                            .load(banner.getImageUrl())
                            .override(bannerWidth, bannerHeight)
                            .format(DecodeFormat.PREFER_RGB_565)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .centerCrop()
                            .into(holder.ivBanner);
                } else if (banner.getImageResId() != 0) {
                    Glide.with(holder.ivBanner.getContext())
                            .load(banner.getImageResId())
                            .override(bannerWidth, bannerHeight)
                            .format(DecodeFormat.PREFER_RGB_565)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .centerCrop()
                            .into(holder.ivBanner);
                }

                holder.itemView.setOnClickListener(v -> {
                    if (banner.getTarget() != null && !banner.getTarget().isEmpty()) {
                        try {
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(banner.getTarget()));
                            v.getContext().startActivity(intent);
                        } catch (Exception e) {
                            Toast.makeText(getContext(), "Can't open link", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }

            @Override
            public int getItemCount() {
                if (bannerList.isEmpty()) return 0;
                if (bannerList.size() == 1) return 1;
                return bannerList.size() * 400;
            }
        };

        bannerViewPager.setAdapter(bannerAdapter);

        CompositePageTransformer transformer = new CompositePageTransformer();
        transformer.addTransformer(new MarginPageTransformer((int) (12 * density)));
        bannerViewPager.setPageTransformer(transformer);
        bannerViewPager.setOffscreenPageLimit(Math.max(1, Math.min(3, bannerList.size())));

        if (bannerList.size() > 1) {
            int mid = (bannerList.size() * 400) / 2;
            int startPosition = mid - (mid % bannerList.size());
            bannerViewPager.setCurrentItem(startPosition, false);
            setupDots(bannerList.size());
        } else if (bannerList.size() == 1) {
            bannerViewPager.setCurrentItem(0, false);
            setupDots(1);
        }

        bannerViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                if (!bannerList.isEmpty()) {
                    updateDots(position % bannerList.size());
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                super.onPageScrollStateChanged(state);
                if (state == ViewPager2.SCROLL_STATE_DRAGGING) {
                    stopAutoScroll();
                } else if (state == ViewPager2.SCROLL_STATE_IDLE) {
                    startAutoScroll();
                }
            }
        });

        startAutoScroll();
    }

    public void setBannerAutoScrollEnabled(boolean enabled) {
        this.isBannerAutoScrollEnabled = enabled;
        if (enabled) {
            startAutoScroll();
        } else {
            stopAutoScroll();
        }
    }

    private void startAutoScroll() {
        stopAutoScroll();
        if (!isBannerAutoScrollEnabled || !isResumed() || !isVisible() || bannerList.size() <= 1) {
            return;
        }
        autoScrollRunnable = new Runnable() {
            @Override
            public void run() {
                if (bannerViewPager != null && isBannerAutoScrollEnabled && isResumed() && isVisible() && bannerList.size() > 1) {
                    bannerViewPager.setCurrentItem(bannerViewPager.getCurrentItem() + 1, true);
                    autoScrollHandler.postDelayed(this, 4000);
                }
            }
        };
        autoScrollHandler.postDelayed(autoScrollRunnable, 4000);
    }

    private void stopAutoScroll() {
        if (autoScrollHandler != null && autoScrollRunnable != null) {
            autoScrollHandler.removeCallbacks(autoScrollRunnable);
        }
    }

    private void setupDots(int count) {
        if (dotsLayout == null || getContext() == null) return;
        dotsLayout.removeAllViews();
        dots = new ImageView[count];
        float density = getResources().getDisplayMetrics().density;
        int dotSize = (int) (10 * density);

        for (int i = 0; i < count; i++) {
            dots[i] = new ImageView(getContext());
            dots[i].setImageResource(R.drawable.ic_carousel_unselected);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dotSize, dotSize);
            params.setMargins((int) (6 * density), 0, (int) (6 * density), 0);
            dotsLayout.addView(dots[i], params);
        }

        if (count > 0) {
            dots[0].setImageResource(R.drawable.ic_carousel_selected);
        }
    }

    private void updateDots(int realPosition) {
        if (dots == null) return;
        for (int i = 0; i < dots.length; i++) {
            dots[i].setImageResource(i == realPosition ? R.drawable.ic_carousel_selected : R.drawable.ic_carousel_unselected);
        }
    }

    private void refreshUI() {
        if (!isAdded()) return;
        List<BannerItem> newBanners = StoreRepository.getInstance().getBanners();
        bannerViewPager.setVisibility(newBanners.isEmpty() ? View.GONE : View.VISIBLE);
        dotsLayout.setVisibility(newBanners.size() > 1 ? View.VISIBLE : View.GONE);
        boolean bannersChanged = newBanners.size() != bannerList.size();
        if (!bannersChanged && !newBanners.isEmpty() && !bannerList.isEmpty()) {
            bannersChanged = !newBanners.get(0).getImageUrl().equals(bannerList.get(0).getImageUrl());
        }
        if (bannersChanged) {
            bannerList.clear();
            bannerList.addAll(newBanners);
            if (bannerAdapter != null) bannerAdapter.notifyDataSetChanged();
            setupDots(bannerList.size());
            if (bannerViewPager != null) {
                bannerViewPager.setOffscreenPageLimit(Math.max(1, Math.min(3, bannerList.size())));
            }
            startAutoScroll();
        }

        List<Object> items = StoreRepository.getInstance().getCategorizedItems(false);
        if (items.isEmpty()) {
            items.add(StoreRepository.getInstance().getCatalogStatus());
        }
        if (appAdapter != null) {
            appAdapter.setItems(items);
        }
        updateFilterIcon(StoreRepository.getInstance().getFilterState(false).isModified());
    }

    @Override
    public void onResume() {
        super.onResume();
        startAutoScroll();
    }

    @Override public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (hidden) stopAutoScroll(); else startAutoScroll();
    }

    @Override
    public void onPause() {
        stopAutoScroll();
        super.onPause();
    }

    @Override
    public void onCatalogUpdated(List<AppItem> apps, List<BannerItem> banners) {
        refreshUI();
    }

    @Override
    public void onDestroyView() {
        StoreRepository.getInstance().removeListener(this);
        stopAutoScroll();
        super.onDestroyView();
    }

    static class BannerViewHolder extends RecyclerView.ViewHolder {
        final ImageView ivBanner;
        BannerViewHolder(@NonNull View itemView) {
            super(itemView);
            ivBanner = itemView.findViewById(R.id.iv_banner_image);
        }
    }
}