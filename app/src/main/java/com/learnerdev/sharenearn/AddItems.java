package com.learnerdev.sharenearn;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.backendless.Backendless;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.exceptions.BackendlessFault;
import com.backendless.geo.GeoPoint;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.PlaceDetectionClient;
import com.google.android.gms.location.places.PlaceLikelihood;
import com.google.android.gms.location.places.PlaceLikelihoodBufferResponse;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;

import static com.learnerdev.sharenearn.Defaults.EARTH;
import static com.learnerdev.sharenearn.Defaults.GEO_POINTS_CATEGORY_NAME;

public class AddItems extends AppCompatActivity implements
        GoogleApiClient.OnConnectionFailedListener,
        GoogleApiClient.ConnectionCallbacks, LocationListener {
    private static final int M_MAX_ENTRIES = 5;
    static String TAG = "AddItemsActivity";
    private static final int GOOGLE_API_CLIENT_ID = 0;
    private Button saveButton;
    private EditText itemName;
    //    private AutoCompleteTextView itemLocation;
    private EditText itemDetails;
    private GeoPoint itemLocation;
    private AutoCompleteTextView itemAutoCompleteLocation;
    private TextView tvStatus;//TODO this text view is only for debug purposes
    //    private String locationTextBuffer;
    private GoogleApiClient mGoogleApiClient;
    private GoogleApiClient lGoogleApiClient;
    PlaceDetectionClient mPlaceDetectionClient;


    private PlaceArrayAdapter mPlaceArrayAdapter;
    Location mLastLocation;
    LocationRequest mLocationRequest;
    private ImageButton getCurrentLocationBtn;
    private ArrayAdapter<String> adapter;
    private Place[] places;
    private String[] placeNames;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_items);
        initUI();

    }

    private void initUI() {
        itemName = findViewById(R.id.input_item_name);
//        itemLocation=(AutoCompleteTextView) findViewById(R.id.input_item_location);
        itemDetails = findViewById(R.id.input_item_details);
        saveButton = findViewById(R.id.save_button);
        itemAutoCompleteLocation = findViewById(R.id.input_auto_complete_location);
        tvStatus = findViewById(R.id.tv_status);
        getCurrentLocationBtn = findViewById(R.id.btn_current_location);

        mPlaceDetectionClient = Places.getPlaceDetectionClient(this, null);

        //To get the necessary google api client for location auto complete and for likely places
        mGoogleApiClient = new GoogleApiClient.Builder(AddItems.this)
                .addApi(Places.GEO_DATA_API)
                .addApi(LocationServices.API)
                .addOnConnectionFailedListener(this)
                .enableAutoManage(this, GOOGLE_API_CLIENT_ID, this)
                .addConnectionCallbacks(this)
                .build();

        //To check connectivity
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        } else {
            Toast.makeText(this, "Not Connected!", Toast.LENGTH_SHORT).show();
            Log.i(TAG,"Device not connected");
        }

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveItem();
            }
        });
        getCurrentLocationBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getCurrentPlace();
            }
        });

        itemAutoCompleteLocation.setOnItemClickListener(itemAutoCompleteClickListener);
        mPlaceArrayAdapter = new PlaceArrayAdapter(this, android.R.layout.simple_list_item_1, EARTH, null);
        itemAutoCompleteLocation.setAdapter(mPlaceArrayAdapter);
    }

    //TODO 1. Rearrange( Related to getting current place, taken from internet, understand and re-arrange the code)
    public void settingRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);    // 10 seconds, in milliseconds
        mLocationRequest.setFastestInterval(1000);   // 1 second, in milliseconds
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);

        //SettingsApi deprecated, find alternative or possible work-around
        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient,
                        builder.build());

        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {

            @Override
            public void onResult(@NonNull LocationSettingsResult result) {
                final Status status = result.getStatus();
                final LocationSettingsStates state = result.getLocationSettingsStates();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        // All location settings are satisfied. The client can
                        // initialize location requests here.
                        getLocation();
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        // Location settings are not satisfied, but this can be fixed
                        // by showing the user a dialog.
                        try {
                            // Show the dialog by calling startResolutionForResult(),
                            // and check the result in onActivityResult().
                            status.startResolutionForResult(AddItems.this, 1000);

                        } catch (IntentSender.SendIntentException e) {
                            // Ignore the error.

                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        // Location settings are not satisfied. However, we have no way
                        // to fix the settings so we won't show the dialog.
                        break;
                }
            }

        });
    }

    //TODO 2. Rearrange( Related to getting current place, taken from internet, understand and re-arrange the code)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        final LocationSettingsStates states = LocationSettingsStates.fromIntent(data);
        switch (requestCode) {
            case 1000:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        // All required changes were successfully made
                        getLocation();
                        break;
                    case Activity.RESULT_CANCELED:
                        // The user was asked to change settings, but chose not to
                        Toast.makeText(this, "Location Service not Enabled", Toast.LENGTH_SHORT).show();
                        break;
                    default:
                        break;
                }
                break;
        }
    }
    //TODO 3. Rearrange( Related to getting current place, taken from internet, understand and re-arrange the code)
    public void getLocation() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(AddItems.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    1000);
            ActivityCompat.requestPermissions(AddItems.this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    1000);
            return;
        } else {
            /*Getting the location after acquiring location service*/
            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                    mGoogleApiClient);
            if (mLastLocation != null) {
                tvStatus.setText("Latitude: " + String.valueOf(mLastLocation.getLatitude()));
                tvStatus.append("\nLongitude: " + String.valueOf(mLastLocation.getLongitude()));
            } else {
                /*if there is no last known location. Which means the device has no data for the location currently.
                * So we will get the current location.
                * For this we'll implement Location Listener and override onLocationChanged*/
                Log.i("Current Location", "No data for location found");
                if (!mGoogleApiClient.isConnected())
                    mGoogleApiClient.connect();
                LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, AddItems.this);
            }
        }
    }

    //is called when the save button is clicked
    /*TODO
    *   1. Validate the inputs made
    */

    private void saveItem() {
        final Item[] item = new Item[1];
        String tempItemName = itemName.getText().toString();
        String tempItemDetails = itemDetails.getText().toString();
        itemLocation.addCategory(GEO_POINTS_CATEGORY_NAME);//TODO convert this string to constant later, it defines the name of the table in backendless

        if (!tempItemName.isEmpty() && !tempItemDetails.isEmpty()) {

            item[0] = new Item(tempItemName, tempItemDetails);
            //TODO this is metadata for adding a locaiton in backendless, it has hard coded string
            itemLocation.addMetadata("ItemName", tempItemName);
            Backendless.Geo.savePoint(itemLocation, new AsyncCallback<GeoPoint>() {
                @Override
                public void handleResponse(GeoPoint response) {
                    Toast.makeText(getApplicationContext(), "Successfully inserted geo",
                            Toast.LENGTH_SHORT).show();
                    itemLocation = response;
                    Backendless.Persistence.of(Item.class).save(item[0], new AsyncCallback<Item>() {
                        public void handleResponse(Item response) {
                            Toast.makeText(getApplicationContext(), "Successfully inserted",
                                    Toast.LENGTH_LONG).show();
                            item[0] = response;
                            ArrayList<GeoPoint> location = new ArrayList<GeoPoint>();
                            location.add(itemLocation);
                            Backendless.Persistence.of(Item.class).setRelation(item[0], "location:GeoPoint:1", location, new AsyncCallback<Integer>() {
                                @Override
                                public void handleResponse(Integer response) {
                                    Toast.makeText(getApplicationContext(), "Successfully related",
                                            Toast.LENGTH_SHORT).show();
                                }
                                @Override
                                public void handleFault(BackendlessFault fault) {
                                    Toast.makeText(getApplicationContext(), "Error:" + fault.getCode() + fault.getMessage(),
                                            Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                        public void handleFault(BackendlessFault fault) {
                            Toast.makeText(getApplicationContext(), "Error:" + fault.getCode() + fault.getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                }

                @Override
                public void handleFault(BackendlessFault fault) {
                    Toast.makeText(getApplicationContext(), "Error:" + fault.getCode() + fault.getMessage(),
                            Toast.LENGTH_LONG).show();
                }
            });
        } else {
            Toast.makeText(getApplicationContext(), "Error something is empty",
                    Toast.LENGTH_LONG).show();
        }
    }

    //This is when a user selects a place from the auto complete suggestions
    private AdapterView.OnItemClickListener itemAutoCompleteClickListener
            = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            final PlaceArrayAdapter.PlaceAutocomplete item = mPlaceArrayAdapter.getItem(position);
            final String placeId = String.valueOf(item.placeId);
            Log.i(TAG, "Selected: " + item.description);
            PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi
                    .getPlaceById(mGoogleApiClient, placeId);
            placeResult.setResultCallback(mUpdatePlaceDetailsCallback);
            Log.i(TAG, "Fetching details for ID: " + item.placeId);
        }
    };

    private ResultCallback<PlaceBuffer> mUpdatePlaceDetailsCallback
            = new ResultCallback<PlaceBuffer>() {
        @Override
        public void onResult(PlaceBuffer places) {
            if (!places.getStatus().isSuccess()) {
                Log.e(TAG, "Place query did not complete. Error: " +
                        places.getStatus().toString());
                return;
            }
            // Selecting the first object buffer.
            final Place place = places.get(0);
            CharSequence attributions = places.getAttributions();
            LatLng latLng = place.getLatLng();
            itemLocation = new GeoPoint(latLng.latitude, latLng.longitude);
        }
    };

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mPlaceArrayAdapter.setGoogleApiClient(mGoogleApiClient);
        Log.i(TAG, "Google Places API connected.");
        //TODO here we are calling the start of current place search, somethings needs to be done/
        settingRequest();
    }

    @Override
    public void onConnectionSuspended(int i) {
        mPlaceArrayAdapter.setGoogleApiClient(null);
        Log.e(TAG, "Google Places API connection suspended.");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e(TAG, "Google Places API connection failed with error code: "
                + connectionResult.getErrorCode());
        Toast.makeText(this,
                "Google Places API connection failed with error code:" +
                        connectionResult.getErrorCode(),
                Toast.LENGTH_LONG).show();
    }

    //TODO 4. Rearrange( Related to getting current place, taken from internet, understand and re-arrange the code)
    public void getCurrentPlace() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(AddItems.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    1000);
            ActivityCompat.requestPermissions(AddItems.this,
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
                    adapter = new ArrayAdapter<>(AddItems.this,
                            android.R.layout.simple_list_item_1, android.R.id.text1, placeNames);
                        placeSelectDialogBox(likelyPlaces);
                }
            }
        });
    }

    private void placeSelectDialogBox(final PlaceLikelihoodBufferResponse placeLikelihoodBufferResponse){
        AlertDialog.Builder builder = new AlertDialog.Builder(AddItems.this);
        builder.setTitle(R.string.app_name)
                .setItems(placeNames, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                LatLng latLng = places[which].getLatLng();
                itemLocation = new GeoPoint(latLng.latitude, latLng.longitude);
                itemAutoCompleteLocation.setText(places[which].getName());
                placeLikelihoodBufferResponse.release();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = location;
        tvStatus.setText("Latitude: " + String.valueOf(mLastLocation.getLatitude()));
        tvStatus.append("Longitude: " + String.valueOf(mLastLocation.getLongitude()));
    }

}