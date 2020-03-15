package com.example.animewatchlist;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;

import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static android.app.Activity.*;

public class mainActivity extends AppCompatActivity {
    private int searchLocation = 0; // 0 = JINKAN, 1 = MY_LIST
    private int searchCriteria = 0; // 0 = NAME
    private int currentRadioButton = 0; // 0 = ALL, 1 = WATCHLIST, 2 = COMPLETED

    private JSONArray currentJsonArray;
    private List<Anime> currentBddAnimeArray;
    private AnimeViewModel mAnimeViewModel;
    private ArrayList<String> idList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        addRadioButton_listener();

        mAnimeViewModel = new ViewModelProvider(this).get(AnimeViewModel.class);
        mAnimeViewModel.getAllAnimes().observe(this, new Observer<List<Anime>>() {
            @Override
            public void onChanged(@Nullable final List<Anime> animes) {
                // Update the cached copy of the words in the adapter.
                currentBddAnimeArray = mAnimeViewModel.getAllAnimes().getValue();
                if( searchLocation == 1 ) updateMyAnimeListDisplay();
            }
        });

    }

    public void switchToJikanSearchLocation(View view) throws JSONException {
        searchLocation = 0;

        EditText searchInput = findViewById(R.id.searchInput);
        searchInput.getText().clear();

        RadioGroup radioGroup = findViewById(R.id.radioGroup);
        radioGroup.setVisibility(view.GONE);

        currentJsonArray = null;
        updateJikanAnimeListDisplay();
    }

    public void switchToMyListSearchLocation(View view) {
        searchLocation = 1;
        RadioGroup radioGroup = findViewById(R.id.radioGroup);
        radioGroup.setVisibility(view.VISIBLE);

        EditText searchInput = findViewById(R.id.searchInput);
        searchInput.getText().clear();

        // TODO dynamic username
        currentBddAnimeArray = mAnimeViewModel.getAllAnimes().getValue();
        updateMyAnimeListDisplay();
    }

    public void requestDataToFillList(View view) throws ExecutionException, InterruptedException, JSONException {
        EditText searchInput = findViewById(R.id.searchInput);

        if( searchLocation == 0 ) {
            String requestUrl = "";
            String requestFilter = "";
            String resultKey = "";

            if( searchCriteria == 0 ) requestFilter = "?q=" + searchInput.getText().toString();

            requestUrl = "https://api.jikan.moe/v3/search/anime" + requestFilter;
            Log.i("DebugURL", "URL: " + requestUrl);

            resultKey = "results";

            JSONObject json = getRequest(requestUrl);
            Log.i("DebugResult", "result: " + json.toString());

            currentJsonArray = json.optJSONArray(resultKey);
            updateJikanAnimeListDisplay();
        }
        else {
            //requestUrl = getBaseURL_forRadioButton() + requestFilter;
            //resultKey = "anime";
            currentBddAnimeArray = mAnimeViewModel.getAllAnimes().getValue();
            updateMyAnimeListDisplay();
        }
    }

    public JSONObject getRequest( String requestUrl ) throws JSONException, ExecutionException, InterruptedException {
        //String to place our result in
        String result;

        //Instantiate new instance of our class
        HttpGetRequest getRequest = new HttpGetRequest();

        //Perform the doInBackground method, passing in our url
        result = getRequest.execute(requestUrl).get();

        return new JSONObject(result);
    }

    public void updateJikanAnimeListDisplay() throws JSONException {
        // Retrieving the list...
        ListView list = findViewById(R.id.animeListView);
        // ... and clear it with an empty array adapter
        //ArrayAdapter<String> tableau = new ArrayAdapter<String>(list.getContext(), R.layout.my_text);
        //ArrayAdapter<String> tableau = new ArrayAdapter<>( list.getContext(), R.layout.anime_card, R.id.animeTitleInList );

        LinearLayout noResultLayout = findViewById(R.id.noResultLayout);

        if( currentJsonArray != null && currentJsonArray.length() > 0 ) {
            noResultLayout.setVisibility(View.GONE);

            List<String> titles = new ArrayList<>();
            List<String> imagesURLs = new ArrayList<>();
            for (int i = 0; i < currentJsonArray.length(); i++) {
                String title = currentJsonArray.getJSONObject(i).opt("title").toString();
                String imageURL = currentJsonArray.getJSONObject(i).opt("image_url").toString();
                titles.add(title);
                imagesURLs.add(imageURL);
                //Log.i("DebugTitles", "title: " + titles.get(i));
                //adaptater.add(jsonArray.getJSONObject(i).opt("title").toString());
            }
            ArrayList<String> idList = new ArrayList<>();
            for(int i = 0; i<currentBddAnimeArray.size(); i++ ) {
                String id = currentBddAnimeArray.get(i).getId();
                idList.add(id);
            }

            //list.setAdapter(adaptater);
            String[] arr1 = new String[titles.size()];
            String[] arr2 = new String[titles.size()];
            animeListAdapter adaptater = new animeListAdapter(this, titles.toArray(arr1), imagesURLs.toArray(arr2));

            list.setAdapter(adaptater);

            list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick( AdapterView<?> parent, View view, int position, long id ) {
                    int itemPosition = position;
                    String animeID;
                    try {
                        animeID = currentJsonArray.getJSONObject(position).opt("mal_id").toString();
                        Intent callDetailActivity = new Intent(getApplicationContext(), detailActivity.class);
                        callDetailActivity.putExtra("animeID", animeID);
                        callDetailActivity.putStringArrayListExtra("bddIdList", idList);
                        startActivity(callDetailActivity);
                        // Toast.makeText(getBaseContext(), "Item is at position: " + itemPosition, Toast.LENGTH_LONG).show();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
        else {
            list.setAdapter(null);
            noResultLayout.setVisibility(View.VISIBLE);
        }
    }

    public void updateMyAnimeListDisplay() {
        // Retrieving the list...
        ListView list = findViewById(R.id.animeListView);
        // ... and clear it with an empty adapter
        list.setAdapter(null);

        LinearLayout noResultLayout = findViewById(R.id.noResultLayout);

        if( currentBddAnimeArray != null && currentBddAnimeArray.size() > 0 ) {
            Log.i("updateAnime", "**********************************");

            noResultLayout.setVisibility(View.GONE);

            List<String> titles = new ArrayList<>();
            List<String> imagesURLs = new ArrayList<>();
            idList = new ArrayList<>();
            for (int i = 0; i < currentBddAnimeArray.size(); i++) {
                int status = currentBddAnimeArray.get(i).getStatus();

                if( currentRadioButton == 0 || status == currentRadioButton ) {
                    String title = currentBddAnimeArray.get(i).getTitle();
                    String imageURL = currentBddAnimeArray.get(i).getImgURL();
                    String id = currentBddAnimeArray.get(i).getId();
                    titles.add(title);
                    imagesURLs.add(imageURL);
                    idList.add(id);
                }
            }

            //list.setAdapter(adaptater);
            String[] arr1 = new String[titles.size()];
            String[] arr2 = new String[titles.size()];
            animeListAdapter adaptater = new animeListAdapter(this, titles.toArray(arr1), imagesURLs.toArray(arr2));

            list.setAdapter(adaptater);

            list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick( AdapterView<?> parent, View view, int position, long id) {
                    int itemPosition = position;
                    String animeID;
                    animeID = idList.get(position);

                    Intent callDetailActivity = new Intent(getApplicationContext(), detailActivity.class);
                    callDetailActivity.putExtra("animeID", animeID);
                    callDetailActivity.putStringArrayListExtra("bddIdList", idList);
                    startActivity(callDetailActivity);
                }
            });
        }
        else {
            noResultLayout.setVisibility(View.VISIBLE);
        }
    }

    public void radioButtonRequestUpdate() {
        currentBddAnimeArray = mAnimeViewModel.getAllAnimes().getValue();
        updateMyAnimeListDisplay();
    }

    private void addRadioButton_listener(){
        // ** Radio button ALL **
        View.OnClickListener radioBtnAll_listener = new View.OnClickListener (){
            public void onClick(View v) {
                    currentRadioButton = 0;
                    radioButtonRequestUpdate();
            }
        };
        RadioButton rb_all = findViewById(R.id.radioBtnAll);
        rb_all.setOnClickListener(radioBtnAll_listener);

        // ** Radio button WATCHLIST **
        View.OnClickListener radioBtnWatchlist_listener = new View.OnClickListener (){
            public void onClick(View v) {
                currentRadioButton = 1;
                radioButtonRequestUpdate();
            }
        };
        RadioButton rb_watchlist = findViewById(R.id.radioBtnWatchlist);
        rb_watchlist.setOnClickListener(radioBtnWatchlist_listener);

        // ** Radio button COMPLETED **
        View.OnClickListener radioBtnCompleted_listener = new View.OnClickListener (){
            public void onClick(View v) {
                currentRadioButton = 2;
                radioButtonRequestUpdate();
            }
        };
        RadioButton rb_completed = findViewById(R.id.radioBtnCompleted);
        rb_completed.setOnClickListener(radioBtnCompleted_listener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if( searchLocation == 1 ){
            currentBddAnimeArray = mAnimeViewModel.getAllAnimes().getValue();
            updateMyAnimeListDisplay();
        }
    }

}
