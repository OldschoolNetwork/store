package com.oldschoolnetwork.store;

import android.content.Context;
import android.util.Base64;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AppItem implements Serializable {
    private int id;
    private String name;
    private String packageName;
    private String appMode; // "own" or "maintained"
    private String appType; // "game" or "app"
    private String networkType; // "online" or "offline"
    private String description;
    private final List<String> screenshotUrls = new ArrayList<>();
    private final List<String> videoUrls = new ArrayList<>();
    private final List<String> videoPosters = new ArrayList<>();
    private boolean owned;
    private double priceAmount;
    private java.util.List<String> installedDeviceIds = new java.util.ArrayList<>();
    public java.util.List<String> getInstalledDeviceIds() { return installedDeviceIds; }
    private boolean favorite;
    private boolean storeExtensions;
    private String iconUrl;
    private String publisherUsername;
    private String publisherStudio;
    private String publisherAvatar = "", originalDevLogo = "";
    public String getPublisherAvatar() { return publisherAvatar; }
    public String getOriginalDevLogo() { return originalDevLogo; }
    private String originalDev;
    private String originalDevDesc;
    private List<String> genresList;
    private List<String> permissionsList;
    private String guardyRating;
    private List<String> guardyReasons;
    private String ratingAvg;
    private int ratingCount;
    private int downloads;
    private String price;
    private int iconResId;
    private int[] screenshots;
    private List<PackageVersionItem> packages;
    private transient byte[] cachedIconBytes;

    public AppItem() {
        this.genresList = new ArrayList<>();
        this.permissionsList = new ArrayList<>();
        this.guardyReasons = new ArrayList<>();
        this.packages = new ArrayList<>();
        this.screenshots = new int[0];
        this.price = "Free";
    }

    public AppItem(String packageName, String title, String developer, String rating, String downloadsStr, String price, int iconResId, int[] screenshots) {
        this();
        this.packageName = packageName != null ? packageName : "";
        this.name = title != null ? title : "";
        this.originalDev = developer != null ? developer : "";
        this.publisherUsername = developer != null ? developer : "";
        this.publisherStudio = "";
        this.appMode = "own";
        this.appType = "app";
        this.networkType = "offline";
        this.description = "";
        this.iconUrl = "";
        this.guardyRating = "3";
        this.ratingAvg = rating != null ? rating : "0.0";
        this.ratingCount = 0;
        this.downloads = parseDownloadsString(downloadsStr);
        this.price = price != null ? price : "Free";
        this.iconResId = iconResId;
        this.screenshots = screenshots != null ? screenshots : new int[0];
    }

    public static AppItem fromJson(JSONObject obj) {
        if (obj == null) return null;
        AppItem item = new AppItem();
        item.id = obj.optInt("id", 0);
        item.name = obj.optString("name", "Untitled App");
        item.packageName = obj.optString("package_name", "");
        item.appMode = obj.optString("app_mode", "maintained");
        item.appType = obj.optString("app_type", "game");
        item.networkType = obj.optString("network_type", "online");
        item.description = obj.optString("description", "");
        item.favorite = obj.optBoolean("is_favorite", false);
        item.storeExtensions = obj.optBoolean("store_extensions", false);
        item.owned = obj.optBoolean("is_owned", false);
        JSONArray posters = obj.optJSONArray("video_posters");
        if (posters != null) for (int i = 0; i < posters.length(); i++) item.videoPosters.add(posters.optString(i, ""));
        JSONArray videos = obj.optJSONArray("videos");
        if (videos != null) for (int i = 0; i < videos.length(); i++) {
            String url = videos.optString(i, "");
            if (url.startsWith("https://") || url.startsWith("http://")) item.videoUrls.add(url);
        }
        JSONArray shots = obj.optJSONArray("screenshots");
        if (shots != null) for (int i = 0; i < shots.length(); i++) {
            String url = shots.optString(i, "");
            if (url.startsWith("https://") || url.startsWith("http://")) item.screenshotUrls.add(url);
        }
        item.iconUrl = obj.optString("icon_url", "");
        if (item.iconUrl != null && item.iconUrl.startsWith("data:")) {
            try {
                int commaIdx = item.iconUrl.indexOf(",");
                String b64 = commaIdx >= 0 ? item.iconUrl.substring(commaIdx + 1) : item.iconUrl;
                item.cachedIconBytes = Base64.decode(b64, Base64.DEFAULT);
            } catch (Throwable ignored) {}
        }
        org.json.JSONArray deviceIds = obj.optJSONArray("installed_device_ids");
        if (deviceIds != null) for (int n=0;n<deviceIds.length();n++) item.installedDeviceIds.add(deviceIds.optString(n));
        String avatarKey="own".equalsIgnoreCase(item.getAppMode())?"publisher_studio_logo":"publisher_person_avatar";
        item.publisherAvatar = obj.isNull(avatarKey)?"":obj.optString(avatarKey, "");
        item.originalDevLogo = obj.optString("original_dev_logo", "");
        item.publisherUsername = obj.optString("publisher_username", "developer");
        item.publisherStudio = obj.optString("publisher_studio", "");
        item.originalDev = obj.optString("original_dev", "@" + item.publisherUsername);
        item.originalDevDesc = obj.optString("original_dev_desc", "");
        item.guardyRating = obj.optString("guardy_rating", "UR");
        item.ratingAvg = obj.optString("rating_avg", "0.0");
        item.ratingCount = obj.optInt("rating_count", 0);
        item.downloads = obj.optInt("downloads", 0);
        double amount = obj.optDouble("price_amount", 0); item.priceAmount = amount;
        item.price = amount <= 0 ? "Free" : String.format(Locale.US, "$%.2f", amount);

        JSONArray genresArr = obj.optJSONArray("genres_list");
        if (genresArr != null) {
            for (int i = 0; i < genresArr.length(); i++) {
                String g = genresArr.optString(i, "").trim();
                if (!g.isEmpty()) item.genresList.add(g);
            }
        } else {
            String genresStr = obj.optString("genres", "");
            if (!genresStr.isEmpty()) {
                for (String g : genresStr.split(",")) {
                    if (!g.trim().isEmpty()) item.genresList.add(g.trim());
                }
            }
        }

        JSONArray permsArr = obj.optJSONArray("permissions_list");
        if (permsArr != null) {
            for (int i = 0; i < permsArr.length(); i++) {
                String p = permsArr.optString(i, "").trim();
                if (!p.isEmpty()) item.permissionsList.add(p);
            }
        } else {
            String permsStr = obj.optString("permissions", "");
            if (!permsStr.isEmpty()) {
                for (String p : permsStr.split(",")) {
                    if (!p.trim().isEmpty()) item.permissionsList.add(p.trim());
                }
            }
        }

        JSONArray reasonsArr = obj.optJSONArray("guardy_reasons");
        if (reasonsArr != null) {
            for (int i = 0; i < reasonsArr.length(); i++) {
                String r = reasonsArr.optString(i, "").trim();
                if (!r.isEmpty()) item.guardyReasons.add(r);
            }
        }

        JSONArray pkgsArr = obj.optJSONArray("packages");
        if (pkgsArr != null) {
            for (int i = 0; i < pkgsArr.length(); i++) {
                PackageVersionItem pkg = PackageVersionItem.fromJson(pkgsArr.optJSONObject(i));
                if (pkg != null) item.packages.add(pkg);
            }
        }

        return item;
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put("id", id); json.put("name", name); json.put("package_name", packageName);
            json.put("description", description); json.put("app_mode", appMode); json.put("app_type", appType);
            json.put("publisher_studio_logo", publisherAvatar); json.put("publisher_person_avatar", publisherAvatar); json.put("original_dev_logo", originalDevLogo);
            json.put("icon_url", iconUrl); json.put("publisher_username", publisherUsername); json.put("publisher_studio", publisherStudio);
            json.put("original_dev", originalDev); json.put("rating_avg", ratingAvg); json.put("rating_count", ratingCount); json.put("downloads", downloads);
            json.put("guardy_rating", guardyRating); json.put("guardy_reasons", new JSONArray(guardyReasons));
            json.put("genres_list", new JSONArray(genresList)); json.put("permissions_list", new JSONArray(permissionsList));
            json.put("screenshots", new JSONArray(screenshotUrls)); json.put("videos", new JSONArray(videoUrls)); json.put("video_posters", new JSONArray(videoPosters));
            json.put("installed_device_ids", new org.json.JSONArray(installedDeviceIds));
            json.put("is_owned", owned); json.put("price_amount", priceAmount);
            JSONArray versions = new JSONArray();
            for (PackageVersionItem pkg : packages) {
                JSONObject version = new JSONObject();
                version.put("supports_on_account", pkg.supportsOnAccount()); version.put("id", pkg.getId()); version.put("file_name", pkg.getFileName()); version.put("version_name", pkg.getVersionName()); version.put("version_code", pkg.getVersionCode());
                if(pkg.hasPermissionMetadata())version.put("permissions_list",new JSONArray(pkg.getPermissions()));version.put("supported_abis",new JSONArray(pkg.getSupportedAbis()));
                version.put("size_label", pkg.getSizeLabel()); version.put("min_android", pkg.getMinAndroid()); version.put("download_url", pkg.getDownloadUrl()); version.put("sha256", pkg.getSha256()); version.put("vt_summary", pkg.getVtSummary()); version.put("vt_status", pkg.getVtStatus()); versions.put(version);
            }
            json.put("packages", versions);
        } catch (Exception ignored) {}
        return json;
    }

    private static int parseDownloadsString(String d) {
        if (d == null) return 0;
        String clean = d.replace("+", "").trim().toUpperCase(Locale.US);
        try {
            if (clean.endsWith("M")) return (int) (Float.parseFloat(clean.replace("M", "")) * 1000000);
            if (clean.endsWith("K")) return (int) (Float.parseFloat(clean.replace("K", "")) * 1000);
            return Integer.parseInt(clean);
        } catch (Exception e) {
            return 0;
        }
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getTitle() { return name; }
    public String getPackageName() { return packageName; }
    public String getAppMode() { return appMode != null ? appMode : "maintained"; }
    public String getAppType() { return appType != null ? appType : "game"; }
    public boolean isGame() { return "game".equalsIgnoreCase(getAppType()); }
    public String getNetworkType() { return networkType != null ? networkType : "online"; }
    public String getDescription() { return description != null && !description.isEmpty() ? description : "No description provided."; }
    public List<String> getScreenshotUrls() { return screenshotUrls; }
    public List<String> getVideoUrls() { return videoUrls; }
    public String getVideoPoster(int index) { return index < videoPosters.size() && !videoPosters.get(index).isEmpty() ? videoPosters.get(index) : screenshotUrls.isEmpty() ? "" : screenshotUrls.get(0); }
    public double getPriceAmount() { return priceAmount; }
    public boolean isOwned() { return owned; }
    public boolean isFavorite() { return favorite; }
    public void setFavorite(boolean value) { favorite = value; }
    public boolean hasStoreExtensions() { return storeExtensions; }
    public String getIconUrl() { return iconUrl != null ? iconUrl : ""; }
    public String getPublisherUsername() { return publisherUsername != null ? publisherUsername : "developer"; }
    public String getPublisherStudio() { return publisherStudio != null ? publisherStudio : ""; }
    public String getOriginalDev() { return originalDev != null ? originalDev : ("@" + getPublisherUsername()); }
    public String getOriginalDevDesc() { return originalDevDesc != null ? originalDevDesc : ""; }
    public List<String> getGenresList() { return genresList; }
    public List<String> getPermissionsList() { return permissionsList; }
    public String getGuardyRating() { return guardyRating != null && !guardyRating.isEmpty() ? guardyRating : "UR"; }
    public List<String> getGuardyReasons() { return guardyReasons; }
    public String getRatingAvg() { return ratingAvg != null && !ratingAvg.isEmpty() ? ratingAvg : "0.0"; }
    public int getRatingCount() { return ratingCount; }
    public int getDownloadsCount() { return downloads; }
    public List<PackageVersionItem> getPackages() { return packages; }
    public String getPrice() { return price != null ? price : "Free"; }
    public int getIconResId() { return iconResId; }
    public int[] getScreenshots() { return screenshots; }

    public void setRatingStats(String ratingAvg, int ratingCount) {
        this.ratingAvg = ratingAvg;
        this.ratingCount = ratingCount;
    }

    public void setDownloads(int downloads) {
        this.downloads = downloads;
    }

    public String getDeveloper() {
        if ("own".equalsIgnoreCase(getAppMode())) {
            String studio = getPublisherStudio().trim();
            if (!studio.isEmpty()) {
                return studio;
            }
            return "Developer studio";
        } else {
            String orig = getOriginalDev().trim();
            if (!orig.isEmpty()) {
                return orig;
            }
            return "@" + getPublisherUsername();
        }
    }

    public String getRating() {
        return getRatingAvg();
    }

    public float getRatingFloat() {
        try {
            return Float.parseFloat(getRatingAvg());
        } catch (Exception e) {
            return 0f;
        }
    }

    public String getDownloads() {
        if (downloads >= 1000000) {
            return String.format(Locale.US, "%.1fM", downloads / 1000000.0f);
        } else if (downloads >= 1000) {
            return String.format(Locale.US, "%.1fk", downloads / 1000.0f);
        }
        return String.valueOf(downloads);
    }

    public int getMinSdkInt() {
        if (packages == null || packages.isEmpty()) return 14;
        int min = Integer.MAX_VALUE;
        for (PackageVersionItem p : packages) {
            int sdk = p.getMinSdkInt();
            if (sdk < min) min = sdk;
        }
        return min == Integer.MAX_VALUE ? 14 : min;
    }

    public float getPrimarySizeMb() {
        if (packages == null || packages.isEmpty()) return 0f;
        return packages.get(0).getSizeMbFloat();
    }

    public int getGuardyBadgeResId() {
        String r = getGuardyRating().trim().toUpperCase(Locale.US);
        if (r.contains("18")) return R.drawable.gua_18;
        if (r.contains("16")) return R.drawable.gua_16;
        if (r.contains("12")) return R.drawable.gua_12;
        if (r.contains("7")) return R.drawable.gua_7;
        if (r.contains("3")) return R.drawable.gua_3;
        return R.drawable.gua_ur;
    }

    public void loadIconInto(Context context, ImageView imageView, int cornerRadiusPx) {
        if (context == null || imageView == null) return;
        try {
            String url = getIconUrl();
            if (url != null && !url.isEmpty()) {
                if (url.startsWith("data:")) {
                    byte[] bytes = cachedIconBytes;
                    if (bytes == null) {
                        int commaIdx = url.indexOf(",");
                        String b64 = commaIdx >= 0 ? url.substring(commaIdx + 1) : url;
                        bytes = Base64.decode(b64, Base64.DEFAULT);
                        cachedIconBytes = bytes;
                    }
                    if (bytes != null && bytes.length > 0) {
                        if (cornerRadiusPx > 0) {
                            Glide.with(context)
                                    .load(bytes)
                                    .signature(new com.bumptech.glide.signature.ObjectKey("app_icon_" + getId()))
                                    .override(128, 128)
                                    .placeholder(R.drawable.ic_app_icon_placeholder)
                                    .error(R.drawable.ic_app_icon_placeholder)
                                    .transform(new CenterCrop(), new RoundedCorners(cornerRadiusPx))
                                    .into(imageView);
                        } else {
                            Glide.with(context)
                                    .load(bytes)
                                    .signature(new com.bumptech.glide.signature.ObjectKey("app_icon_" + getId()))
                                    .override(128, 128)
                                    .placeholder(R.drawable.ic_app_icon_placeholder)
                                    .error(R.drawable.ic_app_icon_placeholder)
                                    .centerCrop()
                                    .into(imageView);
                        }
                        return;
                    }
                } else {
                    if (cornerRadiusPx > 0) {
                        Glide.with(context)
                                .load(url)
                                .override(128, 128)
                                .placeholder(R.drawable.ic_app_icon_placeholder)
                                .error(R.drawable.ic_app_icon_placeholder)
                                .transform(new CenterCrop(), new RoundedCorners(cornerRadiusPx))
                                .into(imageView);
                    } else {
                        Glide.with(context)
                                .load(url)
                                .override(128, 128)
                                .placeholder(R.drawable.ic_app_icon_placeholder)
                                .error(R.drawable.ic_app_icon_placeholder)
                                .centerCrop()
                                .into(imageView);
                    }
                    return;
                }
            }
            if (iconResId != 0) {
                Glide.with(context)
                        .load(iconResId)
                        .override(128, 128)
                        .placeholder(R.drawable.ic_app_icon_placeholder)
                        .transform(new CenterCrop(), new RoundedCorners(Math.max(1, cornerRadiusPx)))
                        .into(imageView);
            } else {
                imageView.setImageResource(R.drawable.ic_app_icon_placeholder);
            }
        } catch (Throwable e) {
            imageView.setImageResource(R.drawable.ic_app_icon_placeholder);
        }
    }
}
