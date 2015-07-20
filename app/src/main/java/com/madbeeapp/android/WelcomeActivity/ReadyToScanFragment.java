package com.madbeeapp.android.WelcomeActivity;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.madbeeapp.android.Helpers.TypefaceHelper;
import com.madbeeapp.android.R;

public class ReadyToScanFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        Context mContext = getActivity().getApplication();
        View rootView = getActivity().getLayoutInflater().inflate(R.layout.fragment_welcome_screen_6, null);

        TextView welcomeHeader = (TextView) rootView.findViewById(R.id.welcome_header);
        welcomeHeader.setTypeface(TypefaceHelper.getTypeface(mContext, "calibri"));

        TextView welcomeText1 = (TextView) rootView.findViewById(R.id.welcome_text_1);
        welcomeText1.setTypeface(TypefaceHelper.getTypeface(mContext, "calibri"));

        TextView swipeLeftToContinue = (TextView) rootView.findViewById(R.id.swipe_left_to_continue);
        swipeLeftToContinue.setTypeface(TypefaceHelper.getTypeface(mContext, "calibri"));

        return rootView;
    }

}

