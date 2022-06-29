package shyam.blood.donation;

import static com.google.firebase.messaging.Constants.MessageNotificationKeys.TAG;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.geofire.GeoFireUtils;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.messaging.FirebaseMessaging;

import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class VerifyRequest extends AppCompatActivity {
    Button confirm;
    FirebaseAuth auth;
    FirebaseFirestore firestore;
    SharedPreferences ssp;
    Dialog loadingDialog;
    TextView cName, pName, phone, location, hname, bg, blood, cases, pints, token;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_request);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        auth = FirebaseAuth.getInstance();
        token = findViewById(R.id.tt);
        ssp = getSharedPreferences("LatLong", 0);
        firestore = FirebaseFirestore.getInstance();
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(new OnCompleteListener<String>() {
            @Override
            public void onComplete(@NonNull Task<String> task) {
                if (task.isSuccessful()) {
                    token.setText(task.getResult());
                }
            }
        });
        if (auth.getCurrentUser() == null) {
            auth.signInAnonymously().addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful()) {
                    } else {
                        Toast.makeText(VerifyRequest.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
        loadingDialog = new Dialog(this);
        loadingDialog.setContentView(R.layout.loading_dialog);
        loadingDialog.getWindow().setBackgroundDrawable(getDrawable(R.drawable.loading_item));
        loadingDialog.getWindow().setLayout(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        loadingDialog.setCancelable(false);
        cName = findViewById(R.id.rcName);
        pName = findViewById(R.id.rpName);
        phone = findViewById(R.id.rphone);
        location = findViewById(R.id.rlocation);
        hname = findViewById(R.id.rhospit);
        bg = findViewById(R.id.rbg);
        blood = findViewById(R.id.rblood);
        cases = findViewById(R.id.rcase);
        pints = findViewById(R.id.rpints);
        cName.setText(getIntent().getStringExtra("cName"));
        pName.setText(getIntent().getStringExtra("pName"));
        phone.setText(getIntent().getStringExtra("phone"));
        location.setText(getIntent().getStringExtra("address"));
        hname.setText(getIntent().getStringExtra("hospital"));
        bg.setText(getIntent().getStringExtra("group"));
        blood.setText(getIntent().getStringExtra("bloodType"));
        cases.setText(getIntent().getStringExtra("notes"));
        pints.setText(getIntent().getStringExtra("pint"));


        confirm = findViewById(R.id.confirmRequest);


        confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (checkConnection()) {
                    if (auth.getCurrentUser() != null) {
                        loadingDialog.show();
                        uploadField(auth.getCurrentUser().getUid());
                    }else {
                        Toast.makeText(VerifyRequest.this, "Something went wrong, Please try again!!", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(VerifyRequest.this, "Please check your internet connection", Toast.LENGTH_SHORT).show();
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

    private void uploadField(String user) {
        Date c = Calendar.getInstance().getTime();
        SimpleDateFormat df = new SimpleDateFormat("MMM d, yyyy       hh:mm aaa", Locale.getDefault());
        Map<String, Object> person = new HashMap<>();
        person.put("pName", pName.getText().toString());
        person.put("cName", cName.getText().toString());
        person.put("address", location.getText().toString());
        person.put("phone", phone.getText().toString());
        person.put("hospital", hname.getText().toString());
        person.put("notes", cases.getText().toString());
        person.put("bloodType", blood.getText().toString());
        person.put("group", bg.getText().toString());
        person.put("pints", pints.getText().toString());
        person.put("latitude", ssp.getString("lat", ""));
        person.put("longitude", ssp.getString("long", ""));
        person.put("success", false);
        person.put("donorCount", 0);
        person.put("token", token.getText().toString());
        person.put("visibility", true);
        person.put("time", df.format(c));
        person.put("timestamp",calculateTime() );
        person.put("leader", "");
        person.put("leaderPic", "");
        person.put("leaderPhone", "");
        firestore.collection("requests").document(user).set(person).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    loadingDialog.dismiss();
                    startActivity(new Intent(VerifyRequest.this, RequestDetailActivity.class));
                    Toast.makeText(VerifyRequest.this, "Request sent successfully!", Toast.LENGTH_SHORT).show();
                    finishAffinity();
                } else {
                    loadingDialog.dismiss();
                    String error = task.getException().getMessage();
                    Toast.makeText(VerifyRequest.this, error, Toast.LENGTH_SHORT).show();
                }
            }
        });


        final GeoLocation center = new GeoLocation(Double.parseDouble(ssp.getString("lat", "")), Double.parseDouble(ssp.getString("long", "")));
        final double radiusInM = 15 * 1000;  //35 km

        if (center != null) {
            FirebaseFirestore.getInstance().collection("Volunteer").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                @Override
                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                    for (QueryDocumentSnapshot snapshot : task.getResult()) {
                        if (!snapshot.get("latitude").toString().isEmpty() && !snapshot.get("longitude").toString().isEmpty()) {
                            GeoLocation docLocation = new GeoLocation(Double.parseDouble(snapshot.get("latitude").toString()), Double.parseDouble(snapshot.get("longitude").toString()));
                            double distanceInM = GeoFireUtils.getDistanceBetween(docLocation, center);
                            if (distanceInM <= radiusInM) {
                                if (!snapshot.get("token").toString().isEmpty()) {
                                    sendVolunteer(snapshot.get("token").toString());
                                }
                            }
                        }

                    }
                    FirebaseFirestore.getInstance().collection("donors").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            for (QueryDocumentSnapshot snapshot : task.getResult()) {
                                if (!snapshot.get("latitude").toString().isEmpty() && !snapshot.get("longitude").toString().isEmpty()) {
                                    if (snapshot.get("bgroup").toString().equalsIgnoreCase(bg.getText().toString())) {
                                        GeoLocation docLocation = new GeoLocation(Double.parseDouble(snapshot.get("latitude").toString()), Double.parseDouble(snapshot.get("longitude").toString()));
                                        double distanceInM = GeoFireUtils.getDistanceBetween(docLocation, center);
                                        if (distanceInM <= radiusInM) {
                                            if (!snapshot.get("token").toString().isEmpty()) {
                                                sendDonor(snapshot.get("token").toString(), snapshot.get("fName").toString(), bg.getText().toString());
                                            }
                                        }
                                    }

                                }
                            }

                            FirebaseFirestore.getInstance().collection("Admin").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                    for (QueryDocumentSnapshot snapshot : task.getResult()) {
                                        if (!snapshot.get("latitude").toString().isEmpty() && !snapshot.get("longitude").toString().isEmpty()) {
                                            GeoLocation docLocation = new GeoLocation(Double.parseDouble(snapshot.get("latitude").toString()), Double.parseDouble(snapshot.get("longitude").toString()));
                                            double distanceInM = GeoFireUtils.getDistanceBetween(docLocation, center);
                                            if (distanceInM <= radiusInM) {
                                                if (!snapshot.get("token").toString().isEmpty()) {
                                                    sendVolunteer(snapshot.get("token").toString());
                                                }
                                            }
                                        }

                                    }
                                }
                            });
                        }
                    });
                }
            });


        }
    }

    public boolean checkConnection() {
        ConnectivityManager manager = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        return manager.getActiveNetworkInfo() != null && manager.getActiveNetworkInfo().isConnectedOrConnecting();

    }

    private void sendDonor(String token, String name, String bg) {
        if (!token.isEmpty()) {
            FcmNotificationsSender notificationsSender = new FcmNotificationsSender(
                    token,
                    "Dear " + name,
                    bg + " रगतको आवश्यकता परेको हुनाले रक्तदान गरी मदत गरिदिनु हुन अनुरोध गर्दछौं !" + "\n" +
                            "Hospital: " + hname.getText().toString() + "\n" +
                            "Blood Required: " + blood.getText().toString() + "\n" +
                            "Case: " + cases.getText().toString() + "\n" +
                            "Patient Name: " + pName.getText().toString() + "\n" +
                            "Phone: " + phone.getText().toString(),
                    getApplicationContext(), VerifyRequest.this);
            notificationsSender.SendNotifications();
        }
    }
