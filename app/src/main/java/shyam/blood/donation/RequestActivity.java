package shyam.blood.donation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.ImageViewTarget;
import com.bumptech.glide.request.transition.Transition;
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
import com.google.android.libraries.places.api.Places;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class RequestActivity extends AppCompatActivity {
    EditText patient, phone, notes, hospital, cPerson;
    TextView fixddress;
    Spinner pint1, pint2, pint3, pint4;
    Button sendReqeust;
    ImageView autoaddress;
    FirebaseFirestore firestore;
    FirebaseAuth mAuth;
    String pintList1[], pintList2[], pintList3[], pintList4[]; //1. fresh blood      2. prp   3. pcv   4.ffp
    String selectedbg;
    int freshPint, prpPint, pcvPint, ffpPint, totalPint;

    private FusedLocationProviderClient mFusedLocationProviderClient;
    private Location mLastKnownLocation;
    private boolean mLocationPermissionGranted;
    Geocoder geocoder;
    int PERMISSION_ID = 44;
    Double latitude, longitude;
    SharedPreferences  askbank, ssp;
    private Dialog askBankDialog, loadingDialog;
    TextView apBtn, anBtn, bpBtn, bnBtn, abpBtn, abnBtn, opBtn, onBtn;
    boolean bgSelected = false;
    CheckBox freshB, prpB, pcvB, ffpB;
    String fresh, prp, pcv, ffp, totalBlood;
    LinearLayout sp1, sp2, sp3, sp4;
    private GoogleApiClient googleApiClient;
    final static int REQUEST_LOCATION = 199;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        loadingDialog = new Dialog(this);
        loadingDialog.setContentView(R.layout.loading_dialog);
        loadingDialog.getWindow().setBackgroundDrawable(getDrawable(R.drawable.loading_item));
        loadingDialog.getWindow().setLayout(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        loadingDialog.setCancelable(true);
        sp1 = findViewById(R.id.sp1);
        sp2 = findViewById(R.id.sp2);
        sp3 = findViewById(R.id.sp3);
        sp4 = findViewById(R.id.sp4);

        firestore = FirebaseFirestore.getInstance();
        geocoder = new Geocoder(this);
        mAuth = FirebaseAuth.getInstance();
        patient = findViewById(R.id.pName);
        cPerson = findViewById(R.id.cName);
        phone = findViewById(R.id.phone);
        fixddress = findViewById(R.id.address);
        notes = findViewById(R.id.notes);
        hospital = findViewById(R.id.hospital);
        pint1 = findViewById(R.id.spinner1);
        pint2 = findViewById(R.id.spinner2);
        pint3 = findViewById(R.id.spinner3);
        pint4 = findViewById(R.id.spinner4);
        freshB = findViewById(R.id.freshB);
        prpB = findViewById(R.id.prpB);
        pcvB = findViewById(R.id.pcvB);
        ffpB = findViewById(R.id.ffpB);


        sendReqeust = findViewById(R.id.sendRequest);
        autoaddress = findViewById(R.id.autoAddress);
        askbank = getSharedPreferences("AskBank", 0);
        SharedPreferences.Editor bankEditor = askbank.edit();
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        getDeviceLocation();
        apBtn = findViewById(R.id.apBtn);
        anBtn = findViewById(R.id.anBtn);
        bpBtn = findViewById(R.id.bpBtn);
        bnBtn = findViewById(R.id.bnBtn);
        abpBtn = findViewById(R.id.abpBtn);
        abnBtn = findViewById(R.id.abnBtn);
        opBtn = findViewById(R.id.opBtn);
        onBtn = findViewById(R.id.onBtn);

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
/// ask for blood bank for the first time

        askBankDialog = new Dialog(RequestActivity.this);
        askBankDialog.setContentView(R.layout.ask_for_bank);
        askBankDialog.setCancelable(false);
        askBankDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        Button yesBtn = askBankDialog.findViewById(R.id.yesBtn);
        Button noBtn = askBankDialog.findViewById(R.id.noBtn);
        if (askbank.getString("bank", "").isEmpty()) {
            askBankDialog.show();
        }
        yesBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                askBankDialog.dismiss();
                bankEditor.putString("bank", "yes");
                bankEditor.apply();
            }
        });
        noBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bankEditor.putString("bank", "yes");
                bankEditor.apply();
                startActivity(new Intent(RequestActivity.this, BloodBankActivity.class));
                finish();
            }
        });
        /// ask for blood bank for the first time
      if (checkConnection()){
          if (mAuth.getCurrentUser()!=null){
              firestore.collection("requests").document(FirebaseAuth.getInstance().getCurrentUser().getUid()).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                  @Override
                  public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                      if (task.isSuccessful()){
                          DocumentSnapshot snapshot = task.getResult();
                          if (snapshot.exists()){
                              if (!snapshot.get("success").toString().equalsIgnoreCase("true")){
                                  startActivity(new Intent(RequestActivity.this, RequestDetailActivity.class));
                                  finish();
                              }
                          }
                      }
                  }
              });
          }

      }else {
          Toast.makeText(RequestActivity.this, "No internet Connection!!", Toast.LENGTH_SHORT).show();
      }


        pint1 = (Spinner) findViewById(R.id.spinner1);
        pint2 = (Spinner) findViewById(R.id.spinner2);
        pint3 = (Spinner) findViewById(R.id.spinner3);
        pint4 = (Spinner) findViewById(R.id.spinner4);
        pintList1 = getResources().getStringArray(R.array.pintList);
        pintList2 = getResources().getStringArray(R.array.pintList);
        pintList3 = getResources().getStringArray(R.array.pintList);
        pintList4 = getResources().getStringArray(R.array.pintList);


        autoaddress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getDeviceLocation();
            }
        });


        ArrayAdapter pintAdapter1 = new ArrayAdapter(this, android.R.layout.simple_spinner_item, pintList1);
        ArrayAdapter pintAdapter2 = new ArrayAdapter(this, android.R.layout.simple_spinner_item, pintList2);
        ArrayAdapter pintAdapter3 = new ArrayAdapter(this, android.R.layout.simple_spinner_item, pintList3);
        ArrayAdapter pintAdapter4 = new ArrayAdapter(this, android.R.layout.simple_spinner_item, pintList4);
        pintAdapter1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        pintAdapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        pintAdapter3.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        pintAdapter4.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        pint1.setAdapter(pintAdapter1);
        pint2.setAdapter(pintAdapter2);
        pint3.setAdapter(pintAdapter3);
        pint4.setAdapter(pintAdapter4);


        pint1.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                freshPint = Integer.parseInt(pintList1[position]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                freshPint = 0;
            }
        });
        pint2.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                prpPint = Integer.parseInt(pintList2[position]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                prpPint = 0;
            }
        });
        pint3.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                pcvPint = Integer.parseInt(pintList3[position]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                pcvPint = 0;
            }
        });
        pint4.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                ffpPint = Integer.parseInt(pintList4[position]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                ffpPint = 0;
            }
        });

        freshB.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    sp1.setVisibility(View.VISIBLE);
                } else {
                    sp1.setVisibility(View.GONE);
                }
            }
        });
        prpB.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    sp2.setVisibility(View.VISIBLE);
                } else {
                    sp2.setVisibility(View.GONE);
                }
            }
        });
        pcvB.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    sp3.setVisibility(View.VISIBLE);
                } else {
                    sp3.setVisibility(View.GONE);
                }
            }
        });
        ffpB.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    sp4.setVisibility(View.VISIBLE);
                } else {
                    sp4.setVisibility(View.GONE);
                }
            }
        });


        sendReqeust.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (checkConnection()) {
                    if (freshB.isChecked()) {
                        fresh = "Fresh Blood(" + freshPint + ")";
                    } else {
                        fresh = "";
                        freshPint = 0;
                    }
                    if (prpB.isChecked()) {
                        prp = "  PRP(" + prpPint + ")";
                    } else {
                        prp = "";
                        prpPint = 0;
                    }
                    if (pcvB.isChecked()) {
                        pcv = "  PCV(" + pcvPint + ")";
                    } else {
                        pcv = "";
                        pcvPint = 0;
                    }
                    if (ffpB.isChecked()) {
                        ffp = "  FFP(" + ffpPint + ")";
                    } else {
                        ffp = "";
                        ffpPint = 0;
                    }
                    totalBlood = fresh + prp + pcv + ffp;
                    totalPint = freshPint + prpPint + ffpPint + pcvPint;
                    if (!patient.getText().toString().isEmpty()) {
                        if (!cPerson.getText().toString().isEmpty()) {
                            if (!phone.getText().toString().isEmpty() && phone.getText().length() == 10) {
                                if (!fixddress.getText().toString().isEmpty() || fixddress.getText().toString().equalsIgnoreCase("Nothing selected")) {
                                    if (bgSelected) {
                                        if (!hospital.getText().toString().isEmpty()) {
                                            if (!totalBlood.equalsIgnoreCase("")) {
                                                if (latitude != null || longitude != null) {
                                                    if (totalPint > 0) {
                                                        uploadField(totalBlood);
                                                    } else {
                                                        Toast.makeText(RequestActivity.this, "Please select pints of blood", Toast.LENGTH_SHORT).show();
                                                    }
                                                } else {
                                                    Toast.makeText(RequestActivity.this, "Please locate your address once again!", Toast.LENGTH_SHORT).show();
                                                }
                                            } else {
                                                Toast.makeText(RequestActivity.this, "Please select blood constituents!", Toast.LENGTH_SHORT).show();
                                            }
                                        } else {
                                            Toast.makeText(RequestActivity.this, "Please fill hospital name and address!", Toast.LENGTH_SHORT).show();
                                        }
                                    } else {
                                        Toast.makeText(RequestActivity.this, "Please select blood group", Toast.LENGTH_SHORT).show();
                                    }
                                } else {
                                    fixddress.setError("Please select address");
                                }
                            } else {
                                phone.setError("10 digit required");
                            }

                        } else {
                            cPerson.setError("Please fill contact person name");
                        }
                    } else {
                        patient.setError("Please fill patient name!!");
                    }
                }
                else {
                    Toast.makeText(RequestActivity.this, "No Internet Connection!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }
    private void enableLoc() {

        if (googleApiClient == null) {
            googleApiClient = new GoogleApiClient.Builder(RequestActivity.this)
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
                                status.startResolutionForResult(RequestActivity.this, REQUEST_LOCATION);

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
    public boolean checkConnection() {
        ConnectivityManager manager = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        return manager.getActiveNetworkInfo() != null && manager.getActiveNetworkInfo().isConnectedOrConnecting();

    }

    private void uploadField(String blood) {
        Intent intent = new Intent(RequestActivity.this, VerifyRequest.class);
        intent.putExtra("pName", patient.getText().toString());
        intent.putExtra("cName", cPerson.getText().toString());
        intent.putExtra("address", fixddress.getText().toString());
        intent.putExtra("phone", phone.getText().toString().trim());
        intent.putExtra("hospital", hospital.getText().toString().trim());
        intent.putExtra("notes", notes.getText().toString().trim());
        intent.putExtra("bloodType", blood);
        intent.putExtra("group", selectedbg);
        intent.putExtra("pint", "" + totalPint);
        startActivity(intent);
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
                                    latitude = lat;
                                    longitude = lon;
                                    ssp = getSharedPreferences("LatLong", 0);
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
            }, 44);

        }
    }

    private boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }


}