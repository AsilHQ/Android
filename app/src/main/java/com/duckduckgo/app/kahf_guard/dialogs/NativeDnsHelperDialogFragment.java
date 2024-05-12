package com.duckduckgo.app.kahf_guard.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.viewpager2.widget.ViewPager2;

import com.duckduckgo.app.browser.BrowserTabFragment;
import com.duckduckgo.app.browser.R;
import com.duckduckgo.app.kahf_guard.animations.ZoomOutPageTransformer;
import com.duckduckgo.app.kahf_guard.dialogs.NativeDnsHelperFragments.Fragments.NativeDnsHelperFragmentChangePrivateDns;
import com.duckduckgo.app.kahf_guard.dialogs.NativeDnsHelperFragments.Fragments.NativeDnsHelperFragmentHostname;
import com.duckduckgo.app.kahf_guard.dialogs.NativeDnsHelperFragments.Fragments.NativeDnsHelperFragmentIntro;
import com.duckduckgo.app.kahf_guard.dialogs.NativeDnsHelperFragments.Fragments.NativeDnsHelperFragmentNetworkSettings;
import com.duckduckgo.app.kahf_guard.dialogs.NativeDnsHelperFragments.NativeDnsHelperAdapter;
import com.duckduckgo.app.kahf_guard.dialogs.NativeDnsHelperFragments.NativeDnsHelperFragmentCreator;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.ArrayList;

public class NativeDnsHelperDialogFragment extends DialogFragment {
    private @Nullable Integer _selectedItemIndex = null;
    private @Nullable DialogOnDismiss _onDismissListener;

    // Empty constructor required for Fragment instantiation
    public NativeDnsHelperDialogFragment() {
    }

    public void newInstance(@Nullable DialogOnDismiss onDismissListener){
        _onDismissListener = onDismissListener;
    }

    public void setSelectedItemIndex(int selectedItem){
        _selectedItemIndex = selectedItem;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        //get layout
        final Activity activity = requireActivity();
        LayoutInflater inflater = activity.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_native_dns_helper, null);
        ViewPager2 viewPager = dialogView.findViewById(R.id.view_pager);

        //set view pager's animation
        viewPager.setPageTransformer(new ZoomOutPageTransformer());
        //setup view pager adapter
        ArrayList<NativeDnsHelperFragmentCreator> items = new ArrayList<>();
        items.add(NativeDnsHelperFragmentIntro::new);
        items.add(NativeDnsHelperFragmentHostname::new);
        items.add(NativeDnsHelperFragmentNetworkSettings::new);
        items.add(NativeDnsHelperFragmentChangePrivateDns::new);

        NativeDnsHelperAdapter adapter = new NativeDnsHelperAdapter(items, this);
        viewPager.setAdapter(adapter);

        if (_selectedItemIndex != null){
            viewPager.setCurrentItem(_selectedItemIndex, true);
        }

        //attach tab layout
        if (!BrowserTabFragment.Companion.getLAST_CONNECT_OR_DISCONNECT_PRESSED()){
            //set style if about to disconnect
            TabLayout tabLayout = dialogView.findViewById(R.id.tab_layout_for_disconnect);
            new TabLayoutMediator((TabLayout) tabLayout, viewPager, new TabLayoutMediator.TabConfigurationStrategy() {
                @Override
                public void onConfigureTab(@NonNull TabLayout.Tab tab, int position) {
                    //tab.setText(tabTitles[position]);
                }
            }).attach();
            tabLayout.setVisibility(View.VISIBLE);
            dialogView.findViewById(R.id.tab_layout).setVisibility(View.GONE);

            //change dialog bg to red
            dialogView.setBackgroundColor(activity.getColor(R.color.bg_disconnected_light));

            //change tab layout
            tabLayout.setSelectedTabIndicatorColor(activity.getColor(R.color.tab_indicator_disconnected));
        } else {
            TabLayout tabLayout = dialogView.findViewById(R.id.tab_layout);
            new TabLayoutMediator((TabLayout) tabLayout, viewPager, new TabLayoutMediator.TabConfigurationStrategy() {
                @Override
                public void onConfigureTab(@NonNull TabLayout.Tab tab, int position) {
                    //tab.setText(tabTitles[position]);
                }
            }).attach();
        }

        //prepare dialog
        builder.setView(dialogView);

        //create dialog
        Dialog dialog = builder.create();
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);

        //close button
        dialogView.findViewById(R.id.dialog_close_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.cancel();
            }
        });

        return dialog;
    }

    //it will not execute dismiss callback.
    public void manualDismiss(){
        try {
            _onDismissListener = null;
            dismiss();
        } catch (Exception e){
            Log.v("NativeDnsHelperDialogFragment manualDismiss", "Exception: " + e.getMessage());
        }
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);

        if (_onDismissListener != null) {
            _onDismissListener.onDismiss(dialog);
        }
    }
}
