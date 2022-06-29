package shyam.blood.donation;

import static java.lang.Thread.sleep;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Dialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
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
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.messaging.FirebaseMessaging;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class DonorProfileActivity extends AppCompatActivity implements AcceptToDonateInterface {
    TextView name, donarAddress, bg, donatedTimes, donatedDate, status, phone, aname, aaddress, abg, aphone, anote, showMsg;
    RecyclerView donarRec;
    FirebaseFirestore firestore;
    FirebaseAuth mAuth;
    FirebaseUser user;
    String photo;
    ReqestDonorAdapter adapter;
    List<BloodRequestModel> list = new ArrayList<>();
    Dialog loadingDialog, donateDialog;
    ImageView call, edit;
    Button decline;
    LinearLayout savedData;
    CircleImageView profilePhoto;
    Calendar calendar;
    int year, month, day;
    int y1, y2, m1, m2, d1, d2;

    SharedPreferences sp;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private Location mLastKnownLocation;
    private boolean mLocationPermissionGranted;
    Geocoder geocoder;
    int PERMISSION_ID = 44;
    String catchbg;
    String formattedDate;
    String gender;
    TextView tokenTake;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_donor_profile);

        firestore = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Your Profile");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        savedData = findViewById(R.id.savedDatatoDonate);
        tokenTake = findViewById(R.id.tokenTake);
        aname = findViewById(R.id.volName);
        edit = findViewById(R.id.editProfile);
        showMsg = findViewById(R.id.showMsg);
        aaddress = findViewById(R.id.volAddress);
        abg = findViewById(R.id.rabg);
        profilePhoto = findViewById(R.id.profilePhoto);
        aphone = findViewById(R.id.volPhone);
        anote = findViewById(R.id.ranotes);
        call = findViewById(R.id.volcall);
        decline = findViewById(R.id.acceptedRequest);

        geocoder = new Geocoder(this);
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        loadingDialog = new Dialog(this);
        loadingDialog.setContentView(R.layout.loading_dialog);
        loadingDialog.getWindow().setBackgroundDrawable(getDrawable(R.drawable.loading_item));
        loadingDialog.getWindow().setLayout(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        loadingDialog.setCancelable(false);


        name = findViewById(R.id.donorName);
        donarAddress = findViewById(R.id.donorAddress);
        bg = findViewById(R.id.donorBG);
        status = findViewById(R.id.donorStatus);
        donatedDate = findViewById(R.id.donatedDate);
        donatedTimes = findViewById(R.id.donatedTimes);
        phone = findViewById(R.id.donorPhone);
        donarRec = findViewById(R.id.donorRec);
        user = mAuth.getCurrentUser();
        edit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(DonorProfileActivity.this, EditProfileActivity.class));
            }
        });

        donateDialog = new Dialog(DonorProfileActivity.this);
        donateDialog.setContentView(R.layout.sign_in_dialog);
        donateDialog.setCancelable(true);
        donateDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        Button yesBtn = donateDialog.findViewById(R.id.yesBtn);
        Button noBtn = donateDialog.findViewById(R.id.noBtn);

        calendar = Calendar.getInstance();
        year = calendar.get(Calendar.YEAR);
        month = calendar.get(Calendar.MONTH);
        day = calendar.get(Calendar.DAY_OF_MONTH);
        y1 = year;
        m1 = month + 1;
        d1 = day;
        Date c = Calendar.getInstance().getTime();
        SimpleDateFormat df = new SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault());
        formattedDate = df.format(c);
        int dateCount = y1 * 360 + m1 * 30 + d1;


        donarAddress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getDeviceLocation();
            }
        });
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(RecyclerView.VERTICAL);
        donarRec.setLayoutManager(layoutManager);
        adapter = new ReqestDonorAdapter(DonorProfileActivity.this, list, this);
        donarRec.setAdapter(adapter);
        loadingDialog.show();
        if (user != null && !user.isAnonymous()) {
            firestore.collection("donors").document(user.getUid()).addSnapshotListener(new EventListener<DocumentSnapshot>() {
                @Override
                public void onEvent(@Nullable DocumentSnapshot snapshot, @Nullable FirebaseFirestoreException error) {
                    if (snapshot.exists()) {
                        name.setText(snapshot.get("fName").toString());
                        donarAddress.setText(snapshot.get("address").toString());
                        phone.setText(snapshot.get("phone").toString());
                        donatedDate.setText(snapshot.get("lastDonated").toString());
                        donatedTimes.setText(snapshot.get("history").toString());
                        bg.setText(snapshot.get("bgroup").toString());
                        catchbg = snapshot.get("bgroup").toString();
                        photo = snapshot.get("profile").toString();
                        Glide.with(DonorProfileActivity.this).load(photo).apply(new RequestOptions().placeholder(R.drawable.user1)).into(profilePhoto);
                        status.setText(snapshot.get("status").toString());

                        if (!snapshot.get("request").toString().isEmpty()) {
                            donarRec.setVisibility(View.GONE);
                            savedData.setVisibility(View.VISIBLE);
                            firestore.collection("donors").document(user.getUid()).collection("acceptedRequest").document(snapshot.get("request").toString())
                                    .get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                    if (task.isSuccessful()) {
                                        DocumentSnapshot ds = task.getResult();
                                        if (ds.exists()) {
                                            aname.setText(ds.get("name").toString());
                                            aaddress.setText(ds.get("address").toString());
                                            abg.setText(ds.get("bg").toString());
                                            aphone.setText(ds.get("phone").toString());
                                            anote.setText(ds.get("notes").toString());
                                            loadingDialog.dismiss();
                                            call.setOnClickListener(new View.OnClickListener() {
                                                @Override
                                                public void onClick(View view) {
                                                    Intent intent = new Intent(Intent.ACTION_CALL);
                                                    intent.setData(Uri.parse("tel: " + aphone.getText().toString().trim()));
                                                    if (ActivityCompat.checkSelfPermission(DonorProfileActivity.this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                                                        Toast.makeText(DonorProfileActivity.this, "Please grant the permission to call", Toast.LENGTH_SHORT).show();
                                                        requestPermission();
                                                    } else {
                                                        startActivity(intent);
                                                    }
                                                }

                                                private void requestPermission() {
                                                    ActivityCompat.requestPermissions(DonorProfileActivity.this, new String[]{Manifest.permission.CALL_PHONE}, 1);
                                                }
                                            });
                                            decline.setOnClickListener(new View.OnClickListener() {
                                                @Override
                                                public void onClick(View view) {
                                                    loadingDialog.show();
                                                    firestore.collection("donors").document(user.getUid()).collection("acceptedRequest").document(ds.getId()).delete();
                                                    firestore.collection("donors").document(user.getUid()).update("request", "");
                                                    firestore.collection("requests").document(snapshot.get("request").toString()).collection("acceptedList").document(user.getUid()).delete();
                                                    firestore.collection("requests").document(snapshot.get("request").toString()).update("visibility", true);
                                                    firestore.collection("requests").document(snapshot.get("request").toString()).update("donorCount", FieldValue.increment(-1)).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                        @Override
                                                        public void onComplete(@NonNull Task<Void> task) {
                                                            donarRec.setVisibility(View.VISIBLE);
                                                            savedData.setVisibility(View.INVISIBLE);
                                                        }
                                                    });


                                                }
                                            });
                                        } else {
                                            firestore.collection("donors").document(user.getUid()).update("request", "");
                                            donarRec.setVisibility(View.VISIBLE);
                                            savedData.setVisibility(View.INVISIBLE);
                                            loadingDialog.dismiss();
                                        }

                                        firestore.collection("requests").document(ds.getId()).addSnapshotListener(new EventListener<DocumentSnapshot>() {
                                            @Override
                                            public void onEvent(@Nullable DocumentSnapshot snapshot, @Nullable FirebaseFirestoreException error) {
                                                if (snapshot.exists()) {
                                                    loadingDialog.show();
                                                    DocumentReference ref = firestore.collection("requests").document(ds.getId()).collection("acceptedList").document(user.getUid());
                                                    ref.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                                        @Override
                                                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                                            if (task.isSuccessful()) {
                                                                DocumentSnapshot snapshot = task.getResult();
                                                                if (snapshot.exists()) {
                                                                    if (snapshot.get("donated").toString().equalsIgnoreCase("true")) {
                                                                        loadingDialog.dismiss();
                                                                        donateDialog.show();
                                                                        //ref.update("donated", false);
                                                                    }
                                                                } else {
                                                                    donarRec.setVisibility(View.VISIBLE);
                                                                    savedData.setVisibility(View.INVISIBLE);
                                                                    loadingDialog.dismiss();
                                                                    firestore.collection("donors").document(user.getUid()).update("request", "");
                                                                    firestore.collection("donors").document(user.getUid()).
                                                                            collection("acceptedRequest").document().delete();
                                                                }
                                                            } else {
                                                                loadingDialog.dismiss();
                                                                Toast.makeText(DonorProfileActivity.this, "Something went wrong", Toast.LENGTH_SHORT).show();
                                                            }
                                                        }
                                                    });
                                                    loadingDialog.dismiss();
                                                } else {
                                                    firestore.collection("donors").document(user.getUid()).update("request", "");
                                                    firestore.collection("donors").document(user.getUid()).
                                                            collection("acceptedRequest").document().delete();
                                                    donarRec.setVisibility(View.VISIBLE);
                                                    savedData.setVisibility(View.INVISIBLE);
                                                    loadingDialog.dismiss();
                                                }
                                            }
                                        });
                                    }
                                }
                            });
                            yesBtn.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    loadingDialog.show();
                                    firestore.collection("requests").document(snapshot.get("request").toString()).collection("acceptedList").document(user.getUid()).delete();
                                    Map<String, Object> date = new HashMap<>();
                                    date.put("lastDonated", formattedDate);
                                    date.put("dateCounter", dateCount);
                                    date.put("history", FieldValue.increment(1));
                                    firestore.collection("donors").document(user.getUid()).update(date).addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                Intent intent = new Intent(DonorProfileActivity.this, CertifyActivity.class);
                                                intent.putExtra("dName", name.getText().toString());
                                                intent.putExtra("gender", gender);
                                                startActivity(intent);
                                                finish();
                                            } else {
                                                loadingDialog.dismiss();
                                                Toast.makeText(DonorProfileActivity.this, "Something went wrong", Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    });
                                }
                            });
                            noBtn.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    donateDialog.dismiss();
                                }
                            });
                        } else {
                            loadingDialog.show();
                            donarRec.setVisibility(View.VISIBLE);
                            savedData.setVisibility(View.GONE);
                            list.clear();
                            firestore.collection("requests")
                                    .orderBy("time")
                                    .get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                    for (QueryDocumentSnapshot document : task.getResult()) {
                                        if (!document.get("phone").toString().equalsIgnoreCase(phone.getText().toString())) {
                                            if (document.get("group").toString().equalsIgnoreCase(catchbg)) {
                                                if (!document.getId().equals(user.getUid())) {
                                                    if(document.get("visibility").toString().equalsIgnoreCase("true")){
                                                    BloodRequestModel data = new BloodRequestModel(
                                                            document.getId(),
                                                            document.get("pName").toString(),
                                                            "Hospital: " + document.get("hospital").toString(),
                                                            document.get("phone").toString(),
                                                            "Case: " + document.get("notes").toString(),
                                                            document.get("group").toString() + ", " + document.get("pints").toString() + " Pints",
                                                            document.get("bloodType").toString(),
                                                            document.get("time").toString()

                                                    );
                                                    list.add(data);
                                                }}
                                            }
                                        }
                                    }
                                    adapter.notifyDataSetChanged();
                                    if (list.size() == 0) {
                                        showMsg.setText("No Blood requests found for you ");
                                    }
                                    loadingDialog.dismiss();
                                }
                            });
                        }

                        if (snapshot.get("dateCounter").toString().equalsIgnoreCase("0")) {
                            d2 = 0;
                            gender = snapshot.get("gender").toString();
                            countDate(d2, gender);
                        } else {
                            d2 = Integer.parseInt(snapshot.get("dateCounter").toString());
                            gender = snapshot.get("gender").toString();
                            countDate(d2, gender);
                        }

                        loadingDialog.dismiss();
                    } else {
                        finish();
                    }
                }
            });
        } else {
            finish();
        }


        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(new OnCompleteListener<String>() {
            @Override
            public void onComplete(@NonNull Task<String> task) {
                tokenTake.setText(task.getResult());
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            startActivity(new Intent(DonorProfileActivity.this, MainActivity.class));
            finishAffinity();
        }
        return super.onOptionsItemSelected(item);
    }

    public boolean checkConnection() {
        ConnectivityManager manager = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        return manager.getActiveNetworkInfo() != null && manager.getActiveNetworkInfo().isConnectedOrConnecting();

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
                                    //String streetAddress = address.getAddressLine(0);
                                    String streetAddress = address.getLocality() + ", " + address.getSubAdminArea() + ", " + address.getAdminArea();
                                    Map<String, Object> updateDonor = new HashMap<>();
                                    updateDonor.put("latitude", "" + lat);
                                    updateDonor.put("longitude", "" + lon);
                                    updateDonor.put("address", streetAddress);
                                    firestore.collection("donors").document(user.getUid()).update(updateDonor).isSuccessful();
                                    sp = getSharedPreferences("LatLong", 0);
                                    SharedPreferences.Editor sspEditor = sp.edit();
                                    sspEditor.putString("lat", "" + lat);
                                    sspEditor.putString("long", "" + lon);
                                    sspEditor.apply();
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

    private void countDate(int d1, String gender) {
        int today;

        year = calendar.get(Calendar.YEAR);
        month = calendar.get(Calendar.MONTH);
        day = calendar.get(Calendar.DAY_OF_MONTH);
        y2 = year;
        m2 = month + 1;
        d2 = day;
        today = y2 * 360 + m2 * 30 + d2 + 1;
        int remainingDay = today - d1;
        if (gender.equalsIgnoreCase("Male")) {
            if (remainingDay > 0 && remainingDay < 90) {
                int dd = 90 - remainingDay;
                showMsg.setText("" + dd + " days left to become donor");
                donarRec.setVisibility(View.INVISIBLE);
            } else {
                showMsg.setText("Request for You");
                donarRec.setVisibility(View.VISIBLE);
                //send(tokenTake.getText().toString());
                //setAlarm(90);
            }
        }
        if (gender.equalsIgnoreCase("female")) {
            if (remainingDay > 0 && remainingDay < 120) {
                int df = 120 - remainingDay;
                showMsg.setText("" + df + " days left to become donor");
                donarRec.setVisibility(View.INVISIBLE);
            } else {
                showMsg.setText("Request for You");
                donarRec.setVisibility(View.VISIBLE);
                //send(tokenTake.getText().toString());
                //setAlarm(120);
            }

        }

    }

    private void send(String token) {
        if (token.isEmpty()) {
            FirebaseMessaging.getInstance().getToken().addOnCompleteListener(new OnCompleteListener<String>() {
                @Override
                public void onComplete(@NonNull Task<String> task) {
                    tokenTake.setText(task.getResult());
                }
            });
        } else {
            FcmNotificationsSender notificationsSender = new FcmNotificationsSender(
                    token,
                    "Hello " + name.getText().toString() + ",",
                    "You are now eligible to donate blood for needy person, Please checkout your profile to see blood request. Thank you",
                    getApplicationContext(), DonorProfileActivity.this);
            notificationsSender.SendNotifications();
        }
    }

    @Override
    public void onItemClick(String id) {
        loadingDialog.show();
        //SharedPreferences.Editor edito = spp.edit();
        Map<String, Object> map = new HashMap<>();
        map.put("name", name.getText().toString());
        map.put("address", donarAddress.getText().toString());
        map.put("phone", phone.getText().toString());
        map.put("bg", bg.getText().toString());
        map.put("profile", photo);
        map.put("donated", false);


        firestore.collection("requests").document(id).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot sssp = task.getResult();
                    Map<String, Object> request = new HashMap<>();
                    request.put("name", sssp.get("pName").toString());
                    request.put("phone", sssp.get("phone").toString());
                    request.put("address", sssp.get("address").toString());
                    request.put("bg", sssp.get("group").toString());
                    request.put("hospital", sssp.get("hospital").toString());
                    request.put("notes", sssp.get("notes").toString());
                    firestore.collection("donors").document(user.getUid()).collection("acceptedRequest").document(id)
                            .set(request).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            firestore.collection("donors").document(user.getUid()).update("request", id);
                            FcmNotificationsSender notificationsSender = new FcmNotificationsSender(
                                    sssp.get("token").toString(),
                                    "Dear " + sssp.get("cName").toString(),
                                    name.getText().toString() + " accepted your blood request you can contact through call. Thank you",
                                    getApplicationContext(), DonorProfileActivity.this);
                            notificationsSender.SendNotifications();
                            Toast.makeText(DonorProfileActivity.this, "Request Accepted Successfully", Toast.LENGTH_SHORT).show();
                        }
                    });


                    firestore.collection("requests").document(id).update("donorCount", FieldValue.increment(1));
                    firestore.collection("requests").document(id).collection("acceptedList")
                            .document(user.getUid()).set(map)
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        donarRec.setVisibility(View.INVISIBLE);
                                        savedData.setVisibility(View.VISIBLE);
                                        loadingDialog.dismiss();

                                    }
                                }
                            });


                }
            }
        });


    }
}