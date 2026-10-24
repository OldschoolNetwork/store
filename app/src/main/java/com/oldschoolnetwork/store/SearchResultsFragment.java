package com.oldschoolnetwork.store;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import java.util.ArrayList;
import java.util.List;
public class SearchResultsFragment extends Fragment implements StoreRepository.CatalogListener {
    private View root;
    private AppAdapter adapter;
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state) { return inflater.inflate(R.layout.fragment_search_results, container, false); }
    public void onViewCreated(View view, Bundle state) {
        root = view;
        RecyclerView list = root.findViewById(R.id.searchResultsList); list.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new AppAdapter(new ArrayList<>()); list.setAdapter(adapter);
        root.findViewById(R.id.searchResultsFilter).setOnClickListener(v -> FilterBottomSheetFragment.forSearch().show(requireActivity().getSupportFragmentManager(), "SearchFilter"));
        StoreRepository.getInstance().addListener(this); update();
    }
    private void update() {
        if (root == null) return;
        StoreRepository repo = StoreRepository.getInstance();
        ((TextView) root.findViewById(R.id.searchResultsTitle)).setText("Search: " + repo.getSearchQuery());
        List<Object> items = repo.getSearchItems(); if (items.isEmpty()) items.add(repo.getCatalogStatus()); adapter.setItems(items);
    }
    public void onCatalogUpdated(List<AppItem> apps, List<BannerItem> banners) { update(); }
    public void onDestroyView() { StoreRepository.getInstance().removeListener(this); root = null; super.onDestroyView(); }
}
