package com.madbeeapp.android.WelcomeActivity;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.TranslateAnimation;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.madbeeapp.android.Helpers.TypefaceHelper;
import com.madbeeapp.android.MusicFoldersSelectionFragment.MusicFoldersSelectionFragment;
import com.madbeeapp.android.R;
import com.madbeeapp.android.Utils.Common;

public class MusicFoldersFragment extends Fragment {

    private MusicFoldersSelectionFragment mMusicFoldersSelectionFragment;
    private TranslateAnimation mSlideInAnimation;
    private TranslateAnimation mSlideOutAnimation;
    private RelativeLayout mFoldersLayout;
    /**
     * RadioButton selection listener.
     */
    private OnCheckedChangeListener onCheckedChangeListener = new OnCheckedChangeListener() {

        @Override
        public void onCheckedChanged(RadioGroup radioGroup, int radioButtonId) {
            switch (radioButtonId) {
                case R.id.get_all_songs_radio:
                    mFoldersLayout.startAnimation(mSlideOutAnimation);
                    mFoldersLayout.setEnabled(false);
                    break;
                case R.id.pick_folders_radio:
                    mFoldersLayout.startAnimation(mSlideInAnimation);
                    mFoldersLayout.setEnabled(true);
                    break;
            }

        }

    };
    /**
     * Slide out animation listener.
     */
    private AnimationListener slideOutListener = new AnimationListener() {

        @Override
        public void onAnimationEnd(Animation arg0) {
            mFoldersLayout.setVisibility(View.INVISIBLE);
        }

        @Override
        public void onAnimationRepeat(Animation arg0) {
        }

        @Override
        public void onAnimationStart(Animation arg0) {
            mFoldersLayout.setVisibility(View.VISIBLE);

        }

    };
    /**
     * Slide in animation listener.
     */
    private AnimationListener slideInListener = new AnimationListener() {

        @Override
        public void onAnimationEnd(Animation arg0) {
            mFoldersLayout.setVisibility(View.VISIBLE);
        }

        @Override
        public void onAnimationRepeat(Animation arg0) {

        }

        @Override
        public void onAnimationStart(Animation arg0) {
            mFoldersLayout.setVisibility(View.VISIBLE);

        }

    };

    public MusicFoldersFragment() {
        mMusicFoldersSelectionFragment = null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        Context mContext = getActivity().getApplicationContext();
        Common mApp = (Common) mContext;
        View rootView = getActivity().getLayoutInflater().inflate(R.layout.fragment_welcome_screen_2, null);

        mFoldersLayout = (RelativeLayout) rootView.findViewById(R.id.folders_fragment_holder);
        if (mApp.getSharedPreferences().getInt("MUSIC_FOLDERS_SELECTION", 0) == 0) {
            mFoldersLayout.setVisibility(View.INVISIBLE);
            mFoldersLayout.setEnabled(false);
        } else {
            mFoldersLayout.setVisibility(View.VISIBLE);
            mFoldersLayout.setEnabled(true);
        }

        mSlideInAnimation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 2.0f,
                Animation.RELATIVE_TO_SELF, 0.0f);

        mSlideInAnimation.setDuration(600);
        mSlideInAnimation.setInterpolator(new AccelerateDecelerateInterpolator());
        mSlideInAnimation.setAnimationListener(slideInListener);

        mSlideOutAnimation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 2.0f);
        mSlideOutAnimation.setDuration(600);
        mSlideOutAnimation.setInterpolator(new AccelerateDecelerateInterpolator());
        mSlideOutAnimation.setAnimationListener(slideOutListener);

        FragmentManager mChildFragmentManager = this.getChildFragmentManager();
        mChildFragmentManager.beginTransaction()
                .add(R.id.folders_fragment_holder, getMusicFoldersSelectionFragment())
                .commit();

        TextView mWelcomeHeader = (TextView) rootView.findViewById(R.id.welcome_header);
        mWelcomeHeader.setTypeface(TypefaceHelper.getTypeface(mContext, "calibri"));

        RadioGroup mMusicFoldersOptions = (RadioGroup) rootView.findViewById(R.id.music_library_welcome_radio_group);
        RadioButton getAllSongsRadioButton = (RadioButton) mMusicFoldersOptions.findViewById(R.id.get_all_songs_radio);
        RadioButton letMePickFoldersRadioButton = (RadioButton) mMusicFoldersOptions.findViewById(R.id.pick_folders_radio);

        getAllSongsRadioButton.setTypeface(TypefaceHelper.getTypeface(mContext, "calibri"));
        letMePickFoldersRadioButton.setTypeface(TypefaceHelper.getTypeface(mContext, "calibri"));

        mMusicFoldersOptions.setOnCheckedChangeListener(onCheckedChangeListener);
        return rootView;
    }

    /**
     * Instantiates a new fragment if mMusicFoldersSelectionFragment is null.
     * Returns the current fragment, otherwise.
     */
    public MusicFoldersSelectionFragment getMusicFoldersSelectionFragment() {
        if (mMusicFoldersSelectionFragment == null) {
            mMusicFoldersSelectionFragment = new MusicFoldersSelectionFragment();

            Bundle bundle = new Bundle();
            bundle.putBoolean("com.madbeeapp.android.WELCOME", true);
            mMusicFoldersSelectionFragment.setArguments(bundle);
        }
        return mMusicFoldersSelectionFragment;
    }
}

