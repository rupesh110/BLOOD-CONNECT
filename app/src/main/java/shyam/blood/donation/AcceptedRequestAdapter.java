package shyam.blood.donation;

import static java.lang.Thread.sleep;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class AcceptedRequestAdapter extends RecyclerView.Adapter<AcceptedRequestAdapter.Viewholder> {

    private List<BloodRequestModel> detailModelList;
    Context context;


    public AcceptedRequestAdapter(Context context, List<BloodRequestModel> detailModelList) {
        this.context = context;
        this.detailModelList = detailModelList;

    }


    @NonNull
    @Override
    public Viewholder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate
                (R.layout.accepted_donors, parent, false);
        return new Viewholder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Viewholder holder, int position) {

        holder.setData(

                detailModelList.get(position).getRname(),
                detailModelList.get(position).getRaddress(),
                detailModelList.get(position).getRphone(),
                detailModelList.get(position).getId()
        );

    }


    @Override
    public int getItemCount() {
        return detailModelList.size();
    }

    class Viewholder extends RecyclerView.ViewHolder {
        private TextView name, phone;
        private ImageView call;
        private CircleImageView dpic;


        public Viewholder(@NonNull View itemView) {
            super(itemView);

            name = itemView.findViewById(R.id.dName);
            phone = itemView.findViewById(R.id.dPhone);
            call = itemView.findViewById(R.id.dcall);
            dpic = itemView.findViewById(R.id.dPic);
        }

        public void setData(String personName,String pic, String phone, String id) {
            this.name.setText(personName);
            this.phone.setText(phone);
            Glide.with(context).load(pic).apply(new RequestOptions().placeholder(R.drawable.user1)).into(dpic);
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

        }
    }
}
