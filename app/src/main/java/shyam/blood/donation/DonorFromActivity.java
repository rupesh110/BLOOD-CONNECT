package shyam.blood.donation;

import static com.android.volley.VolleyLog.TAG;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.messaging.FirebaseMessaging;
import com.rilixtech.widget.countrycodepicker.CountryCodePicker;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DonorFromActivity extends AppCompatActivity {
    String bglist[], genderList[], genderSelect;
    EditText fname,phoneNo;
    ImageView autoaddress;
    TextView tokentake, fixddress;
    Button register;
    CheckBox agreeDonate;
    FirebaseFirestore firestore;
    FirebaseAuth mAuth;
    Dialog loadingDialog;


    private FusedLocationProviderClient mFusedLocationProviderClient;
    private Location mLastKnownLocation;
    private boolean mLocationPermissionGranted;
    Geocoder geocoder;
    int PERMISSION_ID = 44;
    String selectedbg, wtSelect = "false", userPhone;
    Spinner bgspin, genderSpin;
    TextView apBtn, anBtn, bpBtn, bnBtn, abpBtn, abnBtn, opBtn, onBtn;
    RadioGroup genderGroup, wtGroup;
    boolean bgSelected = false, genderSelected = false, wtSelected = false;
    SharedPreferences ssp;
    CountryCodePicker cpp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_donor_from);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        firestore = FirebaseFirestore.getInstance();
        bgspin = findViewById(R.id.spinner);
        cpp = findViewById(R.id.ccp);
        genderSpin = findViewById(R.id.genderSpin);

        bglist = getResources().getStringArray(R.array.bloodGroup);
        genderList = getResources().getStringArray(R.array.genderList);
        fname = findViewById(R.id.fname);
        tokentake = findViewById(R.id.tokentake);
        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() != null) {
            userPhone = mAuth.getCurrentUser().getPhoneNumber().replace(cpp.getSelectedCountryCodeWithPlus().trim(), "");
        }

        phoneNo = findViewById(R.id.phoneD);
        autoaddress = findViewById(R.id.autoAddress);
        fixddress = findViewById(R.id.address);
        register = findViewById(R.id.register);
        agreeDonate = findViewById(R.id.agreeDonate);
        ssp = getSharedPreferences("LatLong", 0);
        apBtn = findViewById(R.id.apBtn);
        anBtn = findViewById(R.id.anBtn);
        bpBtn = findViewById(R.id.bpBtn);
        bnBtn = findViewById(R.id.bnBtn);
        abpBtn = findViewById(R.id.abpBtn);
        abnBtn = findViewById(R.id.abnBtn);
        opBtn = findViewById(R.id.opBtn);
        onBtn = findViewById(R.id.onBtn);
        genderGroup = findViewById(R.id.genderItem);
        wtGroup = findViewById(R.id.wtGroup);
        loadingDialog = new Dialog(this);
        loadingDialog.setContentView(R.layout.loading_dialog);
        loadingDialog.getWindow().setBackgroundDrawable(getDrawable(R.drawable.loading_item));
        loadingDialog.getWindow().setLayout(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        loadingDialog.setCancelable(true);
        phoneNo.setText(userPhone);
        apBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                changeBG();
                bgSelected = true;
                selectedbg = "A+";
                apBtn.setTextColor(getResources().getColor(R.color.white));
                apBtn.setBackgroundDrawable(getDrawable(R.drawable.bgrp_bg2));
            }
        });
        bpBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                changeBG();
                bgSelected = true;
                selectedbg = "B+";
                bpBtn.setTextColor(getResources().getColor(R.color.white));
                bpBtn.setBackgroundDrawable(getDrawable(R.drawable.bgrp_bg2));
            }
        });
        abpBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                changeBG();
                bgSelected = true;
                selectedbg = "AB+";
                abpBtn.setTextColor(getResources().getColor(R.color.white));
                abpBtn.setBackgroundDrawable(getDrawable(R.drawable.bgrp_bg2));
            }
        });
        bnBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                changeBG();
                bgSelected = true;
                selectedbg = "B-";
                bnBtn.setTextColor(getResources().getColor(R.color.white));
                bnBtn.setBackgroundDrawable(getDrawable(R.drawable.bgrp_bg2));
            }
        });
        anBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                changeBG();
                bgSelected = true;
                selectedbg = "A-";
                anBtn.setTextColor(getResources().getColor(R.color.white));
                anBtn.setBackgroundDrawable(getDrawable(R.drawable.bgrp_bg2));
            }
        });
        abnBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                changeBG();
                bgSelected = true;
                selectedbg = "AB-";
                abnBtn.setTextColor(getResources().getColor(R.color.white));
                abnBtn.setBackgroundDrawable(getDrawable(R.drawable.bgrp_bg2));
            }
        });
        opBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                changeBG();
                bgSelected = true;
                selectedbg = "O+";
                opBtn.setTextColor(getResources().getColor(R.color.white));
                opBtn.setBackgroundDrawable(getDrawable(R.drawable.bgrp_bg2));
            }
        });
        onBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                changeBG();
                bgSelected = true;
                selectedbg = "O-";
                onBtn.setTextColor(getResources().getColor(R.color.white));
                onBtn.setBackgroundDrawable(getDrawable(R.drawable.bgrp_bg2));
            }
        });
        ConstraintLayout ll2 = findViewById(R.id.wtShow);
        genderGroup.clearCheck();
        wtGroup.clearCheck();
        ll2.setVisibility(View.GONE);
        RadioButton wt1 = findViewById(R.id.wt1);
        RadioButton wt2 = findViewById(R.id.wt2);
        RadioButton wt3 = findViewById(R.id.wt3);

        // Add the Listener to the RadioGroup
        genderGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group,
                                         int checkedId) {
                RadioButton radioButton = (RadioButton) group.findViewById(checkedId);
                if (radioButton.getText().toString().equalsIgnoreCase("Male")) {
                    genderSelected = true;
                    ll2.setVisibility(View.VISIBLE);
                    genderSelect = "Male";
                    wt2.setVisibility(View.GONE);
                    wt1.setVisibility(View.VISIBLE);
                    wt3.setVisibility(View.VISIBLE);
                } else if (radioButton.getText().toString().equalsIgnoreCase("Female")) {
                    ll2.setVisibility(View.VISIBLE);
                    genderSelected = true;
                    genderSelect = "Female";
                    wt1.setVisibility(View.GONE);
                    wt2.setVisibility(View.VISIBLE);
                    wt3.setVisibility(View.VISIBLE);
                } else if (radioButton.getText().toString().equalsIgnoreCase("Others")) {
                    ll2.setVisibility(View.VISIBLE);
                    genderSelected = true;
                    genderSelect = "Others";
                    wt2.setVisibility(View.GONE);
                    wt1.setVisibility(View.VISIBLE);
                    wt3.setVisibility(View.VISIBLE);
                } else {
                    ll2.setVisibility(View.GONE);
                }
                //Toast.makeText(DonorFromActivity.this, "" + radioButton.getText(), Toast.LENGTH_SHORT).show();
            }
        });
        wtGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                //RadioButton radioButton = (RadioButton) radioGroup.findViewById(i);
                if (i==2131362624) {
                    wtSelect = "true";
                } else {
                    wtSelect = "false";
                }
            }
        });

        geocoder = new Geocoder(this);
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        getDeviceLocation();

        autoaddress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getDeviceLocation();
            }
        });


        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(new OnCompleteListener<String>() {
            @Override
            public void onComplete(@NonNull Task<String> task) {
                tokentake.setText(task.getResult());

            }
        });

        register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mAuth.getCurrentUser()!=null){
                    if (!TextUtils.isEmpty(fname.getText())) {
                    if (!TextUtils.isEmpty(phoneNo.getText()) && phoneNo.getText().length() == 10) {
                        if (!TextUtils.isEmpty(fixddress.getText()) ) {
                            if (bgSelected) {
                                if (genderSelected) {
                                    if (agreeDonate.isChecked()) {
                                        Map<String, Object> person = new HashMap<>();
                                        person.put("fName", fname.getText().toString());
                                        person.put("address", fixddress.getText().toString());
                                        person.put("bgroup", selectedbg);
                                        person.put("email", "");
                                        person.put("gender", genderSelect);
                                        person.put("status", "Active");
                                        person.put("token", tokentake.getText().toString());
                                        person.put("profile", "");
                                        person.put("history", 0);
                                        person.put("latitude", ssp.getString("lat", ""));
                                        person.put("longitude", ssp.getString("long", ""));
                                        person.put("wtSelect", wtSelect);
                                        person.put("dateCounter", 0);
                                        person.put("request", "");
                                        person.put("lastDonated", "Not Donated yet");
                                        person.put("phone", phoneNo.getText().toString().trim());
                                        firestore.collection("donors").document(mAuth.getCurrentUser().getUid()).set(person).addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                if (task.isSuccessful()) {
                                                    startActivity(new Intent(DonorFromActivity.this, DonorProfileActivity.class));
                                                    finish();
                                                    Toast.makeText(DonorFromActivity.this, "User successfully added", Toast.LENGTH_SHORT).show();
                                                } else {
                                                    String error = task.getException().getMessage();
                                                    Toast.makeText(DonorFromActivity.this, error, Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                        });

                                    } else
                                        Toast.makeText(DonorFromActivity.this, "Please agree to donate!", Toast.LENGTH_SHORT).show();
                                } else
                                    Toast.makeText(DonorFromActivity.this, "Please select your gender", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(DonorFromActivity.this, "Please select your blood group", Toast.LENGTH_SHORT).show();
                            }
                        } else
                            fixddress.setError("Please select address");
                    } else {

                    }


                } else
                    fname.setError("Please fill your name!!");
            } else {
                    Toast.makeText(DonorFromActivity.this, "Please try again!", Toast.LENGTH_SHORT).show();
                }
                }

        });

    }

    private void changeBG() {
        apBtn.setTextColor(getResources().getColor(R.color.primary));
        anBtn.setTextColor(getResources().getColor(R.color.primary));
        bpBtn.setTextColor(getResources().getColor(R.color.primary));
        bnBtn.setTextColor(getResources().getColor(R.color.primary));
        abpBtn.setTextColor(getResources().getColor(R.color.primary));
        abnBtn.setTextColor(getResources().getColor(R.color.primary));
        opBtn.setTextColor(getResources().getColor(R.color.primary));
        onBtn.setTextColor(getResources().getColor(R.color.primary));
        apBtn.setBackgroundDrawable(getDrawable(R.drawable.bgrp_bg1));
        anBtn.setBackgroundDrawable(getDrawable(R.drawable.bgrp_bg1));
        bpBtn.setBackgroundDrawable(getDrawable(R.drawable.bgrp_bg1));
        bnBtn.setBackgroundDrawable(getDrawable(R.drawable.bgrp_bg1));
        abpBtn.setBackgroundDrawable(getDrawable(R.drawable.bgrp_bg1));
        abnBtn.setBackgroundDrawable(getDrawable(R.drawable.bgrp_bg1));
        opBtn.setBackgroundDrawable(getDrawable(R.drawable.bgrp_bg1));
        onBtn.setBackgroundDrawable(getDrawable(R.drawable.bgrp_bg1));
    }

    @SuppressLint("MissingPermission")
    private void getDeviceLocation() {
        if (checkPermissions()) {
            if (isLocationEnabled()) {
                loadingDialog.show();
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
                            try {
                                addresses = geocoder.getFromLocation(lat, lon, 1);
                                if (addresses.size() > 0) {
                                    Address address = addresses.get(0);
                                    SharedPreferences.Editor sspEditor = ssp.edit();
                                    sspEditor.putString("lat", "" + lat);
                                    sspEditor.putString("long", "" + lon);
                                    sspEditor.apply();
                                    String streetAddress = address.getLocality() + ", " + address.getSubAdminArea() + ", " + address.getAdminArea();
                                    fixddress.setText(streetAddress);
                                    loadingDialog.dismiss();
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });

            } else {
                Toast.makeText(this, "Please turn on" + " your location...", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        } else {
            getLocationPermission();
        }
    }

    public boolean checkConnection() {
        ConnectivityManager manager = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        return manager.getActiveNetworkInfo() != null && manager.getActiveNetworkInfo().isConnectedOrConnecting();

    }

    @SuppressLint("MissingPermission")
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
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ;
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
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            startActivity(new Intent(DonorFromActivity.this, MainActivity.class));
        }
        return super.onOptionsItemSelected(item);
    }

}