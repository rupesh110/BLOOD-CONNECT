package shyam.blood.donation;

import static com.google.firebase.messaging.Constants.MessageNotificationKeys.TAG;
import static java.lang.Thread.sleep;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.rilixtech.widget.countrycodepicker.CountryCodePicker;

import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;


public class LoginActivity extends AppCompatActivity {
    EditText phone;
    Button loginBtn;
    ProgressBar progressBar;
    private FirebaseAuth mAuth;
    FirebaseUser user;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;
    Dialog loadingDialog;
    SharedPreferences skp;
    //CountryCodePicker cpp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        phone = findViewById(R.id.etPhone);
//        cpp = findViewById(R.id.ccp);
//        cpp.registerPhoneNumberTextView(phone);
//        cpp.isValid();
        loginBtn = findViewById(R.id.btnSend);
        progressBar = findViewById(R.id.progressBar);
        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();
        progressBar.setVisibility(View.GONE);
        loginBtn.setVisibility(View.VISIBLE);
        skp = getSharedPreferences("storeRequestID",0);
        SharedPreferences.Editor editor = skp.edit();
        loadingDialog = new Dialog(this);
        loadingDialog.setContentView(R.layout.loading_dialog);
        loadingDialog.getWindow().setBackgroundDrawable(getDrawable(R.drawable.loading_item));
        loadingDialog.getWindow().setLayout(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        loadingDialog.setCancelable(false);
        TextView loadingText = loadingDialog.findViewById(R.id.textView4);
        loadingText.setText("Please wait...");
        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (phone.getText().length() ==10 && phone.getText().toString().startsWith("98")){
                    otpSend();
                }else {
                    if (phone.getText().toString().trim().isEmpty()) {
                        Toast.makeText(LoginActivity.this, "Invalid Phone Number", Toast.LENGTH_SHORT).show();
                    } else if (phone.getText().toString().trim().length() != 10) {
                        Toast.makeText(LoginActivity.this, "Type valid Phone Number", Toast.LENGTH_SHORT).show();
                    }
                }

            }
        });

        if (user != null) {
            if(!user.isAnonymous()){
                {
                    loadingDialog.show();
                    FirebaseFirestore.getInstance().collection("donors").document(user.getUid()).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull com.google.android.gms.tasks.Task<DocumentSnapshot> task) {
                            if (task.isSuccessful()) {
                                DocumentSnapshot snapshot = task.getResult();
                                if (snapshot.exists()) {
                                    loadingDialog.dismiss();
                                    startActivity(new Intent(LoginActivity.this, DonorProfileActivity.class));
                                    finish();
                                } else {
                                    String userPhone = mAuth.getCurrentUser().getPhoneNumber().replace("+977", "");
                                    if (userPhone.length() == 10) {
                                        loadingDialog.show();

                                        FirebaseFirestore.getInstance().collection("donors").whereEqualTo("phone", userPhone).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                            @Override
                                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                                for (QueryDocumentSnapshot snapshot : task.getResult()) {
                                                    if (snapshot.exists()) {
                                                        if (snapshot.getId().length() > 30) {
                                                            FirebaseFirestore.getInstance().collection("donors").document(mAuth.getCurrentUser().getUid()).set(snapshot.getData()).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                @Override
                                                                public void onComplete(@NonNull Task<Void> task) {
                                                                    if (task.isSuccessful()) {
                                                                        FirebaseFirestore.getInstance().collection("donors").document(snapshot.getId()).delete();
                                                                        Toast.makeText(LoginActivity.this, "Your profile has been already created!", Toast.LENGTH_SHORT).show();
                                                                        startActivity(new Intent(LoginActivity.this, DonorProfileActivity.class));
                                                                        finish();
                                                                    }
                                                                }
                                                            });
                                                        }
                                                    } else {
                                                        startActivity(new Intent(LoginActivity.this, DonorFromActivity.class));
                                                        finish();
                                                    }
                                                }
                                            }
                                        });
                                    } else {
                                        startActivity(new Intent(LoginActivity.this, DonorFromActivity.class));
                                        finish();
                                    }
                                }
                            } else {
                                Toast.makeText(LoginActivity.this, "Something went wrong!!", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            }else {
                editor.putString("ID",user.getUid());
                editor.apply();
            }
        }
    }

    private void otpSend() {
        progressBar.setVisibility(View.VISIBLE);
        loginBtn.setVisibility(View.INVISIBLE);

        mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

            @Override
            public void onVerificationCompleted(PhoneAuthCredential credential) {

            }

            @Override
            public void onVerificationFailed(FirebaseException e) {
                progressBar.setVisibility(View.GONE);
                loginBtn.setVisibility(View.VISIBLE);
                Toast.makeText(LoginActivity.this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCodeSent(@NonNull String verificationId,
                                   @NonNull PhoneAuthProvider.ForceResendingToken token) {
                progressBar.setVisibility(View.GONE);
                loginBtn.setVisibility(View.VISIBLE);
                Toast.makeText(LoginActivity.this, "OTP is successfully send.", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(LoginActivity.this, OTPVerificationActivity.class);
                intent.putExtra("code", "+977"+phone.getText().toString());
                intent.putExtra("verificationId", verificationId);
                startActivity(intent);
                finish();

            }
        };

        PhoneAuthOptions options =
                PhoneAuthOptions.newBuilder(mAuth)
                        .setPhoneNumber("+977"+phone.getText().toString())
                        .setTimeout(60L, TimeUnit.SECONDS)
                        .setActivity(LoginActivity.this)
                        .setCallbacks(mCallbacks)
                        .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }



    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }
}

