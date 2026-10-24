package com.oldschoolnetwork.store;

import org.json.JSONObject;

import java.io.Serializable;

public class ReviewItem implements Serializable {
    private int id;
    private int appId;
    private int userId;
    private String onId;
    private String username;
    private String userDisplayName;
    private String userSurname;
    private String avatarBase64;
    private int rating;
    private int likes, dislikes, shares, myReaction;
    private String reviewText;
    private String updatedAt;
    private String replyText = "", replyUsername = "", replyRole = "";

    public ReviewItem(int id, int appId, int userId, String onId, String username,
                      String userDisplayName, String userSurname, String avatarBase64,
                      int rating, String reviewText, String updatedAt) {
        this.id = id;
        this.appId = appId;
        this.userId = userId;
        this.onId = onId != null ? onId : "";
        this.username = username != null ? username : "user";
        this.userDisplayName = userDisplayName != null ? userDisplayName : "";
        this.userSurname = userSurname != null ? userSurname : "";
        this.avatarBase64 = avatarBase64 != null ? avatarBase64 : "";
        this.rating = rating;
        this.reviewText = reviewText != null ? reviewText : "";
        this.updatedAt = updatedAt != null ? updatedAt : "";
    }

    public static ReviewItem fromJson(JSONObject obj) {
        if (obj == null) return null;
        ReviewItem item = new ReviewItem(
                obj.optInt("id", 0),
                obj.optInt("app_id", 0),
                obj.optInt("user_id", 0),
                "",
                obj.optString("username", "user"),
                "",
                "",
                obj.optString("avatar_base64", ""),
                obj.optInt("rating", 5),
                obj.optString("review_text", ""),
                obj.optString("updated_at", "")
        );
        item.replyText = obj.isNull("reply_text") ? "" : obj.optString("reply_text", "");
        item.replyUsername = obj.isNull("reply_username") ? "" : obj.optString("reply_username", "");
        item.replyRole = obj.isNull("reply_role") ? "" : obj.optString("reply_role", "");
        item.setEngagement(obj);
        return item;
    }

    public void setEngagement(JSONObject obj) { likes=obj.optInt("likes",0);dislikes=obj.optInt("dislikes",0);shares=obj.optInt("shares",0);myReaction=obj.optInt("my_reaction",0); }
    public int getLikes(){return likes;} public int getDislikes(){return dislikes;} public int getShares(){return shares;} public int getMyReaction(){return myReaction;}
    public int getId() { return id; }
    public int getAppId() { return appId; }
    public int getUserId() { return userId; }
    public String getOnId() { return onId; }
    public String getUsername() { return username; }
    public String getUserDisplayName() { return userDisplayName; }
    public String getUserSurname() { return userSurname; }
    public String getAvatarBase64() { return avatarBase64; }
    public int getRating() { return rating; }
    public String getReviewText() { return reviewText; }
    public String getUpdatedAt() { return updatedAt; }
    public String getReplyText() { return replyText; }
    public String getReplyUsername() { return replyUsername; }
    public String getReplyRole() { return replyRole; }

    public String getFormattedAuthor() {
        return username.isEmpty() ? "ON User" : "@" + username;
    }
}
