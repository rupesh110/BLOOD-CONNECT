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
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ReqestPublicAdapter extends RecyclerView.Adapter<ReqestPublicAdapter.Viewholder> {

    private List<BloodRequestModel> detailModelList;
    Context context;


    public ReqestPublicAdapter(Context context, List<BloodRequestModel> detailModelList) {
        this.context = context;
        this.detailModelList = detailModelList;
    }


    @NonNull
    @Override
    public Viewholder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate
                (R.layout.public_item, parent, false);
        return new Viewholder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Viewholder holder, int position) {

        holder.setData(

                detailModelList.get(position).getRname(),
                detailModelList.get(position).getRaddress(),
                detailModelList.get(position).getRphone(),
                detailModelList.get(position).getRblood()
                        + "\n" + detailModelList.get(position).getRpint(),
                detailModelList.get(position).getRnote(),
                detailModelList.get(position).getId(),
                detailModelList.get(position).getDate()
        );

    }


    @Override
    public int getItemCount() {
        return detailModelList.size();
    }

    class Viewholder extends RecyclerView.ViewHolder {
        private TextView name, address, phone, notes, bloodgp, datee;
        private ImageView call, share;

        public Viewholder(@NonNull View itemView) {
            super(itemView);


            name = itemView.findViewById(R.id.rName);
            address = itemView.findViewById(R.id.rhospital);
            phone = itemView.findViewById(R.id.rPhone);
            bloodgp = itemView.findViewById(R.id.rBG);
            notes = itemView.findViewById(R.id.rnotes);
            call = itemView.findViewById(R.id.call);
            share = itemView.findViewById(R.id.shareBtn);
            datee = itemView.findViewById(R.id.date);

        }

        public void setData(String personName, String address, String phone, String bg, String note, String id, String date) {
            this.name.setText(personName);
            this.address.setText("Hospital: " + address);
            this.phone.setText(phone);
            this.bloodgp.setText(bg);
            this.notes.setText("Case: " + note);
            this.datee.setText("Posted at: " + date);

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
            this.share.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent sharingIntent = new Intent(Intent.ACTION_SEND);
                    sharingIntent.setType("text/plain");
                    String shareBody = "--BLOOD CONNECT--\n"
                            + "Patient Name: " + personName + "\n" +
                            "Blood: " + bg + "\n" +
                            "Case: " + note + "\n" +
                            "Hospital: " + address + "\n" +
                            "Phone: " + phone + "\n\n" +
                            "https://play.google.com/store/apps/details?id=" + context.getPackageName();
                    String shareSubject = "Blood Request For You";
                    sharingIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
                    sharingIntent.putExtra(Intent.EXTRA_SUBJECT, shareSubject);
                    context.startActivity(Intent.createChooser(sharingIntent, "Share Using"));
                }
            });
        }
    }
}
