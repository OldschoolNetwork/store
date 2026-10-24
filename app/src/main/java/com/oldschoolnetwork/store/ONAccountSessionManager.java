package com.oldschoolnetwork.store;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Base64;

import com.oldschoolnetwork.core.IONCoreService;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ONAccountSessionManager {

    public static final int REQ_QUICK_AUTH = 7001;
    public static final int REQ_ONBOARDING = 7002;
    private static final String PREFS_NAME = "ONStore_Account";
    private static final String API_URL = "https://api.oldschoolnetwork.online/v1/on-account/api/ecosystem.php?action=session_check";

    public interface SessionListener {
        void onSessionUpdated();
    }

    private static ONAccountSessionManager instance;
    private final Context appContext;
    private final SharedPreferences prefs;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final List<SessionListener> listeners = new CopyOnWriteArrayList<>();

    private volatile IONCoreService coreService;
    private boolean isBound = false;
    private volatile boolean coreAvailable;
    private boolean coreUpdating;
    private boolean coreInstalled(){try{return appContext.getPackageManager().getApplicationInfo("com.oldschoolnetwork.core",0).enabled;}catch(Exception ignored){return false;}}
    private void clearLocalSession(){
        accountGeneration++;lastServerRefresh=0;prefs.edit().clear().apply();cachedAvatarBase64=null;cachedAvatarBitmap=null;notifyListeners();
    }
    private void checkCorePresence(){if(coreUpdating)return;coreAvailable=coreInstalled();if(!coreAvailable){if(isBound)try{appContext.unbindService(serviceConnection);}catch(Exception ignored){}isBound=false;coreService=null;if(prefs.getBoolean("logged_in",false)||!prefs.getString("token","").isEmpty())clearLocalSession();}}

    private boolean syncInProgress = false;
    private long lastServerRefresh;
    public void refreshProfileFromServer() { refreshProfileFromServer(false); }
    public void refreshProfileFromServer(boolean force) {
        long now = android.os.SystemClock.elapsedRealtime();
        if (!force && now - lastServerRefresh < 30000 && lastServerRefresh != 0) return;
        String identifier = getAccountIdentifier();
        if (!identifier.isEmpty()) { lastServerRefresh = now; fetchFullProfileAsync(identifier, null); }
    }
    private volatile int accountGeneration = 0;
    private final ExecutorService syncExecutor = Executors.newSingleThreadExecutor();
    private final Set<String> profileRequests = Collections.synchronizedSet(new HashSet<>());
    private String cachedAvatarBase64 = null;
    private Bitmap cachedAvatarBitmap = null;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            coreService = IONCoreService.Stub.asInterface(service);
            isBound = true;
            syncFromCoreService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            coreService = null;
            // Android retains the binding and reconnects it when Core restarts.
        }
    };

    private ONAccountSessionManager(Context context) {
        this.appContext = context.getApplicationContext();
        this.prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        coreAvailable=coreInstalled();if(!coreAvailable)prefs.edit().clear().apply();
        android.content.IntentFilter filter=new android.content.IntentFilter();filter.addAction(Intent.ACTION_PACKAGE_REMOVED);filter.addAction(Intent.ACTION_PACKAGE_ADDED);filter.addAction(Intent.ACTION_PACKAGE_CHANGED);filter.addAction(Intent.ACTION_PACKAGE_REPLACED);filter.addDataScheme("package");
        appContext.registerReceiver(new android.content.BroadcastReceiver(){public void onReceive(Context context,Intent intent){if(intent.getData()==null||!"com.oldschoolnetwork.core".equals(intent.getData().getSchemeSpecificPart()))return;if(Intent.ACTION_PACKAGE_REMOVED.equals(intent.getAction())&&intent.getBooleanExtra(Intent.EXTRA_REPLACING,false)){coreUpdating=true;return;}coreUpdating=false;checkCorePresence();if(coreAvailable)syncFromCoreService();}},filter);
    }

    public static synchronized ONAccountSessionManager getInstance(Context context) {
        if (instance == null) {
            instance = new ONAccountSessionManager(context);
        }
        return instance;
    }

    public void addListener(SessionListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(SessionListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        mainHandler.post(() -> {
            for (SessionListener l : listeners) {
                l.onSessionUpdated();
            }
        });
    }

    public void bindCoreService() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post(this::bindCoreService);
            return;
        }
        checkCorePresence();if(!coreAvailable)return;
        // Includes a connection still pending: never bind again from a UI refresh.
        if (isBound) return;
        try {
            Intent intent = new Intent();
            intent.setClassName("com.oldschoolnetwork.core", "com.oldschoolnetwork.core.ONCoreService");
            isBound = appContext.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        } catch (Exception ignored) {
            isBound = false;
        }
    }

    public void syncFromCoreService() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post(this::syncFromCoreService);
            return;
        }
        bindCoreService();
        if (syncInProgress) return;
        syncInProgress = true;
        final IONCoreService service = coreService;
        final int generation = accountGeneration;
        syncExecutor.execute(() -> {
            try {
                syncFromCoreServiceInBackground(service, generation);
            } finally {
                mainHandler.post(() -> {
                    syncInProgress = false;
                    // A connection may have arrived while the fallback was running.
                    if (coreService != null && coreService != service) syncFromCoreService();
                });
            }
        });
    }

    private void syncFromCoreServiceInBackground(IONCoreService service, int generation) {
        if (service != null) {
            try {
                boolean loggedIn=service.isLoggedIn();
                if (!loggedIn) {mainHandler.post(()->{if(generation==accountGeneration)clearLocalSession();});return;}
                if (loggedIn) {
                    String coreEmail = safe(service.getEmail());
                    String coreUsername = safe(service.getUsername());
                    String coreOnId = safe(service.getOnId());
                    String coreName = safe(service.getName());
                    String coreSurname = safe(service.getSurname());
                    String coreAvatar = safe(service.getAvatar());
                    String coreToken = safe(service.getToken());

                    // If no local override account or same account, update from Core Service
                    String savedEmail = getEmail();
                    if (savedEmail.isEmpty() || savedEmail.equalsIgnoreCase(coreEmail) || savedEmail.equalsIgnoreCase(coreUsername)) {
                        Map<String, String> values = new HashMap<>();
                        if (!coreEmail.isEmpty()) values.put("email", coreEmail);
                        if (!coreUsername.isEmpty()) values.put("username", coreUsername);
                        if (!coreOnId.isEmpty()) values.put("on_id", coreOnId);
                        boolean serverProfile = !coreEmail.isEmpty() && prefs.getString("server_profile_account", "").equalsIgnoreCase(coreEmail);
                        if (!serverProfile && !coreName.isEmpty()) values.put("name", coreName);
                        if (!serverProfile && !coreSurname.isEmpty()) values.put("surname", coreSurname);
                        if (!serverProfile && !coreAvatar.isEmpty()) values.put("avatar_base64", coreAvatar);
                        if (!coreToken.isEmpty()) values.put("token", coreToken);
                        mainHandler.post(() -> applySessionValues(values, generation));

                        String loginIdent = !coreEmail.isEmpty() ? coreEmail : (!coreUsername.isEmpty() ? coreUsername : coreOnId);
                        if (!loginIdent.isEmpty() && (coreOnId.isEmpty() || coreUsername.isEmpty())) {
                            fetchFullProfileAsync(loginIdent, null, generation);
                        }
                        return;
                    }
                }
            } catch (Exception ignored) {
            }
        }
        checkSystemAccountFallback(generation);
    }

    private void checkSystemAccountFallback(int generation) {
        if (isLoggedIn()) {
            String ident = !getEmail().isEmpty() ? getEmail() : getUsername();
            if (!ident.isEmpty() && getOnId().isEmpty()) {
                fetchFullProfileAsync(ident, null, generation);
            }
            return;
        }
        // An account name alone is not an authenticated session. Wait for Core/QuickAuth.
    }

    public void updateFromQuickAuthIntent(Intent data) {
        updateFromQuickAuthIntent(data, null);
    }

    public void updateFromQuickAuthIntent(Intent data, Runnable onComplete) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post(() -> updateFromQuickAuthIntent(data, onComplete));
            return;
        }
        checkCorePresence();if(!coreAvailable){if(onComplete!=null)mainHandler.post(onComplete);return;}
        if (data == null) {
            syncFromCoreService();
            if (onComplete != null) mainHandler.post(onComplete);
            return;
        }

        // Ignore older Core/network results after the user switches accounts.
        accountGeneration++;
        String account = safe(data.getStringExtra("ON_ACCOUNT"));
        String onId = safe(data.getStringExtra("ON_ID"));
        String username = safe(data.getStringExtra("ON_USERNAME"));
        String name = safe(data.getStringExtra("ON_NAME"));
        String surname = safe(data.getStringExtra("ON_SURNAME"));
        String avatar = safe(data.getStringExtra("ON_AVATAR"));
        String token = safe(data.getStringExtra("AUTH_TOKEN"));

        SharedPreferences.Editor ed = prefs.edit();
        ed.putBoolean("logged_in", true);
        if (!account.isEmpty()) ed.putString("email", account);
        if (!onId.isEmpty()) ed.putString("on_id", onId);
        if (!username.isEmpty()) ed.putString("username", username);
        if (!name.isEmpty()) ed.putString("name", name);
        if (!surname.isEmpty()) ed.putString("surname", surname);
        if (!avatar.isEmpty()) ed.putString("avatar_base64", avatar);
        if (!token.isEmpty()) ed.putString("token", token);
        ed.apply();

        notifyListeners();
        if (onComplete != null) {
            mainHandler.post(onComplete);
        }

        String lookup = !account.isEmpty() ? account : (!username.isEmpty() ? username : onId);
        if (!lookup.isEmpty()) {
            fetchFullProfileAsync(lookup, onComplete);
        }
    }

    public void fetchFullProfileAsync(final String loginIdentifier, final Runnable onComplete) {
        fetchFullProfileAsync(loginIdentifier, onComplete, accountGeneration);
    }

    private void fetchFullProfileAsync(final String loginIdentifier, final Runnable onComplete,
                                       final int generation) {
        if (loginIdentifier == null || loginIdentifier.trim().isEmpty()) return;
        if (generation != accountGeneration) return;
        final String requestKey = generation + ":" + loginIdentifier.trim();
        if (!profileRequests.add(requestKey)) return;
        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(API_URL);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setDoOutput(true);

                JSONObject req = new JSONObject();
                req.put("token", getToken());

                OutputStream os = conn.getOutputStream();
                os.write(req.toString().getBytes("UTF-8"));
                os.close();

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                reader.close();

                JSONObject res = new JSONObject(sb.toString());
                if (res.optJSONObject("user") != null) {
                    JSONObject u = res.optJSONObject("user");
                    if (u != null) {
                        String onId = safe(u.optString("on_id", ""));
                        String username = safe(u.optString("username", ""));
                        String email = safe(u.optString("email", ""));
                        String name = safe(u.optString("name", ""));
                        String surname = safe(u.optString("surname", ""));
                        String avatarBase64 = safe(u.optString("avatar_base64", ""));

                        Map<String, String> values = new HashMap<>();
                        values.put("on_id", onId);
                        values.put("username", username);
                        values.put("email", email.isEmpty() ? loginIdentifier : email);
                        values.put("name", name);
                        values.put("surname", surname);
                        values.put("avatar_base64", avatarBase64);
                        mainHandler.post(() -> {
                            if (generation != accountGeneration) return;
                            applySessionValues(values, generation);
                            prefs.edit().putString("server_profile_account", values.get("email")).apply();
                            if (onComplete != null) onComplete.run();
                        });
                    }
                }
            } catch (Exception ignored) {
            } finally {
                if (conn != null) conn.disconnect();
                profileRequests.remove(requestKey);
            }
        }).start();
    }

    public boolean isLoggedIn() {
        // UI getters must only read the local snapshot, never make Binder calls.
        return prefs.getBoolean("logged_in", false)
                && coreAvailable && !prefs.getString("token", "").isEmpty()
                && (!getEmail().isEmpty() || !getUsername().isEmpty() || !getOnId().isEmpty());
    }

    // Called on the main thread; unchanged snapshots do not redraw every listener.
    private void applySessionValues(Map<String, String> values, int generation) {
        if (generation != accountGeneration) return;
        boolean changed = !prefs.getBoolean("logged_in", false);
        SharedPreferences.Editor editor = prefs.edit();
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (!entry.getValue().equals(prefs.getString(entry.getKey(), ""))) {
                editor.putString(entry.getKey(), entry.getValue());
                changed = true;
            }
        }
        if (changed) {
            editor.putBoolean("logged_in", true).apply();
            notifyListeners();
        }
    }

    public String getOnId() {
        return prefs.getString("on_id", "");
    }

    public String getToken() { return isLoggedIn() ? prefs.getString("token", "") : ""; }

    public String getUsername() {
        return prefs.getString("username", "");
    }

    public String getEmail() {
        return prefs.getString("email", "");
    }

    public String getName() {
        return prefs.getString("name", "");
    }

    public String getSurname() {
        return prefs.getString("surname", "");
    }

    public String getFullName() {
        String n = getName().trim();
        String s = getSurname().trim();
        String full = (n + " " + s).trim();
        if (!full.isEmpty()) return full;
        if (!getUsername().isEmpty()) return "@" + getUsername();
        if (!getEmail().isEmpty()) return getEmail();
        return "ON User";
    }

    public String getAvatarBase64() {
        return prefs.getString("avatar_base64", "");
    }

    public synchronized Bitmap getAvatarBitmap() {
        String b64 = getAvatarBase64();
        if (b64 == null || b64.trim().isEmpty()) {
            cachedAvatarBase64 = null;
            cachedAvatarBitmap = null;
            return null;
        }
        String clean = b64.trim();
        if (clean.equals(cachedAvatarBase64) && cachedAvatarBitmap != null && !cachedAvatarBitmap.isRecycled()) {
            return cachedAvatarBitmap;
        }
        try {
            if (clean.contains("base64,")) {
                clean = clean.substring(clean.indexOf("base64,") + 7);
            }
            byte[] bytes = Base64.decode(clean, Base64.DEFAULT);
            if (bytes != null && bytes.length > 0) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);

                int targetSize = 256;
                int inSampleSize = 1;
                while (options.outWidth / (inSampleSize * 2) >= targetSize &&
                       options.outHeight / (inSampleSize * 2) >= targetSize) {
                    inSampleSize *= 2;
                }
                options.inSampleSize = inSampleSize;
                options.inJustDecodeBounds = false;

                Bitmap decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
                cachedAvatarBase64 = b64.trim();
                cachedAvatarBitmap = decoded;
                return decoded;
            }
        } catch (Throwable t) {
            // Catches OutOfMemoryError and other critical errors without crashing
            t.printStackTrace();
        }
        return null;
    }

    public String getAccountIdentifier() {
        if (!getOnId().isEmpty()) return getOnId();
        if (!getUsername().isEmpty()) return getUsername();
        return getEmail();
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }
}
