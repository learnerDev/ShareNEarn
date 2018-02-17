package com.learnerdev.sharenearn;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceDetectionClient;
import com.google.android.gms.location.places.PlaceLikelihood;
import com.google.android.gms.location.places.PlaceLikelihoodBufferResponse;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

public class SearchItems extends AppCompatActivity implements
        GoogleApiClient.OnConnectionFailedListener,
        GoogleApiClient.ConnectionCallbacks  {
    static final String TAG = "SNE-SearchItems";
    private static final int M_MAX_ENTRIES = 5;

    private EditText itemName;
    private EditText itemDetails;
    private AutoCompleteTextView itemAutoCompleteLocation;
    private ImageButton currentLocationBtn;
    private Button searchButton;
    private TextView tvStatus;
    private static final int GOOGLE_API_CLIENT_ID = 0;


    private GoogleApiClient mGoogleApiClient;
    PlaceDetectionClient mPlaceDetectionClient;

    private Place[] places;
    private String[] placeNames;
    private PlaceArrayAdapter mPlaceArrayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_items);
        initUI();
    }

    private void initUI() {
        itemName=findViewById(R.id.s_input_item_name);
        itemDetails=findViewById(R.id.s_input_item_details);
        itemAutoCompleteLocation=findViewById(R.id.s_input_auto_complete_location);
        currentLocationBtn=findViewById(R.id.s_btn_current_location);
        searchButton= findViewById(R.id.s_search_button);
        tvStatus=findViewById(R.id.s_tv_status);

        mPlaceDetectionClient = Places.getPlaceDetectionClient(this, null);

        //To get the necessary google api client for location auto complete and for likely places
        mGoogleApiClient = new GoogleApiClient.Builder(SearchItems.this)
                .addApi(Places.GEO_DATA_API)
                .addApi(LocationServices.API)
                .addOnConnectionFailedListener(this)
                .enableAutoManage(this, GOOGLE_API_CLIENT_ID, this)
                .addConnectionCallbacks(this)
                .build();


    }

    public void getNearByItems(){

    }

    private void getCurrentPlace() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(SearchItems.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    1000);
            ActivityCompat.requestPermissions(SearchItems.this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    1000);
            return;
        }
        Task<PlaceLikelihoodBufferResponse> placeResult = mPlaceDetectionClient.getCurrentPlace(null);
        placeResult.addOnCompleteListener(new OnCompleteListener<PlaceLikelihoodBufferResponse>() {
            @Override
            public void onComplete(@NonNull Task<PlaceLikelihoodBufferResponse> task) {
                if(task.isSuccessful() && task.getResult() != null){
                    PlaceLikelihoodBufferResponse likelyPlaces=task.getResult();
                    int count;
                    if (likelyPlaces.getCount() < M_MAX_ENTRIES) {
                        count = likelyPlaces.getCount();
                    } else {
                        count = M_MAX_ENTRIES;
                    }
                    int i = 0;
                    places=new Place[count];
                    placeNames = new String[count];
                    /*
                    * TODO
                    * Planning to search for all the nearby products here
                     */
                    for (PlaceLikelihood placeLikelihood : likelyPlaces) {
                        // Build a list of likely places to show the user.
                        places[i] =  placeLikelihood.getPlace();
                        placeNames[i]= placeLikelihood.getPlace().getName()+"";
                        Log.i(TAG,"Likely place "+i+": "+placeNames[i]);
                        i++;
                        if (i > (count - 1)) {
                            break;
                        }
                    }
//                    placeSelectDialogBox(likelyPlaces);
                }
            }
        });
    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}

