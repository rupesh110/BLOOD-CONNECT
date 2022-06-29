package shyam.blood.donation;

import static com.android.volley.VolleyLog.TAG;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Calendar;
import java.util.List;

public class ReqestDonorAdapter extends RecyclerView.Adapter<ReqestDonorAdapter.Viewholder> {

    private List<BloodRequestModel> detailModelList;
    Context context;
    private AcceptToDonateInterface donateInterface;



    public ReqestDonorAdapter(Context context, List<BloodRequestModel> detailModelList,AcceptToDonateInterface donateInterface) {
        this.context = context;
        this.detailModelList = detailModelList;
        this.donateInterface = donateInterface;
    }


    @NonNull
    @Override
    public Viewholder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate
                (R.layout.reqests_item, parent, false);
        return new Viewholder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Viewholder holder, int position) {

        holder.setData(

                detailModelList.get(position).getRname(),
                detailModelList.get(position).getRaddress(),
                detailModelList.get(position).getRphone(),
                "Blood Required: "+detailModelList.get(position).getRblood()+"\n"+detailModelList.get(position).getRpint(),
                detailModelList.get(position).getRnote(),
                detailModelList.get(position).getId()
        );

    }


    @Override
    public int getItemCount() {
        return detailModelList.size();
    }

    class Viewholder extends RecyclerView.ViewHolder {
        private TextView name, address, phone, notes, bloodgp;
        private ImageView call;
        private Button accept;

        public Viewholder(@NonNull View itemView) {
            super(itemView);


            name = itemView.findViewById(R.id.rname);
            address = itemView.findViewById(R.id.raddress);
            phone = itemView.findViewById(R.id.rphone);
            bloodgp = itemView.findViewById(R.id.rbg);
            notes = itemView.findViewById(R.id.rnotes);
            call = itemView.findViewById(R.id.rcall);
            accept = itemView.findViewById(R.id.acceptRequest);



        }

        public void setData(String personName, String address, String phone,String bg,String note,String id) {
            this.name.setText(personName);
            this.address.setText(address);
            this.phone.setText(phone);
            this.bloodgp.setText(bg);
            this.notes.setText(note);

            this.call.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(Intent.ACTION_CALL);
                    if (phone.trim().isEmpty()) {
                        intent.setData(Uri.parse("tel: 9988774455"));
                    } else {
                        intent.setData(Uri.parse("tel: " + phone));
                    }
                    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(context, "Please grant the permission to call", Toast.LENGTH_SHORT).show();
                        requestPermission();
                    } else {
                        context.startActivity(intent);
                    }
                }

                private void requestPermission() {
                    ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.CALL_PHONE}, 1);
                }
            });
            this.accept.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    donateInterface.onItemClick(id);
                }
            });
        }
    }
}
