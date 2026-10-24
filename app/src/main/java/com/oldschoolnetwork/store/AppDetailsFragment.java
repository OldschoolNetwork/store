package com.oldschoolnetwork.store;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import com.bumptech.glide.Glide;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AppDetailsFragment extends Fragment implements ONAccountSessionManager.SessionListener {
    private TransferNotification transferNotification;
    private boolean silentInstalling;
    private boolean installerOpened;
    private View root;
    private AppItem app;
    private ONAccountSessionManager session;
    private int requestVersion, selectedRating, shownReviews;
    private boolean reviewDirty, submittingReview, changingFavorite;
    private String account = "";
    private ReviewItem ownReview;
    private boolean editingReview, bindingReview;
    private final List<ReviewItem> reviews = new ArrayList<>();
    private ApkDownloader download;
    private File pendingApk;
    private android.animation.ValueAnimator installSweep;
    private int pendingPackageId;
    private final Handler main = new Handler(Looper.getMainLooper());

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        return inflater.inflate(R.layout.fragment_app_details, container, false);
    }
    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        root = view;
        session = ONAccountSessionManager.getInstance(requireContext());
        account = session.getAccountIdentifier();
        if (getArguments() != null) app = (AppItem) getArguments().getSerializable("APP_ITEM");
        click(R.id.btnBack, () -> requireActivity().onBackPressed());
        click(R.id.btnInitialAction, () -> setExpanded(true));
        click(R.id.btnCollapseMenu, () -> setExpanded(false));
        click(R.id.btnFinalDownload, this::startDownload);
        click(R.id.btnUpdateApp, () -> {
            PackageVersionItem latest = newestCompatiblePackage();
            if (latest == null || latest.getVersionCode() <= installedVersion()) return;
            Spinner versions = root.findViewById(R.id.androidVersionSpinner);
            for (int i = 0; i < versions.getCount(); i++) {
                if (((PackageVersionItem) versions.getItemAtPosition(i)).getId() == latest.getId()) {
                    versions.setSelection(i);
                    startDownload();
                    return;
                }
            }
        });
        click(R.id.btnCancelDownload, () -> { if (download != null) download.cancel(); download = null; if (transferNotification != null) transferNotification.finish(); renderInstallState(); });
        click(R.id.btnOpenApp, () -> {
            if (app == null) return;
            Intent intent = requireContext().getPackageManager().getLaunchIntentForPackage(app.getPackageName());
            if (intent != null) startActivity(intent); else toast("No launchable activity in this package.");
        });
        click(R.id.btnUninstallApp, () -> { if (app != null) startActivity(new Intent(Intent.ACTION_DELETE, Uri.parse("package:" + app.getPackageName()))); });
        click(R.id.btnShare, () -> {
            if (app == null) return;
            Intent intent = new Intent(Intent.ACTION_SEND).setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, app.getTitle() + "\nhttps://oldschoolnetwork.online/store/?app_id=" + app.getId());
            startActivity(Intent.createChooser(intent, "Share via"));
        });
        click(R.id.btnLike, this::changeFavorite);
        click(R.id.btnReport, this::reportApp);
        click(R.id.guardyInfo, this::showGuardyInfo);
        click(R.id.descriptionMore, this::showDescription);
        click(R.id.tvDetailsStatus, this::loadDetails);
        click(R.id.tvReviewsStatus, this::loadDetails);
        click(R.id.btnMoreReviews, this::appendReviews);
        click(R.id.btnSubmitReview, this::submitReview);
        LinearLayout stars = root.findViewById(R.id.layoutReviewStars);
        for (int i = 0; i < stars.getChildCount(); i++) {
            final int rating = i + 1;
            stars.getChildAt(i).setOnClickListener(v -> { selectedRating = rating; reviewDirty = true; renderStars(); saveDraft(); });
        }
        input().addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) { if(bindingReview)return; reviewDirty = true; saveDraft(); }
            public void afterTextChanged(Editable s) {}
        });
        if (state != null) {
            selectedRating = state.getInt("review_rating");
            input().setText(state.getString("review_text", ""));
            reviewDirty = state.getBoolean("review_dirty");
        }
        if (state == null) restoreDraft();
        session.addListener(this);
        renderApp(); renderStars(); renderReviewAvatar(); loadDetails();
    }
    private EditText input() { return root.findViewById(R.id.etReviewInput); }
    private void click(int id, Runnable action) { root.findViewById(id).setOnClickListener(v -> action.run()); }
    private void text(int id, String value) { ((TextView) root.findViewById(id)).setText(value); }
    private void toast(String message) { if (isAdded()) Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show(); }
    private void setExpanded(boolean expanded) {
        root.findViewById(R.id.btnInitialAction).setVisibility(expanded?View.GONE:View.VISIBLE);
        root.findViewById(R.id.btnCollapseMenu).setVisibility(expanded?View.VISIBLE:View.GONE);
        View normal = root.findViewById(R.id.layoutDefaultMenu);
        View panel = root.findViewById(R.id.layoutExpandedMenu);
        panel.animate().cancel();
        if (expanded) {
            normal.setVisibility(View.GONE); panel.setVisibility(View.VISIBLE);
            panel.measure(View.MeasureSpec.makeMeasureSpec(root.getWidth(), View.MeasureSpec.AT_MOST), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            int height = panel.getMeasuredHeight(); panel.getLayoutParams().height = 0; panel.setAlpha(0f);
            android.animation.ValueAnimator animator = android.animation.ValueAnimator.ofInt(0, height);
            animator.setDuration(240); animator.addUpdateListener(value -> { panel.getLayoutParams().height = (Integer) value.getAnimatedValue(); panel.requestLayout(); panel.setAlpha(value.getAnimatedFraction()); });
            animator.addListener(new android.animation.AnimatorListenerAdapter() { public void onAnimationEnd(android.animation.Animator value) { panel.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT; panel.requestLayout(); } }); animator.start();
        } else {
            androidx.core.view.ViewCompat.animate(panel).alpha(0f).setDuration(140).withEndAction(() -> { panel.setVisibility(View.GONE); panel.setAlpha(1f); normal.setVisibility(View.VISIBLE); normal.setAlpha(0f); normal.animate().alpha(1f).setDuration(140).start(); }).start();
        }
    }
    private JSONObject appRequest() {
        JSONObject request = new JSONObject();
        try {
            request.put("app_id", app != null ? app.getId() : getArguments().getInt("APP_ID"));
            request.put("package_name", app != null ? app.getPackageName() : getArguments().getString("APP_PACKAGE", ""));
        } catch (Exception ignored) {}
        return request;
    }
    private void loadDetails() {
        if (root == null) return;
        int version = ++requestVersion;
        text(R.id.tvDetailsStatus, "Loading…"); root.findViewById(R.id.tvDetailsStatus).setVisibility(View.VISIBLE);
        StoreRepository.getInstance().requestAccountAction("store_app_details", appRequest(), session, (result, error) -> {
            if (root == null || version != requestVersion) return;
            AppItem loaded = result != null ? AppItem.fromJson(result.optJSONObject("app")) : null;
            if("NOT_FOUND".equals(error)){startActivity(new Intent(requireContext(),StoreNotFoundActivity.class));return;}
            if (error != null || loaded == null) {
                text(R.id.tvDetailsStatus, "Could not load details. Tap to retry.");
                root.findViewById(R.id.tvReviewsStatus).setVisibility(View.VISIBLE);text(R.id.tvReviewsStatus, "Could not load reviews. Tap to retry."); return;
            }
            app = loaded; StoreRepository.getInstance().updateApp(app);
            root.findViewById(R.id.tvDetailsStatus).setVisibility(View.GONE);
            ownReview = ReviewItem.fromJson(result.optJSONObject("my_review"));
            renderApp(); setReviews(result.optJSONArray("reviews"));
            renderComposer();
            if (!reviewDirty) {
                ReviewItem own = ownReview;
                selectedRating = own != null ? own.getRating() : 0;
                bindingReview=true;input().setText(own != null ? own.getReviewText() : "");bindingReview=false;reviewDirty = false; renderStars();
            }
        });
    }
    private void renderApp() {
        if (root == null || app == null) return;
        text(R.id.detailTitle, app.getTitle()); text(R.id.detailDev, app.getDeveloper());
        String description = app.getDescription();
        int count = description.codePointCount(0, description.length());
        text(R.id.tvAppDescription, count > 300 ? description.substring(0, description.offsetByCodePoints(0, 300)) + "…" : description);
        root.findViewById(R.id.descriptionMore).setVisibility(count > 300 ? View.VISIBLE : View.GONE); text(R.id.detailPrice, app.getPrice()); text(R.id.expandedPrice, app.getPrice());
        ((ImageView) root.findViewById(R.id.guardyBadge)).setImageResource(app.getGuardyBadgeResId());
        loadAvatar((ImageView) root.findViewById(R.id.ivDevIcon), "own".equals(app.getAppMode()) ? app.getPublisherAvatar() : app.getOriginalDevLogo());
        loadAvatar((ImageView) root.findViewById(R.id.ivPubIcon), app.getPublisherAvatar());
        android.graphics.drawable.GradientDrawable frame = new android.graphics.drawable.GradientDrawable();
        int ageColor = guardyColor(app.getGuardyRating());
        frame.setColor((ageColor & 0x00ffffff) | 0x18000000); frame.setCornerRadius(12 * getResources().getDisplayMetrics().density);
        frame.setStroke(Math.max(1, (int)getResources().getDisplayMetrics().density), ageColor);
        root.findViewById(R.id.guardyInfo).setBackgroundDrawable(frame);
        text(R.id.tvDevBlockName, app.getDeveloper()); text(R.id.tvPubBlockName, "@" + app.getPublisherUsername());
        root.findViewById(R.id.containerPubBlock).setOnClickListener(v->startActivity(new Intent(requireContext(),PublicProfileActivity.class).putExtra("username",app.getPublisherUsername()).putExtra("studio",false)));
        if("own".equals(app.getAppMode()))root.findViewById(R.id.containerDevBlock).setOnClickListener(v->startActivity(new Intent(requireContext(),PublicProfileActivity.class).putExtra("username",app.getPublisherUsername()).putExtra("studio",true)));

        if(!"own".equals(app.getAppMode()))root.findViewById(R.id.containerDevBlock).setOnClickListener(v->startActivity(new Intent(requireContext(),PublicProfileActivity.class).putExtra("username",app.getOriginalDev()).putExtra("creator",true)));
        boolean publisher = !"own".equals(app.getAppMode()) && !app.getDeveloper().equalsIgnoreCase("@" + app.getPublisherUsername());
        root.findViewById(R.id.containerPubBlock).setVisibility(publisher ? View.VISIBLE : View.GONE);
        root.findViewById(R.id.devDivider).setVisibility(publisher ? View.VISIBLE : View.GONE);
        text(R.id.tvRatingValue, app.getRatingCount() > 0 ? app.getRating() : "—"); text(R.id.tvDownloadsValue, app.getDownloads());
        int color = Color.parseColor(app.getRatingCount() == 0 ? "#888888" : app.getRatingFloat() >= 4.8f ? "#4CAF50" : app.getRatingFloat() < 4.4f ? "#FF5722" : "#00BCD4");
        ((TextView) root.findViewById(R.id.tvRatingValue)).setTextColor(color);
        ((ImageView) root.findViewById(R.id.iconRating)).setColorFilter(color);
        app.loadIconInto(requireContext(), root.findViewById(R.id.detailIcon), Math.round(12 * getResources().getDisplayMetrics().density));

        Spinner spinner = root.findViewById(R.id.androidVersionSpinner);
        ArrayAdapter<PackageVersionItem> adapter = new ArrayAdapter<PackageVersionItem>(requireContext(), R.layout.item_package_version, app.getPackages()) {
            @NonNull @Override public View getView(int position, View old, @NonNull ViewGroup parent) {
                TextView view = (TextView) super.getView(position, old, parent); view.setTextColor(Color.WHITE); view.setSingleLine(false); return view;
            }
        };
        adapter.setDropDownViewResource(R.layout.item_package_version); spinner.setAdapter(adapter);
        for (int i = 0; i < app.getPackages().size(); i++) if (app.getPackages().get(i).isCompatible()) { spinner.setSelection(i); break; }
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) { renderPermissions();renderInstallState(); }
            public void onNothingSelected(AdapterView<?> p) { renderInstallState(); }
        });
        renderFavorite();
        renderMedia();renderPermissions();renderInstallState();
    }
    private void renderPermissions(){
        if(root==null||app==null)return;
        LinearLayout list=root.findViewById(R.id.permissionsList);list.removeAllViews();
        Spinner spinner=root.findViewById(R.id.androidVersionSpinner);
        PackageVersionItem selected=(PackageVersionItem)spinner.getSelectedItem();
        List<String> permissions=selected!=null&&selected.hasPermissionMetadata()?selected.getPermissions():app.getPermissionsList();
        if(permissions.isEmpty()){TextView empty=new TextView(requireContext());empty.setText("No permissions listed in package metadata.");empty.setTextColor(Color.LTGRAY);list.addView(empty);return;}
        java.util.LinkedHashMap<String,java.util.LinkedHashSet<String>> groups=PermissionCatalog.group(permissions);
        for(java.util.Map.Entry<String,java.util.LinkedHashSet<String>> group:groups.entrySet()){
            PermissionCatalog.Entry entry=PermissionCatalog.describe(group.getValue().iterator().next());
            View row=LayoutInflater.from(requireContext()).inflate(R.layout.item_permission_group,list,false);
            ImageView icon=row.findViewById(R.id.permissionGroupIcon);icon.setImageResource(entry.icon);icon.setColorFilter(Color.CYAN);
            ((TextView)row.findViewById(R.id.permissionGroupTitle)).setText(entry.title);
            ((TextView)row.findViewById(R.id.permissionGroupDescription)).setText(entry.description);
            ((TextView)row.findViewById(R.id.permissionGroupCount)).setText(String.valueOf(group.getValue().size()));
            TextView identifiers=row.findViewById(R.id.permissionGroupIdentifiers);
            identifiers.setText(android.text.TextUtils.join("\n\n",group.getValue()));
            View header=row.findViewById(R.id.permissionGroupHeader),arrow=row.findViewById(R.id.permissionGroupArrow);
            String label=entry.title+", "+group.getValue().size()+(group.getValue().size()==1?" permission":" permissions");
            header.setContentDescription(label+", collapsed");
            header.setOnClickListener(v->{boolean expand=identifiers.getVisibility()!=View.VISIBLE;identifiers.setVisibility(expand?View.VISIBLE:View.GONE);arrow.animate().rotation(expand?180:0).setDuration(150).start();header.setContentDescription(label+(expand?", expanded":", collapsed"));});
            list.addView(row);
        }
    }

    private void renderFavorite(){ImageView button=root.findViewById(R.id.btnLike);androidx.core.widget.ImageViewCompat.setImageTintList(button,android.content.res.ColorStateList.valueOf(app.isFavorite()?Color.CYAN:Color.WHITE));button.setSelected(app.isFavorite());button.setContentDescription(app.isFavorite()?"Remove from favorites":"Add to favorites");}
    private int guardyColor(String rating) {
        return Color.parseColor(rating.startsWith("3") ? "#4CAF50" : rating.startsWith("7") ? "#8BC34A" : rating.startsWith("12") ? "#FFC107" : rating.startsWith("16") ? "#FF9800" : rating.startsWith("18") ? "#F44336" : "#9E9E9E");
    }
    private void loadAvatar(ImageView image, String source) {
        Object model = source;
        if (source != null && !source.isEmpty() && !source.startsWith("https://") && !source.startsWith("http://")) {
            try { model = android.util.Base64.decode(source.substring(source.indexOf(',') + 1), android.util.Base64.DEFAULT); } catch (Exception ignored) { model = null; }
        }
        Glide.with(this).load(model).placeholder(R.drawable.ic_profile_placeholder).error(R.drawable.ic_profile_placeholder).circleCrop().into(image);
    }
    private void renderMedia() {
        LinearLayout container = root.findViewById(R.id.screenshotsContainer); container.removeAllViews();
        ArrayList<String> urls = new ArrayList<>(app.getVideoUrls()); urls.addAll(app.getScreenshotUrls());
        int videos = app.getVideoUrls().size(); ((View) container.getParent()).setVisibility(urls.isEmpty() ? View.GONE : View.VISIBLE);
        for (int i = 0; i < urls.size(); i++) {
            final int position = i;
            View item = getLayoutInflater().inflate(R.layout.item_detail_media, container, false);
            if (i < videos) item.findViewById(R.id.mediaPlay).setVisibility(View.VISIBLE);
            ImageView thumbnail = item.findViewById(R.id.mediaThumbnail);
            String imageUrl = i < videos ? app.getVideoPoster(i) : urls.get(i);
            if (!imageUrl.isEmpty()) Glide.with(this).load(imageUrl)
                .transform(new com.bumptech.glide.load.resource.bitmap.FitCenter(), new com.bumptech.glide.load.resource.bitmap.RoundedCorners(Math.round(16 * getResources().getDisplayMetrics().density)))
                .override(960, 640).listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                    public boolean onLoadFailed(com.bumptech.glide.load.engine.GlideException error, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, boolean first) { return false; }
                    public boolean onResourceReady(android.graphics.drawable.Drawable image, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, com.bumptech.glide.load.DataSource source, boolean first) {
                        if (image.getIntrinsicHeight() > 0) { item.getLayoutParams().width = Math.round(item.getLayoutParams().height * image.getIntrinsicWidth() / (float) image.getIntrinsicHeight()); item.requestLayout(); }
                        return false;
                    }
                }).error(R.drawable.ic_screenshot_placeholder).into(thumbnail);
            item.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), MediaViewerActivity.class);
                intent.putStringArrayListExtra("MEDIA_URLS", urls); intent.putExtra("VIDEO_COUNT", videos); intent.putExtra("START_POSITION", position); startActivity(intent);
            }); container.addView(item);
        }
    }
    private void renderStars() {
        if (root == null) return;
        LinearLayout stars = root.findViewById(R.id.layoutReviewStars);
        for (int i = 0; i < stars.getChildCount(); i++) {
            ImageView star = (ImageView) stars.getChildAt(i);
            star.setImageResource(i < selectedRating ? R.drawable.ic_star_selected : R.drawable.ic_star_unselected);
            star.setColorFilter(i < selectedRating ? Color.CYAN : Color.WHITE);
        }
    }
    private void renderReviewAvatar() {
        if (root == null) return;
        Object image = session.isLoggedIn() ? session.getAvatarBitmap() : null;
        Glide.with(this).load(image != null ? image : R.drawable.ic_profile_placeholder).circleCrop().into((ImageView) root.findViewById(R.id.ivUserReviewAvatar));
        input().setHint(session.isLoggedIn() ? "Write or edit your review…" : "Sign in to write a review");
    }
    private void setReviews(JSONArray array) {
        reviews.clear();
        if (array != null) for (int i = 0; i < array.length(); i++) { ReviewItem review = ReviewItem.fromJson(array.optJSONObject(i)); if (review != null) reviews.add(review); }
        ((LinearLayout) root.findViewById(R.id.reviewsContainer)).removeAllViews(); shownReviews = 0;
        root.findViewById(R.id.tvReviewsStatus).setVisibility(reviews.isEmpty()?View.VISIBLE:View.GONE);text(R.id.tvReviewsStatus, reviews.isEmpty() ? "No reviews yet. Be the first to rate this app." : ""); appendReviews();
    }
    private void appendReviews() {
        LinearLayout container = root.findViewById(R.id.reviewsContainer); int end = Math.min(shownReviews + 10, reviews.size());
        while (shownReviews < end) {
            ReviewItem review = reviews.get(shownReviews++); View row = getLayoutInflater().inflate(R.layout.item_review, container, false);
            ((TextView) row.findViewById(R.id.reviewAuthor)).setText(review.getFormattedAuthor());
            row.findViewById(R.id.reviewAuthor).setOnClickListener(v->startActivity(new Intent(requireContext(),PublicProfileActivity.class).putExtra("username",review.getUsername())));
            LinearLayout stars = row.findViewById(R.id.reviewRating);
            int starSize = (int) (16 * getResources().getDisplayMetrics().density + 0.5f);
            for (int index = 1; index <= 5; index++) {
                androidx.appcompat.widget.AppCompatImageView star = new androidx.appcompat.widget.AppCompatImageView(requireContext());
                star.setImageResource(index <= review.getRating() ? R.drawable.ic_star_selected : R.drawable.ic_star_unselected);
                star.setColorFilter(Color.parseColor("#00BCD4"));
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(starSize, starSize);
                params.rightMargin = starSize / 4;
                stars.addView(star, params);
                androidx.core.view.ViewCompat.setImportantForAccessibility(star, androidx.core.view.ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO);
            }
            row.findViewById(R.id.reviewRating).setContentDescription(review.getRating() + " out of 5 stars");
            ((TextView) row.findViewById(R.id.reviewDate)).setText(review.getUpdatedAt());
            loadAvatar((ImageView) row.findViewById(R.id.reviewAvatar), review.getAvatarBase64());
            ((TextView) row.findViewById(R.id.reviewText)).setText(review.getReviewText());
            row.findViewById(R.id.reportReviewContent).setVisibility(View.GONE);
            addReviewActions((LinearLayout) row, review);
            if (ownReview!=null && ownReview.getId()>0 && ownReview.getId()==review.getId()) {
                android.graphics.drawable.GradientDrawable frame = new android.graphics.drawable.GradientDrawable(); frame.setColor(0x1400BCD4);frame.setCornerRadius(dp(12));frame.setStroke(dp(1),0xff00BCD4);row.setBackgroundDrawable(frame);row.setPadding(dp(12),dp(12),dp(12),dp(12));
                ImageButton edit = reviewAction(R.drawable.ic_edit, "Edit your review");
                edit.setOnClickListener(v -> { editingReview=true;selectedRating=review.getRating();input().setText(review.getReviewText());renderStars();renderComposer();input().requestFocus(); });
                ((LinearLayout)row.findViewById(R.id.reviewAuthor).getParent()).addView(edit);
            }
            if (!review.getReplyText().isEmpty()) {
                row.findViewById(R.id.reviewReply).setVisibility(View.VISIBLE);
                TextView author = row.findViewById(R.id.reviewReplyAuthor);
                boolean maintainer = "maintainer".equals(review.getReplyRole()) || "maintained".equals(review.getReplyRole());
                author.setText("@" + review.getReplyUsername() + " · " + (maintainer ? "Maintainer" : "Developer"));
                author.setTextColor(Color.parseColor(maintainer ? "#FF9800" : "#00BCD4"));
                author.setOnClickListener(v -> startActivity(new Intent(requireContext(), PublicProfileActivity.class).putExtra("username", review.getReplyUsername()).putExtra("studio", !maintainer)));
                ((TextView) row.findViewById(R.id.reviewReplyText)).setText(review.getReplyText());
                row.findViewById(R.id.reportReplyContent).setOnClickListener(v -> reportContent("reply", review.getId()));
            }
            container.addView(row);
        }
        root.findViewById(R.id.btnMoreReviews).setVisibility(shownReviews < reviews.size() ? View.VISIBLE : View.GONE);
    }
    private int dp(int value) { return Math.round(value*getResources().getDisplayMetrics().density); }
    private String draftKey(){return account+":"+(app!=null?app.getId():getArguments().getInt("APP_ID",0));}
    private void saveDraft(){
        if(root==null||!isAdded()||!reviewDirty||account.isEmpty())return;
        if(ownReview!=null&&!editingReview)return;
        requireContext().getSharedPreferences("ONStore_review_drafts",0).edit().putString(draftKey(),input().getText().toString()).putInt(draftKey()+":stars",selectedRating).apply();
    }
    private void restoreDraft(){
        if(account.isEmpty())return;
        android.content.SharedPreferences drafts=requireContext().getSharedPreferences("ONStore_review_drafts",0);
        if(drafts.contains(draftKey())){selectedRating=drafts.getInt(draftKey()+":stars",0);bindingReview=true;input().setText(drafts.getString(draftKey(),""));bindingReview=false;reviewDirty=true;editingReview=true;}
    }
    private void clearDraft(){requireContext().getSharedPreferences("ONStore_review_drafts",0).edit().remove(draftKey()).remove(draftKey()+":stars").apply();}
    private void renderComposer(){if(root!=null)root.findViewById(R.id.reviewComposer).setVisibility(ownReview!=null&&!editingReview?View.GONE:View.VISIBLE);}
    private ImageButton reviewAction(int icon,String label){
        ImageButton button=new androidx.appcompat.widget.AppCompatImageButton(requireContext());button.setImageResource(icon);button.setScaleType(ImageView.ScaleType.FIT_CENTER);button.setAdjustViewBounds(false);button.setColorFilter(Color.WHITE);button.setContentDescription(label);button.setBackgroundResource(R.drawable.bg_action_btn);button.setPadding(dp(11),dp(11),dp(11),dp(11));button.setLayoutParams(new LinearLayout.LayoutParams(dp(44),dp(44)));return button;
    }
    private void addReviewActions(LinearLayout row,ReviewItem review){
        LinearLayout actions=new LinearLayout(requireContext());actions.setGravity(android.view.Gravity.CENTER_VERTICAL);actions.setPadding(0,dp(8),0,dp(8));
        ImageButton report=reviewAction(R.drawable.ic_report,"Report review");report.setOnClickListener(v->reportContent("review",review.getId()));actions.addView(report);
        View spacer=new View(requireContext());actions.addView(spacer,new LinearLayout.LayoutParams(0,1,1));
        addEngagementAction(actions,review,R.drawable.ic_share,0,review.getShares(),"Share review");
        addEngagementAction(actions,review,R.drawable.ic_like,1,review.getLikes(),"Like review");
        addEngagementAction(actions,review,R.drawable.ic_dislike,-1,review.getDislikes(),"Dislike review");
        row.addView(actions);
    }
    private void addEngagementAction(LinearLayout actions,ReviewItem review,int icon,int reaction,int count,String label){
        ImageButton button=reviewAction(icon,label);button.setLayoutParams(new LinearLayout.LayoutParams(dp(36),dp(36)));button.setPadding(dp(8),dp(8),dp(8),dp(8));TextView value=new TextView(requireContext());value.setText(compactReviewCount(count));value.setContentDescription(count+" "+label);value.setTextSize(12);value.setTextColor(Color.WHITE);value.setPadding(dp(3),0,dp(6),0);actions.addView(button);actions.addView(value);
        if(reaction!=0&&review.getMyReaction()==reaction)button.setColorFilter(Color.CYAN);
        button.setOnClickListener(v->{
            if(reaction==0){Intent share=new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT,"https://oldschoolnetwork.online/store/?app_id="+app.getId()+"#review-"+review.getId());startActivity(Intent.createChooser(share,"Share review"));if(!session.isLoggedIn())return;}
            else if(!requireLogin())return;
            JSONObject request=new JSONObject();try{request.put("review_id",review.getId());if(reaction!=0)request.put("reaction",review.getMyReaction()==reaction?0:reaction);}catch(Exception ignored){}
            String owner=account;button.setEnabled(false);
            StoreRepository.getInstance().requestAccountAction(reaction==0?"store_review_share":"store_review_react",request,session,(result,error)->{if(root==null||!owner.equals(account))return;button.setEnabled(true);if(error!=null){toast(error);return;}JSONObject updated=result.optJSONObject("review");if(updated!=null){review.setEngagement(updated);int now=reaction==0?review.getShares():reaction==1?review.getLikes():review.getDislikes();value.setText(compactReviewCount(now));loadDetails();}});
        });
    }
    private String compactReviewCount(int count){return count<1000?String.valueOf(count):String.format(java.util.Locale.US,count<1000000?"%.1fK":"%.1fM",count<1000000?count/1000.0:count/1000000.0);}
    private boolean requireLogin() { if (session.isLoggedIn()) return true; toast("Sign in through the Profile tab first."); return false; }
    private void reportContent(String kind,int id){if(requireLogin())StoreReportDialog.show(requireContext(),session,kind,id);}
    private void submitReview() {
        if (app == null || submittingReview || !requireLogin()) return;
        if (selectedRating < 1) { toast("Select a rating from 1 to 5 stars."); return; }
        JSONObject request = appRequest();
        try { request.put("rating", selectedRating); request.put("review_text", input().getText().toString().trim()); } catch (Exception ignored) {}
        submittingReview = true; root.findViewById(R.id.btnSubmitReview).setEnabled(false); final String author = account;
        StoreRepository.getInstance().requestAccountAction("store_submit_review", request, session, (result, error) -> {
            submittingReview = false; if (root == null || !account.equals(author)) return;
            root.findViewById(R.id.btnSubmitReview).setEnabled(true); if (error != null) { toast(error); return; }
            app.setRatingStats(result.optString("rating_avg", "0.0"), result.optInt("rating_count", 0));
            StoreRepository.getInstance().updateApp(app); renderApp(); setReviews(result.optJSONArray("reviews")); reviewDirty = false;
            for (ReviewItem review : reviews) if (review.getUsername().equalsIgnoreCase(session.getUsername())) ownReview = review;
            editingReview = false;clearDraft();renderComposer();setReviews(result.optJSONArray("reviews"));loadDetails();
            toast(result.optString("message", "Review published."));
        });
    }
    private void changeFavorite() {
        if (app == null || changingFavorite || !requireLogin()) return;
        JSONObject request = appRequest(); try { request.put("favorite", !app.isFavorite()); } catch (Exception ignored) {}
        changingFavorite = true; final String owner = account;
        StoreRepository.getInstance().requestAccountAction("store_set_favorite", request, session, (result, error) -> {
            changingFavorite = false; if (root == null || !account.equals(owner)) return;
            if (error != null || !result.has("is_favorite")) { toast(error != null ? error : "Update the Store API on the server first."); return; }
            app.setFavorite(result.optBoolean("is_favorite")); renderFavorite();
            toast(app.isFavorite() ? "Added to favorites" : "Removed from favorites");
        });
    }
    private void showDescription() {
        if (app == null) return;
        android.app.Dialog dialog = new android.app.Dialog(requireContext());
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_description);
        ((TextView) dialog.findViewById(R.id.descriptionTitle)).setText(app.getTitle());
        ((TextView) dialog.findViewById(R.id.descriptionFullText)).setText(app.getDescription());
        dialog.findViewById(R.id.descriptionClose).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
        android.view.Window window = dialog.getWindow();
        if (window != null) { window.setBackgroundDrawableResource(android.R.color.transparent); window.addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND); window.setDimAmount(0.8f); window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT); }
    }
    private void showGuardyInfo() {
        String rating = app != null ? app.getGuardyRating() : "UR";
        String reasons = app != null ? android.text.TextUtils.join("\n", app.getGuardyReasons()) : "";
        new androidx.appcompat.app.AlertDialog.Builder(requireContext()).setTitle("ON Guardy · " + rating)
            .setMessage("ON Guardy is Oldschool Network's content and age rating system. It describes the recommended age and potentially sensitive content. It is separate from user reviews and the APK malware scan. UR means the application has not been rated yet.\n\n" + reasons)
            .setPositiveButton("OK", null).show();
    }
    private void reportApp(){if(app!=null&&requireLogin())StoreReportDialog.show(requireContext(),session,"app",app.getId());}
    private PackageVersionItem selectedPackage() { return root == null ? null : (PackageVersionItem) ((Spinner) root.findViewById(R.id.androidVersionSpinner)).getSelectedItem(); }
    private int installedVersion() {
        try { return requireContext().getPackageManager().getPackageInfo(app.getPackageName(), 0).versionCode; }
        catch (android.content.pm.PackageManager.NameNotFoundException ignored) { return -1; }
    }
    private PackageVersionItem newestCompatiblePackage() {
        PackageVersionItem newest = null;
        if (app != null) for (PackageVersionItem candidate : app.getPackages()) {
            if (candidate.isCompatible() && !candidate.getDownloadUrl().isEmpty()
                && (newest == null || candidate.getVersionCode() > newest.getVersionCode())) newest = candidate;
        }
        return newest;
    }
    private void renderInstallState() {
        if (root == null || app == null) return;
        int installedCode = installedVersion();
        boolean installed = installedCode >= 0;
        PackageVersionItem newest = newestCompatiblePackage();
        boolean updateAvailable = installed && newest != null && newest.getVersionCode() > installedCode;
        PackageVersionItem pkg = selectedPackage(); boolean supported = pkg != null && !pkg.getDownloadUrl().isEmpty() && pkg.isCompatible();
        Button button = root.findViewById(R.id.btnFinalDownload);
        button.setText(pendingApk != null && pkg != null && pendingPackageId == pkg.getId() ? "Install downloaded APK" : pkg == null ? "No APK available" : !supported ? "Not compatible" : "Install");
        boolean installing=silentInstalling||installerOpened;
        button.setEnabled(supported && download == null && !installing); button.setVisibility(download != null || installed || installing ? View.GONE : View.VISIBLE);
        root.findViewById(R.id.layoutInstalledAction).setVisibility(installed && download == null && !installing ? View.VISIBLE : View.GONE);
        root.findViewById(R.id.btnUpdateApp).setVisibility(updateAvailable ? View.VISIBLE : View.GONE);
        root.findViewById(R.id.layoutDownloadProgress).setVisibility(download != null || installing ? View.VISIBLE : View.GONE);
        ((Spinner) root.findViewById(R.id.androidVersionSpinner)).setEnabled(download == null && !installing);
        root.findViewById(R.id.btnCancelDownload).setVisibility(installing?View.GONE:View.VISIBLE);
        View sweep=root.findViewById(R.id.viewInstallSweep);sweep.setVisibility(installing?View.VISIBLE:View.GONE);
        if(installing){text(R.id.tvProgressText,"Installing…");View fill=root.findViewById(R.id.viewProgressFill);fill.getLayoutParams().width=ViewGroup.LayoutParams.MATCH_PARENT;fill.requestLayout();if(installSweep==null){installSweep=android.animation.ValueAnimator.ofFloat(-1f,1f);installSweep.setDuration(1200);installSweep.setRepeatCount(android.animation.ValueAnimator.INFINITE);installSweep.addUpdateListener(animation->{if(root!=null){View shine=root.findViewById(R.id.viewInstallSweep);shine.setTranslationX((Float)animation.getAnimatedValue()*((View)shine.getParent()).getWidth());}});installSweep.start();}}
        else if(installSweep!=null){installSweep.cancel();installSweep=null;}
        root.findViewById(R.id.onAccountSupport).setVisibility(pkg != null && pkg.supportsOnAccount() ? View.VISIBLE : View.GONE);
        text(R.id.tvPackageVerification, pkg != null ? pkg.getVtSummary() : "");
        ((TextView) root.findViewById(R.id.tvPackageVerification)).setTextColor(Color.parseColor(pkg != null && "clean".equals(pkg.getVtStatus()) ? "#4CAF50" : pkg != null && ("flagged".equals(pkg.getVtStatus()) || "malicious".equals(pkg.getVtStatus()) || "suspicious".equals(pkg.getVtStatus())) ? "#FF5722" : "#AAAAAA"));
        ((ImageView) root.findViewById(R.id.packageShield)).setColorFilter(((TextView) root.findViewById(R.id.tvPackageVerification)).getCurrentTextColor());
    }
    private boolean acquiring;
    private void startDownload() {
        if (app == null || acquiring || download != null) return;
        if (pendingApk != null && pendingApk.isFile() && selectedPackage() != null && pendingPackageId == selectedPackage().getId()) { launchInstaller(); return; }
        if (!session.isLoggedIn()) {
            if (!"Free".equals(app.getPrice())) { requireLogin(); return; }
            LibraryStore.remember(requireContext(), session, app); downloadPackage(); return;
        }
        acquiring = true; final String acquiringAccount = account;
        StoreRepository.getInstance().requestAccountAction("store_acquire_app", appRequest(), session, (result, error) -> {
            acquiring = false; if (root == null || !account.equals(acquiringAccount)) return;
            if (error != null || !"success".equals(result.optString("status"))) { toast(error != null ? error : "Server update required to add this app to Library."); return; }
            LibraryStore.remember(requireContext(), session, app); downloadPackage();
        });
    }
    private void downloadPackage() {
        if (download != null || app == null) return;
        if (pendingApk != null && pendingApk.isFile() && selectedPackage() != null && pendingPackageId == selectedPackage().getId()) { launchInstaller(); return; }
        PackageVersionItem pkg = selectedPackage(); if (pkg == null || !pkg.isCompatible()) return;
        final ApkDownloader task = new ApkDownloader(); task.setHeader("Authorization", "Bearer " + session.getToken()); task.setHeader("X-ON-ID", session.getOnId()); download = task;
        final android.content.Context context = requireContext().getApplicationContext(); final String packageName = app.getPackageName();
        transferNotification=new TransferNotification(context,4201,app.getTitle());
        renderInstallState(); text(R.id.tvProgressText, "Connecting…");
        new Thread(() -> {
            File file = null;
            try {
                file = task.download(pkg.getDownloadUrl(), new File(context.getCacheDir(), "downloads"), pkg.getSha256(), (bytes, total) -> main.post(() -> {
                    if (root == null || download != task) return;
                    if(transferNotification!=null)transferNotification.progress(bytes,total);
                    text(R.id.tvProgressText, total > 0 ? (bytes * 100 / total) + "% · " + String.format(Locale.US, "%.1f / %.1f MB", bytes / 1048576.0, total / 1048576.0) : String.format(Locale.US, "%.1f MB", bytes / 1048576.0));
                    View fill = root.findViewById(R.id.viewProgressFill); fill.getLayoutParams().width = total > 0 ? (int) (((View) fill.getParent()).getWidth() * bytes / total) : 0; fill.requestLayout();
                }));
                PackageInfo archive = context.getPackageManager().getPackageArchiveInfo(file.getAbsolutePath(), android.content.pm.PackageManager.GET_SIGNATURES);
                if (archive == null || !packageName.equals(archive.packageName) || archive.versionCode != pkg.getVersionCode()) throw new java.io.IOException("Downloaded APK does not match this application version");
                try {
                    PackageInfo existing = context.getPackageManager().getPackageInfo(packageName, android.content.pm.PackageManager.GET_SIGNATURES);
                    if (existing.versionCode > archive.versionCode) throw new java.io.IOException("A newer version is already installed.");
                    if (archive.signatures == null || !java.util.Arrays.equals(existing.signatures, archive.signatures)) throw new java.io.IOException("The signing certificate differs from the installed app.");
                } catch (android.content.pm.PackageManager.NameNotFoundException ignored) {}
                final File completed = file;
                main.post(() -> {
                    if (root == null || download != task || task.isCancelled()) { completed.delete(); return; }
                    download = null; pendingApk = completed; pendingPackageId = pkg.getId(); launchInstaller();
                });
            } catch (Exception error) {
                if (file != null) file.delete();
                main.post(() -> { if (root == null || download != task) return; download = null; renderInstallState(); if(transferNotification!=null)transferNotification.finish(); if (!task.isCancelled()) toast(error.getMessage()); });
            }
        }, "store-apk-download").start();
    }
    private void launchInstaller() {
        if (pendingApk == null || !pendingApk.isFile() || root == null) return;
        if(silentInstalling)return;
        if(InstallationStatus.complete(requireContext())){
            silentInstalling=true;renderInstallState();android.content.Context context=requireContext().getApplicationContext();File file=pendingApk;
            if(transferNotification==null)transferNotification=new TransferNotification(context,4201,app.getTitle());transferNotification.installing();
            new Thread(()->{String failure=null;try{PrivilegedInstaller.install(context,file);}catch(Exception error){failure=error.getMessage();}final String message=failure;main.post(()->{silentInstalling=false;if(transferNotification!=null)transferNotification.finish();if(root==null)return;renderInstallState();if(message==null){if(pendingApk!=null)pendingApk.delete();pendingApk=null;renderInstallState();toast("Application installed.");}else new androidx.appcompat.app.AlertDialog.Builder(requireContext()).setTitle("Automatic installation unavailable").setMessage(message).setPositiveButton("Use Android installer",(dialog,which)->launchAndroidInstaller()).setNegativeButton("Cancel",null).show();});},"store-privileged-install").start();return;
        }
        launchAndroidInstaller();
    }
    private void launchAndroidInstaller(){
        if(pendingApk==null||root==null)return;
        try {
            if (Build.VERSION.SDK_INT >= 26 && !requireContext().getPackageManager().canRequestPackageInstalls()) {
                toast("Allow installation from ON Store, then tap Install again.");
                startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + requireContext().getPackageName()))); return;
            }
            Uri uri;
            if (Build.VERSION.SDK_INT < 24) {
                pendingApk.getParentFile().setExecutable(true, false); requireContext().getCacheDir().setExecutable(true, false); pendingApk.setReadable(true, false);
                uri = Uri.fromFile(pendingApk);
            } else uri = FileProvider.getUriForFile(requireContext(), requireContext().getPackageName() + ".fileprovider", pendingApk);
            Intent intent = new Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            if (Build.VERSION.SDK_INT >= 16) intent.setClipData(android.content.ClipData.newRawUri("APK", uri));
            if(transferNotification==null)transferNotification=new TransferNotification(requireContext(),4201,app.getTitle());
            transferNotification.installing();installerOpened=true;renderInstallState();
            startActivity(intent);
        } catch (Exception error) { installerOpened=false;renderInstallState();if(transferNotification!=null)transferNotification.finish(); toast("Could not open installer: " + error.getMessage()); }
    }
    @Override public void onResume() { super.onResume(); if(installerOpened){installerOpened=false;if(transferNotification!=null)transferNotification.finish();} renderInstallState(); }
    @Override public void onSessionUpdated() {
        if (root == null) return; renderReviewAvatar();
        if (!account.equals(session.getAccountIdentifier())) {
            saveDraft(); ownReview=null;editingReview=false;account = session.getAccountIdentifier(); selectedRating = 0; bindingReview=true;input().setText("");bindingReview=false;reviewDirty = false;
            root.findViewById(R.id.btnSubmitReview).setEnabled(true); restoreDraft();renderComposer();renderStars();loadDetails();
        }
    }
    @Override public void onSaveInstanceState(@NonNull Bundle state) {
        super.onSaveInstanceState(state); state.putInt("review_rating", selectedRating); state.putBoolean("review_dirty", reviewDirty);
        if (root != null) state.putString("review_text", input().getText().toString());
    }
    @Override public void onDestroyView() {
        saveDraft();
        if(installSweep!=null){installSweep.cancel();installSweep=null;}
        if(transferNotification!=null)transferNotification.finish();
        requestVersion++; if (download != null) download.cancel(); download = null;
        if (session != null) session.removeListener(this); root = null; super.onDestroyView();
    }
}
