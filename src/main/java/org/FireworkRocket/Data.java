package org.FireworkRocket;

import com.google.gson.annotations.SerializedName;

public class Data {
    @SerializedName("uid")
    private long uid;

    @SerializedName("room_id")
    private long roomId;

    @SerializedName("live_status")
    private int liveStatus;

    @SerializedName("title")
    private String title;

    @SerializedName("user_cover")
    private String userCover;

    // Getters
    public long getUid() {
        return uid;
    }

    public long getRoomId() {
        return roomId;
    }

    public int getLiveStatus() {
        return liveStatus;
    }

    public String getTitle() {
        return title;
    }

    public String getUserCover() {
        return userCover;
    }
}