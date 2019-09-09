package com.pigeonmessenger;

import android.app.Dialog;
import android.os.Bundle;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.appcompat.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.pnikosis.materialishprogress.ProgressWheel;

public class LoadingDialog extends DialogFragment {

    private static final String TAG = "LoadingDialog";
    private TextView mTextView;
    private ProgressWheel progressWheel;
    private String message;


    public static LoadingDialog show(FragmentManager manager) {
        LoadingDialog dialog = new LoadingDialog();
        dialog.showAllowingStateLoss(manager, TAG);
        return dialog;
    }

    /**
     * This method is adapted from {@link #show(FragmentManager, String)}
     */
    public void showAllowingStateLoss(FragmentManager manager, String tag) {
        // This prevents us from hitting FragmentManager.checkStateLoss() which
        // throws a runtime exception if state has already been saved.
        if (manager.isStateSaved()) {
            return;
        }

        show(manager, tag);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View rootView = inflater.inflate(R.layout.loading_dialog, null);


        progressWheel = rootView.findViewById(R.id.progressWheel);
        progressWheel.spin();
        mTextView = rootView.findViewById(R.id.messageView);

        if (message != null)
            mTextView.setText(message);

        AlertDialog dialog = new AlertDialog.Builder(getActivity()).setView(rootView).create();
        dialog.getWindow().setLayout(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT);
        return dialog;
    }


    public void setMessage(String message) {
        this.message = message;
    }

}
