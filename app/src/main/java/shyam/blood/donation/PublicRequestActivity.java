package shyam.blood.donation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.geofire.GeoFireUtils;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;

public class PublicRequestActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener {
    private RecyclerView recyclerview;
    FirebaseFirestore firestore;
    ReqestPublicAdapter adapter;
    List<BloodRequestModel> list = new ArrayList<>();
    Dialog loadingDialog;
    BloodRequestModel model;
    FirebaseAuth auth;
    FirebaseUser user;
    String vName;
    SharedPreferences sp;
    GeoLocation center = null;
    double radiusInM;
    View noInternet;
    SwipeRefreshLayout refreshLayout;
    private Handler handler = new Handler();
    TextView error;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_public_request);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Blood Requests near you");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        error = findViewById(R.id.error);
        handler.postDelayed(runnable, 5000);
        refreshLayout = findViewById(R.id.swipePublic);
        refreshLayout.setOnRefreshListener(this);
        user = auth.getCurrentUser();
        sp = getSharedPreferences("LatLong", 0);
        SharedPreferences.Editor editor = sp.edit();
        recyclerview = (RecyclerView) findViewById(R.id.recyclerview);
        noInternet = findViewById(R.id.noIntenet);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(RecyclerView.VERTICAL);
        recyclerview.setLayoutManager(layoutManager);
        adapter = new ReqestPublicAdapter(this, list);
        recyclerview.setAdapter(adapter);
        loadingDialog = new Dialog(this);
        loadingDialog.setContentView(R.layout.loading_dialog);
        loadingDialog.setCancelable(false);
        loadingDialog.getWindow().setBackgroundDrawable(getDrawable(R.drawable.loading_item));
        loadingDialog.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);


        if (!sp.getString("lat", "").isEmpty()) {
            center = new GeoLocation(Double.parseDouble(sp.getString("lat", "")), Double.parseDouble(sp.getString("long", "")));
            radiusInM = 50 * 1000;
        } else {
            error.setVisibility(View.VISIBLE);
        }

    }

    public boolean checkConnection() {
        ConnectivityManager manager = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        return manager.getActiveNetworkInfo() != null && manager.getActiveNetworkInfo().isConnectedOrConnecting();

    }

    @Override
    protected void onStart() {
        super.onStart();
        if (center != null) {
            loadingDialog.show();
            loadData();
        } else {
            error.setVisibility(View.VISIBLE);
        }
    }

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if (center != null) {
                loadData();
                handler.postDelayed(this, 5000);
            }
        }
    };
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
        String  dateCount = ""+(year * 360 + month * 30 + day-12)+""+hrr;
        long ret = Long.parseLong(dateCount) ;
        //Toast.makeText(this, ""+ret, Toast.LENGTH_SHORT).show();
        return ret;
    }
    public void loadData() {
        if (checkConnection()) {
            firestore.collection("requests")
                    //.whereEqualTo("visibility", true)
                    .whereGreaterThan("timestamp",calculateTime())
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                @Override
                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                    error.setVisibility(View.GONE);
                    list.clear();
                    for (QueryDocumentSnapshot snapshot : task.getResult()) {
                        if (snapshot.get("success").toString().equalsIgnoreCase("false") && snapshot.get("visibility").toString().equalsIgnoreCase("true")) {
                            GeoLocation docLocation = new GeoLocation(Double.parseDouble(snapshot.get("latitude").toString()), Double.parseDouble(snapshot.get("longitude").toString()));
                            double distanceInM = GeoFireUtils.getDistanceBetween(docLocation, center);
                            if (distanceInM <= radiusInM) {
                                if (snapshot.get("time") != null) {
                                    model = new BloodRequestModel(
                                            snapshot.getId(),
                                            snapshot.get("pName").toString(),
                                            snapshot.get("hospital").toString(),
                                            snapshot.get("phone").toString(),
                                            snapshot.get("notes").toString(),
                                            snapshot.get("group").toString() + ", " + snapshot.get("pints").toString() + " Pints",
                                            snapshot.get("bloodType").toString()
                                            , snapshot.get("time").toString()
                                    );
                                    list.add(model);
                                }else {
                                    model = new BloodRequestModel(
                                            snapshot.getId(),
                                            snapshot.get("pName").toString(),
                                            snapshot.get("hospital").toString(),
                                            snapshot.get("phone").toString(),
                                            snapshot.get("notes").toString(),
                                            snapshot.get("group").toString() + ", " + snapshot.get("pints").toString() + " Pints",
                                            snapshot.get("bloodType").toString()
                                            , snapshot.get("timestamp").toString()
                                    );
                                    list.add(model);
                                }
                            }
                        }
                    }
                    if (list.size() == 0) {
                        error.setVisibility(View.VISIBLE);
                        error.setText("No Blood Request in your area !!");
                    }
//                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//                        Collections.sort(list, Comparator.comparing(BloodRequestModel::getDate));
//                    }
                    loadingDialog.dismiss();
                    refreshLayout.setRefreshing(false);
                    adapter.notifyDataSetChanged();
                }

            });
        } else {
            refreshLayout.setRefreshing(false);
            loadingDialog.dismiss();
            error.setVisibility(View.GONE);
            recyclerview.setVisibility(View.GONE);
            noInternet.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRefresh() {
        if (center != null) {
            loadingDialog.show();
            list.clear();
            loadData();
        } else {
            error.setVisibility(View.VISIBLE);
            refreshLayout.setRefreshing(false);
        }
    }
}