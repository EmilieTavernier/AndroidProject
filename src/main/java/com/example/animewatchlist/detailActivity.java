package com.example.animewatchlist;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;
import com.warkiz.widget.IndicatorSeekBar;
import com.warkiz.widget.OnSeekChangeListener;
import com.warkiz.widget.SeekParams;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ExecutionException;

// Java class for the detailActivity, whose purpose is to display a given anime's data
public class detailActivity extends AppCompatActivity {
    private Anime anime;                    // Selected anime data holder
    private AnimeViewModel animeViewModel;  // Access to database
    private ArrayList<String> bddListId;    // List of the anime's ids in saved in database

    // Permission to write to external storage (in our case, use to write in picture galleria)
    private static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1;

    // Light/night theme variables:
    private float previousLight = 100; // arbitrary (condition > 20)
    int oldBckgColor;
    int oldTextColor;
    int oldPrimaryTextColor;
    Boolean firstChange = true;

    // Method to initialize all necessary components and data on activity creation
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        // Getting the information pass by the previous activity:
        Bundle extras = getIntent().getExtras();
        String id = extras.getString("animeID");
        bddListId = extras.getStringArrayList("bddIdList");

        // Getting the components of the current activity:
        Button editWatchList = findViewById(R.id.addToWatchlist);
        TextView title = findViewById(R.id.title);
        TextView synopsis = findViewById(R.id.synopsisContent);
        ImageView imageView = findViewById(R.id.animePicture);
        IndicatorSeekBar seekBar = findViewById(R.id.customSeekBar);

        // Setting an access to my anime database:
        animeViewModel = new ViewModelProvider(this).get(AnimeViewModel.class);
        anime = null;

