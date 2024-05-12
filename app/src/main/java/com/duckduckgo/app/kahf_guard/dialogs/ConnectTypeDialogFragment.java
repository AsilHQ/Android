package com.duckduckgo.app.kahf_guard.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.duckduckgo.app.browser.R;
import com.duckduckgo.app.kahf_guard.utils.KahfGuardSharedPrefManager;
import com.google.android.material.button.MaterialButton;

import timber.log.Timber;

public class ConnectTypeDialogFragment extends DialogFragment {
    private @Nullable String _selectedTag = null;
    private @Nullable DialogOnDismiss _onDismissListener;

    public ConnectTypeDialogFragment(){
    }

    public void newInstance(@Nullable String selectedIndex, @Nullable DialogOnDismiss onDismissListener) {
        _selectedTag = selectedIndex;
        _onDismissListener = onDismissListener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        //get layout
        final Activity activity = requireActivity();
        LayoutInflater inflater = activity.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_connect_type, null);

        //set selected
        if (_selectedTag != null) {
            MaterialButton button = (MaterialButton) dialogView.findViewWithTag(_selectedTag);
            if (button != null) {
                button.setTextColor(activity.getColor(R.color.text_selected_connect_type));
                button.setIconTintResource(R.color.text_selected_connect_type);
            }
        }

        //prepare dialog
        builder.setView(dialogView);

        //create dialog
        final Dialog dialog = builder.create();

        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);

        //set buttons
        dialogView.findViewById(R.id.button_connect_type_vpn).setOnClickListener(view -> {
            Timber.tag("ConnectTypeDialogFragment").v("Selected: vpn");
            KahfGuardSharedPrefManager.INSTANCE.setConnectType(activity.getString(R.string.connect_type_vpn));
            new Handler().postDelayed(dialog::cancel, 250); //small delay to let user know his selection is accepted
        });
        dialogView.findViewById(R.id.button_connect_type_private_dns).setOnClickListener(view -> {
            Timber.tag("ConnectTypeDialogFragment").v("Selected: Private dns");
            KahfGuardSharedPrefManager.INSTANCE.setConnectType(activity.getString(R.string.connect_type_private_dns));
            new Handler().postDelayed(dialog::cancel, 250); //small delay to let user know his selection is accepted
        });

        return dialog;
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);

        if (_onDismissListener != null) {
            _onDismissListener.onDismiss(dialog);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Log.v("ConnectTypeDialogFragment", "Destroyed");
    }
}
