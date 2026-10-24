package com.oldschoolnetwork.store;

import android.app.Dialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatCheckBox;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.List;

public class FilterBottomSheetFragment extends BottomSheetDialogFragment implements StoreRepository.CatalogListener {

    public interface FilterListener {
        void onFilterClosed(boolean isModified);
    }

    private FilterListener listener;
    private boolean isGames = false;
    private boolean isSearch;
    public static FilterBottomSheetFragment forSearch() { FilterBottomSheetFragment f = new FilterBottomSheetFragment(); Bundle args = new Bundle(); args.putBoolean("IS_SEARCH", true); f.setArguments(args); return f; }
    private StoreRepository.FilterState state() { return isSearch ? StoreRepository.getInstance().getSearchFilter() : StoreRepository.getInstance().getFilterState(isGames); }
    private List<String> genres() { return isSearch ? StoreRepository.getInstance().getSearchGenres() : StoreRepository.getInstance().getGenresForType(isGames); }
    private boolean updatingCategories;

    private AppCompatCheckBox cbSortDownloads, cbSortRelevance, cbSortSize, cbSortRating, cbSortAndroidVer, cbSortName;
    private AppCompatCheckBox cbDirHighest, cbDirLowest, cbDirAZ, cbDirZA, cbFilterAll;

    private AppCompatCheckBox[] sortBoxes;
    private AppCompatCheckBox[] valueBoxes;
    private AppCompatCheckBox[] alphaBoxes;

    private LinearLayout containerDynamicCategories;
    private List<String> dynamicCategories;
    private AppCompatCheckBox[] categoryBoxes;

    public static FilterBottomSheetFragment newInstance(boolean isGames) {
        FilterBottomSheetFragment f = new FilterBottomSheetFragment();
        Bundle args = new Bundle();
        args.putBoolean("IS_GAMES", isGames);
        f.setArguments(args);
        return f;
    }

    public void setFilterListener(FilterListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);

