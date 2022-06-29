package shyam.blood.donation;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Dialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class CertifyActivity extends AppCompatActivity {
    ProgressDialog pd;
    Button share;
    ConstraintLayout savingLayout;
    Dialog thankDialog;
    Button okBtn;
    FirebaseAuth auth;
    FirebaseFirestore firestore;
    TextView donarName, date;
    private AlarmManager alarmManager;
    private PendingIntent pendingIntent;
    Boolean storagePermission;
    Calendar calendar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_certify);
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        pd = new ProgressDialog(CertifyActivity.this);
        createNotificationChannel();
        calendar = Calendar.getInstance();
        share = findViewById(R.id.share);
        donarName = findViewById(R.id.donarName);
        date = findViewById(R.id.date);
        String name = getIntent().getStringExtra("dName");
        String gender = getIntent().getStringExtra("gender");
        donarName.setText(name.toUpperCase());
        switch (gender) {
            case "Male":
                setAlarm(90);
                break;
            case "Female":
                setAlarm(120);
                break;
            case "Others":
                setAlarm(92);
                break;
            default:
                setAlarm(91);
        }

        Date c = Calendar.getInstance().getTime();
        SimpleDateFormat df = new SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault());
        date.setText("" + df.format(c));
        DocumentReference ref = firestore.collection("donors").document(auth.getCurrentUser().getUid());
        ref.collection("acceptedRequest").document().delete();
        ref.update("request","");
        thankDialog = new Dialog(CertifyActivity.this);
        thankDialog.setContentView(R.layout.thank_dialog);
        thankDialog.setCancelable(false);
        thankDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        thankDialog.show();
        okBtn = thankDialog.findViewById(R.id.okBtn);
        savingLayout = (ConstraintLayout) findViewById(R.id.idForSaving);
        share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (checkPermissions()){
                    shareClick(savingLayout);
                }else {
                    getStoragePermission();
                }
            }
        });

        okBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                thankDialog.dismiss();
            }
        });
    }

    private void setAlarm(int days) {
        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, AlarmReceiver.class);
        pendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0);
        alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME, calendar.get(Calendar.MONDAY),
                86400 * 1000 * days, pendingIntent);
    }

    public void SaveClick(View view) {
        if (checkPermissions()){
            pd.setMessage("saving your image");
            pd.show();
            File file = saveBitMap(this, savingLayout);
            if (file != null) {
                pd.cancel();
            } else {
                pd.cancel();
            }
        }else {
            getStoragePermission();
        }

    }

    private boolean checkPermissions() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private void getStoragePermission() {
        storagePermission = false;
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            storagePermission = true;
        } else {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE}, 44);

        }
    }

    private void createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "donorNotif";
            String description = "Channel For Donor Notification";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel("donorNotification", name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);

        }
    }

    private File saveBitMap(Context context, View drawView) {
        File f;
        final File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM + "/BloodConnect/");
        if (!path.exists()) {
            path.mkdirs();
        }
        f = new File(path.getAbsolutePath() + path.separator + "image_" + System.currentTimeMillis() + ".png");
        Bitmap bitmap = getBitmapFromView(drawView);
        try {
            f.createNewFile();
            FileOutputStream oStream = new FileOutputStream(f);
            final BufferedOutputStream bos = new BufferedOutputStream(oStream, 8192);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, bos);
            bos.flush();
            bos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        scanGallery(context, f.getAbsolutePath());
        Toast.makeText(CertifyActivity.this, "Certificate Saved Successfully!", Toast.LENGTH_SHORT).show();
        return f;
    }

    private Bitmap getBitmapFromView(View view) {
        Bitmap returnedBitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(returnedBitmap);
        Drawable bgDrawable = view.getBackground();
        if (bgDrawable != null) {
            bgDrawable.draw(canvas);
        } else {
            canvas.drawColor(Color.WHITE);
        }
        view.draw(canvas);
        return returnedBitmap;
    }

    private void scanGallery(Context cntx, String path) {
        try {
            MediaScannerConnection.scanFile(cntx, new String[]{path}, null, new MediaScannerConnection.OnScanCompletedListener() {
                public void onScanCompleted(String path, Uri uri) {
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void shareClick(View view) {
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        final File path =
                Environment.getExternalStoragePublicDirectory
                        (
                                Environment.DIRECTORY_DCIM + "/BloodConnect/"
                        );
        if (!path.exists()) {
            path.mkdirs();
        }
        final File f = new File(path.getAbsolutePath() + path.separator + "image_" + System.currentTimeMillis() + ".png");
        Bitmap bitmap = getBitmapFromView(view);
        Intent intent = new Intent(Intent.ACTION_SEND);
        ;
        try {
            f.createNewFile();
            FileOutputStream oStream = new FileOutputStream(f);
            final BufferedOutputStream bos = new BufferedOutputStream(oStream, 8192);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, bos);
            bos.flush();
            bos.close();
            intent.setType("image/*");
            String shareBody = "Congratulations Bro! ";
            String shareSubject = "Blood Donation Certificate";
            intent.putExtra(Intent.EXTRA_TEXT, shareBody);
            intent.putExtra(Intent.EXTRA_SUBJECT, shareSubject);
            intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(f));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        } catch (IOException e) {
            e.printStackTrace();
        }
        startActivity(Intent.createChooser(intent, "Share via:"));
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }
}