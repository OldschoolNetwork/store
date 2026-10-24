package com.oldschoolnetwork.store;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class CollectionFragment extends Fragment implements ONAccountSessionManager.SessionListener {
    private View root;
    private AppAdapter adapter;
    private ONAccountSessionManager session;
    private boolean favorites;
    private int generation;
    private int renderVersion;
    private String selectedDevice = "this";
    private final java.util.List<String> deviceValues = new java.util.ArrayList<>();
    private JSONArray displayed = new JSONArray();
    private int installFilter, priceFilter, sortOrder;
    private String syncedInstalled = "";
    public static CollectionFragment favorites() { CollectionFragment f = new CollectionFragment(); Bundle args = new Bundle(); args.putBoolean("favorites", true); f.setArguments(args); return f; }
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state) { return inflater.inflate(R.layout.fragment_store_collection, container, false); }
    public void onViewCreated(View view, Bundle state) {
        root = view; favorites = getArguments() != null && getArguments().getBoolean("favorites");
        ((TextView) root.findViewById(R.id.collectionTitle)).setText(favorites ? "Favorites" : "Library");
        session = ONAccountSessionManager.getInstance(requireContext()); session.addListener(this);
        RecyclerView list = root.findViewById(R.id.collectionList); list.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new AppAdapter(new ArrayList<>()); list.setAdapter(adapter);
        adapter.setOnAppClickListener(app -> { if (getActivity() instanceof StoreMainActivity) ((StoreMainActivity) getActivity()).openAppDetails(app); else if (getActivity() instanceof LibraryActivity) ((LibraryActivity) getActivity()).openDetails(app); });
        root.findViewById(R.id.collectionFilters).setVisibility(favorites ? View.GONE : View.VISIBLE);
        root.findViewById(R.id.collectionFilterButton).setOnClickListener(v->{
            View panel=root.findViewById(R.id.collectionFilterPanel);
            boolean expanding=panel.getVisibility()!=View.VISIBLE;
            if(expanding){panel.setVisibility(View.VISIBLE);panel.setAlpha(0);panel.setTranslationY(-20);panel.animate().alpha(1).translationY(0).setDuration(180).start();}
            else panel.setVisibility(View.GONE);
            ((TextView)v).setText(expanding?"Close filters":"Filter");
        });
        bindDevices(new JSONArray());
        filter(R.id.libraryInstallFilter, new String[]{"All apps", "Installed", "Not installed"}, 0);
        filter(R.id.libraryPriceFilter, new String[]{"All prices", "Free", "Paid"}, 1);
        filter(R.id.librarySort, new String[]{"Recently added", "Name A–Z", "Name Z–A"}, 2);
        root.findViewById(R.id.collectionStatus).setOnClickListener(v -> load()); load();
    }
    public void onResume() { super.onResume(); if (root != null) load(); }
    public void onHiddenChanged(boolean hidden) { super.onHiddenChanged(hidden); if (!hidden && root != null) load(); }
    public void onSessionUpdated() { if (root != null) load(); }
    private void load() {
        int version = ++generation;
        if (!session.isLoggedIn()) { displayed=new JSONArray(); adapter.setItems(new ArrayList<>()); root.findViewById(R.id.collectionFilters).setVisibility(View.GONE); status("Sign in through the Profile tab to view your " + (favorites?"favorites.":"library.")); return; }
        root.findViewById(R.id.collectionFilters).setVisibility(favorites?View.GONE:View.VISIBLE);
        if (!favorites) render(LibraryStore.read(requireContext(), session));
        if (!session.isLoggedIn() && favorites) { status("Sign in to view your favorites."); adapter.setItems(new ArrayList<>()); return; }
        status("Loading…");
        if (!session.isLoggedIn()) { status("Installed apps on this device. Sign in to sync your library."); return; }
        StoreRepository.getInstance().requestAccountAction(favorites ? "store_favorites" : "store_library", new JSONObject(), session, (result, error) -> {
            if (root == null || version != generation) return;
            if (error != null || !result.has("apps")) { status("Could not update. Showing saved apps. Tap to retry."); return; }
            JSONArray apps = result.optJSONArray("apps"); if (apps == null) apps = new JSONArray();
            if (!favorites) bindDevices(result.optJSONArray("devices"));
            if (!favorites) LibraryStore.save(requireContext(), session, apps);
            render(apps); status(favorites && apps.length() == 0 ? "No favorites yet." : "");
        });
    }
    private void bindDevices(JSONArray devices) {
        deviceValues.clear(); deviceValues.add("this"); deviceValues.add("all");
        java.util.List<String> labels = new java.util.ArrayList<>(); labels.add("This device (" + android.os.Build.MODEL + ")"); labels.add("All synced devices");
        if (devices != null) for (int i=0;i<devices.length();i++) {
            JSONObject device = devices.optJSONObject(i); if (device == null || device.optString("device_id").equals(LibraryStore.deviceId(requireContext()))) continue;
            String id = device.optString("device_id"); deviceValues.add(id);
            labels.add(device.optString("manufacturer") + " " + device.optString("model") + " · " + id.substring(Math.max(0,id.length()-6)));
        }
        android.widget.Spinner spinner = root.findViewById(R.id.libraryDeviceFilter);
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(requireContext(),R.layout.item_package_version,labels); adapter.setDropDownViewResource(R.layout.item_package_version); spinner.setAdapter(adapter);
        int selected = deviceValues.indexOf(selectedDevice); if (selected < 0) { selectedDevice = "this"; selected = 0; } spinner.setSelection(selected);
        spinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) { String choice = deviceValues.get(position); if (!choice.equals(selectedDevice)) { selectedDevice=choice;render(displayed); } }
        });
    }
    private void filter(int id, String[] labels, int kind) {
        android.widget.Spinner spinner = root.findViewById(id);
        android.widget.ArrayAdapter<String> choices = new android.widget.ArrayAdapter<>(requireContext(), R.layout.item_package_version, labels);
        choices.setDropDownViewResource(R.layout.item_package_version); spinner.setAdapter(choices);
        spinner.setSelection(kind == 0 ? installFilter : kind == 1 ? priceFilter : sortOrder);
        spinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int index, long id) {
                int old = kind == 0 ? installFilter : kind == 1 ? priceFilter : sortOrder;
                if (kind == 0) installFilter = index; else if (kind == 1) priceFilter = index; else sortOrder = index;
                if (old != index) render(displayed);
            }
        });
    }
    private void render(JSONArray saved) {
        displayed = saved;
        final String device = selectedDevice;
        final int installation = installFilter, price = priceFilter, order = sortOrder;
        final int version = generation;
        final int draw = ++renderVersion;
        final android.content.Context context = requireContext().getApplicationContext();
        final List<AppItem> catalog = StoreRepository.getInstance().getAllApps();
        new Thread(() -> {
            LinkedHashMap<Integer, AppItem> items = new LinkedHashMap<>();
            for (int i = 0; i < saved.length(); i++) { AppItem app = AppItem.fromJson(saved.optJSONObject(i)); if (app != null) items.put(app.getId(), app); }
            List<Object> installed = new ArrayList<>(), available = new ArrayList<>(); JSONArray ids = new JSONArray();
            if (!favorites) for (AppItem app : items.values()) {
                try { context.getPackageManager().getPackageInfo(app.getPackageName(), 0); ids.put(app.getId()); } catch (Exception ignored) {}
            }
            List<AppItem> sorted = new ArrayList<>(items.values());
            if (order > 0) java.util.Collections.sort(sorted, (left,right) -> order == 1 ? left.getTitle().compareToIgnoreCase(right.getTitle()) : right.getTitle().compareToIgnoreCase(left.getTitle()));
            for (AppItem app : sorted) {
                boolean local = false; try { context.getPackageManager().getPackageInfo(app.getPackageName(), 0); local = true; } catch (Exception ignored) {}
                boolean inScope = device.equals("this") ? local : device.equals("all") ? local || !app.getInstalledDeviceIds().isEmpty() : app.getInstalledDeviceIds().contains(device);
                if (!favorites && !device.equals("this") && !device.equals("all") && installation == 0 && !inScope) continue;
                if (!favorites && ((installation == 1 && !inScope) || (installation == 2 && inScope) || (price == 1 && app.getPriceAmount() > 0) || (price == 2 && app.getPriceAmount() <= 0))) continue;
                (local && !favorites ? installed : available).add(app);
            }
            List<Object> rows = new ArrayList<>();
            if (!installed.isEmpty()) { rows.add("Installed on this device"); rows.addAll(installed); }
            if (!available.isEmpty()) { if (!favorites) rows.add("Available to download"); rows.addAll(available); }
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                if (root == null || version != generation || draw != renderVersion) return;
                adapter.setItems(rows);
                if (!favorites && session.isLoggedIn() && !catalog.isEmpty()) {
                    String snapshot = session.getAccountIdentifier() + ":" + ids;
                    if (!snapshot.equals(syncedInstalled)) {
                        syncedInstalled = snapshot;
                        JSONObject request = new JSONObject(); try { request.put("installed_app_ids", ids); request.put("device_id", LibraryStore.deviceId(context)); request.put("manufacturer", android.os.Build.MANUFACTURER); request.put("model", android.os.Build.MODEL); request.put("sdk", android.os.Build.VERSION.SDK_INT); } catch (Exception ignored) {}
                        StoreRepository.getInstance().requestAccountAction("store_sync_library", request, session, (result, error) -> { if (error != null) syncedInstalled = ""; });
                    }
                }
            });
        }, "store-installed-apps").start();
    }
    private void status(String text) { if (root != null) ((TextView) root.findViewById(R.id.collectionStatus)).setText(text); }
    public void onDestroyView() { generation++; if (session != null) session.removeListener(this); root = null; super.onDestroyView(); }
}
