package com.madbeeapp.android.Utils;

import android.app.Application;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.provider.ContactsContract;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.TelephonyManager;

import com.madbeeapp.android.DBHelpers.DBAccessHelper;
import com.madbeeapp.android.LauncherActivity.LauncherActivity;
import com.madbeeapp.android.PlaybackKickstarter.PlaybackKickstarter;
import com.madbeeapp.android.R;
import com.madbeeapp.android.Services.AudioPlaybackService;
import com.nostra13.universalimageloader.cache.memory.impl.WeakMemoryCache;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.Random;

/**
 * Singleton class that provides access to common objects
 * and methods used in the application.
 *
 * @author Arpit Gandhi
 */
public class Common extends Application {

    //Update UI broadcast flags.
    public static final String UPDATE_UI_BROADCAST = "com.madbeeapp.android.NEW_SONG_UPDATE_UI";
    public static final String SHOW_EXPANDEDVIEW = "ShowExpandedView";
    public static final String SHOW_AUDIOBOOK_TOAST = "AudiobookToast";
    public static final String UPDATE_SEEKBAR_DURATION = "UpdateSeekbarDuration";
    public static final String UPDATE_PAGER_POSTIION = "UpdatePagerPosition";
    public static final String UPDATE_PLAYBACK_CONTROLS = "UpdatePlabackControls";
    public static final String SERVICE_STOPPING = "ServiceStopping";
    public static final String INIT_PAGER = "InitPager";
    public static final String NEW_QUEUE_ORDER = "NewQueueOrder";

    //Contants for identifying each fragment/activity.
    public static final int ARTISTS_FRAGMENT = 0;
    public static final int ALBUMS_FRAGMENT = 1;
    public static final int SONGS_FRAGMENT = 2;
    public static final int GENRES_FRAGMENT = 3;
    public static final int ARTISTS_FLIPPED_FRAGMENT = 4;
    public static final int ARTISTS_FLIPPED_SONGS_FRAGMENT = 5;
    public static final int ALBUM_ARTISTS_FLIPPED_FRAGMENT = 6;
    public static final int ALBUM_ARTISTS_FLIPPED_SONGS_FRAGMENT = 7;
    public static final int ALBUMS_FLIPPED_FRAGMENT = 8;
    public static final int GENRES_FLIPPED_FRAGMENT = 9;
    public static final int GENRES_FLIPPED_SONGS_FRAGMENT = 10;
    public static final int TOP_25_PLAYED_FRAGMENT = 11;
    public static final int RECENTLY_PLAYED_FRAGMENT = 13;
    public static final int RECENTLY_ADDED_FRAGMENT = 14;

    //Constants for identifying playback routes.
    public static final int PLAY_ALL_SONGS = 0;

    //Miscellaneous flags/identifiers.
    public static final String FRAGMENT_ID = "FragmentId";
    public static final String CONTACT_NUMBER = "contact_number";
    public static final String SONG_TITLE = "SongTitle";
    public static final String SONG_ALBUM = "SongAlbum";
    public static final String SONG_ARTIST = "SongArtist";

    //SharedPreferences keys.
    public static final String CROSSFADE_ENABLED = "CrossfadeEnabled";
    public static final String CROSSFADE_DURATION = "CrossfadeDuration";
    public static final String REPEAT_MODE = "RepeatMode";
    public static final String SHUFFLE_ON = "ShuffleOn";
    public static final String FIRST_RUN = "FirstRun";

    //Repeat mode constants.
    public static final int REPEAT_PLAYLIST = 1;
    public static final int REPEAT_SONG = 2;
    public static final int A_B_REPEAT = 3;
    public static ProgressDialog dialog;
    //SharedPreferences.
    private static SharedPreferences mSharedPreferences;
    //Context.
    private Context mContext;
    //Service reference and flags.
    private AudioPlaybackService mService;
    private boolean mIsServiceRunning = false;
    //Playback kickstarter object.
    private PlaybackKickstarter mPlaybackKickstarter;
    //Picasso instance.
    private Picasso mPicasso;
    //Indicates if the library is currently being built.
    private boolean mIsBuildingLibrary = false;
    private boolean mIsScanFinished = false;
    //ImageLoader/ImageLoaderConfiguration objects for ListViews and GridViews.
    private ImageLoader mImageLoader;

    public static void show_PDialog(Context con, String message, boolean cancelable) {
        dialog = new ProgressDialog(con, ProgressDialog.THEME_DEVICE_DEFAULT_LIGHT);
        dialog.setMessage(message);
        dialog.setCancelable(cancelable);
        dialog.show();
    }

