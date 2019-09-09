package com.pigeonmessenger.customviews;

import android.content.Context;
import android.util.AttributeSet;

import com.pigeonmessenger.R;


public class CustomButton extends androidx.appcompat.widget.AppCompatButton {

    private boolean isClickable = false;

    public CustomButton(Context context) {
        this(context,null);
    }

    public CustomButton(Context context, AttributeSet attrs) {
        super(context, attrs);

    }

    public void disableClickable(){
        isClickable = false;
        setTextColor(getResources().getColor(R.color.black));
        setClickable(false);
        setBackgroundResource(R.drawable.button_stroke);
    }

    public void enableClickable(){
        isClickable = true;
        setClickable(true);
        setTextColor(getResources().getColor(R.color.white));
        setBackgroundResource(R.drawable.button_gradient);
        setElevation(0);
    }

    @Override
    public boolean isClickable() {
        return isClickable;
    }
}
