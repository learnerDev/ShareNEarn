package com.learnerdev.sharenearn;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.backendless.Backendless;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.exceptions.BackendlessFault;
import com.backendless.geo.GeoPoint;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;

import static com.learnerdev.sharenearn.Defaults.EARTH;
import static com.learnerdev.sharenearn.Defaults.GEO_POINTS_CATEGORY_NAME;

public class AddItems extends AppCompatActivity implements
        GoogleApiClient.OnConnectionFailedListener,
        GoogleApiClient.ConnectionCallbacks{

    static String TAG = "AddItemsActivity";
    private static final int GOOGLE_API_CLIENT_ID = 0;
    private Button saveButton;
    private EditText itemName;
    //    private AutoCompleteTextView itemLocation;
    private EditText itemDetails;
    private GeoPoint itemLocation;
    private AutoCompleteTextView itemAutoCompleteLocation;
//    private String locationTextBuffer;
    private GoogleApiClient mGoogleApiClient;
    private PlaceArrayAdapter mPlaceArrayAdapter;


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
        itemAutoCompleteLocation=findViewById(R.id.input_auto_complete_location);

        mGoogleApiClient = new GoogleApiClient.Builder(AddItems.this)
                .addApi(Places.GEO_DATA_API)
                .enableAutoManage(this, GOOGLE_API_CLIENT_ID, this)
                .addConnectionCallbacks(this)
                .build();

//        PlaceAutocompleteFragment autocompleteFragment = (PlaceAutocompleteFragment)
//                getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);

//        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
//            @Override
//            public void onPlaceSelected(Place place) {
//                // TODO: Get info about the selected place.
//                Log.i(TAG, "Place: " + place.getName());
//                LatLng latLng = place.getLatLng();
//                itemLocation = new GeoPoint(latLng.latitude, latLng.longitude);
//            }



//            @Override
//            public void onError(Status status) {
//                // TODO: Handle the error.
//                Log.i(TAG, "An error occurred: " + status);
//            }
//        });

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveItem();
            }
        });

        itemAutoCompleteLocation.setOnItemClickListener(itemAutoCompleteClickListener);
        mPlaceArrayAdapter= new PlaceArrayAdapter(this,android.R.layout.simple_list_item_1,EARTH,null);
        itemAutoCompleteLocation.setAdapter(mPlaceArrayAdapter);



//        itemAutoCompleteLocation.setOnFocusChangeListener(new View.OnFocusChangeListener() {
//            @Override
//            public void onFocusChange(View v, boolean hasFocus) {
//                if(hasFocus){
//                    locationTextBuffer=itemAutoCompleteLocation.getText().toString();
//
//                }
//            }
//        });
    }

    private void saveItem() {
        final Item[] item = new Item[1];
        String tempItemName = itemName.getText().toString();
        String tempItemDetails = itemDetails.getText().toString();
        itemLocation.addCategory(GEO_POINTS_CATEGORY_NAME);//TODO convert this string to constant later, it defines the name of the table in backendless

        if (!tempItemName.isEmpty() && !tempItemDetails.isEmpty()) {

            item[0] = new Item(tempItemName, tempItemDetails);

            itemLocation.addMetadata("ItemName",tempItemName);

            Backendless.Geo.savePoint(itemLocation, new AsyncCallback<GeoPoint>() {

                @Override
                public void handleResponse(GeoPoint response) {
                    Toast.makeText(getApplicationContext(), "Successfully inserted geo",
                            Toast.LENGTH_SHORT).show();

                    itemLocation=response;

                    Backendless.Persistence.of(Item.class).save(item[0], new AsyncCallback<Item>() {
                        public void handleResponse(Item response) {
                            Toast.makeText(getApplicationContext(), "Successfully inserted",
                                    Toast.LENGTH_LONG).show();
                            item[0] =response;

                            ArrayList<GeoPoint> location =new ArrayList<GeoPoint>();
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
                            // new Item instance has been saved
                        }

                        public void handleFault(BackendlessFault fault) {
                            Toast.makeText(getApplicationContext(), "Error:" + fault.getCode() + fault.getMessage(),
                                    Toast.LENGTH_LONG).show();
                            // an error has occurred, the error code can be retrieved with fault.getCode()
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
//            place.
            CharSequence attributions = places.getAttributions();

//            mNameView.setText(Html.fromHtml(place.getAddress() + ""));

            LatLng latLng = place.getLatLng();
            itemLocation = new GeoPoint(latLng.latitude, latLng.longitude);


        }
    };
    @Override
    public void onConnected(@Nullable Bundle bundle) {

        mPlaceArrayAdapter.setGoogleApiClient(mGoogleApiClient);
        Log.i(TAG, "Google Places API connected.");

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
}