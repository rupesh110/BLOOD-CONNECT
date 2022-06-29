package shyam.blood.donation;

import static com.google.firebase.messaging.Constants.MessageNotificationKeys.TAG;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.MenuItemCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.geofire.GeoFireUtils;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class AmbulanceActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener {
    FirebaseFirestore firestore;
    Dialog loadingDialog;
    RecyclerView ambulanceRec;
    AmbulanceAdapter adapter;
    View noInternet;
    List<AmbulanceModel> list = new ArrayList<>();
    GeoLocation center = null;
    double radiusInM;
    SharedPreferences sp;
    LinearLayout showSearch;
    Toolbar toolbar;
    SearchView mSearchView;
    private String mSearchString = "";
    AmbulanceModel model;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    SwipeRefreshLayout refreshLayout;
    private Location mLastKnownLocation;
    private Handler handler = new Handler();
    TextView error;
    private boolean mLocationPermissionGranted;
    int PERMISSION_ID = 44;
    Geocoder geocoder;
    private GoogleApiClient googleApiClient;
    final static int REQUEST_LOCATION = 199;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ambulance);
        firestore = FirebaseFirestore.getInstance();
        ambulanceRec = findViewById(R.id.ambulanceRec);
        noInternet = findViewById(R.id.noInternet);
        toolbar = findViewById(R.id.toolbar);
        error = findViewById(R.id.error);
        refreshLayout = findViewById(R.id.refreshAmbulance);
        refreshLayout.setOnRefreshListener(this);
        setSupportActionBar(toolbar);
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        sp = getSharedPreferences("LatLong", 0);
        loadingDialog = new Dialog(this);
        loadingDialog.setContentView(R.layout.loading_dialog);
        loadingDialog.getWindow().setBackgroundDrawable(getDrawable(R.drawable.loading_item));
        loadingDialog.getWindow().setLayout(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        loadingDialog.setCancelable(true);

        error.setVisibility(View.VISIBLE);


        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(RecyclerView.VERTICAL);
        ambulanceRec.setLayoutManager(layoutManager);
        adapter = new AmbulanceAdapter(AmbulanceActivity.this, list);
        ambulanceRec.setAdapter(adapter);
        radiusInM = 50 * 1000;
        if (!sp.getString("lat", "").isEmpty()) {
            center = new GeoLocation(Double.parseDouble(sp.getString("lat", "")), Double.parseDouble(sp.getString("long", "")));
        } else {
            getDeviceLocation();
        }
        if (center != null) {
            loadingDialog.show();
            loadData();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_LOCATION) {
            switch (resultCode) {
                case Activity.RESULT_OK:
                    getDeviceLocation();
                    break;
                case Activity.RESULT_CANCELED:
                    finish();//ep asking if imp or do whatever
                    break;
            }
        }

    }

    private boolean checkPermissions() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void getLocationPermission() {
        mLocationPermissionGranted = false;
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_COARSE_LOCATION
            }, 44);

        }
    }

    private boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    @SuppressLint("MissingPermission")
    private void getDeviceLocation() {
        if (checkPermissions()) {
            if (isLocationEnabled()) {
                mFusedLocationProviderClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        Location location = task.getResult();
                        List<Address> addresses = null;
                        if (location == null) {
                            requestNewLocationData();
                        } else {
                            Double lat = location.getLatitude();
                            Double lon = location.getLongitude();

                            sp = getSharedPreferences("LatLong", 0);
                            SharedPreferences.Editor sspEditor = sp.edit();
                            sspEditor.putString("lat", "" + lat);
                            sspEditor.putString("long", "" + lon);
                            sspEditor.apply();
                        }
                    }
                });

            } else {
                Toast.makeText(this, "Please turn on your location...", Toast.LENGTH_LONG).show();
                enableLoc();
            }
        } else {
            getLocationPermission();
        }
    }

    @SuppressLint("MissingPermission")
    private void requestNewLocationData() {
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        mLocationRequest.setInterval(5);
        mLocationRequest.setFastestInterval(0);
        mLocationRequest.setNumUpdates(2);
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        mFusedLocationProviderClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
    }

    private LocationCallback mLocationCallback = new LocationCallback() {

        @Override
        public void onLocationResult(LocationResult locationResult) {
            Location mLastLocation = locationResult.getLastLocation();
            mLastKnownLocation = mLastLocation;
        }
    };

    private void loadData() {
        list.clear();
        error.setVisibility(View.GONE);
        firestore.collection("Ambulance")
                .get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                for (QueryDocumentSnapshot doc : task.getResult()) {
                    GeoLocation docLocation = new GeoLocation(Double.parseDouble(doc.get("latitude").toString()), Double.parseDouble(doc.get("longitude").toString()));
                    double distanceInM = GeoFireUtils.getDistanceBetween(docLocation, center);
                    if (distanceInM <= radiusInM) {
                        model = new AmbulanceModel(
                                doc.getId(),
                                doc.get("name").toString(),
                                doc.get("phone").toString(),
                                doc.get("address").toString(),
                                distanceInM
                        );
                        if (list.size() < 50) list.add(model);
                        //list.add(model);
                    }
                }
                if (list.size() == 0) {
                    error.setVisibility(View.VISIBLE);
                    error.setText("Sorry! No ambulance found in your area !!");
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    Collections.sort(list, Comparator.comparing(AmbulanceModel::getDistance));
                }
                adapter.notifyDataSetChanged();
                loadingDialog.dismiss();
                refreshLayout.setRefreshing(false);
            }
        });


    }

    private void enableLoc() {

        if (googleApiClient == null) {
            googleApiClient = new GoogleApiClient.Builder(AmbulanceActivity.this)
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                        @Override
                        public void onConnected(Bundle bundle) {

                        }

                        @Override
                        public void onConnectionSuspended(int i) {
                            googleApiClient.connect();
                        }
                    })
                    .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                        @Override
                        public void onConnectionFailed(ConnectionResult connectionResult) {

                            // Log.d("Location error","Location error " + connectionResult.getErrorCode());
                        }
                    }).build();
            googleApiClient.connect();

            LocationRequest locationRequest = LocationRequest.create();
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            locationRequest.setInterval(30 * 1000);
            locationRequest.setFastestInterval(5 * 1000);
            LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                    .addLocationRequest(locationRequest);

            builder.setAlwaysShow(true);

            PendingResult<LocationSettingsResult> result =
                    LocationServices.SettingsApi.checkLocationSettings(googleApiClient, builder.build());
            result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
                @Override
                public void onResult(LocationSettingsResult result) {
                    final Status status = result.getStatus();
                    switch (status.getStatusCode()) {
                        case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                            try {
                                // Show the dialog by calling startResolutionForResult(),
                                // and check the result in onActivityResult().
                                status.startResolutionForResult(AmbulanceActivity.this, REQUEST_LOCATION);

//                                finish();
                            } catch (IntentSender.SendIntentException e) {
                                // Ignore the error.
                            }
                            break;
                    }
                }
            });
        }
    }

    private void processsearch(String s) {
        if (!s.isEmpty()) {
            list.clear();
            firestore.collection("Ambulance")
                    .orderBy("address").startAt(s).endAt(s + "\uf8ff")
                    .get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                @Override
                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                    for (QueryDocumentSnapshot doc : task.getResult()) {
                        if (task.isSuccessful()) {
                            AmbulanceModel model = new AmbulanceModel(
                                    doc.getId(),
                                    doc.get("name").toString(),
                                    doc.get("phone").toString(),
                                    doc.get("address").toString(),
                                    0
                            );
                            list.add(model);
                        }
                    }
                    loadingDialog.dismiss();
                    adapter.notifyDataSetChanged();
                }

            });
        } else {
            showSearch.setVisibility(View.GONE);
            toolbar.getMenu().findItem(R.id.search).setVisible(true);
            loadData();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {


        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.ambulance, menu);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        mSearchView = (SearchView) MenuItemCompat.getActionView(menu.findItem(R.id.search));
        mSearchView.setQueryHint("Search address...");
        if (mSearchView != null) {
            mSearchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
            MenuItem searchMenu = menu.add("searchMenu").setVisible(false).setActionView(mSearchView);
            searchMenu.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);

            SearchView.OnQueryTextListener queryTextListener = new SearchView.OnQueryTextListener() {
                public boolean onQueryTextChange(String newText) {
                    mSearchString = newText;
                    if (newText.isEmpty()) {
                        list.clear();
                        if (center != null) {
                            loadingDialog.dismiss();
                            loadData();
                        }
                    } else {
                        processsearch(mSearchString.toUpperCase());
                    }
                    return false;
                }

                public boolean onQueryTextSubmit(String query) {
                    mSearchString = query;
                    loadingDialog.show();
                    processsearch(mSearchString.toUpperCase());
                    return true;
                }
            };

            mSearchView.setOnQueryTextListener(queryTextListener);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressLint("MissingPermission")
    public boolean checkConnection() {
        ConnectivityManager manager = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        return manager.getActiveNetworkInfo() != null && manager.getActiveNetworkInfo().isConnectedOrConnecting();

    }

    @Override
    protected void onStart() {
        super.onStart();
        getDeviceLocation();
    }

    @Override
    public void onRefresh() {
        if (center != null) {
            loadData();
        } else {
            getDeviceLocation();
            refreshLayout.setRefreshing(false);
            Toast.makeText(AmbulanceActivity.this, "Please make sure your location is on!!", Toast.LENGTH_SHORT).show();
        }
    }
}