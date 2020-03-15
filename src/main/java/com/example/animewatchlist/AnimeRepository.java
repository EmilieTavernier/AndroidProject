package com.example.animewatchlist;

import android.app.Application;

import androidx.lifecycle.LiveData;

import java.util.List;

class AnimeRepository {

    private AnimeDao animeDao;
    private LiveData<List<Anime>> allAnime;

    // Note that in order to unit test the WordRepository, you have to remove the Application
    // dependency. This adds complexity and much more code, and this sample is not about testing.
    // See the BasicSample in the android-architecture-components repository at
    // https://github.com/googlesamples
    AnimeRepository(Application application) {
        AnimeRoomDatabase db = AnimeRoomDatabase.getDatabase(application);
        animeDao = db.animeDao();
        allAnime = animeDao.getAlphabetizedWords();
    }

    // Room executes all queries on a separate thread.
    // Observed LiveData will notify the observer when the data has changed.
    LiveData<List<Anime>>getAllAnime() {
        return allAnime;
    }

    // You must call this on a non-UI thread or your app will throw an exception. Room ensures
    // that you're not doing any long running operations on the main thread, blocking the UI.
    void insert(Anime anime) {
        AnimeRoomDatabase.databaseWriteExecutor.execute(() -> {
            animeDao.insert(anime);
        });
    }

    void deleteById(String id) {
        AnimeRoomDatabase.databaseWriteExecutor.execute(() -> {
            animeDao.deleteById(id);
        });
    }

    void updateEpisode(int progress, String id){
        AnimeRoomDatabase.databaseWriteExecutor.execute(() -> {
            animeDao.updateEpisode(progress, id);
        });
    }

    void updateStatus(int status, String id){
        AnimeRoomDatabase.databaseWriteExecutor.execute(() -> {
            animeDao.updateStatus(status, id);
        });
    }

    Anime getAnimeById(String id){
        return animeDao.getAnimeById(id);
    }
}