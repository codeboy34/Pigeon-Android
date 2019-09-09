package com.pigeonmessenger.customviews;

import android.content.Context;
import android.os.CountDownTimer;
import android.util.AttributeSet;
import android.view.View;

import com.pigeonmessenger.R;

import java.util.concurrent.TimeUnit;

public class OTPCountDownView extends androidx.appcompat.widget.AppCompatTextView implements View.OnClickListener {

    private CountDownTimer countDownTimer;

    private OnResendListener resendListener;

    public OTPCountDownView(Context context, AttributeSet attrs) {
        this(context, attrs,0);
    }

    public OTPCountDownView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        countDownTimer = new CountDownTimer(60000,1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                 long secounds =  TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished);
                 String str = "Resend code ";
                 if(secounds<10) str = str+"00:0"+secounds;
                 else str = str+"00:"+secounds;

                 setText(str);
            }

            @Override
            public void onFinish() {
                setText("Resend Code");
                setTextColor(getResources().getColor(R.color.otp_resend ));
                setOnClickListener(OTPCountDownView.this);
            }
        };
    }

    public void startCounter(){
        countDownTimer.start();
        setOnClickListener(null);
        setTextColor(getResources().getColor(R.color.otp_resend_timer ));
    }

    public void stopCounter(){
        countDownTimer.cancel();
    }

    @Override
    public void onClick(View v) {
        if (resendListener!=null)
            resendListener.onResend();
    }

    public void setResendListener(OnResendListener resendListener) {
        this.resendListener = resendListener;
    }

    public interface OnResendListener{
        void onResend();
    }

}
