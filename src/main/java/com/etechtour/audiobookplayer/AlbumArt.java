package com.etechtour.audiobookplayer;

/**
 * Created by Phil on 5/21/2016.
 */
public class AlbumArt {
    public String title;
    public String url;
    public int id;

    public AlbumArt(int i, String t, String u) {
        this.title = t;
        this.url = u;
        this.id = i;
    }

    public String getTitle() {
        return this.title;
    }

    public String getUrl() {
        return this.url;
    }

    public int getID() {
        return this.id;
    }
}
