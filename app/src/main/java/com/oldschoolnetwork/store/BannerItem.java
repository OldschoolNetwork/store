package com.oldschoolnetwork.store;

import org.json.JSONObject;

public class BannerItem {
    public static final int TYPE_INTERNAL_APP = 0;
    public static final int TYPE_EXTERNAL_URL = 1;

    private int id;
    private String title;
    private String imageUrl;
    private int imageResId;
    private int type;
    private String target;

    public BannerItem(int imageResId, int type, String target) {
        this.id = 0;
        this.title = "";
        this.imageUrl = "";
        this.imageResId = imageResId;
        this.type = type;
        this.target = target != null ? target : "";
    }

    public BannerItem(int id, String title, String imageUrl, int type, String target) {
        this.id = id;
        this.title = title != null ? title : "";
        this.imageUrl = imageUrl != null ? imageUrl : "";
        this.imageResId = 0;
        this.type = type;
        this.target = target != null ? target : "";
    }

    public static BannerItem fromJson(JSONObject obj) {
        if (obj == null) return null;
        int id = obj.optInt("id", 0);
        String title = obj.optString("title", "");
        String imageUrl = obj.optString("image_url", "");
        String targetUrl = obj.optString("target_url", "");
        return new BannerItem(id, title, imageUrl, TYPE_EXTERNAL_URL, targetUrl);
    }

    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getImageUrl() { return imageUrl; }
    public int getImageResId() { return imageResId; }
    public int getType() { return type; }
    public String getTarget() { return target; }
}