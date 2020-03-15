package com.example.animewatchlist;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

public class AnimeViewModel extends AndroidViewModel {

    private AnimeRepository mRepository;
    private LiveData<List<Anime>> mAllAnime;

    public AnimeViewModel (Application application) {
        super(application);
        mRepository = new AnimeRepository(application);
        mAllAnime = mRepository.getAllAnime();
    }

    LiveData<List<Anime>> getAllAnimes() { return mAllAnime; }

    public void insert(Anime anime) { mRepository.insert(anime); }
    public void deleteById(String id) { mRepository.deleteById(id); }
    public void updateEpisode(int progress, String id) { mRepository.updateEpisode(progress, id); }
    public void updateStatus(int status, String id) { mRepository.updateStatus(status, id); }
    public Anime getAnimeById(String id) { return mRepository.getAnimeById(id); }
}