private long calculateTime(){
        int year, month, day,hr,min;
        Calendar calendar = Calendar.getInstance();
    year = calendar.get(Calendar.YEAR);
    month = calendar.get(Calendar.MONTH);
    day = calendar.get(Calendar.DAY_OF_MONTH);
    hr = calendar.get(Calendar.HOUR);
    min = calendar.get(Calendar.MINUTE);
    year = year-2000;
    month = month + 1;
    int hrs = hr*60+min;
    String hrr;
    if (hrs<100)
        hrr = "0"+hrs;
    else
        hrr = ""+hrs;
    String  dateCount = ""+(year * 360 + month * 30 + day)+""+hrr;
        return Long.parseLong(dateCount);
}
    private void sendVolunteer(String token) {
        if (!token.isEmpty()) {
            FcmNotificationsSender notificationsSender = new FcmNotificationsSender(
                    token,
                    "" + hname.getText().toString(),
                    "Blood Required: " + bg.getText().toString() + ", " + blood.getText().toString() + "\n" +
                            "Case: " + cases.getText().toString() + "\n" +
                            "Patient Name: " + pName.getText().toString() + "\n" +
                            "Phone: " + phone.getText().toString(),
                    getApplicationContext(), VerifyRequest.this);
            notificationsSender.SendNotifications();
        }
    }
}
