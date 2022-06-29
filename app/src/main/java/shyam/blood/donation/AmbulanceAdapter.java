package shyam.blood.donation;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
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

import java.text.DecimalFormat;
import java.util.List;

public class AmbulanceAdapter extends RecyclerView.Adapter<AmbulanceAdapter.Viewholder> {

    private List<AmbulanceModel> detailModelList;
    Context context;


    public AmbulanceAdapter(Context context, List<AmbulanceModel> detailModelList) {
        this.context = context;
        this.detailModelList = detailModelList;
    }


    @NonNull
    @Override
    public Viewholder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate
                (R.layout.ambulance_item, parent, false);
        return new Viewholder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Viewholder holder, int position) {

        holder.setData(

                detailModelList.get(position).getId(),
                detailModelList.get(position).getName(),
                detailModelList.get(position).getAddress(),
                detailModelList.get(position).getPhone(),
                detailModelList.get(position).getDistance()

        );

    }


    @Override
    public int getItemCount() {
        return detailModelList.size();
    }

    class Viewholder extends RecyclerView.ViewHolder {
        private TextView name, address, phone, distance;
        private Button map;
        private ImageView call;


        public Viewholder(@NonNull View itemView) {
            super(itemView);


            name = itemView.findViewById(R.id.bankName);
            address = itemView.findViewById(R.id.bankAddress);
            phone = itemView.findViewById(R.id.bankPhone);
            call = itemView.findViewById(R.id.callAmb);
            distance = itemView.findViewById(R.id.showDistance);

        }

        public void setData(String id, String personName, String address, String phone, Double distance) {
            this.name.setText(address);
            this.address.setText(personName);
            this.phone.setText(phone);
            if (distance<=1000){
                this.distance.setText("Distance: "+new DecimalFormat("##.##").format(distance)+ " M");
            }else {
                distance = distance/1000;
                this.distance.setText("Distance: "+new DecimalFormat("##.##").format(distance)+ " KM");
            }
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
//            this.map.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View view) {
//                    String geoUri = "http://maps.google.com/maps?q=" + address;
//                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(geoUri));
//                    context.startActivity(intent);
//                }
//            });
        }
    }
}
