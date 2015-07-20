package com.madbeeapp.android.LauncherActivity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.madbeeapp.android.Helpers.TypefaceHelper;
import com.madbeeapp.android.MainActivity.MainActivity;
import com.madbeeapp.android.R;
import com.madbeeapp.android.Services.BuildMusicLibraryService;
import com.madbeeapp.android.Utils.Common;
import com.madbeeapp.android.WelcomeActivity.WelcomeActivity;

import java.io.File;

public class LauncherActivity extends ActionBarActivity {

    public static TextView buildingLibraryMainText;
    public static TextView buildingLibraryInfoText;
    public Context mContext;
    public Activity mActivity;
    private Common mApp;
    private RelativeLayout buildingLibraryLayout;
    private Handler mHandler;
    private Runnable scanFinishedCheckerRunnable = new Runnable() {

        @Override
        public void run() {

            if (!mApp.isBuildingLibrary()) {
                launchMainActivity();
            } else {
                mHandler.postDelayed(this, 100);
            }

        }

    };

    @SuppressLint("NewApi")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher);

        mContext = this;
        mActivity = this;
        mApp = (Common) mContext.getApplicationContext();
        mHandler = new Handler();

        //Increment the start count. This value will be used to determine when the library should be rescanned.
        int startCount = mApp.getSharedPreferences().getInt("START_COUNT", 1);
        mApp.getSharedPreferences().edit().putInt("START_COUNT", startCount + 1).commit();

        //Save the dimensions of the layout for later use on KitKat devices.
        final RelativeLayout launcherRootView = (RelativeLayout) findViewById(R.id.launcher_root_view);
        launcherRootView.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {

            @Override
            public void onGlobalLayout() {
                try {
                    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR1) {
                        //API levels 17+.
                        Display display = getWindowManager().getDefaultDisplay();
                        DisplayMetrics metrics = new DisplayMetrics();
                        display.getRealMetrics(metrics);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }

        });

        //Build the music library based on the user's scan frequency preferences.
        int scanFrequency = mApp.getSharedPreferences().getInt("SCAN_FREQUENCY", 5);
        int updatedStartCount = mApp.getSharedPreferences().getInt("START_COUNT", 1);

        //Launch the appropriate activity based on the "FIRST RUN" flag.
        if (mApp.getSharedPreferences().getBoolean(Common.FIRST_RUN, true)) {

            //Create the default Playlists directory if it doesn't exist.
            File playlistsDirectory = new File(Environment.getExternalStorageDirectory() + "/Playlists/");
            if (!playlistsDirectory.exists() || !playlistsDirectory.isDirectory()) {
                playlistsDirectory.mkdir();
            }

            //Disable equalizer for HTC devices by default.
            if (mApp.getSharedPreferences().getBoolean(Common.FIRST_RUN, true) &&
                    Build.PRODUCT.contains("HTC")) {
                mApp.getSharedPreferences().edit().putBoolean("EQUALIZER_ENABLED", false).commit();
            }

            //Send out a test broadcast to initialize the homescreen/lockscreen widgets.
            sendBroadcast(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME));

            Intent intent = new Intent(this, WelcomeActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);

        } else if (mApp.isBuildingLibrary()) {
            buildingLibraryMainText = (TextView) findViewById(R.id.building_music_library_text);
            buildingLibraryInfoText = (TextView) findViewById(R.id.building_music_library_info);
            buildingLibraryLayout = (RelativeLayout) findViewById(R.id.building_music_library_layout);

            buildingLibraryInfoText.setTypeface(TypefaceHelper.getTypeface(mContext, "calibri"));
            buildingLibraryInfoText.setPaintFlags(buildingLibraryInfoText.getPaintFlags() |
                    Paint.ANTI_ALIAS_FLAG |
                    Paint.SUBPIXEL_TEXT_FLAG);

            buildingLibraryMainText.setTypeface(TypefaceHelper.getTypeface(mContext, "calibri"));
            buildingLibraryMainText.setPaintFlags(buildingLibraryMainText.getPaintFlags() |
                    Paint.ANTI_ALIAS_FLAG |
                    Paint.SUBPIXEL_TEXT_FLAG);

            buildingLibraryMainText.setText(R.string.jams_is_building_library);
            buildingLibraryLayout.setVisibility(View.VISIBLE);

            //Initialize the runnable that will fire once the scan process is complete.
            mHandler.post(scanFinishedCheckerRunnable);

        } else if (mApp.getSharedPreferences().getBoolean("RESCAN_ALBUM_ART", false)) {

            buildingLibraryMainText = (TextView) findViewById(R.id.building_music_library_text);
            buildingLibraryInfoText = (TextView) findViewById(R.id.building_music_library_info);
            buildingLibraryLayout = (RelativeLayout) findViewById(R.id.building_music_library_layout);

            buildingLibraryInfoText.setTypeface(TypefaceHelper.getTypeface(mContext, "calibri"));
            buildingLibraryInfoText.setPaintFlags(buildingLibraryInfoText.getPaintFlags() |
                    Paint.ANTI_ALIAS_FLAG |
                    Paint.SUBPIXEL_TEXT_FLAG);

            buildingLibraryMainText.setTypeface(TypefaceHelper.getTypeface(mContext, "calibri"));
            buildingLibraryMainText.setPaintFlags(buildingLibraryMainText.getPaintFlags() |
                    Paint.ANTI_ALIAS_FLAG |
                    Paint.SUBPIXEL_TEXT_FLAG);

            buildingLibraryMainText.setText(R.string.jams_is_caching_artwork);
            initScanProcess(0);

        } else if ((mApp.getSharedPreferences().getBoolean("REBUILD_LIBRARY", false)) ||
                (scanFrequency == 0 && !mApp.isScanFinished()) ||
                (scanFrequency == 1 && !mApp.isScanFinished() && updatedStartCount % 3 == 0) ||
                (scanFrequency == 2 && !mApp.isScanFinished() && updatedStartCount % 5 == 0) ||
                (scanFrequency == 3 && !mApp.isScanFinished() && updatedStartCount % 10 == 0) ||
                (scanFrequency == 4 && !mApp.isScanFinished() && updatedStartCount % 20 == 0)) {

            buildingLibraryMainText = (TextView) findViewById(R.id.building_music_library_text);
            buildingLibraryInfoText = (TextView) findViewById(R.id.building_music_library_info);
            buildingLibraryLayout = (RelativeLayout) findViewById(R.id.building_music_library_layout);

            buildingLibraryInfoText.setTypeface(TypefaceHelper.getTypeface(mContext, "calibri"));
            buildingLibraryInfoText.setPaintFlags(buildingLibraryInfoText.getPaintFlags() |
                    Paint.ANTI_ALIAS_FLAG |
                    Paint.SUBPIXEL_TEXT_FLAG);

            buildingLibraryMainText.setTypeface(TypefaceHelper.getTypeface(mContext, "calibri"));
            buildingLibraryMainText.setPaintFlags(buildingLibraryMainText.getPaintFlags() |
                    Paint.ANTI_ALIAS_FLAG |
                    Paint.SUBPIXEL_TEXT_FLAG);

            initScanProcess(1);

        } else {
            //initInAppBilling();
            launchMainActivity();
        }
    }

    private void initScanProcess(int scanCode) {

        //Start the service that will start scanning the user's library/caching album art.
        mApp.setIsBuildingLibrary(true);
        buildingLibraryLayout.setVisibility(View.VISIBLE);
        if (scanCode == 0) {
            Intent intent = new Intent(this, BuildMusicLibraryService.class);
            intent.putExtra("SCAN_TYPE", "RESCAN_ALBUM_ART");
            startService(intent);

            mApp.getSharedPreferences().edit().putBoolean("RESCAN_ALBUM_ART", false).commit();

        } else if (scanCode == 1) {
            Intent intent = new Intent(this, BuildMusicLibraryService.class);
            intent.putExtra("SCAN_TYPE", "FULL_SCAN");
            startService(intent);
            mApp.getSharedPreferences().edit().putBoolean("REBUILD_LIBRARY", false).commit();
        }

        //Initialize the runnable that will fire once the scan process is complete.
        mHandler.post(scanFinishedCheckerRunnable);

    }

    private void launchMainActivity() {
        Intent intent = new Intent(mContext, MainActivity.class);
        int startupScreen = mApp.getSharedPreferences().getInt("STARTUP_SCREEN", 0);

        switch (startupScreen) {
            case 0:
                intent.putExtra("TARGET_FRAGMENT", "ARTISTS");
                break;
            case 1:
                intent.putExtra("TARGET_FRAGMENT", "ALBUM_ARTISTS");
                break;
            case 2:
                intent.putExtra("TARGET_FRAGMENT", "ALBUMS");
                break;
            case 3:
                intent.putExtra("TARGET_FRAGMENT", "SONGS");
                break;
            case 4:
                intent.putExtra("TARGET_FRAGMENT", "PLAYLISTS");
                break;
            case 5:
                intent.putExtra("TARGET_FRAGMENT", "GENRES");
                break;
            case 6:
                intent.putExtra("TARGET_FRAGMENT", "FOLDERS");
                break;
        }

        startActivity(intent);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        finish();

    }
}
