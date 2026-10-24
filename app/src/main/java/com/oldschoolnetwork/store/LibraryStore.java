package com.oldschoolnetwork.store;
import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;

/** Per-account offline library, independent of favorites. */
public final class LibraryStore {
    public static synchronized String deviceId(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("ONStore_library", 0);
        String id = prefs.getString("device_id", "");
        if (id.isEmpty()) { id = java.util.UUID.randomUUID().toString(); prefs.edit().putString("device_id", id).apply(); }
        return id;
    }
    private static String key(ONAccountSessionManager session) { return "library:" + session.getAccountIdentifier(); }
    public static synchronized JSONArray read(Context context, ONAccountSessionManager session) {
        try { return new JSONArray(context.getSharedPreferences("ONStore_library", 0).getString(key(session), "[]")); }
        catch (Exception ignored) { return new JSONArray(); }
    }
    public static synchronized void save(Context context, ONAccountSessionManager session, JSONArray apps) {
        context.getSharedPreferences("ONStore_library", 0).edit().putString(key(session), apps.toString()).apply();
    }
    public static synchronized void remember(Context context, ONAccountSessionManager session, AppItem app) {
        JSONArray apps = read(context, session); JSONArray updated = new JSONArray();
        updated.put(app.toJson());
        for (int i = 0; i < apps.length(); i++) { JSONObject item = apps.optJSONObject(i); if (item != null && item.optInt("id") != app.getId()) updated.put(item); }
        save(context, session, updated);
    }
}
