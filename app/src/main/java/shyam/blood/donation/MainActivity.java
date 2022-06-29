package shyam.blood.donation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager.widget.ViewPager;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
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
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsApi;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.android.play.core.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;

import hotchemi.android.rate.AppRate;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, SwipeRefreshLayout.OnRefreshListener {
    LinearLayout request, donate, banks, ambulance;
    private long backPressedTime;
    private Toast toast;
    private AppUpdateManager appUpdateManager;
    private static final int IMMEDIATE_APP_UPDATE_REQ_CODE = 124;
    FirebaseFirestore firestore;
    FirebaseAuth auth;
    TextView username, userTime;
    ImageView userImage, shareApp;
    SharedPreferences ssp;
    Dialog loadingDialog;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private Location mLastKnownLocation;
    private boolean mLocationPermissionGranted = false;
    Geocoder geocoder;
    int PERMISSION_ID = 44;
    TextView noInter;
    List<SliderItem> sliderItemList = new ArrayList<>();
    private Dialog locationDialog;
    Button okBtn, cancelBtn, refresh;
    int count = 0;
    SwipeRefreshLayout refreshLayout;
    ViewPager page;
    ImageSliderAdapter itemsPager_adapter;
    TabLayout tabLayout;
    LinearLayout refreshNet;
    private GoogleApiClient googleApiClient;
    final static int REQUEST_LOCATION = 199;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_main);
        appUpdateManager = AppUpdateManagerFactory.create(getApplicationContext());
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        getDeviceLocation();
        checkUpdate();
        loadImages();
        page = findViewById(R.id.my_pager);
        tabLayout = (TabLayout) findViewById(R.id.my_tablayout);
        tabLayout.setupWithViewPager(page);
        shareApp = findViewById(R.id.shareApp);
        refreshNet = findViewById(R.id.refreshLayout);
        refresh = findViewById(R.id.refresh);
        noInter = findViewById(R.id.noInt);
        Glide.with(MainActivity.this).load(R.drawable.blink_eye).into(shareApp);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ssp = getSharedPreferences("LatLong", 0);
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        refreshLayout = findViewById(R.id.swipeMain);
        refreshLayout.setOnRefreshListener(this);
        refresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onRefresh();
            }
        });
        locationDialog = new Dialog(MainActivity.this);
        locationDialog.setContentView(R.layout.ask_for_location);
        locationDialog.setCancelable(false);
        locationDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        okBtn = locationDialog.findViewById(R.id.yesBtn);
        cancelBtn = locationDialog.findViewById(R.id.noBtn);

        okBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                locationDialog.dismiss();
                checkForPermissions();
            }
        });
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
                locationDialog.dismiss();
            }
        });

        FirebaseUser user = auth.getCurrentUser();
        AppRate.with(this)
                .setInstallDays(1)
                .setLaunchTimes(3)
                .setRemindInterval(2)
                .monitor();
        AppRate.showRateDialogIfMeetsConditions(this);


        geocoder = new Geocoder(this);
        loadingDialog = new Dialog(this);
        loadingDialog.setContentView(R.layout.loading_dialog);
        loadingDialog.getWindow().setBackgroundDrawable(getDrawable(R.drawable.loading_item));
        loadingDialog.getWindow().setLayout(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        loadingDialog.setCancelable(false);
        itemsPager_adapter = new ImageSliderAdapter(this, sliderItemList);
        page.setAdapter(itemsPager_adapter);

        if (!checkConnection()) {
            refreshNet.setVisibility(View.VISIBLE);
        }


        request = findViewById(R.id.requestBlood);
        banks = findViewById(R.id.bloodBank);
        donate = findViewById(R.id.donate);
        ambulance = findViewById(R.id.ambulance);

        shareApp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, PublicRequestActivity.class));
            }
        });
        request.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (checkConnection()) {
                    if (user != null) {
                        FirebaseFirestore.getInstance().collection("requests").document(FirebaseAuth.getInstance().getCurrentUser().getUid()).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                            @Override
                            public void onComplete(@NonNull com.google.android.gms.tasks.Task<DocumentSnapshot> task) {
                                if (task.isSuccessful()) {
                                    DocumentSnapshot snapshot = task.getResult();
                                    if (snapshot.exists()) {
                                        if (!snapshot.get("success").toString().equalsIgnoreCase("true")) {
                                            startActivity(new Intent(MainActivity.this, RequestDetailActivity.class));
                                        } else {
                                            startActivity(new Intent(MainActivity.this, RequestActivity.class));
                                        }

                                    } else {
                                        startActivity(new Intent(MainActivity.this, RequestActivity.class));
                                    }
                                }
                            }
                        });
                    } else {
                        startActivity(new Intent(MainActivity.this, RequestActivity.class));
                    }

                } else {
                    Toast.makeText(MainActivity.this, "No internet Connection!!", Toast.LENGTH_SHORT).show();
                }
            }
        });
        banks.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, BloodBankActivity.class));
            }
        });
        ambulance.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, AmbulanceActivity.class));
            }
        });
        donate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (checkConnection()) {
                    loadingDialog.show();
                    if (user != null && !user.isAnonymous()) {
                        firestore.collection("donors").document(user.getUid()).addSnapshotListener(new EventListener<DocumentSnapshot>() {
                            @Override
                            public void onEvent(@Nullable DocumentSnapshot snapshot, @Nullable FirebaseFirestoreException error) {
                                if (!snapshot.exists()) {
                                    loadingDialog.dismiss();
                                    startActivity(new Intent(MainActivity.this, DonorFromActivity.class));
                                } else {
                                    loadingDialog.dismiss();
                                    startActivity(new Intent(MainActivity.this, DonorProfileActivity.class));
                                }
                            }
                        });
                    } else {
                        loadingDialog.dismiss();
                        startActivity(new Intent(MainActivity.this, LoginActivity.class));
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Please check your internet connections", Toast.LENGTH_SHORT).show();
                }
            }
        });


        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        username = navigationView.getHeaderView(0).findViewById(R.id.userName);
        userTime = navigationView.getHeaderView(0).findViewById(R.id.userTime);
        userImage = navigationView.getHeaderView(0).findViewById(R.id.imageView);
        userImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (checkConnection()) {
                    loadingDialog.show();
                    if (user != null && !user.isAnonymous()) {
                        firestore.collection("donors").document(user.getUid()).addSnapshotListener(new EventListener<DocumentSnapshot>() {
                            @Override
                            public void onEvent(@Nullable DocumentSnapshot snapshot, @Nullable FirebaseFirestoreException error) {
                                if (!snapshot.exists()) {
                                    loadingDialog.dismiss();
                                    startActivity(new Intent(MainActivity.this, DonorFromActivity.class));
                                } else {
                                    loadingDialog.dismiss();
                                    startActivity(new Intent(MainActivity.this, DonorProfileActivity.class));
                                }
                            }
                        });
                    } else {
                        loadingDialog.dismiss();
                        startActivity(new Intent(MainActivity.this, LoginActivity.class));
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Please check your internet connections", Toast.LENGTH_SHORT).show();
                }
            }
        });
        if (user != null) {
            firestore.collection("donors")
                    .document(user.getUid()).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull com.google.android.gms.tasks.Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        DocumentSnapshot snapshot = task.getResult();
                        if (!snapshot.exists()) {
                            username.setText("BLOOD CONNECT");
                        } else {
                            username.setText(snapshot.get("fName").toString());
                            Glide.with(MainActivity.this).load(snapshot.get("profile").toString()).apply(new RequestOptions().placeholder(R.drawable.user1)).into(userImage);
                        }

                    }
                }
            });
        }

        navigationView.setItemIconTintList(null);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkForPermissions();
    }

    private void checkForPermissions() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSION_ID);
        }
    }

    private void enableLoc() {

        if (googleApiClient == null) {
            googleApiClient = new GoogleApiClient.Builder(MainActivity.this)
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
                                status.startResolutionForResult(MainActivity.this, REQUEST_LOCATION);

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

    private void loadImages() {
        sliderItemList.clear();

            FirebaseFirestore.getInstance().collection("Banner")
                    .orderBy("position")
                    .addSnapshotListener(new EventListener<QuerySnapshot>() {
                        @Override
                        public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                            for (QueryDocumentSnapshot snapshot : value) {
                                SliderItem model = new SliderItem("", snapshot.get("imageurl").toString());
                                sliderItemList.add(model);
                            }
                            itemsPager_adapter.notifyDataSetChanged();
                            refreshLayout.setRefreshing(false);
                        }
                    });


            // The_slide_timer
            java.util.Timer timer = new java.util.Timer();
            timer.scheduleAtFixedRate(new The_slide_timer(), 2000, 5000);

    }

    public class The_slide_timer extends TimerTask {
        @Override
        public void run() {

            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (page.getCurrentItem() < sliderItemList.size() - 1) {
                        page.setCurrentItem(page.getCurrentItem() + 1);
                    } else
                        page.setCurrentItem(0);
                }
            });
        }
    }

    @Override
    public void onBackPressed() {
        if (backPressedTime + 2000 > System.currentTimeMillis()) {
            toast.cancel();
            finishAffinity();
            super.onBackPressed();
            return;
        } else {
            toast = Toast.makeText(this, "Press back AGAIN to exit", Toast.LENGTH_SHORT);
            toast.show();
        }
        backPressedTime = System.currentTimeMillis();

    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        if (id == R.id.nav_request) {
            startActivity(new Intent(MainActivity.this, PublicRequestActivity.class));
        }
//        else if (id == R.id.nav_cotribute) {
//            startActivity(new Intent(MainActivity.this, ContributeActivity.class));
//
//        }
        else if (id == R.id.nav_thank) {
            if (checkConnection()) {
                startActivity(new Intent(MainActivity.this, ThankYouActivity.class));
            } else {
                Toast.makeText(MainActivity.this, "No Internet Connection!!", Toast.LENGTH_SHORT).show();
            }

        } else if (id == R.id.nav_policy) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://bloodconnect2022.blogspot.com/p/privacy-policy.html")));

        } else if (id == R.id.nav_liscence) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://bloodconnect2022.blogspot.com/p/end-user-license-agreement.html")));

        } else if (id == R.id.nav_contact) {
            Intent intent = new Intent(Intent.ACTION_SEND);
            String[] recipients = {"bloodfornepal2022@gmail.com"};//Add multiple recipients here
            String[] emailcc = {"sarojkumarmahato2078@gmail.com", "kumariaachal568@gmail.com", "shyamkishor1439@gmail.com"};//Add multiple recipients here
            intent.putExtra(Intent.EXTRA_EMAIL, recipients);
            intent.putExtra(Intent.EXTRA_SUBJECT, ""); //Add Mail Subject
            intent.putExtra(Intent.EXTRA_TEXT, "");//Add mail body
            intent.putExtra(Intent.EXTRA_BCC, emailcc);//Add BCC email id if any
            intent.setType("text/plain");
            intent.setType("message/rfc822");
            intent.setPackage("com.google.android.gm");//Added Gmail Package to forcefully open Gmail App
            startActivity(Intent.createChooser(intent, "Send mail"));
            return true;

        } else if (id == R.id.nav_rate) {

            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + getPackageName())));

            } catch (ActivityNotFoundException e) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=" + getPackageName())));
            }
        } else if (id == R.id.nav_share) {

            Intent sharingIntent = new Intent(Intent.ACTION_SEND);
            sharingIntent.setType("text/plain");
            String shareBody = "\"BLOOD CONNECT\" मार्फत रगत आभव भएको बेलामा रगत माग गर्न अथवा दान गर्न सक्नुहुन्छ र आफ्नो नजिकै को \"BLOOD BANK\" को जानकारी पनि लिन सक्नु हुन्छ । नेपाल को जुनसुकै ठाउँ बाट आफु नजिकैको \"AMBULANCE\" लाई \"CALL\" गर्न सक्नु हुन्छ । यो सेवा पूर्णतया नि:शुल्क छ ।" +
                    " Download Link: https://play.google.com/store/apps/details?id=" + getPackageName();
            String shareSubject = "Sharing BLOOD CONNECT App";
            sharingIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
            sharingIntent.putExtra(Intent.EXTRA_SUBJECT, shareSubject);
            startActivity(Intent.createChooser(sharingIntent, "Share Using"));

        } else if (id == R.id.nav_feedback) {
            Intent intent = new Intent(Intent.ACTION_SEND);
            String[] recipients = {"bloodfornepal2022@gmail.com"};//Add multiple recipients here
            String[] emailcc = {"shyamkishor1439@gmail.com"};//Add multiple recipients here
            intent.putExtra(Intent.EXTRA_EMAIL, recipients);
            intent.putExtra(Intent.EXTRA_SUBJECT, ""); //Add Mail Subject
            intent.putExtra(Intent.EXTRA_TEXT, "");//Add mail body
            intent.putExtra(Intent.EXTRA_BCC, emailcc);//Add BCC email id if any
            intent.setType("text/plain");
            intent.setType("message/rfc822");
            intent.setPackage("com.google.android.gm");//Added Gmail Package to forcefully open Gmail App
            startActivity(Intent.createChooser(intent, "Send mail"));
            return true;
        } else if (id == R.id.nav_about) {
            startActivity(new Intent(MainActivity.this, AboutAppActivity.class));
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public boolean checkConnection() {
        ConnectivityManager manager = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        return manager.getActiveNetworkInfo() != null && manager.getActiveNetworkInfo().isConnectedOrConnecting();

    }


    private void checkUpdate() {

        Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();

        appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                startUpdateFlow(appUpdateInfo);
            } else if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                startUpdateFlow(appUpdateInfo);
            }
        });
    }

    private void startUpdateFlow(AppUpdateInfo appUpdateInfo) {
        try {
            appUpdateManager.startUpdateFlowForResult(appUpdateInfo, AppUpdateType.IMMEDIATE, this, IMMEDIATE_APP_UPDATE_REQ_CODE);
        } catch (IntentSender.SendIntentException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == IMMEDIATE_APP_UPDATE_REQ_CODE) {
            if (resultCode == RESULT_CANCELED) {
                //Toast.makeText(getApplicationContext(), "Update canceled by user! Result Code: " + resultCode, Toast.LENGTH_LONG).show();
            } else if (resultCode == RESULT_OK) {
                Toast.makeText(getApplicationContext(), "Update success! ", Toast.LENGTH_LONG).show();
            } else {
                //Toast.makeText(getApplicationContext(), "Update Failed! Result Code: " + resultCode, Toast.LENGTH_LONG).show();
                checkUpdate();
            }
        } else if (requestCode == REQUEST_LOCATION) {
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


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        if (requestCode == PERMISSION_ID) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            } else {
                count++;
                if (count >= 2) {
                    enableLoc();
                    Toast.makeText(MainActivity.this, "Please allow location permission for using app with fully functional...", Toast.LENGTH_SHORT).show();
                } else {
                    locationDialog.show();
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void getDeviceLocation() {
        if (checkPermissions()) {
            if (isLocationEnabled()) {
                mFusedLocationProviderClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull com.google.android.gms.tasks.Task<Location> task) {
                        Location location = task.getResult();
                        List<Address> addresses = null;
                        if (location == null) {
                            requestNewLocationData();
                        } else {
                            Double lat = location.getLatitude();
                            Double lon = location.getLongitude();

                            try {
                                addresses = geocoder.getFromLocation(lat, lon, 1);
                                if (addresses.size() > 0) {

                                    ssp = getSharedPreferences("LatLong", 0);
                                    SharedPreferences.Editor sspEditor = ssp.edit();
                                    sspEditor.putString("lat", "" + lat);
                                    sspEditor.putString("long", "" + lon);
                                    sspEditor.apply();
                                    loadingDialog.dismiss();
                                    refreshLayout.setRefreshing(false);
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });

            } else {
                enableLoc();
                Toast.makeText(this, "Please turn on your location...", Toast.LENGTH_LONG).show();
            }
        }

    }


    private void requestNewLocationData() {
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
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
        boolean shouldProvideRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_FINE_LOCATION);
        if (!shouldProvideRationale) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    PERMISSION_ID);
        }
    }

    private boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) || locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    @Override
    protected void onResume() {
        super.onResume();
        getDeviceLocation();
    }

    @Override
    public void onRefresh() {
        if (checkConnection()) {
            getDeviceLocation();
            checkUpdate();
            refreshNet.setVisibility(View.GONE);
        } else {
            refreshLayout.setRefreshing(false);
            refreshNet.setVisibility(View.VISIBLE);
        }
    }
}