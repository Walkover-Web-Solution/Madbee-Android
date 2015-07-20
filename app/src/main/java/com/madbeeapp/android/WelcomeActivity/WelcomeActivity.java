package com.madbeeapp.android.WelcomeActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v7.app.ActionBarActivity;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import com.madbeeapp.android.AsyncTasks.AsyncSaveMusicFoldersTask;
import com.madbeeapp.android.MiscFragments.BuildingLibraryProgressFragment;
import com.madbeeapp.android.R;
import com.madbeeapp.android.Services.BuildMusicLibraryService;
import com.madbeeapp.android.Utils.Prefs;
import com.splunk.mint.Mint;
import com.viewpagerindicator.LinePageIndicator;

public class WelcomeActivity extends ActionBarActivity {

    public static BuildingLibraryProgressFragment mBuildingLibraryProgressFragment;
    private Context mContext;
    private ViewPager welcomeViewPager;
    private LinePageIndicator indicator;
    /**
     * Fade out animation listener.
     */
    private AnimationListener fadeOutListener = new AnimationListener() {

        @Override
        public void onAnimationEnd(Animation arg0) {
            indicator.setVisibility(View.INVISIBLE);
            Intent intent = new Intent(mContext, BuildMusicLibraryService.class);
            startService(intent);
        }

        @Override
        public void onAnimationRepeat(Animation arg0) {

        }

        @Override
        public void onAnimationStart(Animation arg0) {

        }
    };

    /**
     * Page scroll listener.
     */
    private OnPageChangeListener pageChangeListener = new OnPageChangeListener() {

        @Override
        public void onPageScrollStateChanged(int scrollState) {

        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int page) {
            if (page == 2 || page == 3) {
                if (Prefs.getMobileNumber(mContext) == null || Prefs.getMobileNumber(mContext).equals("")) {
                    welcomeViewPager.setCurrentItem(1);
                    Toast.makeText(mContext, "Number verification is compulsory", Toast.LENGTH_LONG).show();
                } else {
                    if (page == 3)
                        showBuildingLibraryProgress();
                }
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mint.initAndStartSession(WelcomeActivity.this, "b48ec200");
        mContext = this;
        setContentView(R.layout.activity_welcome);
        try {
            getSupportActionBar().hide();
        } catch (Exception ignored) {
        }
        welcomeViewPager = (ViewPager) findViewById(R.id.welcome_pager);

        FragmentManager fm = getSupportFragmentManager();
        welcomeViewPager.setAdapter(new WelcomePagerAdapter(fm));
        welcomeViewPager.setOffscreenPageLimit(4);

        indicator = (LinePageIndicator) findViewById(R.id.indicator);
        indicator.setViewPager(welcomeViewPager);

        final float density = getResources().getDisplayMetrics().density;
        indicator.setSelectedColor(0x880099CC);
        indicator.setUnselectedColor(0xFF4F4F4F);
        indicator.setStrokeWidth(2 * density);
        indicator.setLineWidth(30 * density);
        indicator.setOnPageChangeListener(pageChangeListener);

        //Check if the library needs to be rebuilt and this isn't the first run.
        if (getIntent().hasExtra("REFRESH_MUSIC_LIBRARY"))
            showBuildingLibraryProgress();

        MusicFoldersFragment mMusicFoldersFragment = new MusicFoldersFragment();
        new AsyncSaveMusicFoldersTask(mContext.getApplicationContext(),
                mMusicFoldersFragment.getMusicFoldersSelectionFragment()
                        .getMusicFoldersHashMap())
                .execute();

    }

    private void showBuildingLibraryProgress() {
        //Disables swiping events on the pager.
        welcomeViewPager.setCurrentItem(4);
        welcomeViewPager.setOnTouchListener(new OnTouchListener() {

            @Override
            public boolean onTouch(View arg0, MotionEvent arg1) {
                return true;
            }

        });

        //Fade out the ViewPager indicator.
        Animation fadeOutAnim = AnimationUtils.loadAnimation(mContext, R.anim.fade_out);
        fadeOutAnim.setDuration(600);
        fadeOutAnim.setAnimationListener(fadeOutListener);
        indicator.startAnimation(fadeOutAnim);
    }

    public void loadFinishFragment() {
        welcomeViewPager.setCurrentItem(2);
    }

    class WelcomePagerAdapter extends FragmentStatePagerAdapter {

        public WelcomePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        //This method controls which fragment should be shown on a specific screen.
        @Override
        public Fragment getItem(int position) {

            //Assign the appropriate screen to the fragment object, based on which screen is displayed.
            switch (position) {
                case 0:
                    return new WelcomeFragment();
                case 1:
                    return new SocialFragment();
                case 2:
                    return new ReadyToScanFragment();
                case 3:
                    mBuildingLibraryProgressFragment = new BuildingLibraryProgressFragment();
                    return mBuildingLibraryProgressFragment;
                default:
                    return null;
            }
        }

        @Override
        public int getCount() {
            return 4;
        }
    }
}
