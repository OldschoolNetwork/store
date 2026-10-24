package com.oldschoolnetwork.store;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.bumptech.glide.Glide;

public class StoreMainActivity extends AppCompatActivity implements ONAccountSessionManager.SessionListener {

    private CoreAvailabilityNotice coreNotice;
    private NetworkNotice networkNotice;
    private DrawerLayout drawerLayout;
    private FrameLayout detailContainer;

    private LinearLayout expandableSearchContainer;
    private EditText etGlobalSearch;
    private boolean isSearchVisible = false;
    private int suggestionVersion;

    private LinearLayout navApps, navGames, navProfile;
    private ImageView ivNavApps, ivNavGames, ivNavProfileAvatar;
    private ImageView ivNavProfileRing;
    private TextView tvNavApps, tvNavGames, tvNavProfile;
    private View navIndicator;
    private LinearLayout drawerItemMain, drawerItemBest, drawerItemNotif, drawerItemSettings;

    private final Fragment[] tabFragments = new Fragment[5];
    private int currentTab = -1;

    private final int COLOR_ACTIVE = Color.parseColor("#FF00BCD4");
    private final int COLOR_INACTIVE = Color.parseColor("#FF888888");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_store_main);
        AppUpdateReceiver.schedule(this);

        drawerLayout = findViewById(R.id.drawer_layout);
        detailContainer = findViewById(R.id.detail_fragment_container);
        networkNotice=new NetworkNotice(this,(LinearLayout)drawerLayout.getChildAt(0));
        coreNotice=new CoreAvailabilityNotice(this,(LinearLayout)drawerLayout.getChildAt(0));
        findViewById(R.id.iv_menu_toggle).setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        findViewById(R.id.iv_drawer_menu_close).setOnClickListener(v -> drawerLayout.closeDrawer(GravityCompat.START));

        expandableSearchContainer = findViewById(R.id.expandable_search_container);
        etGlobalSearch = findViewById(R.id.et_global_search);
        findViewById(R.id.iv_top_search_btn).setOnClickListener(v -> toggleSearch());

        if (etGlobalSearch != null) {
            Handler searchHandler = new Handler(Looper.getMainLooper());
            final Runnable[] searchRunnable = new Runnable[1];
            etGlobalSearch.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (searchRunnable[0] != null) {
                        searchHandler.removeCallbacks(searchRunnable[0]);
                    }
                    final String query = s != null ? s.toString() : "";
                    searchRunnable[0] = () -> loadSuggestions(query);
                    searchHandler.postDelayed(searchRunnable[0], 150);
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }

        etGlobalSearch.setOnEditorActionListener((text, action, event) -> {
            if (action == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH || (event != null && event.getKeyCode() == android.view.KeyEvent.KEYCODE_ENTER && event.getAction() == android.view.KeyEvent.ACTION_UP)) { openSearchResults(etGlobalSearch.getText().toString()); return true; }
            return false;
        });
        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            boolean details = getSupportFragmentManager().getBackStackEntryCount() > 0;
            detailContainer.setVisibility(details ? View.VISIBLE : View.GONE);
            if (!details) StoreRepository.getInstance().setSearchQuery("");
            if (currentTab >= 0) notifyAutoScroll(tabFragments[currentTab], !details);
        });
        navApps = findViewById(R.id.nav_apps);
        navGames = findViewById(R.id.nav_games);
        navProfile = findViewById(R.id.nav_profile);

        ivNavApps = findViewById(R.id.iv_nav_apps);
        ivNavGames = findViewById(R.id.iv_nav_games);
        ivNavProfileRing = findViewById(R.id.iv_nav_profile_outline);
        ivNavProfileAvatar = findViewById(R.id.iv_nav_profile_avatar);

        tvNavApps = findViewById(R.id.tv_nav_apps);
        tvNavGames = findViewById(R.id.tv_nav_games);
        tvNavProfile = findViewById(R.id.tv_nav_profile);
        navIndicator = findViewById(R.id.nav_indicator);

        drawerItemMain = findViewById(R.id.drawer_item_main);
        drawerItemBest = findViewById(R.id.drawer_item_best);
        drawerItemNotif = findViewById(R.id.drawer_item_notif);
        drawerItemSettings = findViewById(R.id.drawer_item_settings);

        ONAccountSessionManager.getInstance(this).addListener(this);
        ONAccountSessionManager.getInstance(this).bindCoreService();
        refreshNavbarAvatar();

        StoreRepository.getInstance().fetchCatalog(false);

        navApps.post(() -> {
            ViewGroup.LayoutParams params = navIndicator.getLayoutParams();
            params.width = navApps.getWidth();
            navIndicator.setLayoutParams(params);
            View initial = currentTab == 1 ? navGames : currentTab == 2 ? navProfile : navApps;
            navIndicator.setTranslationX(initial.getX());
        });

        navApps.setOnClickListener(v -> handleTabClick(0, navApps));
        navGames.setOnClickListener(v -> handleTabClick(1, navGames));
        navProfile.setOnClickListener(v -> handleTabClick(2, navProfile));

        drawerItemMain.setOnClickListener(v -> {
            drawerLayout.closeDrawers();
            handleTabClick(0, navApps);
            highlightDrawerItem(0);
        });
        drawerItemBest.setOnClickListener(v -> {
            drawerLayout.closeDrawers();
            closeDetails();
            highlightDrawerItem(1);
            selectTab(3);
            showTab(3);
        });
        drawerItemNotif.setOnClickListener(v -> {
            drawerLayout.closeDrawers();
            closeDetails();
            highlightDrawerItem(2);
            selectTab(4);
            showTab(4);
        });
        drawerItemSettings.setOnClickListener(v -> {
            drawerLayout.closeDrawers();
            highlightDrawerItem(3);
            startActivity(new Intent(StoreMainActivity.this, SettingsActivity.class));
        });

        int initialTab = 0;
        if (savedInstanceState != null) {
            initialTab = savedInstanceState.getInt("KEY_CURRENT_TAB", 0);
        }
        showTab(initialTab);
        selectTab(initialTab);
        detailContainer.setVisibility(getSupportFragmentManager().getBackStackEntryCount() > 0 ? View.VISIBLE : View.GONE);
        highlightDrawerItem(initialTab <= 2 ? 0 : (initialTab == 3 ? 1 : 2));
        if(savedInstanceState==null)handleStoreLink(getIntent());
    }

    @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);if(intent.getBooleanExtra("store_home",false)){getSupportFragmentManager().popBackStackImmediate(null,FragmentManager.POP_BACK_STACK_INCLUSIVE);detailContainer.setVisibility(View.GONE);showTab(0);selectTab(0);return;}handleStoreLink(intent);}
    private void handleStoreLink(Intent intent){
        android.net.Uri uri=intent.getData();if(uri==null)return;
        String path=uri.getPath();String id=uri.getQueryParameter("app_id");if(id==null)id=uri.getQueryParameter("app");String pkg=uri.getQueryParameter("pkg");if(pkg==null)pkg=uri.getQueryParameter("package_name");
        if("/store/creator.html".equals(path)){String creator=uri.getQueryParameter("creator");if(creator==null||creator.trim().isEmpty()){startActivity(new Intent(this,StoreNotFoundActivity.class));return;}startActivity(new Intent(this,PublicProfileActivity.class).putExtra("username",creator).putExtra("creator",true));return;}
        if("/store/studio.html".equals(path)||"/store/profile.html".equals(path)){String username=uri.getQueryParameter("username");if(username==null||username.trim().isEmpty()){startActivity(new Intent(this,StoreNotFoundActivity.class));return;}startActivity(new Intent(this,PublicProfileActivity.class).putExtra("username",username).putExtra("studio",path.contains("studio")));return;}
        boolean home="/store".equals(path)||"/store/".equals(path)||"/store/index.html".equals(path);
        String category=uri.getQueryParameter("category");if(home&&category!=null){openCategory(category);return;}
        String feature=uri.getQueryParameter("feature");
        if(home&&feature!=null){int tab="all".equals(feature)||"app".equals(feature)?0:"game".equals(feature)?1:"profile".equals(feature)?2:"top".equals(feature)?3:-1;if(tab>=0){showTab(tab);if(tab<3)selectTab(tab);return;}if("library".equals(feature)||"favorites".equals(feature)){startActivity(new Intent(this,LibraryActivity.class).putExtra("favorites","favorites".equals(feature)));return;}}
        if(!home || feature!=null){startActivity(new Intent(this,StoreNotFoundActivity.class));return;}
        if(id==null&&pkg==null)return;
        org.json.JSONObject request=new org.json.JSONObject();try{if(id!=null)request.put("app_id",Integer.parseInt(id));if(pkg!=null)request.put("package_name",pkg);}catch(Exception error){startActivity(new Intent(this,StoreNotFoundActivity.class));return;}
        StoreRepository.getInstance().requestAccountAction("store_app_details",request,ONAccountSessionManager.getInstance(this),(result,error)->{
            if(isFinishing())return;if("NOT_FOUND".equals(error)){startActivity(new Intent(this,StoreNotFoundActivity.class));return;}
            AppItem app=result==null?null:AppItem.fromJson(result.optJSONObject("app"));if(app!=null)openAppDetails(app);else android.widget.Toast.makeText(this,error!=null?error:"Could not open application. Try again.",android.widget.Toast.LENGTH_LONG).show();
        });
    }

    private void showTab(int index) {
        if (index == currentTab && tabFragments[index] != null && tabFragments[index].isAdded()) {
            return;
        }

        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        if (currentTab >= 0) ft.setCustomAnimations(R.anim.tab_enter, R.anim.tab_exit);
        for (int i = 0; i < tabFragments.length; i++) {
            if (tabFragments[i] == null) tabFragments[i] = fm.findFragmentByTag("TAB_FRAGMENT_" + i);
            if (i != index && tabFragments[i] != null && tabFragments[i].isAdded()) ft.hide(tabFragments[i]);
        }

        if (currentTab >= 0 && currentTab < tabFragments.length && tabFragments[currentTab] != null && tabFragments[currentTab].isAdded()) {
            ft.hide(tabFragments[currentTab]);
            notifyAutoScroll(tabFragments[currentTab], false);
        }

        Fragment target = tabFragments[index];
        if (target == null) {
            String tag = "TAB_FRAGMENT_" + index;
            target = fm.findFragmentByTag(tag);
            if (target == null) {
                if (index == 0) target = new AppsFragment();
                else if (index == 1) target = new GamesFragment();
                else if (index == 2) target = new ProfileFragment();
                else if (index == 3) target = new TopAppsFragment();
                else if (index == 4) target = new NotificationsFragment();
                ft.add(R.id.main_fragment_container, target, tag);
            } else {
                ft.show(target);
            }
            tabFragments[index] = target;
        } else {
            ft.show(target);
        }

        notifyAutoScroll(target, true);
        currentTab = index;
        ft.commitNowAllowingStateLoss();
    }

    private void notifyAutoScroll(Fragment fragment, boolean enable) {
        if (fragment instanceof AppsFragment) {
            ((AppsFragment) fragment).setBannerAutoScrollEnabled(enable);
        } else if (fragment instanceof GamesFragment) {
            ((GamesFragment) fragment).setBannerAutoScrollEnabled(enable);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("KEY_CURRENT_TAB", currentTab);
    }

    @Override
    protected void onResume() {
        super.onResume();
        StoreRepository.getInstance().fetchCatalog(false);
        if(coreNotice!=null)coreNotice.update();
        PermissionPrompt.show(this,false);
        ONAccountSessionManager.getInstance(this).syncFromCoreService();
        ONAccountSessionManager.getInstance(this).refreshProfileFromServer();
        refreshNavbarAvatar();
        highlightDrawerItem(currentTab == 3 ? 1 : currentTab == 4 ? 2 : 0);
        if (currentTab >= 0 && currentTab < tabFragments.length && tabFragments[currentTab] != null) {
            notifyAutoScroll(tabFragments[currentTab], true);
        }
    }

    @Override
    protected void onPause() {
        if (currentTab >= 0 && currentTab < tabFragments.length && tabFragments[currentTab] != null) {
            notifyAutoScroll(tabFragments[currentTab], false);
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if(coreNotice!=null)coreNotice.close();
        if(networkNotice!=null)networkNotice.close();
        ONAccountSessionManager.getInstance(this).removeListener(this);
        super.onDestroy();
    }

    @Override
    public void onSessionUpdated() {
        refreshNavbarAvatar();
    }

    private void refreshNavbarAvatar() {
        if (ivNavProfileAvatar == null) return;
        ONAccountSessionManager session = ONAccountSessionManager.getInstance(this);
        if (session.isLoggedIn()) {
            Bitmap bmp = session.getAvatarBitmap();
            if (bmp != null) {
                Glide.with(this)
                        .load(bmp)
                        .circleCrop()
                        .into(ivNavProfileAvatar);
                return;
            }
        }
        Glide.with(this)
                .load(R.drawable.ic_profile_placeholder)
                .circleCrop()
                .into(ivNavProfileAvatar);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if ((requestCode == ONAccountSessionManager.REQ_QUICK_AUTH || requestCode == ONAccountSessionManager.REQ_ONBOARDING)
                && resultCode == Activity.RESULT_OK) {
            ONAccountSessionManager.getInstance(this).updateFromQuickAuthIntent(data, this::refreshNavbarAvatar);
        }
    }

    private void handleTabClick(int index, View targetTab) {
        closeDetails();
        highlightDrawerItem(0);
        StoreRepository.getInstance().setSearchQuery("");
        if (isSearchVisible) toggleSearch();
        if (index == 2) ONAccountSessionManager.getInstance(this).refreshProfileFromServer(true);
        selectTab(index);
        moveIndicator(targetTab);
        showTab(index);
    }

    private void selectTab(int index) {
        ivNavApps.setColorFilter(index == 0 ? COLOR_ACTIVE : COLOR_INACTIVE);
        tvNavApps.setTextColor(index == 0 ? COLOR_ACTIVE : COLOR_INACTIVE);
        tvNavApps.setTypeface(null, index == 0 ? Typeface.BOLD : Typeface.NORMAL);

        ivNavGames.setColorFilter(index == 1 ? COLOR_ACTIVE : COLOR_INACTIVE);
        tvNavGames.setTextColor(index == 1 ? COLOR_ACTIVE : COLOR_INACTIVE);
        tvNavGames.setTypeface(null, index == 1 ? Typeface.BOLD : Typeface.NORMAL);

        if (ivNavProfileRing != null) {
            ivNavProfileRing.setColorFilter(index == 2 ? COLOR_ACTIVE : COLOR_INACTIVE);
        }
        tvNavProfile.setTextColor(index == 2 ? COLOR_ACTIVE : COLOR_INACTIVE);
        tvNavProfile.setTypeface(null, index == 2 ? Typeface.BOLD : Typeface.NORMAL);

        if (index <= 2 || index == 5) {
            navIndicator.setVisibility(View.VISIBLE);
        } else {
            navIndicator.setVisibility(View.GONE);
        }
    }

    private void toggleSearch() {
        isSearchVisible = !isSearchVisible;
        if (isSearchVisible) {
            expandableSearchContainer.setVisibility(View.VISIBLE);
            expandableSearchContainer.setAlpha(0f);
            expandableSearchContainer.setTranslationY(-30f);
            expandableSearchContainer.animate().alpha(1f).translationY(0f)
                    .setDuration(300).setInterpolator(new DecelerateInterpolator(2.0f)).start();
            if (etGlobalSearch != null) etGlobalSearch.requestFocus();
            loadSuggestions(etGlobalSearch.getText().toString());
        } else {
            if (etGlobalSearch != null && etGlobalSearch.getText().length() > 0) {
                etGlobalSearch.setText("");
            }
            StoreRepository.getInstance().setSearchQuery("");
            expandableSearchContainer.animate().alpha(0f).translationY(-30f)
                    .setDuration(250).setInterpolator(new DecelerateInterpolator(2.0f))
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            expandableSearchContainer.setVisibility(View.GONE);
                            expandableSearchContainer.animate().setListener(null);
                        }
                    }).start();
        }
    }

    private void closeDetails() {
        getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
    }
    private void loadSuggestions(String query) {
        if (!isSearchVisible) return;
        int version = ++suggestionVersion;
        if (query.trim().isEmpty()) { renderSuggestions(SearchHistory.recent(this), 4); return; }
        org.json.JSONObject request = new org.json.JSONObject();
        try { request.put("query", query); } catch (Exception ignored) {}
        StoreRepository.getInstance().requestAccountAction("store_search_suggestions", request, ONAccountSessionManager.getInstance(this), (result, error) -> {
            if (!isSearchVisible || version != suggestionVersion || isFinishing()) return;
            org.json.JSONArray suggestions = query.trim().isEmpty() ? SearchHistory.recent(this) : result != null ? result.optJSONArray("suggestions") : null;
            if (suggestions == null) suggestions = SearchHistory.suggestions(this, query);
            renderSuggestions(suggestions, 3);
        });
    }
    private void renderSuggestions(org.json.JSONArray suggestions, int limit) {
            LinearLayout rows = findViewById(R.id.searchSuggestions); rows.removeAllViews();
            for (int i = 0; i < Math.min(limit, suggestions.length()); i++) {
                org.json.JSONObject suggestion = suggestions.optJSONObject(i); if (suggestion == null) continue;
                String value = suggestion.optString("text", "");
                TextView row = (TextView) getLayoutInflater().inflate(R.layout.item_search_suggestion, rows, false);
                row.setText(value); row.setOnClickListener(v -> openSearchResults(value)); rows.addView(row);
            }
    }
    private void openSearchResults(String query) {
        query = query.trim(); if (query.isEmpty()) return;
        SearchHistory.record(this, query);
        org.json.JSONObject request = new org.json.JSONObject(); try { request.put("query", query); } catch (Exception ignored) {}
        StoreRepository.getInstance().requestAccountAction("store_record_search", request, ONAccountSessionManager.getInstance(this), (result, error) -> {});
        isSearchVisible = false; suggestionVersion++; expandableSearchContainer.setVisibility(View.GONE);
        ((android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(etGlobalSearch.getWindowToken(), 0);
        StoreRepository.getInstance().setSearchQuery(query);
        detailContainer.setVisibility(View.VISIBLE);
        getSupportFragmentManager().beginTransaction().setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
            .replace(R.id.detail_fragment_container, new SearchResultsFragment()).addToBackStack(null).commit();
    }

    public void openCategory(String slug){
        detailContainer.setVisibility(View.VISIBLE);
        getSupportFragmentManager().beginTransaction().setCustomAnimations(R.anim.slide_in_right,R.anim.slide_out_left,R.anim.slide_in_left,R.anim.slide_out_right).replace(R.id.detail_fragment_container,CategoryFragment.create(slug)).addToBackStack(null).commit();
    }
    public void openStoreUrl(String value){
        android.net.Uri uri=android.net.Uri.parse(value.startsWith("/")?"https://oldschoolnetwork.online"+value:value);String host=uri.getHost();
        boolean local="oldschoolnetwork.online".equalsIgnoreCase(host)||"store.oldschoolnetwork.online".equalsIgnoreCase(host);
        if(local){if("store.oldschoolnetwork.online".equalsIgnoreCase(host)&&(uri.getPath()==null||uri.getPath().equals("/")))uri=uri.buildUpon().path("/store/").build();handleStoreLink(new Intent(Intent.ACTION_VIEW,uri));}
        else if("https".equals(uri.getScheme())||"http".equals(uri.getScheme())){try{startActivity(new Intent(Intent.ACTION_VIEW,uri));}catch(Exception failure){android.widget.Toast.makeText(this,"Could not open link.",android.widget.Toast.LENGTH_LONG).show();}}
    }
    public void openAppDetails(AppItem app) {
        if (detailContainer == null || app == null) return;

        AppDetailsFragment fragment = new AppDetailsFragment();
        Bundle args = new Bundle();
        args.putSerializable("APP_ITEM", app);
        args.putInt("APP_ID", app.getId());
        args.putString("APP_PACKAGE", app.getPackageName());
        args.putString("APP_TITLE", app.getTitle());
        args.putString("APP_DEV", app.getDeveloper());
        args.putString("APP_PRICE", app.getPrice());
        args.putInt("APP_ICON", app.getIconResId());
        args.putIntArray("APP_SCREENSHOTS", app.getScreenshots());
        fragment.setArguments(args);

        detailContainer.setVisibility(View.VISIBLE);
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                .replace(R.id.detail_fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
        } else {
            super.onBackPressed();
        }
    }

    private void moveIndicator(View targetTab) {
        navIndicator.animate().translationX(targetTab.getX()).setDuration(200)
                .setInterpolator(new DecelerateInterpolator(1.5f)).start();
    }

    private void highlightDrawerItem(int index) {
        LinearLayout[] rows = {drawerItemMain, drawerItemBest, drawerItemNotif, drawerItemSettings};
        for (int i = 0; i < rows.length; i++) {
            boolean active = i == index;
            rows[i].setBackgroundColor(active ? Color.parseColor("#222834") : Color.TRANSPARENT);
            for (int c = 0; c < rows[i].getChildCount(); c++) {
                View child = rows[i].getChildAt(c);
                if (child instanceof ImageView) ((ImageView) child).setColorFilter(active ? COLOR_ACTIVE : Color.WHITE);
                if (child instanceof TextView) ((TextView) child).setTextColor(active ? COLOR_ACTIVE : Color.WHITE);
            }
        }
    }
}
