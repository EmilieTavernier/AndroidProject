package com.example.animewatchlist;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.ArrayList;

@Entity(tableName = "anime_table")
public class Anime {

    @PrimaryKey
    @NonNull
    private String id;
    @NonNull
    private String title;
    private String imgURL;
    private String synopsis;
    private int npEpisode;
    private int lastSeenEpisode;
    private int status; // 0 = watching, 1 = completed

    public Anime(@NonNull String id, @NonNull String title) {
        this.id = id;
        this.title = title;
    }

    public Anime(@NonNull String id, @NonNull String title, String imgURL, String synopsis,
                 int nbEpisode, int lastSeenEpisode, int status) {
        this.id = id;
        this.title = title;
        this.imgURL = imgURL;
        this.synopsis = synopsis;
        this.npEpisode = nbEpisode;
        this.lastSeenEpisode = lastSeenEpisode;
        this.status = status;
    }

    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    @NonNull
    public String getTitle() {
        return title;
    }

    public void setTitle(@NonNull String title) {
        this.title = title;
    }

    public String getImgURL() {
        return imgURL;
    }

    public void setImgURL(String imgURL) {
        this.imgURL = imgURL;
    }

    public String getSynopsis() {
        return synopsis;
    }

    public void setSynopsis(String synopsis) {
        this.synopsis = synopsis;
    }

    public int getNpEpisode() {
        return npEpisode;
    }

    public void setNpEpisode(int npEpisode) {
        this.npEpisode = npEpisode;
    }

    public int getLastSeenEpisode() {
        return lastSeenEpisode;
    }

    public void setLastSeenEpisode(int lastSeenEpisode) {
        this.lastSeenEpisode = lastSeenEpisode;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }
}
