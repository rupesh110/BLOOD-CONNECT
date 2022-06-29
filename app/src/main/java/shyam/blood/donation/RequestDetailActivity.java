package shyam.blood.donation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.viewpager.widget.ViewPager;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

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
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

public class RequestDetailActivity extends AppCompatActivity {
    private Toolbar toolbar;
    private TabLayout tabLayout;
    private ViewPager viewPager;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private Location mLastKnownLocation;
    private boolean mLocationPermissionGranted;
    Geocoder geocoder;
    int PERMISSION_ID = 44;
    Dialog loadingDialog, deleteDialog;
    public static double lat, lon;
    SharedPreferences sharedPreferences;

    FirebaseFirestore firestore;
    FirebaseAuth auth;
    FirebaseUser user;
    TextView reqname, reqBG, reqPhone, reqBlood, reqHospi, recCase;
    TextView yesBtn, noBtn, cancelBtn;
    Button deleteBtnn;
    private GoogleApiClient googleApiClient;
    final static int REQUEST_LOCATION = 199;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request_detail);
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();
        geocoder = new Geocoder(this);
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        getDeviceLocation();
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        reqname = findViewById(R.id.reqName);
        reqBG = findViewById(R.id.reqBG);
        reqPhone = findViewById(R.id.reqPhone);
        reqBlood = findViewById(R.id.reqBlood);
        reqHospi = findViewById(R.id.reqHospi);
        recCase = findViewById(R.id.reqCase);

        loadingDialog = new Dialog(this);
        loadingDialog.setContentView(R.layout.loading_dialog);
        loadingDialog.getWindow().setBackgroundDrawable(getDrawable(R.drawable.loading_item));
        loadingDialog.getWindow().setLayout(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        loadingDialog.setCancelable(true);
///ask to delete when go to back
        deleteBtnn = findViewById(R.id.deleteBtn);
        deleteBtnn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                firestore.collection("requests").document(user.getUid()).addSnapshotListener(new EventListener<DocumentSnapshot>() {
                    @Override
                    public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {
                        if (value.exists()) {
                            if (Integer.parseInt(value.get("donorCount").toString()) > 0) {
                                deleteDialog.show();
                            } else {
                                deleteDialog.show();
                                TextView content = deleteDialog.findViewById(R.id.textView15);
                                content.setText("Are you sure, you want to delete your request?");
                                yesBtn.setVisibility(View.GONE);
                            }
                        } else {
                            startActivity(new Intent(RequestDetailActivity.this, MainActivity.class));
                            finish();
                        }
                    }
                });

            }
        });
        deleteDialog = new Dialog(RequestDetailActivity.this);
        deleteDialog.setContentView(R.layout.got_blood);
        deleteDialog.setCancelable(true);
        deleteDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        yesBtn = deleteDialog.findViewById(R.id.yesBtn);
        noBtn = deleteDialog.findViewById(R.id.noBtn);
        cancelBtn = deleteDialog.findViewById(R.id.cancelRequest);

        noBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                deleteDialog.dismiss();
            }
        });
        yesBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                firestore.collection("requests").document(user.getUid()).collection("acceptedList").
                      get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        for (QueryDocumentSnapshot snapshot : task.getResult()) {
                            firestore.collection("requests").document(user.getUid()).collection("acceptedList").document(snapshot.getId()).update("donated", true);
                            firestore.collection("donors").document(snapshot.getId()).collection("acceptedRequest").document(user.getUid()).delete();
                        }
                        if (user.isAnonymous()) {
                            auth.signOut();
                        }
                        firestore.collection("requests").document(user.getUid()).update("visibility", false);
                        firestore.collection("requests").document(user.getUid()).update("success", true);
                        deleteDialog.dismiss();
                        startActivity(new Intent(RequestDetailActivity.this, MainActivity.class));
                        finishAffinity();
                        Toast.makeText(RequestDetailActivity.this, "Your request successfully solved!", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (user != null) {
                    CollectionReference ref = firestore.collection("requests").document(user.getUid()).collection("acceptedList");
                    ref.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            for (QueryDocumentSnapshot snapshot : task.getResult()) {
                                ref.document(snapshot.getId()).delete();
                            }
                        }
                    });
                    firestore.collection("requests").document(user.getUid()).delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                if (user.isAnonymous()) {
                                    auth.signOut();
                                }
                                startActivity(new Intent(RequestDetailActivity.this, MainActivity.class));
                                finish();

                                Toast.makeText(RequestDetailActivity.this, "Request deleted successfully", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(RequestDetailActivity.this, "Something went wrong!", Toast.LENGTH_SHORT).show();
                            }

                        }
                    });
                }
            }
        });

        if (user != null) {
            firestore.collection("requests").document(user.getUid()).addSnapshotListener(new EventListener<DocumentSnapshot>() {
                @Override
                public void onEvent(@Nullable DocumentSnapshot snapshot, @Nullable FirebaseFirestoreException error) {
                    if (snapshot.exists()) {
                        if (!snapshot.get("success").toString().equalsIgnoreCase("true")) {
                            reqname.setText(snapshot.get("pName").toString());
                            reqBG.setText(snapshot.get("group").toString() + ", " + snapshot.get("pints").toString() + " Pint");
                            reqPhone.setText(snapshot.get("phone").toString());
                            reqBlood.setText(snapshot.get("bloodType").toString());
                            reqHospi.setText(snapshot.get("hospital").toString());
                            recCase.setText(snapshot.get("notes").toString());
                            int donor = Integer.parseInt(snapshot.get("donorCount").toString());
                            if (donor > 0) {
                                if (Integer.parseInt(snapshot.get("pints").toString()) <= donor) {
                                    firestore.collection("requests").document(user.getUid()).update("visibility", false);
                                }
                            }
                        } else {
                            startActivity(new Intent(RequestDetailActivity.this, MainActivity.class));
                            finish();
                        }

                    } else {
                        Toast.makeText(RequestDetailActivity.this, "No details found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                }
            });

        } else {
            startActivity(new Intent(RequestDetailActivity.this, MainActivity.class));
            Toast.makeText(RequestDetailActivity.this, "No details found", Toast.LENGTH_SHORT).show();
            finish();
        }

        viewPager = (ViewPager) findViewById(R.id.viewpager);
        setupViewPager(viewPager);
        tabLayout = (TabLayout) findViewById(R.id.tablayout);
        tabLayout.setupWithViewPager(viewPager);

    }


    private void setupViewPager(ViewPager viewPager) {

        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        adapter.addFragment(new RequestFragment(), "Accepted Donors");
        adapter.addFragment(new BloodBankFragment(), "Blood Bank");
        viewPager.setAdapter(adapter);

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
    private void enableLoc() {

        if (googleApiClient == null) {
            googleApiClient = new GoogleApiClient.Builder(RequestDetailActivity.this)
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
                                status.startResolutionForResult(RequestDetailActivity.this, REQUEST_LOCATION);

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
    @SuppressLint("MissingPermission")
    private void getDeviceLocation() {
        if (checkPermissions()) {
            if (isLocationEnabled()) {
                sharedPreferences = getSharedPreferences("LatLong", 0);

                mFusedLocationProviderClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        Location location = task.getResult();
                        if (location == null) {
                            requestNewLocationData();
                        } else {
                            lat = location.getLatitude();
                            lon = location.getLongitude();
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putString("lat", "" + lat);
                            editor.putString("long", "" + lon);
                            editor.apply();
                            editor.commit();
                            loadingDialog.dismiss();
                        }
                    }
                });

            } else {
                Toast.makeText(this, "Please turn on" + " your location...", Toast.LENGTH_LONG).show();
             enableLoc();
            }
        } else {
            getLocationPermission();
        }
    }

    @SuppressLint("MissingPermission")
    private void requestNewLocationData() {
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(5);
        mLocationRequest.setFastestInterval(0);
        mLocationRequest.setNumUpdates(1);
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
            }, PERMISSION_ID);

        }
    }

    private boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            startActivity(new Intent(RequestDetailActivity.this, MainActivity.class));
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    public boolean checkConnection() {
        ConnectivityManager manager = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        return manager.getActiveNetworkInfo() != null && manager.getActiveNetworkInfo().isConnectedOrConnecting();

    }

}