package com.example.animewatchlist;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface AnimeDao {
    // allowing the insert of the same word multiple times by passing a
    // conflict resolution strategy
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(Anime anime);

    @Query("DELETE FROM anime_table")
    void deleteAll();

    @Query("SELECT * from anime_table ORDER BY title ASC")
    LiveData<List<Anime>> getAlphabetizedWords();

    @Query("SELECT * from anime_table WHERE id = :id")
    Anime getAnimeById( String id );

    @Query("DELETE FROM anime_table WHERE id = :id")
    void deleteById( String id );

    @Query("UPDATE anime_table SET lastSeenEpisode = :progress WHERE id = :id")
    void updateEpisode( int progress, String id );

    @Query("UPDATE anime_table SET status = :status WHERE id = :id")
    void updateStatus( int status, String id );
}
