package shyam.blood.donation;

import static org.chromium.base.ContextUtils.getApplicationContext;

import android.app.Dialog;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.firebase.geofire.GeoFireUtils;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.core.OrderBy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class BloodBankFragment extends Fragment {
    FirebaseFirestore firestore;
    Dialog loadingDialog;
    RecyclerView bankRec;
    BloodBankAdapter adapter;
    List<BloodBankModel> list = new ArrayList<>();
    SharedPreferences sp;

    public BloodBankFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_blood_bank, container, false);

        firestore = FirebaseFirestore.getInstance();
        bankRec = view.findViewById(R.id.bloodBankRec);

        sp = getActivity().getSharedPreferences("LatLong", 0);
        loadingDialog = new Dialog(getContext());
        loadingDialog.setContentView(R.layout.loading_dialog);
        loadingDialog.getWindow().setBackgroundDrawable(getContext().getDrawable(R.drawable.loading_item));
        loadingDialog.getWindow().setLayout(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        loadingDialog.setCancelable(false);


        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setOrientation(RecyclerView.VERTICAL);
        bankRec.setLayoutManager(layoutManager);
        adapter = new BloodBankAdapter(getContext(), list);
        bankRec.setAdapter(adapter);
        if (!sp.getString("lat", "").isEmpty()) {
            final GeoLocation center = new GeoLocation(Double.parseDouble(sp.getString("lat", "")), Double.parseDouble(sp.getString("long", "")));
            final double radiusInM = 50 * 1000;
            if (center != null) {
                loadingDialog.show();
                firestore.collection("BloodBanks")
                        .get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            GeoLocation docLocation = new GeoLocation(
                                    Double.parseDouble(document.get("latitude").toString()),
                                    Double.parseDouble(document.get("longitude").toString()));
                            double distanceInM = GeoFireUtils.getDistanceBetween(docLocation, center);
                            if (distanceInM <= radiusInM) {
                                BloodBankModel data = new BloodBankModel(
                                        document.getId(),
                                        document.get("name").toString(),
                                        document.get("address").toString(),
                                        document.get("phone").toString(),
                                        document.get("latitude").toString() + "," +
                                                document.get("longitude").toString(),
                                        distanceInM
                                );
                                list.add(data);
                                adapter.notifyDataSetChanged();
                            }
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            Collections.sort(list, Comparator.comparing(BloodBankModel::getDistance));
                        }
                        loadingDialog.dismiss();
                        adapter.notifyDataSetChanged();
                    }
                });
            }
        } else {
            Toast.makeText(getActivity(), "Please make sure your internet and location is on!", Toast.LENGTH_SHORT).show();
        }
        return view;

    }
}