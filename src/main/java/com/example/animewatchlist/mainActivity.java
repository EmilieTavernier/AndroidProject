package com.example.animewatchlist;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.content.Intent;
import android.opengl.Visibility;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;

import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

// Java class for the mainActivity, whose purpose is to display lists of anime based on search criteria
public class mainActivity extends AppCompatActivity implements OnDataPass {
    private Location searchLocation = Location.JIKAN; // Possible location: JIKAN (api) or MY_LIST (bdd)
    private Criteria searchCriteria = Criteria.NAME;  // Possible criteria:
    private int currentRadioButton = Status.ALL;      // Possible status: ALL, WATCHLIST or COMPLETED
    private String currentCriteria = "by name";

    private JSONArray currentJsonArray;       // Holder for GET request to api result
    private List<Anime> currentBddAnimeArray; // List of anime in bdd
    private AnimeViewModel mAnimeViewModel;   // Access to database
    private ArrayList<String> idList;         // List of ids of currently displayed anime

    FragmentManager fragmentManager;
    SearchCriteriaSelectionFragment criteria_Frag;
    Boolean frag_visible;

    // Method to initialize all necessary components and data on activity creation
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        // Prepare radio button event listenner
        addRadioButton_listener();

        fragmentManager = this.getSupportFragmentManager();
        criteria_Frag = new SearchCriteriaSelectionFragment();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragContainer, criteria_Frag);
        fragmentTransaction.hide(criteria_Frag);
        frag_visible = false;
        fragmentTransaction.commit();

        // Initialize access to database and retrieve the list of all anime
        mAnimeViewModel = new ViewModelProvider(this).get(AnimeViewModel.class);
        mAnimeViewModel.getAllAnimes().observe(this, new Observer<List<Anime>>() {
            @Override
            public void onChanged(@Nullable final List<Anime> animes) {
                // Update the cached copy of the words in the adapter.
                currentBddAnimeArray = mAnimeViewModel.getAllAnimes().getValue();
                if( searchLocation == Location.MY_LIST ) updateMyAnimeListDisplay();
            }
        });
    }

    public void criteriaBtnAction(View view){
        updateCriteriaDisplay( true );
    }

    public void updateCriteriaDisplay( Boolean changeFragment ){
        Button btn = findViewById(R.id.criteriaBtn);

        if( !frag_visible ) {
            String updateText = currentCriteria + " ↓";
            btn.setText(updateText);
            if( changeFragment ) {
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.show(criteria_Frag);
                fragmentTransaction.commit();
                frag_visible = true;
            }
        }
        else {
            String updateText = currentCriteria + " ↑";
            btn.setText(updateText);
            if( changeFragment ) {
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.hide(criteria_Frag);
                fragmentTransaction.commit();
                frag_visible = false;
            }
        }
    }

    @Override
    public void onDataPass(String data) {
        currentCriteria = "by " + data;

        if( data.equalsIgnoreCase("name") )
            searchCriteria = Criteria.NAME;
        else if( data.equalsIgnoreCase("type") )
            searchCriteria = Criteria.TYPE;

        updateCriteriaDisplay( false );
    }

    // Method to update display when "Jikan" button is pressed
    public void switchToJikanSearchLocation(View view) throws JSONException {
        searchLocation = Location.JIKAN;

        EditText searchInput = findViewById(R.id.searchInput);
        searchInput.getText().clear();

        RadioGroup radioGroup = findViewById(R.id.radioGroup);
        radioGroup.setVisibility(view.GONE);

        Button button = findViewById(R.id.criteriaBtn);
        button.setEnabled(true);

        currentJsonArray = null;
        updateJikanAnimeListDisplay();
    }

    // Method to update display when "My anime" button is pressed
    public void switchToMyListSearchLocation(View view) {
        searchLocation = Location.MY_LIST;
        RadioGroup radioGroup = findViewById(R.id.radioGroup);
        radioGroup.setVisibility(view.VISIBLE);

        Button button = findViewById(R.id.criteriaBtn);
        button.setEnabled(false);

        EditText searchInput = findViewById(R.id.searchInput);
        searchInput.setText("by name ↑");
        searchInput.getText().clear();

        if( criteria_Frag != null && frag_visible ){
            RadioButton rb_name = findViewById(R.id.radioName);
            if(rb_name != null) rb_name.setChecked(true);
            RadioButton rb_type = findViewById(R.id.radioType);
            if(rb_type != null)rb_type.setChecked(false);

            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.hide(criteria_Frag);
            fragmentTransaction.commit();
            frag_visible = false;

            searchCriteria = Criteria.NAME;
            currentCriteria = "by name";
        }

        // TODO dynamic username
        currentBddAnimeArray = mAnimeViewModel.getAllAnimes().getValue();
        updateMyAnimeListDisplay();
    }

    public void requestDataToFillList(View view) throws ExecutionException, InterruptedException, JSONException {
        LinearLayout noResultLayout1 = findViewById(R.id.noResultLayout);
        LinearLayout noResultLayout2 = findViewById(R.id.noResultLayoutTYPE);
        noResultLayout1.setVisibility(View.GONE);
        noResultLayout2.setVisibility(View.GONE);

        EditText searchInput = findViewById(R.id.searchInput);

        if( searchLocation == Location.JIKAN ) {
            String requestUrl = "";
            String requestFilter = "";
            String resultKey = "";

            if( searchCriteria == Criteria.NAME )
                requestFilter = "?q=" + searchInput.getText().toString();
            else if ( searchCriteria == Criteria.TYPE )
                requestFilter = "?type=" + searchInput.getText().toString();

            requestUrl = "https://api.jikan.moe/v3/search/anime" + requestFilter;
            Log.i("DebugURL", "URL: " + requestUrl);

            resultKey = "results";

            JSONObject json = getRequest(requestUrl);
            if( json == null )
                Toast.makeText(getBaseContext(), "No internet connection", Toast.LENGTH_LONG).show();

            else {
                //Log.i("DebugResult", "result: " + json.toString());

                currentJsonArray = json.optJSONArray(resultKey);
                updateJikanAnimeListDisplay();
            }
        }
        else {
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

        if( result == null ) return null;
        return new JSONObject(result);
    }

    public void updateJikanAnimeListDisplay() throws JSONException {
        // Retrieving the list...
        ListView list = findViewById(R.id.animeListView);

        LinearLayout noResultLayout;
        if ( searchCriteria == Criteria.NAME )
            noResultLayout = findViewById(R.id.noResultLayout);
        else
            noResultLayout = findViewById(R.id.noResultLayoutTYPE);

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

        LinearLayout noResultLayout;
        if ( searchCriteria == Criteria.NAME )
            noResultLayout = findViewById(R.id.noResultLayout);
        else
            noResultLayout = findViewById(R.id.noResultLayoutTYPE);

        if( currentBddAnimeArray != null && currentBddAnimeArray.size() > 0 ) {
            noResultLayout.setVisibility(View.GONE);

            List<String> titles = new ArrayList<>();
            List<String> imagesURLs = new ArrayList<>();
            idList = new ArrayList<>();
            for (int i = 0; i < currentBddAnimeArray.size(); i++) {
                int status = currentBddAnimeArray.get(i).getStatus();

                if( currentRadioButton == Status.ALL || status == currentRadioButton ) {
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
                    currentRadioButton = Status.ALL;
                    radioButtonRequestUpdate();
            }
        };
        RadioButton rb_all = findViewById(R.id.radioBtnAll);
        rb_all.setOnClickListener(radioBtnAll_listener);

        // ** Radio button WATCHLIST **
        View.OnClickListener radioBtnWatchlist_listener = new View.OnClickListener (){
            public void onClick(View v) {
                currentRadioButton = Status.WATCHLIST;
                radioButtonRequestUpdate();
            }
        };
        RadioButton rb_watchlist = findViewById(R.id.radioBtnWatchlist);
        rb_watchlist.setOnClickListener(radioBtnWatchlist_listener);

        // ** Radio button COMPLETED **
        View.OnClickListener radioBtnCompleted_listener = new View.OnClickListener (){
            public void onClick(View v) {
                currentRadioButton = Status.COMPLETED;
                radioButtonRequestUpdate();
            }
        };
        RadioButton rb_completed = findViewById(R.id.radioBtnCompleted);
        rb_completed.setOnClickListener(radioBtnCompleted_listener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if( searchLocation == Location.MY_LIST ){
            currentBddAnimeArray = mAnimeViewModel.getAllAnimes().getValue();
            updateMyAnimeListDisplay();
        }
    }
}
