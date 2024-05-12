package com.duckduckgo.app.kahf_guard.dialogs.NativeDnsHelperFragments.Fragments;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.duckduckgo.app.browser.BrowserTabFragment;
import com.duckduckgo.app.browser.R;

public class NativeDnsHelperFragmentNetworkSettings extends Fragment {
    public NativeDnsHelperFragmentNetworkSettings(){
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = (ViewGroup) inflater.inflate(R.layout.dialog_native_dns_helper_fragment_open_network_settings, container, false);

        final ImageView logo = (ImageView) view.findViewById(R.id.logo);
        final TextView title = (TextView) view.findViewById(R.id.title);
        final TextView description = (TextView) view.findViewById(R.id.description);
        if (BrowserTabFragment.Companion.getLAST_CONNECT_OR_DISCONNECT_PRESSED()){
            title.setText(R.string.native_dns_helper_title_change_internet_settings);
            description.setText(R.string.native_dns_helper_text_change_internet_settings);
        } else {
            logo.setImageResource(R.drawable.logo_disconnected);
            title.setText(R.string.native_dns_helper_title_change_internet_settings_disable);
            description.setText(R.string.native_dns_helper_text_change_internet_settings_disable);
        }

        //set buttons
        view.findViewById(R.id.button_open_internet_settings).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
                startActivity(intent);
            }
        });

        //return
        return view;
    }

    public void onResume() {
        super.onResume();
        if (getView() != null) getView().requestLayout();
    }
}
