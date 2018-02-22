package com.learnerdev.sharenearn;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
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
import com.backendless.persistence.DataQueryBuilder;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.PlaceDetectionClient;
import com.google.android.gms.location.places.PlaceLikelihood;
import com.google.android.gms.location.places.PlaceLikelihoodBufferResponse;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.learnerdev.sharenearn.Defaults.COLUMN_NAME_ITEM_LOCATION;
import static com.learnerdev.sharenearn.Defaults.COLUMN_NAME_ITEM_NAME;
import static com.learnerdev.sharenearn.Defaults.EARTH;

public class SearchItems extends AppCompatActivity implements
        GoogleApiClient.OnConnectionFailedListener,
        GoogleApiClient.ConnectionCallbacks  {
    static final String TAG = "SNE-SearchItems";
    private static final int M_MAX_ENTRIES = 5;

    private EditText itemName;
    private AutoCompleteTextView itemAutoCompleteLocation;
    private ImageButton getCurrentLocationBtn;
    private Button searchButton;
    private TextView tvStatus;
    private static final int GOOGLE_API_CLIENT_ID = 0;
    private GeoPoint selectedLocation;
    private LatLng selectedLocLatLng;
    private String whereClause;
    private Item selectedItem;

    private ArrayList<Item> foundItems;


    private GoogleApiClient mGoogleApiClient;
    PlaceDetectionClient mPlaceDetectionClient;

    private Place[] places;
    private String[] placeNames;

    private RecyclerView itemsRecycler;
    private ItemsListAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
//    private ItemsListAdapter itemsListAdapter;
    private PlaceArrayAdapter mPlaceArrayAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_items);
        initUI();
    }

    private void initUI() {
        itemName=findViewById(R.id.s_input_item_name);
        itemAutoCompleteLocation=findViewById(R.id.s_input_auto_complete_location);
        getCurrentLocationBtn=findViewById(R.id.s_btn_current_location);
        searchButton= findViewById(R.id.s_search_button);
        tvStatus=findViewById(R.id.s_tv_status);

        mPlaceDetectionClient = Places.getPlaceDetectionClient(this, null);
        foundItems= new ArrayList<>();

        itemsRecycler=findViewById(R.id.s_item_recycler);
        selectedLocLatLng=null;

        mAdapter=new ItemsListAdapter(foundItems);
        mLayoutManager=new LinearLayoutManager(getApplicationContext());
        itemsRecycler.setLayoutManager(mLayoutManager);
        itemsRecycler.setItemAnimator(new DefaultItemAnimator());
        itemsRecycler.setAdapter(mAdapter);

        //To get the necessary google api client for location auto complete and for likely places
        mGoogleApiClient = new GoogleApiClient.Builder(SearchItems.this)
                .addApi(Places.GEO_DATA_API)
                .addApi(LocationServices.API)
                .addOnConnectionFailedListener(this)
                .enableAutoManage(this, GOOGLE_API_CLIENT_ID, this)
                .addConnectionCallbacks(this)
                .build();

        getCurrentLocationBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getCurrentPlace();
            }
        });

        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                TODO: this is just calling getNearByItems which searches only based on current location
                getItemsByLocation();

            }
        });

        itemAutoCompleteLocation.setOnItemClickListener(itemAutoCompleteClickListener);
        mPlaceArrayAdapter = new PlaceArrayAdapter(this, android.R.layout.simple_list_item_1, EARTH, null);
        itemAutoCompleteLocation.setAdapter(mPlaceArrayAdapter);

        mAdapter.setOnItemClickListener(new ItemsListAdapter.ClickListener() {
            @Override
            public void onItemClick(int position, View v) {
                Log.d(TAG, "onItemClick position: " + position);
                int id=v.getId();
                Log.d(TAG, "onItemClick id: " + id);
                selectedItem=foundItems.get(position);
                tvStatus.setText("Selected item's id: "+selectedItem.getObjectId());
                Intent intent=new Intent(getApplicationContext(), ViewItem.class);
                intent.putExtra("EXTRA_OBJECT_ID",selectedItem.getObjectId());
                startActivity(intent);

            }
        });

    }

    /*
    * This method will get the data
     */
    public void getItemsByLocation(){
        int searchRadius=3;
//        whereClause = "distance( "+selectedLocLatLng.latitude+", "+
//                selectedLocLatLng.longitude+", "+
//                COLUMN_NAME_ITEM_LOCATION+".latitude, "+
//                COLUMN_NAME_ITEM_LOCATION+".longitude ) < mi(3)";
        if(selectedLocLatLng != null) {
            whereClause = String.format(Locale.getDefault(), "distance(%s, %s, %s.latitude, %s.longitude)<mi(%d) and %s LIKE '%%%s%%'",
                    selectedLocLatLng.latitude,
                    selectedLocLatLng.longitude,
                    COLUMN_NAME_ITEM_LOCATION,
                    COLUMN_NAME_ITEM_LOCATION,
                    searchRadius,
                    COLUMN_NAME_ITEM_NAME,
                    itemName.getText());

            Log.i(TAG,"Where Clause: "+whereClause);
            DataQueryBuilder queryBuilder = DataQueryBuilder.create();
            queryBuilder.setWhereClause( whereClause ).setRelationsDepth( 1 );
            queryBuilder.setRelated(COLUMN_NAME_ITEM_LOCATION);
            Backendless.Data.of( Item.class ).find(queryBuilder, new AsyncCallback<List<Item>>() {
                @Override
                public void handleResponse(List<Item> response) {
                    //TODO debug message
                    tvStatus.setText("Started searching");
                    foundItems.clear();
                    if(response.size() !=0){
                        for( Item item : response)
                        {   foundItems.add(item);
                            Log.i(TAG,item.getItemName());
                        }
                    }else {
                        Log.d(TAG,"Search hasn't returned any results");
                        Toast.makeText(getApplicationContext(),"Search hasn't returned any results",Toast.LENGTH_SHORT).show();
                    }
                    mAdapter.notifyDataSetChanged();
                }

                @Override
                public void handleFault(BackendlessFault fault) {
                    Log.e(TAG,"Error: "+fault.getCode()+fault.getMessage());
                }
            });
        }else {
            Log.e(TAG,"Please choose a location");
            Toast.makeText(this,"Please choose a location",Toast.LENGTH_SHORT).show();
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
            selectedLocLatLng = place.getLatLng();
        }
    };

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
                    placeSelectDialogBox(likelyPlaces);
                }
            }
        });
    }

    private void placeSelectDialogBox(final PlaceLikelihoodBufferResponse placeLikelihoodBufferResponse){
        AlertDialog.Builder builder = new AlertDialog.Builder(SearchItems.this);
        builder.setTitle(R.string.app_name)
                .setItems(placeNames, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        selectedLocLatLng = places[which].getLatLng();
//                        selectedLocation = new GeoPoint(latLng.latitude, latLng.longitude);
                        itemAutoCompleteLocation.setText(places[which].getName());
                        placeLikelihoodBufferResponse.release();
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }


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

