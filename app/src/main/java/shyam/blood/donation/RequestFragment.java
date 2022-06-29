package shyam.blood.donation;

import static com.google.firebase.messaging.Constants.TAG;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class RequestFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {
    RecyclerView recyclerView;
    FirebaseFirestore firestore;
    FirebaseAuth mAuth;
    FirebaseUser user;
    List<BloodRequestModel> list = new ArrayList<>();
    AcceptedRequestAdapter adapter;
    Dialog donateDialog, deleteDialog, loadingDialog;
    TextView volName, volAddress, volPhone, notice;
    ImageView volCall;
    CircleImageView vPic;
    CardView showVol;
    SwipeRefreshLayout refreshLayout;

    //this page will show when the person who requested for blood want to see how many and who are accepted request to donate blood
    public RequestFragment() {

    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_request, container, false);
        firestore = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();
        refreshLayout = view.findViewById(R.id.swipeRequest);
        refreshLayout.setOnRefreshListener(this);
        recyclerView = view.findViewById(R.id.recyclerview);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setOrientation(RecyclerView.VERTICAL);
        recyclerView.setLayoutManager(layoutManager);
        adapter = new AcceptedRequestAdapter(getActivity(), list);
        recyclerView.setAdapter(adapter);
        loadingDialog = new Dialog(getContext());
        loadingDialog.setContentView(R.layout.loading_dialog);
        loadingDialog.getWindow().setBackgroundDrawable(getContext().getDrawable(R.drawable.loading_item));
        loadingDialog.getWindow().setLayout(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        loadingDialog.setCancelable(false);
        volName = view.findViewById(R.id.volName);
        volAddress = view.findViewById(R.id.volAddress);
        volPhone = view.findViewById(R.id.volPhone);
        volCall = view.findViewById(R.id.volcall);
        vPic = view.findViewById(R.id.vPic);
        notice = view.findViewById(R.id.notice);
        showVol = view.findViewById(R.id.showVol);
        if (user != null) {
            loadingDialog.show();
            FirebaseFirestore.getInstance().collection("requests").document(user.getUid()).addSnapshotListener(new EventListener<DocumentSnapshot>() {
                @Override
                public void onEvent(@Nullable DocumentSnapshot snapshot, @Nullable FirebaseFirestoreException error) {
                    if (snapshot.exists()) {
                        if (!snapshot.get("leader").toString().isEmpty()) {
                            showVol.setVisibility(View.VISIBLE);
                            volName.setText(snapshot.get("leader").toString());
                            volAddress.setText("");
                            volPhone.setText(snapshot.get("leaderPhone").toString());
                            volCall.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    Intent intent = new Intent(Intent.ACTION_CALL);
                                    intent.setData(Uri.parse("tel: " + volPhone.getText().toString()));
                                    if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                                        Toast.makeText(getContext(), "Please grant the permission to call", Toast.LENGTH_SHORT).show();
                                        requestPermission();
                                    } else {
                                        getActivity().startActivity(intent);
                                    }
                                }

                                private void requestPermission() {
                                    ActivityCompat.requestPermissions((Activity) getActivity(), new String[]{Manifest.permission.CALL_PHONE}, 1);
                                }
                            });
                            String volPic = snapshot.get("leaderPic").toString();
                            Glide.with(getContext()).load(volPic).apply(new RequestOptions().placeholder(R.drawable.user1)).into(vPic);
                        } else {
                            showVol.setVisibility(View.GONE);
                        }
                    }
                }
            });
            onRefresh();
//            if (list.size()==0){
//                notice.setVisibility(View.VISIBLE);
//            }
        }


        donateDialog = new Dialog(getContext());
        donateDialog.setContentView(R.layout.got_blood);
        donateDialog.setCancelable(true);
        donateDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        TextView yesBtn = donateDialog.findViewById(R.id.yesBtn);
        TextView noBtn = donateDialog.findViewById(R.id.noBtn);
        yesBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                donateDialog.dismiss();
                deleteDialog.show();
            }
        });
        noBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                donateDialog.dismiss();
            }
        });

        deleteDialog = new Dialog(getContext());
        deleteDialog.setContentView(R.layout.delete_request);
        deleteDialog.setCancelable(true);
        deleteDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        Button noDel = deleteDialog.findViewById(R.id.noDel);
        Button yesDel = deleteDialog.findViewById(R.id.yesDel);

        yesDel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                deleteDialog.dismiss();
                firestore.collection("requests").document(user.getUid()).update("success", true);
            }
        });
        noDel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                deleteDialog.dismiss();
            }
        });


        return view;
    }

    @Override
    public void onRefresh() {
        if (user != null) {
            list.clear();
            firestore.collection("requests").document(user.getUid()).collection("acceptedList")
                    .addSnapshotListener(new EventListener<QuerySnapshot>() {
                        @Override
                        public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                            list.clear();
                            for (QueryDocumentSnapshot snapshot : value) {
                                firestore.collection("donors").document(snapshot.getId()).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                    @Override
                                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                        DocumentSnapshot snapshot1 = task.getResult();
                                        if (snapshot1.exists()) {
                                            if (!snapshot1.get("request").toString().equalsIgnoreCase("")) {
                                                BloodRequestModel model = new BloodRequestModel(
                                                        snapshot.getId(),
                                                        snapshot.get("name").toString(),
                                                        snapshot.get("profile").toString(),
                                                        snapshot.get("phone").toString(),
                                                        "",
                                                        "",
                                                        "",
                                                        ""
                                                );
                                                list.add(model);
                                                adapter.notifyDataSetChanged();
                                            }
                                        }

                                    }
                                });
                            }
                            if (value.size()==0){
                                notice.setVisibility(View.VISIBLE);
                            }
                            adapter.notifyDataSetChanged();
                            refreshLayout.setRefreshing(false);
                            loadingDialog.dismiss();
                        }
                    });

//        if (firestore.collection("requests").document(user.getUid()).collection("acceptedList").get().getResult().size() == 0){
//            notice.setVisibility(View.VISIBLE);
//        }
        } else {
            loadingDialog.dismiss();
        }

    }
}