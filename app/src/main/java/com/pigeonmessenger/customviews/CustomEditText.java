package com.pigeonmessenger.customviews;

import android.content.Context;
import android.content.res.TypedArray;
import androidx.annotation.Nullable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.pigeonmessenger.R;

public class CustomEditText extends LinearLayout {

    ImageView iconView;
    EditText edittext;

    public CustomEditText(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CustomEditText(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.custom_edittext, this, true);

        setOrientation(VERTICAL);
        edittext = findViewById(R.id.edittext);
        iconView  =findViewById(R.id.iconView);
        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.customEdittext, 0, 0);

        String hint = a.getString(R.styleable.customEdittext_hint);
        int resId = a.getResourceId(R.styleable.customEdittext_icon, 0);
        int max_word = a.getInt(R.styleable.customEdittext_max_word,-1);
        String  inputType  =a.getString(R.styleable.customEdittext_input_type);



        if(max_word!=-1){
            edittext.setFilters(new InputFilter[] {new InputFilter.LengthFilter(max_word)});
        }
      if(inputType.equals("text"))
        edittext.setInputType(InputType.TYPE_CLASS_TEXT);
        else if (inputType.equals("number")){
          edittext.setInputType(InputType.TYPE_CLASS_NUMBER);
      }

        iconView.setImageResource(resId);
        edittext.setHint(hint);
        a.recycle();
    }

    public void addTextWatcher(TextWatcher textWatcher){
        edittext.addTextChangedListener(textWatcher);
    }


    public CharSequence getText(){
        return edittext.getText();
    }

    public void setText(String text){
        edittext.setText(text );
    }
}