        dialog.setOnShowListener(dialogInterface -> {
            View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                bottomSheet.setBackgroundColor(Color.TRANSPARENT);
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
                int screenHeight = getResources().getDisplayMetrics().heightPixels;
                behavior.setPeekHeight(screenHeight / 2);
                behavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        });
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_store_filter, container, false);

        if (getArguments() != null) {
            isGames = getArguments().getBoolean("IS_GAMES", false);
            isSearch = getArguments().getBoolean("IS_SEARCH", false);
        }

        view.findViewById(R.id.btnFilterClose).setOnClickListener(v -> dismiss());

        cbSortDownloads = view.findViewById(R.id.cbSortDownloads);
        cbSortRelevance = view.findViewById(R.id.cbSortRelevance);
        cbSortSize = view.findViewById(R.id.cbSortSize);
        cbSortRating = view.findViewById(R.id.cbSortRating);
        cbSortAndroidVer = view.findViewById(R.id.cbSortAndroidVer);
        cbSortName = view.findViewById(R.id.cbSortName);

        cbDirLowest = view.findViewById(R.id.cbDirLowest);
        cbDirHighest = view.findViewById(R.id.cbDirHighest);
        cbDirAZ = view.findViewById(R.id.cbDirAZ);
        cbDirZA = view.findViewById(R.id.cbDirZA);

        cbFilterAll = view.findViewById(R.id.cbFilterAll);
        containerDynamicCategories = view.findViewById(R.id.containerDynamicCategories);

        sortBoxes = new AppCompatCheckBox[]{cbSortDownloads, cbSortRelevance, cbSortSize, cbSortRating, cbSortAndroidVer, cbSortName};
        valueBoxes = new AppCompatCheckBox[]{cbDirLowest, cbDirHighest};
        alphaBoxes = new AppCompatCheckBox[]{cbDirAZ, cbDirZA};

        StoreRepository.FilterState fs = state();

        // Restore saved sort state
        for (AppCompatCheckBox b : sortBoxes) {
            b.setChecked(false);
            b.setTextColor(Color.WHITE);
        }
        AppCompatCheckBox activeSort = cbSortRelevance;
        if ("downloads".equals(fs.sortBy)) activeSort = cbSortDownloads;
        else if ("size".equals(fs.sortBy)) activeSort = cbSortSize;
        else if ("rating".equals(fs.sortBy)) activeSort = cbSortRating;
        else if ("android".equals(fs.sortBy)) activeSort = cbSortAndroidVer;
        else if ("name".equals(fs.sortBy)) activeSort = cbSortName;
        activeSort.setChecked(true);
        activeSort.setTextColor(Color.parseColor("#00BCD4"));

        cbDirHighest.setChecked(fs.highestFirst);
        cbDirLowest.setChecked(!fs.highestFirst);
        cbDirAZ.setChecked(fs.azOrder);
        cbDirZA.setChecked(!fs.azOrder);
        handleNameSelectionLogic();

        setupRadioLogic(sortBoxes, true);
        setupRadioLogic(valueBoxes, false);
        setupRadioLogic(alphaBoxes, false);

        setupDynamicCategories(fs);
        StoreRepository.getInstance().addListener(this);

        return view;
    }

    private void setupRadioLogic(AppCompatCheckBox[] boxes, boolean isSortGroup) {
        for (AppCompatCheckBox box : boxes) {
            box.setOnClickListener(v -> {
                for (AppCompatCheckBox other : boxes) {
                    if (other != box) {
                        other.setChecked(false);
                        other.setTextColor(Color.WHITE);
                    }
                }
                box.setChecked(true);
                box.setTextColor(Color.parseColor("#00BCD4"));

                if (isSortGroup) {
                    handleNameSelectionLogic();
                }
                syncToRepository();
            });
        }
    }

    private void handleNameSelectionLogic() {
        boolean isNameSelected = cbSortName.isChecked();

        for (AppCompatCheckBox box : valueBoxes) {
            box.setEnabled(!isNameSelected);
            box.setTextColor(!isNameSelected ? (box.isChecked() ? Color.parseColor("#00BCD4") : Color.WHITE) : Color.parseColor("#555555"));
        }

        for (AppCompatCheckBox box : alphaBoxes) {
            box.setEnabled(isNameSelected);
            box.setTextColor(isNameSelected ? (box.isChecked() ? Color.parseColor("#00BCD4") : Color.WHITE) : Color.parseColor("#555555"));
        }
    }

    private void setupDynamicCategories(StoreRepository.FilterState fs) {
        dynamicCategories = genres();
        containerDynamicCategories.removeAllViews();
        cbFilterAll.setEnabled(!dynamicCategories.isEmpty());
        cbFilterAll.setText(dynamicCategories.isEmpty() ? "No genres in this catalog yet" : "Select All");

        categoryBoxes = new AppCompatCheckBox[dynamicCategories.size()];
        LayoutInflater inflater = LayoutInflater.from(getContext());

        cbFilterAll.setChecked(fs.selectAllGenres);
        cbFilterAll.setTextColor(fs.selectAllGenres ? Color.parseColor("#00BCD4") : Color.WHITE);

        for (int i = 0; i < dynamicCategories.size(); i++) {
            final String genreName = dynamicCategories.get(i);
            AppCompatCheckBox cb = (AppCompatCheckBox) inflater.inflate(R.layout.item_filter_checkbox, containerDynamicCategories, false);
            cb.setText(genreName);
            boolean checked = fs.selectAllGenres || fs.selectedGenres.contains(genreName);
            cb.setChecked(checked);

            cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (updatingCategories) return;
                if (!isChecked) {
                    cbFilterAll.setChecked(false);
                    cbFilterAll.setTextColor(Color.WHITE);
                } else {
                    boolean allChecked = true;
                    for (AppCompatCheckBox c : categoryBoxes) {
                        if (c != null && !c.isChecked()) {
                            allChecked = false;
                            break;
                        }
                    }
                    if (allChecked) {
                        cbFilterAll.setChecked(true);
                        cbFilterAll.setTextColor(Color.parseColor("#00BCD4"));
                    }
                }
                syncToRepository();
            });

            categoryBoxes[i] = cb;
            containerDynamicCategories.addView(cb);
        }

        cbFilterAll.setOnClickListener(v -> {
            boolean isChecked = cbFilterAll.isChecked();
            cbFilterAll.setTextColor(isChecked ? Color.parseColor("#00BCD4") : Color.WHITE);
            updatingCategories = true;
            for (AppCompatCheckBox cb : categoryBoxes) {
                cb.setChecked(isChecked);
            }
            updatingCategories = false;
            syncToRepository();
        });
    }

    private void syncToRepository() {
        StoreRepository.FilterState fs = state();
        if (cbSortDownloads.isChecked()) fs.sortBy = "downloads";
        else if (cbSortSize.isChecked()) fs.sortBy = "size";
        else if (cbSortRating.isChecked()) fs.sortBy = "rating";
        else if (cbSortAndroidVer.isChecked()) fs.sortBy = "android";
        else if (cbSortName.isChecked()) fs.sortBy = "name";
        else fs.sortBy = "relevance";

        fs.highestFirst = cbDirHighest.isChecked();
        fs.azOrder = cbDirAZ.isChecked();
        fs.selectAllGenres = cbFilterAll.isChecked();
        fs.selectedGenres.clear();
        if (categoryBoxes != null && dynamicCategories != null) {
            for (int i = 0; i < categoryBoxes.length; i++) {
                if (categoryBoxes[i] != null && categoryBoxes[i].isChecked()) {
                    fs.selectedGenres.add(dynamicCategories.get(i));
                }
            }
        }
        StoreRepository.getInstance().notifyFilterChanged();
    }

    @Override
    public void onDestroyView() {
        StoreRepository.getInstance().removeListener(this);
        super.onDestroyView();
        if (listener != null) {
            StoreRepository.FilterState fs = state();
            listener.onFilterClosed(fs.isModified());
        }
    }

    @Override public void onCatalogUpdated(List<AppItem> apps, List<BannerItem> banners) {
        if (containerDynamicCategories == null) return;
        List<String> genres = genres();
        if (!genres.equals(dynamicCategories)) setupDynamicCategories(state());
    }
}
