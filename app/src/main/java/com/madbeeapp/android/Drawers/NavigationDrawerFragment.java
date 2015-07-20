package com.madbeeapp.android.Drawers;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.madbeeapp.android.MainActivity.DrawerHelper;
import com.madbeeapp.android.MainActivity.MainActivity;
import com.madbeeapp.android.R;

import java.util.ArrayList;

public class NavigationDrawerFragment extends Fragment {

    DrawerLayout drawerLayout;
    ArrayList<DrawerHelper> list;
    NavigationDrawerAdapter mBrowsersAdapter;
    ListView browsersListView;

    private OnItemClickListener browsersClickListener = new OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int position, long dbID) {
            drawerLayout.closeDrawer(GravityCompat.START);
            ((MainActivity) getActivity()).loadFragment(position);
        }
    };

    public NavigationDrawerFragment(ArrayList<DrawerHelper> list) {
        this.list = list;
    }

    @Override
    public void onResume() {
        super.onResume();
        int position = ((MainActivity) getActivity()).getSelectedPage();
        mBrowsersAdapter = new NavigationDrawerAdapter(getActivity(), list, position);
        browsersListView.setAdapter(mBrowsersAdapter);
        browsersListView.setOnItemClickListener(browsersClickListener);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.navigation_drawer_layout, container, false);
        drawerLayout = (DrawerLayout) getActivity().findViewById(R.id.main_activity_drawer_root);
        browsersListView = (ListView) rootView.findViewById(R.id.browsers_list_view);
        browsersListView.setDividerHeight(0);
        return rootView;
    }
}
