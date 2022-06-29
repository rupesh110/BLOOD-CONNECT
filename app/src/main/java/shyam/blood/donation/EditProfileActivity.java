package shyam.blood.donation;

import static com.android.volley.VolleyLog.TAG;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import com.android.volley.VolleyLog;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.ImageViewTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
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
import com.google.android.gms.safetynet.SafetyNet;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.firebase.FirebaseApp;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.safetynet.SafetyNetAppCheckProviderFactory;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class EditProfileActivity extends AppCompatActivity {
    private CircleImageView circleImageView;
    private ImageView autoaddress;
    private EditText nameField, emailField, addressField, phoneField;
    private Dialog loadingDialog;
    private String name, email, photo;
    private Uri imageUri = null;
    private boolean updatePhoto = false;
    Spinner bgSpinner, genderSpinner;
    CheckBox status;
    Button updateBtn, changePhotoBtn;
    FirebaseUser user;
    Dialog logoutDialog;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private Location mLastKnownLocation;
    private boolean mLocationPermissionGranted;
    Geocoder geocoder;
    int PERMISSION_ID = 44;
    String selectedbg, dstatus;
    String bglist[], genderList[], genderSelect;
    Button cancelBtn, logoutBtn;
    private GoogleApiClient googleApiClient;
    final static int REQUEST_LOCATION = 199;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Edit Profile");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        circleImageView = findViewById(R.id.profileImage);
        changePhotoBtn = findViewById(R.id.changeImage);
        nameField = findViewById(R.id.fname);
        emailField = findViewById(R.id.donorEmail);
        addressField = findViewById(R.id.address);
        phoneField = findViewById(R.id.phone);
        autoaddress = findViewById(R.id.autoAddress);
        bgSpinner = findViewById(R.id.spinner);
        genderSpinner = findViewById(R.id.genderSpin);
        status = findViewById(R.id.available);
        updateBtn = findViewById(R.id.update);
        bglist = getResources().getStringArray(R.array.bloodGroup);
        genderList = getResources().getStringArray(R.array.genderList);
        user = FirebaseAuth.getInstance().getCurrentUser();

        loadingDialog = new Dialog(EditProfileActivity.this);
        loadingDialog.setContentView(R.layout.loading_dialog);
        loadingDialog.setCancelable(true);
        loadingDialog.getWindow().setBackgroundDrawable(this.getDrawable(R.drawable.slider_background));
        loadingDialog.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        logoutDialog = new Dialog(EditProfileActivity.this);
        logoutDialog.setContentView(R.layout.logout);
        logoutDialog.setCancelable(true);
        logoutDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        logoutBtn = logoutDialog.findViewById(R.id.yesDel);
        cancelBtn = logoutDialog.findViewById(R.id.noDel);

        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                logoutDialog.dismiss();
            }
        });
        logoutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (user != null) {

                    CollectionReference ref = FirebaseFirestore.getInstance().collection("requests").document(user.getUid()).collection("acceptedList");
                    ref.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            for (QueryDocumentSnapshot snapshot : task.getResult()) {
                                ref.document(snapshot.getId()).delete();
                            }
                        }
                    });
                    FirebaseFirestore.getInstance().collection("requests").document(user.getUid()).delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                FirebaseAuth.getInstance().signOut();
                                startActivity(new Intent(EditProfileActivity.this, MainActivity.class));
                                finish();
                                Toast.makeText(EditProfileActivity.this, "logout successfully", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(EditProfileActivity.this, "Something went wrong!", Toast.LENGTH_SHORT).show();
                            }

                        }
                    });
                }
            }
        });


        ArrayAdapter aa = new ArrayAdapter(this, android.R.layout.simple_spinner_item, bglist);
        ArrayAdapter gender = new ArrayAdapter(this, android.R.layout.simple_spinner_item, genderList);
        aa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        gender.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        bgSpinner.setAdapter(aa);
        genderSpinner.setAdapter(gender);

        genderSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                genderSelect = genderList[position];
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        bgSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                selectedbg = bglist[position];
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        FirebaseFirestore.getInstance().collection("donors").document(user.getUid()).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot snapshot = task.getResult();
                    nameField.setText(snapshot.get("fName").toString());
                    emailField.setText(snapshot.get("email").toString());
                    addressField.setText(snapshot.get("address").toString());
                    phoneField.setText(snapshot.get("phone").toString());
                    Glide.with(EditProfileActivity.this).load(snapshot.get("profile").toString()).apply(new RequestOptions().placeholder(R.drawable.user1)).into(circleImageView);
                    int bg = aa.getPosition(snapshot.get("bgroup").toString());
                    bgSpinner.setSelection(bg);
                    int gg = gender.getPosition(snapshot.get("gender").toString());
                    genderSpinner.setSelection(gg);
                    if (snapshot.get("status").toString().equalsIgnoreCase("Active")) {
                        status.setChecked(true);
                    } else {
                        status.setChecked(false);
                    }

                } else {
                    Toast.makeText(EditProfileActivity.this, "Something went wrong!", Toast.LENGTH_SHORT).show();
                }
            }
        });
        geocoder = new Geocoder(this);
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        changePhotoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent galleryIntent = new Intent(Intent.ACTION_PICK);
                galleryIntent.setType("image/*");
                startActivityForResult(galleryIntent, 1);

            }
        });


        autoaddress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getDeviceLocation();
            }
        });
        updateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (status.isChecked()) {
                    dstatus = "Active";
                } else if (!status.isChecked()) {
                    dstatus = "Inactive";
                }
                updatePhoto(user);
                //checkEmailandPassword();
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            startActivity(new Intent(EditProfileActivity.this, DonorProfileActivity.class));
            finishAffinity();
        } else if (item.getItemId() == R.id.logout) {
            if (checkConnection()){
                FirebaseFirestore.getInstance().collection("requests").document(user.getUid()).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()){
                            DocumentSnapshot snapshot = task.getResult();
                            if (snapshot.exists()){
                                logoutDialog.show();
                            }else {
                                FirebaseAuth.getInstance().signOut();
                                Toast.makeText(EditProfileActivity.this, "Logout Success", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });

            }else {
                Toast.makeText(EditProfileActivity.this, "No Internet Connection!!", Toast.LENGTH_SHORT).show();
            }
        }
        return super.onOptionsItemSelected(item);
    }



    public boolean checkConnection() {
        ConnectivityManager manager = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        return manager.getActiveNetworkInfo() != null && manager.getActiveNetworkInfo().isConnectedOrConnecting();

    }

    private void checkEmailandPassword() {
        String emailPattern = "[a-zA-z0-9._-]+@[a-z]+.[a-z]+";
        if (emailField.getText().toString().matches(emailPattern)) {
            loadingDialog.show();
            updatePhoto(user);
        } else {
            emailField.setError("Invalid Email!");
        }
    }

    private void updatePhoto(final FirebaseUser user) {
        loadingDialog.show();
        FirebaseApp.initializeApp(/*context=*/ this);
        FirebaseAppCheck firebaseAppCheck = FirebaseAppCheck.getInstance();
        firebaseAppCheck.installAppCheckProviderFactory(
                SafetyNetAppCheckProviderFactory.getInstance());
        ///////updating photo
        if (updatePhoto) {
            final StorageReference storageReference = FirebaseStorage.getInstance().getReference().child("profile/" + user.getUid() + ".jpg");
            if (imageUri != null) {

                                    Glide.with(this).asBitmap().load(imageUri).circleCrop().into(new ImageViewTarget<Bitmap>(circleImageView) {
                                        @Override
                                        public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {

                                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                            resource.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                                            byte[] data = baos.toByteArray();

                                            UploadTask uploadTask = storageReference.putBytes(data);
                                            uploadTask.addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                                                @Override
                                                public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                                                    if (task.isSuccessful()) {
                                                        storageReference.getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<Uri> task) {
                                                                if (task.isSuccessful()) {
                                                                    imageUri = task.getResult();
                                                                    photo = task.getResult().toString();
                                                                    Glide.with(EditProfileActivity.this).load(photo).into(circleImageView);

                                                                    Map<String, Object> person = new HashMap<>();
                                                                    person.put("fName", nameField.getText().toString());
                                                                    person.put("address", addressField.getText().toString());
                                                                    person.put("email", emailField.getText().toString());
                                                                    person.put("bgroup", selectedbg);
                                                                    person.put("gender", genderSelect);
                                                                    person.put("status", dstatus);
                                                                    person.put("profile", photo);
                                                                    person.put("phone", phoneField.getText().toString().trim());
                                                                    updateFields(user, person);

                                                                } else {
                                                                    loadingDialog.dismiss();
                                                                    photo = "";
                                                                    String error = task.getException().getMessage();
                                                                    Toast.makeText(EditProfileActivity.this, error, Toast.LENGTH_SHORT).show();
                                                                }
                                                            }
                                                        });
                                                    } else {
                                                        loadingDialog.dismiss();
                                                        String error = task.getException().getMessage();
                                                        Toast.makeText(EditProfileActivity.this, error, Toast.LENGTH_SHORT).show();
                                                    }
                                                }
                                            });

                                            return;
                                        }

                                        @Override
                                        protected void setResource(@Nullable Bitmap resource) {

                                            circleImageView.setImageResource(R.drawable.user1);
                                        }
                                    });


            } else {
                //////remove photo
                storageReference.delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {

                            photo = "";


                            Map<String, Object> person = new HashMap<>();
                            person.put("fName", nameField.getText().toString());
                            person.put("address", addressField.getText().toString());
                            person.put("email", emailField.getText().toString());
                            person.put("bgroup", selectedbg);
                            person.put("gender", genderSelect);
                            person.put("status", dstatus);
                            person.put("profile", photo);
                            person.put("phone", phoneField.getText().toString().trim());
                            updateFields(user, person);
                        } else {
                            String error = task.getException().getMessage();
                            Toast.makeText(EditProfileActivity.this, error, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        } else {
            Map<String, Object> person = new HashMap<>();
            person.put("fName", nameField.getText().toString());
            person.put("address", addressField.getText().toString());
            person.put("email", emailField.getText().toString());
            person.put("phone", phoneField.getText().toString().trim());
            person.put("bgroup", selectedbg);
            person.put("gender", genderSelect);
            person.put("status", dstatus);
            updateFields(user, person);
        }
        /////updating photo
    }



    private void updateFields(FirebaseUser user, final Map<String, Object> updateData) {
        FirebaseFirestore.getInstance().collection("donors").document(user.getUid()).update(updateData).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    loadingDialog.dismiss();
                    finish();
                    Toast.makeText(EditProfileActivity.this, "Successfully updated!", Toast.LENGTH_SHORT).show();
                } else {
                    String error = task.getException().getMessage();
                    Toast.makeText(EditProfileActivity.this, error, Toast.LENGTH_SHORT).show();
                }
                loadingDialog.dismiss();
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                if (data != null) {
                    imageUri = data.getData();
                    updatePhoto = true;
                    Glide.with(EditProfileActivity.this).load(imageUri).into(circleImageView);
                } else {
                    imageUri = null;
                    Toast.makeText(EditProfileActivity.this, "Image not found!!", Toast.LENGTH_SHORT).show();
                }
            }
        }else  if (requestCode == REQUEST_LOCATION) {
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 2) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Intent galleryIntent = new Intent(Intent.ACTION_PICK);
                galleryIntent.setType("image/*");
                startActivityForResult(galleryIntent, 1);
            } else {
                Toast.makeText(EditProfileActivity.this, "Permission Denied!", Toast.LENGTH_SHORT).show();
            }
        }
    }


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
                                    String streetAddress = address.getLocality() + ", " + address.getSubAdminArea() + ", " + address.getAdminArea();
                                    addressField.setText(streetAddress);
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

            }
        } else {
            getLocationPermission();
        }
    }
    private void enableLoc() {

        if (googleApiClient == null) {
            googleApiClient = new GoogleApiClient.Builder(EditProfileActivity.this)
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
                                status.startResolutionForResult(EditProfileActivity.this, REQUEST_LOCATION);

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
            }, PERMISSION_ID);

        }
    }

    private boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }
}