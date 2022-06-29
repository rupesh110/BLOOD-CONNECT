package shyam.blood.donation;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.concurrent.TimeUnit;

import shyam.blood.donation.databinding.ActivityOtpverificationBinding;

public class OTPVerificationActivity extends AppCompatActivity {
    private ActivityOtpverificationBinding binding;
    private String verificationId;
    FirebaseAuth auth;
    FirebaseUser user;
    FirebaseFirestore firestore;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;
    SharedPreferences skp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otpverification);
        binding = ActivityOtpverificationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        skp = getSharedPreferences("storeRequestID", 0);
        new MySMSBroadcastReceiver().setEditText_otp(binding.etC1, binding.etC2, binding.etC3, binding.etC4, binding.etC5, binding.etC6);

        editTextInput();

        binding.tvMobile.setText(getIntent().getStringExtra("code"));

        verificationId = getIntent().getStringExtra("verificationId");

        binding.tvResendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                    @Override
                    public void onVerificationCompleted(PhoneAuthCredential credential) {

                    }

                    @Override
                    public void onVerificationFailed(FirebaseException e) {
                        Toast.makeText(OTPVerificationActivity.this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onCodeSent(@NonNull String verificationId,
                                           @NonNull PhoneAuthProvider.ForceResendingToken token) {
                    }
                };

                PhoneAuthOptions options =
                        PhoneAuthOptions.newBuilder(auth)
                                .setPhoneNumber("+977" + getIntent().getStringExtra("phone"))
                                .setTimeout(60L, TimeUnit.SECONDS)
                                .setActivity(OTPVerificationActivity.this)
                                .setCallbacks(mCallbacks)
                                .build();
                PhoneAuthProvider.verifyPhoneNumber(options);
            }
        });

        binding.btnVerify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                verifyOTP();

            }
        });
    }

    private void verifyOTP() {
        binding.progressBarVerify.setVisibility(View.VISIBLE);
        binding.btnVerify.setVisibility(View.INVISIBLE);
        if (binding.etC1.getText().toString().trim().isEmpty() ||
                binding.etC2.getText().toString().trim().isEmpty() ||
                binding.etC3.getText().toString().trim().isEmpty() ||
                binding.etC4.getText().toString().trim().isEmpty() ||
                binding.etC5.getText().toString().trim().isEmpty() ||
                binding.etC6.getText().toString().trim().isEmpty()) {
            Toast.makeText(OTPVerificationActivity.this, "OTP is not Valid!", Toast.LENGTH_SHORT).show();
        } else {
            if (verificationId != null) {
                String code = binding.etC1.getText().toString().trim() +
                        binding.etC2.getText().toString().trim() +
                        binding.etC3.getText().toString().trim() +
                        binding.etC4.getText().toString().trim() +
                        binding.etC5.getText().toString().trim() +
                        binding.etC6.getText().toString().trim();

                PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
                FirebaseAuth
                        .getInstance()
                        .signInWithCredential(credential)
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    binding.progressBarVerify.setVisibility(View.VISIBLE);
                                    binding.btnVerify.setVisibility(View.INVISIBLE);
                                    user = auth.getCurrentUser();

                                    firestore.collection("donors").document(user.getUid()).get()
                                            .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                                @Override
                                                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                                    DocumentSnapshot snapshot = task.getResult();
                                                    if (!snapshot.exists()) {
                                                        Toast.makeText(OTPVerificationActivity.this, "Please fill your registration form!", Toast.LENGTH_SHORT).show();
                                                        startActivity(new Intent(OTPVerificationActivity.this, DonorFromActivity.class));
                                                        finishAffinity();

                                                    } else {
                                                        Toast.makeText(OTPVerificationActivity.this, "Welcome to Donor profile!", Toast.LENGTH_SHORT).show();
                                                        startActivity(new Intent(OTPVerificationActivity.this, DonorProfileActivity.class));
                                                        finishAffinity();

                                                    }
                                                }
                                            });
                                        if (!skp.getString("ID", "").isEmpty()) {
                                            firestore.collection("requests").document(skp.getString("ID", "")).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                                @Override
                                                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                                    if (task.isSuccessful()) {
                                                        DocumentSnapshot snapshot = task.getResult();
                                                        if (snapshot.exists()) {
                                                            firestore.collection("requests").document(user.getUid()).set(snapshot.getData()).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                @Override
                                                                public void onComplete(@NonNull Task<Void> task) {
                                                                    if (task.isSuccessful()) {
                                                                        firestore.collection("requests").document(skp.getString("ID", "")).delete();
                                                                        SharedPreferences.Editor editor = skp.edit();
                                                                        editor.putString("ID", "");
                                                                        editor.apply();
                                                                    }
                                                                }
                                                            });
                                                        }
                                                    }
                                                }
                                            });
                                        }
                                } else {
                                    binding.progressBarVerify.setVisibility(View.GONE);
                                    binding.btnVerify.setVisibility(View.VISIBLE);
                                    Toast.makeText(OTPVerificationActivity.this, "OTP is not Valid!", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            }
        }
    }

    private void editTextInput() {
        binding.etC1.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.etC2.requestFocus();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        binding.etC2.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.etC3.requestFocus();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        binding.etC3.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.etC4.requestFocus();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        binding.etC4.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.etC5.requestFocus();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        binding.etC5.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.etC6.requestFocus();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        binding.etC6.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                verifyOTP();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

}