package com.oldschoolnetwork.store;
import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
public final class SearchHistory {
    private static JSONArray read(Context context) { try { return new JSONArray(context.getSharedPreferences("ONStore_search", 0).getString("terms", "[]")); } catch (Exception ignored) { return new JSONArray(); } }
    public static void record(Context context, String query) {
        JSONArray values = read(context); List<JSONObject> rows = new ArrayList<>(); JSONObject current = null;
        for (int i = 0; i < values.length(); i++) { JSONObject row = values.optJSONObject(i); if (row == null) continue; if (row.optString("text").equalsIgnoreCase(query)) current = row; else rows.add(row); }
        if (current == null) current = new JSONObject();
        try { current.put("text", query); current.put("hits", current.optInt("hits") + 1); current.put("time", System.currentTimeMillis()); } catch (Exception ignored) {}
        rows.add(current); Collections.sort(rows, (a,b) -> b.optLong("time") > a.optLong("time") ? 1 : b.optLong("time") == a.optLong("time") ? 0 : -1);
        JSONArray saved = new JSONArray(); for (int i = 0; i < Math.min(100, rows.size()); i++) saved.put(rows.get(i));
        context.getSharedPreferences("ONStore_search", 0).edit().putString("terms", saved.toString()).apply();
    }
    private static double score(JSONObject row) { return row.optInt("hits") / (1.0 + Math.max(0, System.currentTimeMillis() - row.optLong("time")) / 86400000.0); }
    private static void sort(List<JSONObject> rows) { Collections.sort(rows, (a,b) -> Double.compare(score(b), score(a))); }
    public static JSONArray suggestions(Context context, String query) {
        if (query.trim().isEmpty()) return recent(context);
        String clean = query.toLowerCase(java.util.Locale.ROOT); List<JSONObject> rows = new ArrayList<>(); JSONArray history = read(context);
        for (int i = 0; i < history.length(); i++) { JSONObject row = history.optJSONObject(i); if (row != null && row.optString("text").toLowerCase(java.util.Locale.ROOT).contains(clean)) rows.add(row); }
        sort(rows); JSONArray result = new JSONArray();
        for (AppItem app : StoreRepository.getInstance().getAllApps()) {
            if (!clean.isEmpty() && app.getTitle().toLowerCase(java.util.Locale.ROOT).contains(clean) && result.length() < 2) { JSONObject row = new JSONObject(); try { row.put("text", app.getTitle()); } catch (Exception ignored) {} result.put(row); }
        }
        for (JSONObject row : rows) if (result.length() < 3) result.put(row);
        return result;
    }
    public static JSONArray recent(Context context) {
        List<JSONObject> rows = new ArrayList<>(); JSONArray history = read(context);
        for (int i = 0; i < history.length(); i++) if (history.optJSONObject(i) != null) rows.add(history.optJSONObject(i));
        Collections.sort(rows, (a,b) -> (b.optLong("time") > a.optLong("time") ? 1 : b.optLong("time") == a.optLong("time") ? 0 : -1));
        JSONArray result = new JSONArray();
        for (int i = 0; i < Math.min(4, rows.size()); i++) result.put(rows.get(i));
        return result;
    }
}
