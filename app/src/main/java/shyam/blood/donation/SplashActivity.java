package shyam.blood.donation;

import static java.lang.Thread.sleep;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.Date;

public class SplashActivity extends AppCompatActivity {
    FirebaseAuth auth;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        ImageView image = findViewById(R.id.imageView);
        auth = FirebaseAuth.getInstance();
        Glide.with(this).load(R.drawable.blr).into(image);
//        Calendar calendar = Calendar.getInstance();
//        Toast.makeText(this, ""+calendar.getTimeInMillis(), Toast.LENGTH_SHORT).show();

        final Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    sleep(2700);


                    if (checkConnection()){
                        if (auth.getCurrentUser()!=null){
                            FirebaseFirestore.getInstance().collection("requests").document(auth.getCurrentUser().getUid()).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                    if (task.isSuccessful()){
                                        DocumentSnapshot snapshot = task.getResult();
                                        if (snapshot.exists()){
                                            if (!snapshot.get("success").toString().equalsIgnoreCase("true")){
                                                startActivity(new Intent(SplashActivity.this, RequestDetailActivity.class));
                                                finish();
                                            }else {
                                                startActivity(new Intent(SplashActivity.this, MainActivity.class));
                                                finish();
                                            }
                                        }else {
                                            startActivity(new Intent(SplashActivity.this, MainActivity.class));
                                            finish();
                                        }
                                    }
                                }
                            });
                        }else {
                            startActivity(new Intent(SplashActivity.this, MainActivity.class));
                            finish();
                        }
                    }else {
                        startActivity(new Intent(SplashActivity.this, MainActivity.class));
                        finish();
//                        Toast.makeText(SplashActivity.this, "No Internet Connection !!", Toast.LENGTH_SHORT).show();
                    }

                }catch (InterruptedException e){
                    e.printStackTrace();
                }
            }
        });thread.start();
    }


    public boolean checkConnection() {
        ConnectivityManager manager = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        return manager.getActiveNetworkInfo() != null && manager.getActiveNetworkInfo().isConnectedOrConnecting();

    }
}