package com.oldschoolnetwork.store;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

public class StoreRepository {

    public static int compareNumbers(int a, int b) { return a < b ? -1 : a == b ? 0 : 1; }
    public static final String BASE_API_URL = "https://api.oldschoolnetwork.online/v1/on-account/api/ecosystem.php";

    public interface CatalogListener {
        void onCatalogUpdated(List<AppItem> apps, List<BannerItem> banners);
    }

    public interface AccountCallback { void onResult(JSONObject result, String error); }

    public void requestAccountAction(String action, JSONObject request, ONAccountSessionManager session,
                                     AccountCallback callback) {
        try {
            if (session.isLoggedIn()) {
                request.put("on_id", session.getOnId());
                request.put("account_identifier", session.getAccountIdentifier());
                request.put("token", session.getToken());
            }
        } catch (Exception ignored) {}
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(BASE_API_URL + "?action=" + action).openConnection();
                connection.setRequestMethod("POST");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(15000);
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                connection.setDoOutput(true);
                try (OutputStream output = connection.getOutputStream()) {
                    output.write(request.toString().getBytes("UTF-8"));
                }
                int status = connection.getResponseCode();
                java.io.InputStream stream = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
                if (stream == null) throw new java.io.IOException("Server error (HTTP " + status + ")");
                StringBuilder response = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"))) {
                    String line;
                    while ((line = reader.readLine()) != null) response.append(line);
                }
                JSONObject result = new JSONObject(response.toString());
                String state = result.optString("status");
                if("not_found".equals(state))throw new java.io.IOException("NOT_FOUND");
                if (status >= 400 || (!"ok".equals(state) && !"success".equals(state))) {
                    throw new java.io.IOException(result.optString("message", "Request failed"));
                }
                mainHandler.post(() -> callback.onResult(result, null));
            } catch (Exception error) {
                String message = error.getMessage() != null ? error.getMessage() : "Could not connect to the server";
                mainHandler.post(() -> callback.onResult(null, message));
            } finally {
                if (connection != null) connection.disconnect();
            }
        }, "store-" + action).start();
    }

    public void updateApp(AppItem app) {
        for (int i = 0; i < catalogApps.size(); i++) {
            if (catalogApps.get(i).getId() == app.getId()) { catalogApps.set(i, app); break; }
        }
        notifyListeners();
    }

    public static class FilterState {
        public String sortBy = "relevance"; // relevance, downloads, size, rating, android, name
        public boolean highestFirst = true;
        public boolean azOrder = true;
        public boolean selectAllGenres = true;
        public Set<String> selectedGenres = new LinkedHashSet<>();

        public boolean isModified() {
            return !"relevance".equals(sortBy) || !highestFirst || !selectAllGenres;
        }
    }

    private static StoreRepository instance;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final List<CatalogListener> listeners = new CopyOnWriteArrayList<>();

    private final List<AppItem> catalogApps = new ArrayList<>();
    private final List<BannerItem> catalogBanners = new ArrayList<>();
    private final List<CategoryItem> catalogCategories = new ArrayList<>();
    private boolean isLoaded = false;
    private long lastCatalogRefresh;
    private boolean isLoading = false;
    private String searchQuery = "";
    private String catalogError = "";
    private android.content.SharedPreferences cache;
    public boolean isRefreshing() { return isLoading; }
    public String getNetworkMessage() { return catalogError; }
    public String getCatalogStatus() {
        if (!catalogError.isEmpty()) return "Could not update catalog: " + catalogError + " (tap a category title to retry)";
        return isLoading ? "Loading catalog…" : "No applications match these filters";
    }
    public void initialize(android.content.Context context) {
        cache = context.getSharedPreferences("ONStore_catalog", 0);
        new Thread(() -> {
            String saved = cache.getString("response", "");
            if (!saved.isEmpty()) try {
                JSONObject response = new JSONObject(saved);
                List<AppItem> apps = parseApps(response.optJSONArray("apps"));
                List<BannerItem> banners = parseBanners(response.optJSONArray("banners"));
                List<CategoryItem> categories=parseCategories(response.optJSONArray("categories"));
                mainHandler.post(() -> { if (!isLoaded) { catalogApps.addAll(apps); catalogBanners.addAll(banners);catalogCategories.clear();catalogCategories.addAll(categories);notifyListeners(); } });
            } catch (Exception ignored) {}
        }, "store-cache").start();
    }
    private List<AppItem> parseApps(JSONArray array) {
        List<AppItem> apps = new ArrayList<>();
        if (array != null) for (int i = 0; i < array.length(); i++) { AppItem app = AppItem.fromJson(array.optJSONObject(i)); if (app != null) apps.add(app); }
        return apps;
    }
    private List<CategoryItem> parseCategories(JSONArray array){List<CategoryItem> list=new ArrayList<>();if(array!=null)for(int i=0;i<array.length();i++){JSONObject json=array.optJSONObject(i);if(json!=null&&!json.optString("slug").isEmpty())list.add(new CategoryItem(json));}return list;}
    private List<BannerItem> parseBanners(JSONArray array) {
        List<BannerItem> banners = new ArrayList<>();
        if (array != null) for (int i = 0; i < array.length(); i++) { BannerItem item = BannerItem.fromJson(array.optJSONObject(i)); if (item != null && !item.getImageUrl().isEmpty()) banners.add(item); }
        return banners;
    }

    private final FilterState appsFilter = new FilterState();
    private final FilterState gamesFilter = new FilterState();
    private final FilterState searchFilter = new FilterState();
    public FilterState getSearchFilter() { return searchFilter; }
    public List<String> getSearchGenres() {
        Set<String> genres = new java.util.TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        genres.addAll(getGenresForType(false)); genres.addAll(getGenresForType(true)); return new ArrayList<>(genres);
    }
    public List<Object> getSearchItems() { return getCategorizedItems(false, true); }

    private StoreRepository() {}

    public static synchronized StoreRepository getInstance() {
        if (instance == null) {
            instance = new StoreRepository();
        }
        return instance;
    }

    public void addListener(CatalogListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
            if (isLoaded) {
                listener.onCatalogUpdated(new ArrayList<>(catalogApps), new ArrayList<>(catalogBanners));
            }
        }
    }

    public void removeListener(CatalogListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        final List<AppItem> appsCopy = new ArrayList<>(catalogApps);
        final List<BannerItem> bannersCopy = new ArrayList<>(catalogBanners);
        mainHandler.post(() -> {
            for (CatalogListener l : listeners) {
                l.onCatalogUpdated(appsCopy, bannersCopy);
            }
        });
    }

    public String getSearchQuery() {
        return searchQuery != null ? searchQuery : "";
    }

    public void setSearchQuery(String query) {
        String clean = query != null ? query.trim() : "";
        if (!clean.equals(this.searchQuery)) {
            this.searchQuery = clean;
            notifyListeners();
        }
    }

    public FilterState getFilterState(boolean isGames) {
        return isGames ? gamesFilter : appsFilter;
    }

    public void notifyFilterChanged() {
        notifyListeners();
    }

    public List<AppItem> getAllApps() {
        return new ArrayList<>(catalogApps);
    }

    public List<BannerItem> getBanners() {
        return new ArrayList<>(catalogBanners);
    }

    public List<String> getGenresForType(boolean isGames) {
        Set<String> set = new LinkedHashSet<>();
        for (AppItem app : catalogApps) {
            if (app.isGame() == isGames) {
                if (app.getGenresList().isEmpty()) set.add("Uncategorized");
                for (String g : app.getGenresList()) {
                    if (g != null && !g.trim().isEmpty()) {
                        set.add(g.trim());
                    }
                }
            }
        }
        List<String> genres = new ArrayList<>(set);
        Collections.sort(genres, String.CASE_INSENSITIVE_ORDER);
        return genres;
    }

    public List<Object> getCategorizedItems(boolean isGames) { return getCategorizedItems(isGames, false); }
    private List<Object> getCategorizedItems(boolean isGames, boolean allTypes) {
        FilterState fs = allTypes ? searchFilter : getFilterState(isGames);
        String q = getSearchQuery().toLowerCase(Locale.US);

        List<AppItem> filtered = new ArrayList<>();
        for (AppItem app : catalogApps) {
            if (!allTypes && app.isGame() != isGames) continue;

            if (!q.isEmpty()) {
                boolean matchName = app.getName().toLowerCase(Locale.US).contains(q);
                boolean matchDev = app.getDeveloper().toLowerCase(Locale.US).contains(q);
                boolean matchPkg = app.getPackageName().toLowerCase(Locale.US).contains(q);
                boolean matchGenre = false;
                for (String g : app.getGenresList()) {
                    if (g.toLowerCase(Locale.US).contains(q)) {
                        matchGenre = true;
                        break;
                    }
                }
                if (!matchName && !matchDev && !matchPkg && !matchGenre) continue;
            }

            if (!fs.selectAllGenres) {
                boolean hasGenre = app.getGenresList().isEmpty() && fs.selectedGenres.contains("Uncategorized");
                for (String g : app.getGenresList()) {
                    if (fs.selectedGenres.contains(g)) {
                        hasGenre = true;
                        break;
                    }
                }
                if (!hasGenre) continue;
            }

            filtered.add(app);
        }

        Comparator<AppItem> comparator = null;
        if ("downloads".equals(fs.sortBy)) {
            comparator = (a, b) -> fs.highestFirst
                    ? compareNumbers(b.getDownloadsCount(), a.getDownloadsCount())
                    : compareNumbers(a.getDownloadsCount(), b.getDownloadsCount());
        } else if ("rating".equals(fs.sortBy)) {
            comparator = (a, b) -> fs.highestFirst
                    ? Float.compare(b.getRatingFloat(), a.getRatingFloat())
                    : Float.compare(a.getRatingFloat(), b.getRatingFloat());
        } else if ("size".equals(fs.sortBy)) {
            comparator = (a, b) -> fs.highestFirst
                    ? Float.compare(b.getPrimarySizeMb(), a.getPrimarySizeMb())
                    : Float.compare(a.getPrimarySizeMb(), b.getPrimarySizeMb());
        } else if ("android".equals(fs.sortBy)) {
            comparator = (a, b) -> fs.highestFirst
                    ? compareNumbers(b.getMinSdkInt(), a.getMinSdkInt())
                    : compareNumbers(a.getMinSdkInt(), b.getMinSdkInt());
        } else if ("name".equals(fs.sortBy)) {
            comparator = (a, b) -> fs.azOrder
                    ? a.getName().compareToIgnoreCase(b.getName())
                    : b.getName().compareToIgnoreCase(a.getName());
        } else if (!fs.highestFirst) {
            comparator = (a, b) -> compareNumbers(a.getId(), b.getId());
        }

        if (comparator != null) {
            Collections.sort(filtered, comparator);
        }

        List<Object> result = new ArrayList<>();
        if(!allTypes&&q.isEmpty()&&!fs.isModified())for(CategoryItem category:catalogCategories){for(AppItem app:filtered)if(category.appIds.contains(app.getId())){result.add(category);break;}}

        if (!"relevance".equals(fs.sortBy) || !q.isEmpty()) {
            // Flat sorted list with a header or grouped
            if (!filtered.isEmpty()) {
                String header = !q.isEmpty() ? "Search Results (" + filtered.size() + ")" : (isGames ? "All Games" : "All Applications");
                result.add(header);
                result.addAll(filtered);
            }
            return result;
        }

        // Group by primary genre
        Set<String> genres = new LinkedHashSet<>();
        for (AppItem app : filtered) {
            if (!app.getGenresList().isEmpty()) {
                genres.add(app.getGenresList().get(0));
            } else {
                genres.add("Uncategorized");
            }
        }

        for (String genre : genres) {
            List<AppItem> group = new ArrayList<>();
            for (AppItem app : filtered) {
                String primaryGenre = !app.getGenresList().isEmpty()
                        ? app.getGenresList().get(0)
                        : "Uncategorized";
                if (genre.equalsIgnoreCase(primaryGenre)) {
                    group.add(app);
                }
            }
            if (!group.isEmpty()) {
                result.add(genre);
                result.addAll(group);
            }
        }

        return result;
    }

    public void fetchCatalog(boolean forceRefresh) {
        if (isLoading) return;
        if (isLoaded && !forceRefresh && android.os.SystemClock.elapsedRealtime()-lastCatalogRefresh<300000) {
            notifyListeners();
            return;
        }
        isLoading = true;

        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(BASE_API_URL + "?action=store_catalog");
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                int httpStatus=conn.getResponseCode();
                java.io.InputStream body=httpStatus>=400?conn.getErrorStream():conn.getInputStream();
                if(body==null)throw new java.io.IOException("Server error (HTTP "+httpStatus+")");
                BufferedReader reader = new BufferedReader(new InputStreamReader(body, "UTF-8"));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                reader.close();

                JSONObject response = new JSONObject(sb.toString());
                if (!"ok".equals(response.optString("status"))) {
                    String message=response.optString("message", "Invalid catalog response");
                    JSONObject maintenance=response.optJSONObject("maintenance");
                    if(maintenance!=null && !maintenance.optString("comment").isEmpty())message+="\nDeveloper comment: "+maintenance.optString("comment");
                    if("maintenance".equals(response.optString("status")))message="MAINTENANCE:"+message;
                    throw new java.io.IOException(message);
                }
                List<AppItem> apps = parseApps(response.optJSONArray("apps"));
                List<BannerItem> banners = parseBanners(response.optJSONArray("banners"));
                List<CategoryItem> categories=parseCategories(response.optJSONArray("categories"));
                if (cache != null) cache.edit().putString("response", response.toString()).apply();
                mainHandler.post(() -> {
                    catalogApps.clear(); catalogApps.addAll(apps); catalogBanners.clear(); catalogBanners.addAll(banners);catalogCategories.clear();catalogCategories.addAll(categories);
                    lastCatalogRefresh=android.os.SystemClock.elapsedRealtime();isLoaded = true; isLoading = false; catalogError = ""; notifyListeners();
                });
            } catch (Exception error) {
                android.util.Log.e("ONStore", "Catalog download failed", error);
                mainHandler.post(() -> {
                    isLoading = false;
                    catalogError = error.getMessage() != null ? error.getMessage() : error.getClass().getSimpleName();
                    notifyListeners();
                });
            } finally { if (conn != null) conn.disconnect(); }
        }, "store-catalog").start();
    }
}