    public static String getSongStream(String songId) {
        return "https://api.soundcloud.com/tracks/" + songId + "/stream?client_id=7ff7ee64f4a0dd99d691a670c0108a55&format=son";
    }

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager conMan = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (conMan.getNetworkInfo(0).getState() == NetworkInfo.State.CONNECTED)
            return true;//connected to data
        else if (conMan.getNetworkInfo(1).getState() == NetworkInfo.State.CONNECTED)
            return true; //connected to wifi
        return false;
    }

    public static int generateVerificaitonCode() {
        int min = 1000;
        int max = 9999;
        Random r = new Random();
        return r.nextInt(max - min + 1) + min;
    }

    public HashMap<String, String> getNumberList() {
        HashMap<String, String> numbers = new HashMap<>();
        final Cursor phones = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);
        if (phones.moveToFirst()) {
            do {
                if (Integer.parseInt(phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.HAS_PHONE_NUMBER))) > 0) {
                    try {
                        String displayName = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                        String phoneNumber = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                        if (phoneNumber != null)
                            if (phoneNumber.length() > 8) {
                                if (!numbers.containsKey(phoneNumber)) {
                                    phoneNumber = phoneNumber.replaceAll("[^0-9+]", "");
                                    if (phoneNumber.startsWith("00")) {
                                        phoneNumber = phoneNumber.substring(2);
                                    } else if (phoneNumber.startsWith("0")) {
                                        phoneNumber = phoneNumber.substring(1);
                                    }
                                    if (!phoneNumber.startsWith("+") && !phoneNumber.startsWith(getCountryZipCode())) {
                                        phoneNumber = getCountryZipCode() + phoneNumber;
                                    }
                                    phoneNumber = phoneNumber.replaceAll("[^0-9]", "");
                                    numbers.put(phoneNumber, displayName);
                                }
                            }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } while (phones.moveToNext());
        }
        phones.close();
        return numbers;
    }

    public String getCountryZipCode() {
        String CountryID;
        String CountryZipCode = "";

        TelephonyManager manager = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        CountryID = manager.getSimCountryIso().toUpperCase();
        String[] rl = this.getResources().getStringArray(R.array.CountryCodes);
        for (String aRl : rl) {
            String[] g = aRl.split(",");
            if (g[1].trim().equals(CountryID.trim())) {
                CountryZipCode = g[0];
                break;
            }
        }
        return CountryZipCode;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //Application context.
        mContext = getApplicationContext();
        //SharedPreferences.
        mSharedPreferences = this.getSharedPreferences("com.madbeeapp.android", Context.MODE_MULTI_PROCESS);
        //Playback kickstarter.
        mPlaybackKickstarter = new PlaybackKickstarter(this.getApplicationContext());

        //Picasso.
        mPicasso = new Picasso.Builder(mContext).build();

        //ImageLoader.
        mImageLoader = ImageLoader.getInstance();
        ImageLoaderConfiguration mImageLoaderConfiguration = new ImageLoaderConfiguration.Builder(getApplicationContext())
                .memoryCache(new WeakMemoryCache())
                .memoryCacheSizePercentage(13)
                .imageDownloader(new ByteArrayUniversalImageLoader(mContext))
                .build();
        mImageLoader.init(mImageLoaderConfiguration);
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable e) {
                handleUncaughtException(e);
            }
        });
    }

    /**
     * Sends out a local broadcast that notifies all receivers to update
     * their respective UI elements.
     */
    public void broadcastUpdateUICommand(String[] updateFlags, String[] flagValues) {
        Intent intent = new Intent(UPDATE_UI_BROADCAST);
        for (int i = 0; i < updateFlags.length; i++) {
            intent.putExtra(updateFlags[i], flagValues[i]);
        }

        LocalBroadcastManager mLocalBroadcastManager = LocalBroadcastManager.getInstance(mContext);
        mLocalBroadcastManager.sendBroadcast(intent);
    }

    /**
     * Converts milliseconds to hh:mm:ss format.
     */
    public String convertMillisToMinsSecs(long milliseconds) {

        int secondsValue = (int) (milliseconds / 1000) % 60;
        int minutesValue = (int) ((milliseconds / (1000 * 60)) % 60);
        int hoursValue = (int) ((milliseconds / (1000 * 60 * 60)) % 24);

        String seconds;
        String minutes;
        String hours;

        if (secondsValue < 10) {
            seconds = "0" + secondsValue;
        } else {
            seconds = "" + secondsValue;
        }

        if (minutesValue < 10) {
            minutes = "0" + minutesValue;
        } else {
            minutes = "" + minutesValue;
        }

        hours = "" + hoursValue;

        String output;
        if (hoursValue != 0) {
            output = hours + ":" + minutes + ":" + seconds;
        } else {
            output = minutes + ":" + seconds;
        }

        return output;
    }

    public DBAccessHelper getDBAccessHelper() {
        return DBAccessHelper.getInstance(mContext);
    }

    public SharedPreferences getSharedPreferences() {
        return mSharedPreferences;
    }

    public Picasso getPicasso() {
        return mPicasso;
    }

    public boolean isBuildingLibrary() {
        return mIsBuildingLibrary;
    }

    public boolean isScanFinished() {
        return mIsScanFinished;
    }

    public AudioPlaybackService getService() {
        return mService;
    }

    public void setService(AudioPlaybackService service) {
        mService = service;
    }

    public ImageLoader getImageLoader() {
        return mImageLoader;
    }

    public boolean isServiceRunning() {
        return mIsServiceRunning;
    }

    public boolean isCrossfadeEnabled() {
        return getSharedPreferences().getBoolean(CROSSFADE_ENABLED, false);
    }

    public int getCrossfadeDuration() {
        return getSharedPreferences().getInt(CROSSFADE_DURATION, 5);
    }

    /*
     * Setter methods.
	 */
    public PlaybackKickstarter getPlaybackKickstarter() {
        return mPlaybackKickstarter;
    }

    public void setIsBuildingLibrary(boolean isBuildingLibrary) {
        mIsBuildingLibrary = isBuildingLibrary;
    }

    public void setIsScanFinished(boolean isScanFinished) {
        mIsScanFinished = isScanFinished;
    }

    public void setIsServiceRunning(boolean running) {
        mIsServiceRunning = running;
    }

    public void handleUncaughtException(Throwable e) {
        e.printStackTrace();
        Intent intent = new Intent(getApplicationContext(), LauncherActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }
}
