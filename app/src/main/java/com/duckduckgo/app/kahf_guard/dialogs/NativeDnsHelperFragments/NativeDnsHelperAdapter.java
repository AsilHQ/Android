package com.duckduckgo.app.kahf_guard.dialogs.NativeDnsHelperFragments;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.ArrayList;

public class NativeDnsHelperAdapter extends FragmentStateAdapter {
    private ArrayList<NativeDnsHelperFragmentCreator> _items;

    public NativeDnsHelperAdapter(ArrayList<NativeDnsHelperFragmentCreator> items, Fragment fragment){
        super(fragment);
        this._items = items;
    }

    public void changeItems(ArrayList<NativeDnsHelperFragmentCreator> items){
        this._items = items;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        Log.v("Create fragment", position + "");
        Log.v("Create fragment", _items.get(position).toString());
        return _items.get(position).create();
    }

    @Override
    public int getItemCount() {
        return _items.size();
    }
}
