package shyam.blood.donation;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;

import com.google.android.gms.auth.api.phone.SmsRetriever;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.Status;


public class MySMSBroadcastReceiver extends BroadcastReceiver {

    private OTPReceiveListener otpReceiveListener;
    private static EditText otp1, otp2, otp3, otp4, otp5, otp6;
    public MySMSBroadcastReceiver() {
    }

    public void setEditText_otp(EditText editText1, EditText et2, EditText et3, EditText et4, EditText et5, EditText et6) {
        MySMSBroadcastReceiver.otp1 = editText1;
        MySMSBroadcastReceiver.otp2 = et2;
        MySMSBroadcastReceiver.otp3 = et3;
        MySMSBroadcastReceiver.otp4 = et4;
        MySMSBroadcastReceiver.otp5 = et5;
        MySMSBroadcastReceiver.otp6 = et6;
    }
    @Override
    public void onReceive(Context context, Intent intent) {
        if (SmsRetriever.SMS_RETRIEVED_ACTION.equals(intent.getAction())) {
            Bundle extras = intent.getExtras();
            if (extras != null) {
                Status status = (Status) extras.get(SmsRetriever.EXTRA_STATUS);
                if (status != null)
                    switch (status.getStatusCode()) {
                        case CommonStatusCodes.SUCCESS:
                            String message = (String) extras.get(SmsRetriever.EXTRA_SMS_MESSAGE);
                            if (message != null) {
                                if (message.substring(7, 32).equalsIgnoreCase("is your verification code")) {
                                    String getOTP = message.substring(0, 6);

                                    otp1.setText(getOTP.substring(0, 1));
                                    otp2.setText(getOTP.substring(1, 2));
                                    otp3.setText(getOTP.substring(2, 3));
                                    otp4.setText(getOTP.substring(3, 4));
                                    otp5.setText(getOTP.substring(4, 5));
                                    otp6.setText(getOTP.substring(5, 6));
                                }
                            }
                            break;
                        case CommonStatusCodes.TIMEOUT:
                            if (this.otpReceiveListener != null)
                                this.otpReceiveListener.onOTPTimeOut();
                            break;
                    }
            }
        }
    }

    interface OTPReceiveListener {
        void onOTPReceived(String otp);

        void onOTPTimeOut();
    }
}