        // if the anime is already saved in the database (= in my own list of anime)
        if( isInMyAnimeList( id ) ){
            // The button "- Watchlist" will be to remove anime from bdd when clicked
            editWatchList.setText("- Watchlist");

            // Requesting the selected anime (by id) in database
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    anime = animeViewModel.getAnimeById( id );
                }
            });
            // REALLY UGLY way to wait for asynctask completion:
            while( anime == null ){
                Log.i("UselessLog", "Useless log" );
                // The log is to prevent the while loop from being ignore because empty (REALLY UGLY)
            }
        }
        // else if the anime is not in database, we will get it by requesting the jikan api
        else {
            // The button "+ Watchlist" will be to add the anime to the bdd when clicked
            editWatchList.setText("+ Watchlist");

            // Try requesting the API
            try {
                // Editing the GET request url ...
                String requestUrl = "https://api.jikan.moe/v3/anime/" + id;
                // ... and requesting the API with this url
                JSONObject json = getRequest(requestUrl);

                // ** EXTRACTING DATA FROM THE RESULT **
                // (title, synopsy, imageUrl and total number of episode
                String strTitle = "";
                strTitle = json.opt("title").toString();

                String strSynopsis = "";
                if (json.opt("synopsis") != null)
                    strSynopsis = json.opt("synopsis").toString();

                String imageUrl = "";
                if (json.opt("image_url") != null)
                    imageUrl = json.opt("image_url").toString();

                int intNbEpisode = 1;
                try {
                    if (json.opt("episodes") != null)
                        intNbEpisode = Integer.parseInt(json.opt("episodes").toString());
                } catch (java.lang.NumberFormatException e) {
                    Log.i("error", e.getMessage());
                }

                // Disable seekBar changed
                seekBar.setEnabled(false);

                // Instantiating an anime with the extracted data (lastSeenEpisode and status are given default value)
                anime = new Anime(id, strTitle, imageUrl, strSynopsis, intNbEpisode, 0, Status.WATCHLIST);

            } catch (JSONException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // Updating component fields with the current anime values:
        // (title and synopsis textViews, anime picture ImageView, and seekBar values)
        title.setText(anime.getTitle());
        synopsis.setText(anime.getSynopsis());
        if( !anime.getImgURL().equals("")){
            Picasso.with(this.getApplicationContext())
                    .load(anime.getImgURL())
                    .resize(500, 600)
                    .into(imageView);
        }
        seekBar.setMax( anime.getNpEpisode() );
        seekBar.setProgress( anime.getLastSeenEpisode() );

        // Managing events concerning the seekBar :
        seekBar.setOnSeekChangeListener(new OnSeekChangeListener() {
            @Override
            public void onSeeking(SeekParams seekParams) {} // Not use
            @Override
            public void onStartTrackingTouch(IndicatorSeekBar seekBar) {} // Not use

            // Method trigger when we release the seekbar cursor
            @Override
            public void onStopTrackingTouch(IndicatorSeekBar seekBar) {
                // We update the lastEpisodeSeen in the current anime
                animeViewModel.updateEpisode( seekBar.getProgress(), anime.getId() );

                // If the new progress is equal to the last episode...
                if( seekBar.getProgress() == seekBar.getMax() ){
                    // ... We update the status of the anime to completed ( = 2 )
                    animeViewModel.updateStatus( Status.COMPLETED, anime.getId() );
                    // and notify the user
                    Toast.makeText( getApplicationContext(), anime.getTitle() + " move to completed list", Toast.LENGTH_LONG).show();
                }
                // Else (if the new progress is inferior to the last episode
                else{
                    // Else if the status was completed ( = 2 )...
                    if( anime.getStatus() == Status.COMPLETED ){
                        // ... We change it to watching ( = 1 )
                        animeViewModel.updateStatus( Status.WATCHLIST, anime.getId() );
                        // and notify the user
                        Toast.makeText( getApplicationContext(), anime.getTitle() + " move to watching list", Toast.LENGTH_LONG).show();
                    }
                }
            }
        });

        // Clicking on the anime picture will save the picture in your phone picture galleria:
        ImageView selectImage = findViewById(R.id.animePicture);
        selectImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                askWriteToGallery();
            }
        });

        // ** LIGHT SENSOR **
        // https://stackoverflow.com/questions/17411562/android-light-sensor-detect-significant-light-changes

        // Instantiating a new event listener for the light sensor:
        SensorEventListener lightSensorListener = new SensorEventListener(){
            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {} // Not use

            // Method to trigger actions on sensor change
            @Override
            public void onSensorChanged(SensorEvent event) {
                // If the event comes from the light sensor ...
                if(event.sensor.getType() == Sensor.TYPE_LIGHT){
                    // ... If we get under an arbitrary threshold for "low luminosity" ...
                    if( previousLight > 20 && event.values[0] < 10 ){
                        // ... We store the current value of the sensor and trigger theme swapping
                        previousLight = event.values[0];
                        swapTheme(0); // To dark theme
                    }
                    // Else if we get over an arbitrary threshold for "low luminosity"
                    else if ( previousLight < 10 && event.values[0] > 20 ) {
                        // ... Same logic
                        previousLight = event.values[0];
                        swapTheme(1); // To light theme
                    }
                    //Log.i("LightSensorDebug", "LIGHT: " + event.values[0]);
                }
            }
        };

        // Retrieving the light sensor of the phone
        SensorManager mySensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        Sensor lightSensor = mySensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        // If successfully retrieve...
        if(lightSensor != null){
            // ... we register the sensor to the manager
            Log.i("LightSensorDebug", "Sensor.TYPE_LIGHT Available");
            mySensorManager.registerListener(
                    lightSensorListener,
                    lightSensor,
                    SensorManager.SENSOR_DELAY_NORMAL);
        }
        // Else we notify unavailability via Log message
        else {
            Log.i("LightSensorDebug", "Sensor.TYPE_LIGHT NOT Available");
        }
    }

    // Method to ask permission to save an image on your device
    public void askWriteToGallery(){
        // Check permission to write in an external storage (in our case photo galleria)
        // Ask if first time...
        if (ContextCompat.checkSelfPermission( this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE ) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
        }
        // ... else if permission was already granted save image
        else {
            savePictureInGallery();
        }
    }

    // Method to actually save a picture on your device
    public  void savePictureInGallery(){
        // Picture source : Image view in activity
        ImageView selectImage = findViewById(R.id.animePicture);
        // Convert the image to bitmap
        BitmapDrawable drawable = (BitmapDrawable) selectImage.getDrawable();
        Bitmap bitmap = drawable.getBitmap();

        // To greatly limit chance to have a duplicate name, we generate a big random number (not perfect):
        Random generator = new Random();
        int n = 10000;
        n = generator.nextInt(n);

        // Save the image in the galleria
        MediaStore.Images.Media.insertImage( getContentResolver(),
                bitmap,
                anime.getTitle() + n,
                "Image from myAnimeApp");
        // And notify the user with a toast
        Toast.makeText( getApplicationContext(), anime.getTitle() + " picture added to gallery", Toast.LENGTH_LONG).show();
    }

    // Method to trigger image saving when permission acquired
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE) {
            // If permission given ...
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // ... we save the image
                savePictureInGallery();
            } else {
                // else we notify permission denial
                Toast.makeText(getApplicationContext(), "Permission Denied, picture not saved", Toast.LENGTH_SHORT).show();
            }
            return;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    // Method to check if an anime is already saved in database (based on its id)
    public Boolean isInMyAnimeList( String id ){
        // We loop through all anime
        for(int i=0; i<bddListId.size(); i++){
            if( bddListId.get(i).equals(id) ){
                // the anime is already in database
                return true;
            }
        }
        // the anime is not in database
        return false;
    }

    // Method to request the API
    public JSONObject getRequest(String requestUrl) throws JSONException, ExecutionException, InterruptedException {
        //String to place our result in
        String result;
        //Instantiate new instance of our class
        HttpGetRequest getRequest = new HttpGetRequest();
        //Perform the doInBackground method, passing in our url
        result = getRequest.execute(requestUrl).get();
        // Return the result
        return new JSONObject(result);
    }

    // Method to add or remove and anime from bdd
    // ("addToWatchList" is a bad name and should be change when possible)
    public void addToWatchList(View view){
        Button editWatchList = findViewById(R.id.addToWatchlist);
        IndicatorSeekBar seekBar = findViewById(R.id.customSeekBar);

        // If the button is "- Watchlist" ...
        if( editWatchList.getText().equals("- Watchlist") ){
            // ... we delete the anime from bdd, notify the user and update activity components
            animeViewModel.deleteById( anime.getId() );
            Toast.makeText( getApplicationContext(), anime.getTitle() + " remove from watchlist", Toast.LENGTH_LONG).show();
            editWatchList.setText("+ Watchlist");
            seekBar.setProgress(0);
            seekBar.setEnabled(false);
        }
        // else (the button is "+ Watchlist") ...
        else {
            // ... we add the anime to bdd, notify the user and update activity components
            animeViewModel.insert( anime );
            Toast.makeText( getApplicationContext(), anime.getTitle() + " added to watchlist", Toast.LENGTH_LONG).show();
            editWatchList.setText("- Watchlist");
            seekBar.setEnabled(true);
        }
    }

    // Method to swap between light/drak theme
    public void swapTheme( int toTheme ){
        // Get components that will need an update
        View view = findViewById(R.id.scrollViewDetailedActivity);
        TextView title = findViewById(R.id.title);
        TextView synopsis = findViewById(R.id.synopsisContent);
        TextView synopsisTitle = findViewById(R.id.Synopsis);
        TextView ProgressTitle = findViewById(R.id.progress);

        // Save the current theme colors in temporary var
        int currentBckgColor = ((ColorDrawable)view.getBackground()).getColor();
        int currentTextColor = synopsis.getCurrentTextColor();
        int currentPrimaryTextColor = title.getCurrentTextColor();

        // If it is the first time we change theme (= to dark theme)...
        if( toTheme == 0 && firstChange ){
            // ... we define our dark theme colors (ugly dark theme)
            view.setBackgroundColor(Color.rgb(60, 60, 60));
            title.setTextColor(Color.rgb(255, 255, 255));
            synopsis.setTextColor(Color.rgb(255, 255, 255));
            synopsisTitle.setTextColor(Color.rgb(255, 255, 255));
            ProgressTitle.setTextColor(Color.rgb(255, 255, 255));
            firstChange = false;
        }
        // Else we update our theme with old save colors (default colors for light theme)
        else {
            view.setBackgroundColor(oldBckgColor);
            title.setTextColor(oldPrimaryTextColor);
            synopsis.setTextColor(oldTextColor);
            synopsisTitle.setTextColor(oldTextColor);
            ProgressTitle.setTextColor(oldTextColor);
        }
        // And update old colors with the colors of the previous theme (before change)
        oldBckgColor = currentBckgColor;
        oldTextColor = currentTextColor;
        oldPrimaryTextColor = currentPrimaryTextColor;
    }

    /*
    // CODE TO SELECT IMAGE FROM PHONE GALLERY (not use but works if uncommented)

    private int PICK_IMAGE_REQUEST = 1;

    public void chooseImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {

            Uri uri = data.getData();

            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                // Log.d(TAG, String.valueOf(bitmap));

                ImageView imageView = findViewById(R.id.animePicture);
                imageView.setImageBitmap(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    */
}
