package com.madbeeapp.android.MainActivity;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.astuetz.PagerSlidingTabStrip;
import com.madbeeapp.android.Animations.FadeAnimation;
import com.madbeeapp.android.DBHelpers.DBAccessHelper;
import com.madbeeapp.android.Drawers.NavigationDrawerFragment;
import com.madbeeapp.android.FriendsListFragment.FriendsListViewFragment;
import com.madbeeapp.android.Helpers.SongHelper;
import com.madbeeapp.android.Helpers.TypefaceHelper;
import com.madbeeapp.android.ImageTransformers.PicassoMirrorReflectionTransformer;
import com.madbeeapp.android.ListViewFragment.ListViewFragment;
import com.madbeeapp.android.ListViewFragment.PlaylistViewFragment;
import com.madbeeapp.android.R;
import com.madbeeapp.android.Search.SearchActivity;
import com.madbeeapp.android.Utils.Common;
import com.madbeeapp.android.Utils.Constants;
import com.madbeeapp.android.Utils.Prefs;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import com.splunk.mint.Mint;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.velocity.view.pager.library.VelocityViewPager;

import org.adw.library.widgets.discreteseekbar.DiscreteSeekBar;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import mbanje.kurt.fabbutton.FabButton;

@SuppressWarnings({"ConstantConditions", "deprecation"})
public class MainActivity extends ActionBarActivity {
    public static final String START_SERVICE = "StartService";
    //Layout flags.
    public static final String FRAGMENT_HEADER = "FragmentHeader";
    public static final int LIST_LAYOUT = 0;
    public static final int FRIENDS_LAYOUT = 2;
    public static final int PLAYLIST_LAYOUT = 2;
    private static final int SEEKBAR_STROBE_ANIM_REPEAT = Animation.INFINITE;
    public static int mCurrentFragmentLayout;
    ArrayList<DrawerHelper> complete;
    SlidingUpPanelLayout slidingUpPanelLayout;
    LinearLayout collapseView;
    RelativeLayout expandedView, collapseButtonLayout;
    ImageView collapseButton;
    Toolbar toolbar;
    TextView mMiniSongName, mMiniArtistAlbumName;
    ViewPager pager;
    PagerSlidingTabStrip tabs;
    NavigationDrawerFragment navigationDrawerFragment;
    LibraryAdapter adapter;
    ImageButton mABRepeatButton;
    ArrayList<String> names;
    int position = 0;
    private Context mContext;
    private ActionBarDrawerToggle mDrawerToggle;
    private DrawerLayout mDrawerLayout;
    private Common mApp;
    private DiscreteSeekBar mSeekbar;
    private ImageButton mPlayPauseButton;
    private FabButton mPlayPauseButtonMini;
    private ImageButton mRepeatButton;
    private ImageView mLikeButton;
    /**
     * AB Repeat button click listener.
     */
    private View.OnClickListener mABRepeatButtonClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View arg0) {
            if (mApp.isServiceRunning()) {
                if (mApp.getService().getRepeatMode() == Common.A_B_REPEAT) {
                    mABRepeatButton.setImageResource(R.drawable.ic_ab_repeat);
                    mApp.getService().setRepeatMode(Common.REPEAT_PLAYLIST);
                    mApp.getService().clearABRepeatRange();
                } else {
                    FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                    ABRepeatDialog dialog = new ABRepeatDialog();
                    dialog.show(ft, "repeatSongRangeDialog");
                }
            }
        }
    };

    /**
     * Like button click listener.
     */
    private View.OnClickListener likeButtonClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View arg0) {
            if (mApp.isServiceRunning())
                if (Common.isNetworkAvailable(mContext)) {
                    new AsyncTask<SongHelper, SongHelper, SongHelper>() {
                        String response = null;

                        @Override
                        protected void onPreExecute() {
                            super.onPreExecute();
                            Common.show_PDialog(mContext, "I like this song..", false);
                        }

                        @Override
                        protected SongHelper doInBackground(SongHelper... songHelpers) {
                            if (Common.isNetworkAvailable(mContext))
                                response = likeSong(songHelpers[0]);
                            return null;
                        }

                        @Override
                        protected void onPostExecute(SongHelper songHelper) {
                            super.onPostExecute(songHelper);
                            Common.dialog.dismiss();
                        }
                    }.execute(mApp.getService().getCurrentSong());
                }
        }
    };

    /**
     * Repeat button click listener.
     */
    private View.OnClickListener repeatButtonClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View arg0) {
            if (mApp.isServiceRunning())
                if (mApp.getService().getRepeatMode() == Common.REPEAT_PLAYLIST) {
                    mRepeatButton.setImageResource(R.drawable.ic_repeat_highlight);
                    mApp.getService().setRepeatMode(Common.REPEAT_SONG);
                } else {
                    mRepeatButton.setImageResource(R.drawable.ic_repeat_white);
                    mApp.getService().setRepeatMode(Common.REPEAT_PLAYLIST);
                }
        }
    };
    //Seekbar strobe effect.
    private AlphaAnimation mSeekbarStrobeAnim;
    private VelocityViewPager mViewPager;
    private PlaylistPagerAdapter mViewPagerAdapter;
    private Handler mHandler = new Handler();
    /**
     * Create a new Runnable to update the seekbar and time every 100ms.
     */
    public Runnable seekbarUpdateRunnable = new Runnable() {
        public void run() {
            try {
                if (mApp.isServiceRunning()) {
                    long currentPosition = mApp.getService().getCurrentMediaPlayer().getCurrentPosition();
                    int currentPositionInSecs = (int) currentPosition / 1000;
                    smoothScrollSeekbar(currentPositionInSecs);
                    mHandler.postDelayed(seekbarUpdateRunnable, 100);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };
    private boolean USER_SCROLL = true;
    private NowPlayingActivityListener mNowPlayingActivityListener;
    private boolean mIsCreating = true;

    /**
     * DiscreteSeekBar change listener.
     */
    private DiscreteSeekBar.OnProgressChangeListener seekBarChangeListener = new DiscreteSeekBar.OnProgressChangeListener() {
        @Override
        public void onProgressChanged(DiscreteSeekBar seekBar, int i, boolean changedByUser) {
            try {
                if (mApp.isServiceRunning()) {
                    long currentSongDuration = mApp.getService().getCurrentMediaPlayer().getDuration();
                    seekBar.setMax((int) currentSongDuration / 1000);
                    if (changedByUser) {
                        seekBar.setIndicatorFormatter(mApp.convertMillisToMinsSecs(seekBar.getProgress() * 1000));
                        seekBar.setThumbColor(getResources().getColor(R.color.primary), getResources().getColor(R.color.orange_500));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onStartTrackingTouch(DiscreteSeekBar discreteSeekBar) {
            mHandler.removeCallbacks(seekbarUpdateRunnable);
        }

        @Override
        public void onStopTrackingTouch(DiscreteSeekBar seekBar) {
            int seekBarPosition = seekBar.getProgress();
            mApp.getService().getCurrentMediaPlayer().seekTo(seekBarPosition * 1000);

            mHandler.post(seekbarUpdateRunnable);
        }
    };

    /**
     * Click listener for the play/pause button.
     */
    private View.OnClickListener playPauseClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View view) {
            if (mApp.isServiceRunning()) {
                mApp.getService().togglePlaybackState();
                setPlayPauseButton();
                if (mApp.getService().isPlayingMusic()) {
                    mHandler.removeCallbacks(seekbarUpdateRunnable);
                } else {
                    mHandler.post(seekbarUpdateRunnable);
                }
            }
        }
    };

    /**
     * Click listener for the previous button.
     */
    private View.OnClickListener mOnClickPreviousListener = new View.OnClickListener() {
        @Override
        public void onClick(View arg0) {
            mApp.getService().skipToPreviousTrack();
        }
    };

    /**
     * Click listener for the next button.
     */
    private View.OnClickListener mOnClickNextListener = new View.OnClickListener() {
        @Override
        public void onClick(View arg0) {
            mApp.getService().skipToNextTrack();
        }
    };

    /**
     * Provides callback methods when the ViewPager's position/current page has changed.
     */
    private VelocityViewPager.OnPageChangeListener mPageChangeListener = new VelocityViewPager.OnPageChangeListener() {

        @Override
        public void onPageScrollStateChanged(int scrollState) {
            if (scrollState == VelocityViewPager.SCROLL_STATE_DRAGGING)
                USER_SCROLL = true;
        }

        @Override
        public void onPageScrolled(final int pagerPosition, float swipeVelocity, int offsetFromCurrentPosition) {
            try {
                if (mApp.isServiceRunning() && mApp.getService().getCursor().getCount() > 0) {
                    setMiniPlayerDetails(pagerPosition);
                    if (swipeVelocity == 0.0f && pagerPosition != mApp.getService().getCurrentSongIndex()) {
                        if (USER_SCROLL) {
                            mHandler.removeCallbacks(seekbarUpdateRunnable);
                            smoothScrollSeekbar(0);
                            mHandler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    mApp.getService().skipToTrack(pagerPosition);
                                }
                            }, 200);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onPageSelected(final int pagerPosition) {

        }
    };

    /**
     * Updates this activity's UI elements based on the passed intent's
     * update flag(s).
     */
    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //Grab the bundle from the intent.
            Bundle bundle = intent.getExtras();

            //Initializes the ViewPager.
            if (intent.hasExtra(Common.INIT_PAGER)) {
                slidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
                initViewPager();
            }

            if (intent.hasExtra(Common.NEW_QUEUE_ORDER)) {
                if (slidingUpPanelLayout.getPanelState() == SlidingUpPanelLayout.PanelState.HIDDEN) {
                    slidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
                }
                initViewPager();
            }

            //Updates the ViewPager's current page/position.
            if (intent.hasExtra(Common.UPDATE_PAGER_POSTIION)) {
                int newPosition = Integer.parseInt(intent.getStringExtra(Common.UPDATE_PAGER_POSTIION));
                int currentPosition = mViewPager.getCurrentItem();
                if (currentPosition != newPosition) {
                    if (newPosition > 0 && Math.abs(newPosition - currentPosition) <= 5) {
                        scrollViewPager(newPosition, true, 1, true);
                    } else {
                        mViewPager.setCurrentItem(newPosition, false);
                    }
                    mHandler.post(seekbarUpdateRunnable);
                }

                mApp.getService().getCurrentMediaPlayer().setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
                    @Override
                    public void onBufferingUpdate(MediaPlayer mediaPlayer, int i) {
                        if (i == 100) {
                            mPlayPauseButtonMini.showProgress(false);
                        }
                    }
                });

                if (mApp.getService().getCurrentSong().getSongSourceUri().toString().startsWith("http")) {
                    mPlayPauseButtonMini.showProgress(true);
                } else {
                    mPlayPauseButtonMini.showProgress(false);
                }
            }

            if (intent.hasExtra(Common.SHOW_EXPANDEDVIEW)) {
                slidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
            }

            //Updates the playback control buttons.
            if (intent.hasExtra(Common.UPDATE_PLAYBACK_CONTROLS)) {
                setPlayPauseButton();
                setRepeatButtonIcon();
            }

            //Displays the audibook toast.
            if (intent.hasExtra(Common.SHOW_AUDIOBOOK_TOAST))
                displayAudiobookToast(Long.parseLong(
                        bundle.getString(
                                Common.SHOW_AUDIOBOOK_TOAST)));

            //Updates the duration of the SeekBar.
            if (intent.hasExtra(Common.UPDATE_SEEKBAR_DURATION)) {
                setSeekbarDuration();
            }

            //Close this activity if the service is about to stop running.
            if (intent.hasExtra(Common.SERVICE_STOPPING)) {
                mHandler.removeCallbacks(seekbarUpdateRunnable);
                mViewPagerAdapter.notifyDataSetChanged();
                slidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        mApp = (Common) getApplicationContext();
        super.onCreate(savedInstanceState);
        mContext = this;
        setContentView(R.layout.activity_main);
        Mint.initAndStartSession(MainActivity.this, "b48ec200");
        if (Prefs.getMobileNumber(mContext) != null && !Prefs.getMobileNumber(mContext).equals("")) {
            Mint.setUserIdentifier(Prefs.getMobileNumber(mContext));
        }

        toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);
        Account account = new Account("madbee", Constants.ACCOUNT_TYPE);
        try {
            AccountManager manager = AccountManager.get(this);
            Account[] accounts = manager.getAccountsByType(getPackageName());
            if (accounts != null)
                if (accounts.length == 0) {
                    manager.addAccountExplicitly(account, null, null);
                    ContentResolver.setSyncAutomatically(account, Constants.AUTHORITY, true);
                    ContentResolver.setIsSyncable(account, Constants.AUTHORITY, 1);
                    ContentResolver.addPeriodicSync(account, Constants.AUTHORITY, new Bundle(), 12L * 60L * 60L);
                }
        } catch (Exception e) {
            e.printStackTrace();
        }

        ContentResolver.setMasterSyncAutomatically(true);

        //Init the UI elements.
        expandedView = (RelativeLayout) findViewById(R.id.nowPlayingRootContainer);
        collapseButtonLayout = (RelativeLayout) findViewById(R.id.collapseButtonLayout);
        collapseButton = (ImageView) findViewById(R.id.collapseButton);
        collapseView = (LinearLayout) findViewById(R.id.panel_collapse_view);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.main_activity_drawer_root);
        slidingUpPanelLayout = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout);
        complete = new ArrayList<>();

        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, 0, 0);

        //Apply the drawer toggle to the DrawerLayout.
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        //Check if this is the first time the app is being started.
        if (mApp.getSharedPreferences().getBoolean(Common.FIRST_RUN, true)) {
            showAlbumArtScanningDialog();
            mApp.getSharedPreferences().edit().putBoolean(Common.FIRST_RUN, false).apply();
        }
        pager = (ViewPager) findViewById(R.id.pager);
        tabs = (PagerSlidingTabStrip) findViewById(R.id.tabs);
        final int pageMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, getResources()
                .getDisplayMetrics());
        pager.setPageMargin(pageMargin);
        adapter = new LibraryAdapter(getSupportFragmentManager());
        pager.setAdapter(adapter);
        tabs.setViewPager(pager);
        tabs.setVisibility(View.GONE);
        pager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                try {
                    getSupportActionBar().setTitle(complete.get(position).getTitle());
                    navigationDrawerFragment.onResume();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        collapseButtonLayout.setVisibility(View.GONE);

        slidingUpPanelLayout.setPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() {
            @Override
            public void onPanelSlide(View view, float slideOffset) {
                collapseView.setVisibility(View.VISIBLE);
                collapseButtonLayout.setVisibility(View.VISIBLE);
                collapseButtonLayout.setAlpha(slideOffset);
                collapseView.setAlpha(1 - slideOffset);
            }

            @Override
            public void onPanelCollapsed(View view) {
                mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
                collapseButtonLayout.setVisibility(View.GONE);
                collapseView.setVisibility(View.VISIBLE);
                slidingUpPanelLayout.setTouchEnabled(true);
            }

            @Override
            public void onPanelExpanded(View view) {
                mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
                collapseButtonLayout.setVisibility(View.VISIBLE);
                collapseView.setVisibility(View.GONE);
            }

            @Override
            public void onPanelAnchored(View view) {

            }

            @Override
            public void onPanelHidden(View view) {

            }
        });

        collapseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                slidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
            }
        });

        loadDrawerFragments();

        pager.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    pager.setCurrentItem(position);
                    getSupportActionBar().setTitle(complete.get(position).getTitle());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, 200);
    }

    public int getSelectedPage() {
        return pager.getCurrentItem();
    }

    private String getContactImage(String number) {
        String image = null;
        try {
            // define the columns I want the query to return
            String[] projection = new String[]{
                    ContactsContract.PhoneLookup.PHOTO_THUMBNAIL_URI,
                    ContactsContract.PhoneLookup._ID};

            // encode the phone number and build the filter URI
            Uri contactUri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));

            // query time
            Cursor cursor = getContentResolver().query(contactUri, projection, null, null, null);

            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    image = cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup.PHOTO_THUMBNAIL_URI));
                }
                cursor.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return image;
    }

    private String getContactName(String number) {
        String name = "+" + number;
        try {
            // define the columns I want the query to return
            String[] projection = new String[]{
                    ContactsContract.PhoneLookup.DISPLAY_NAME,
                    ContactsContract.PhoneLookup._ID};

            // encode the phone number and build the filter URI
            Uri contactUri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));

            // query time
            Cursor cursor = getContentResolver().query(contactUri, projection, null, null, null);

            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    name = cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME));
                }
                cursor.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return name;
    }

    public void loadFragment(int index) {
        pager.setCurrentItem(index);
    }

    /**
     * Retrieves the correct fragment based on the saved layout preference.
     */
    private Fragment getLayoutFragment(int fragmentId) {
        //Instantiate a new bundle.
        Fragment fragment;
        Bundle bundle = new Bundle();

        //Retrieve layout preferences for the current fragment.
        if (complete.get(fragmentId).getType().equals("default")) {
            switch (fragmentId) {
                case 0:
                    mCurrentFragmentLayout = LIST_LAYOUT;
                    bundle.putInt(Common.FRAGMENT_ID, Common.SONGS_FRAGMENT);
                    bundle.putString(FRAGMENT_HEADER, mContext.getResources().getString(R.string.songs));
                    break;
                case 1:
                    mCurrentFragmentLayout = LIST_LAYOUT;
                    bundle.putInt(Common.FRAGMENT_ID, Common.TOP_25_PLAYED_FRAGMENT);
                    bundle.putString(FRAGMENT_HEADER, mContext.getResources().getString(R.string.the_top_25_played_tracks));
                    break;
                case 2:
                    mCurrentFragmentLayout = LIST_LAYOUT;
                    bundle.putInt(Common.FRAGMENT_ID, Common.RECENTLY_PLAYED_FRAGMENT);
                    bundle.putString(FRAGMENT_HEADER, mContext.getResources().getString(R.string.the_most_recently_played_songs));
                    break;

            }
            fragment = new ListViewFragment();
            fragment.setArguments(bundle);
        } else if (complete.get(fragmentId).getType().equals("friend")) {
            mCurrentFragmentLayout = FRIENDS_LAYOUT;
            bundle.putString(Common.CONTACT_NUMBER, complete.get(fragmentId).getNumber());
            fragment = new FriendsListViewFragment();
            fragment.setArguments(bundle);
        } else {
            mCurrentFragmentLayout = PLAYLIST_LAYOUT;
            bundle.putString(DBAccessHelper.PLAYLIST_NAME, complete.get(fragmentId).getTitle());
            fragment = new PlaylistViewFragment();
            fragment.setArguments(bundle);
        }
        return fragment;
    }

    /**
     * Loads the drawer fragments.
     */
    public void loadDrawerFragments() {
        complete.clear();
        names = mApp.getDBAccessHelper().getPlaylistNames();
        if (names.contains("Trash")) {
            names.remove("Trash");
        }

        complete.add(new DrawerHelper("All Songs", "default", "0", String.valueOf(R.drawable.ic_songs)));
        complete.add(new DrawerHelper("Most Played", "default", "0", String.valueOf(R.drawable.ic_most_played)));
        complete.add(new DrawerHelper("Recently Played", "default", "0", String.valueOf(R.drawable.ic_recently_played)));
        Cursor friendNumber = mApp.getDBAccessHelper().getDistinctFriends();
        if (friendNumber != null && friendNumber.getCount() > 0) {
            if (mApp.getDBAccessHelper().getFriendSongs("likes") != null && mApp.getDBAccessHelper().getFriendSongs("likes").getCount() > 0) {
                position = 4;
                complete.add(new DrawerHelper("Likes", "friend", "likes", String.valueOf(R.drawable.ic_like)));
            } else {
                position = 3;
            }
            complete.add(new DrawerHelper("Suggestions", "friend", "trending", String.valueOf(R.drawable.ic_trending)));
        }
        if (mApp.getDBAccessHelper().getFriendSongs("toptrending") != null && mApp.getDBAccessHelper().getFriendSongs("toptrending").getCount() > 0) {
            complete.add(new DrawerHelper("Top Trending", "friend", "toptrending", String.valueOf(R.drawable.ic_trending)));
        }

        if (names != null) {
            for (String name : names) {
                complete.add(new DrawerHelper(name, "playlist", "0", String.valueOf(R.drawable.ic_playlist)));
            }
        }

        if (friendNumber != null && friendNumber.getCount() > 0) {
            if (friendNumber.moveToFirst()) {
                do {
                    String number = friendNumber.getString(friendNumber.getColumnIndex(DBAccessHelper.CONTACT_NUMBER));
                    if (!number.equals("likes") && !number.equals("toptrending")) {
                        complete.add(new DrawerHelper(getContactName(number), "friend", number, getContactImage(number)));
                    }
                } while (friendNumber.moveToNext());
            }
        }

        complete.add(new DrawerHelper("Send Invites", "playlist", "0", String.valueOf(R.drawable.ic_invite)));
        complete.add(new DrawerHelper("Trash", "playlist", "0", String.valueOf(R.drawable.ic_trash)));

        adapter.notifyDataSetChanged();
        navigationDrawerFragment = new NavigationDrawerFragment(complete);
        //Load the navigation drawer.
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.nav_drawer_container, navigationDrawerFragment)
                .commit();

        //Load the Sliding Up Panel.
        initExpandedView();
    }

    /**
     * Displays the message dialog for album art processing.
     */
    private void showAlbumArtScanningDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.album_art);
        builder.setCancelable(true);
        builder.setMessage(R.string.scanning_for_album_art_details);
        builder.setPositiveButton(R.string.got_it, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }

        });
        builder.create().show();
    }

    private void showMainActivityActionItems(Menu menu) {
        try {
            //Inflate the menu.
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.main_activity, menu);

            //Set the ActionBar background
            getSupportActionBar().setDisplayShowTitleEnabled(true);
            getSupportActionBar().setDisplayUseLogoEnabled(false);
            getSupportActionBar().setHomeButtonEnabled(true);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);

            //Set the ActionBar text color.
            int actionBarTitleId = Resources.getSystem().getIdentifier("action_bar_title", "id", "android");
            if (actionBarTitleId > 0) {
                TextView title = (TextView) findViewById(actionBarTitleId);
                if (title != null) {
                    title.setTextColor(0xFFFFFFFF);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Initializes the ActionBar.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.clear();
        showMainActivityActionItems(menu);
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * ActionBar item selection listener.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        switch (item.getItemId()) {
            case R.id.action_search:
                Intent i = new Intent(MainActivity.this, SearchActivity.class);
                startActivityForResult(i, 1);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
        Bundle bundle = data.getExtras();
        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                if (!bundle.isEmpty())
                    if (bundle.getBoolean("loadDrawer", false)) {
                        loadDrawerFragments();
                    }
            }
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    public int getListPreferredItemHeight() {
        final TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(
                android.R.attr.listPreferredItemHeight, typedValue, true);
        final DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        return (int) typedValue.getDimension(metrics);
    }

    @Override
    public void onBackPressed() {
        if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) { // Close left drawer if opened
            mDrawerLayout.closeDrawer(GravityCompat.START);
        } else if (slidingUpPanelLayout.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED) {
            slidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
        } else if (pager.getCurrentItem() != 3) {
            pager.setCurrentItem(3);
        } else {
            super.onBackPressed();
        }
    }

    /**
     * Helper method that checks whether the audio playback service
     * is running or not.
     */
    private void checkServiceRunning() {
        if (mApp.isServiceRunning()) {
            if (mApp.getService().getCursor() != null) {
                if (mApp.getService().getCursor().getCount() > 0) {
                    slidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
                    setMiniPlayerDetails(mApp.getService().getCursor().getPosition());
                    setPlayPauseButton();
                    initViewPager();
                } else {
                    showEmptyMiniPlayer();
                }
            } else {
                showEmptyMiniPlayer();
            }
        } else {
            showEmptyMiniPlayer();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(mContext)
                .registerReceiver((mReceiver), new IntentFilter(Common.UPDATE_UI_BROADCAST));
        if (mApp.isServiceRunning() && mApp.getService().getCursor() != null) {
            String[] updateFlags = new String[]{Common.UPDATE_PAGER_POSTIION,
                    Common.UPDATE_SEEKBAR_DURATION,
                    Common.INIT_PAGER,
                    Common.UPDATE_PLAYBACK_CONTROLS};

            String[] flagValues = new String[]{"" + mApp.getService().getCurrentSongIndex(),
                    "" + mApp.getService().getCurrentMediaPlayer().getDuration(),
                    "", ""};
            mApp.broadcastUpdateUICommand(updateFlags, flagValues);
        }
    }

    @Override
    public void onStop() {
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(mReceiver);
        super.onStop();
    }

    /**
     * Sets the play/pause button states.
     */
    private void setPlayPauseButton() {
        if (mApp.isServiceRunning()) {
            if (mApp.getService().isPlayingMusic()) {
                mPlayPauseButton.setImageResource(R.drawable.ic_pause_circle_outline_white);
                mPlayPauseButtonMini.setIcon(R.drawable.pause_light, R.drawable.pause_light);
                stopSeekbarStrobeEffect();
            } else {
                mPlayPauseButton.setImageResource(R.drawable.ic_play_circle_outline_white);
                mPlayPauseButtonMini.setIcon(R.drawable.play_light, R.drawable.play_light);
                initSeekbarStrobeEffect();
            }
        }
    }

    /**
     * Called if the audio playback service is not running.
     */
    public void showEmptyMiniPlayer() {
        slidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);
    }

    void initExpandedView() {
        setNowPlayingActivityListener(mNowPlayingActivityListener);
        this.setVolumeControlStream(AudioManager.STREAM_MUSIC);
        mViewPager = (VelocityViewPager) findViewById(R.id.nowPlayingPlaylistPager);
        mMiniSongName = (TextView) findViewById(R.id.songNameMini);
        mMiniArtistAlbumName = (TextView) findViewById(R.id.artistAlbumNameMini);
        mMiniSongName.setSelected(true);
        mMiniArtistAlbumName.setSelected(true);
        mMiniSongName.setTypeface(TypefaceHelper.getTypeface(mContext, "calibri"));
        mMiniArtistAlbumName.setTypeface(TypefaceHelper.getTypeface(mContext, "calibri"));
        //Playback Controls.
        RelativeLayout mPlayPauseButtonBackground = (RelativeLayout) findViewById(R.id.playPauseButtonBackground);
        mPlayPauseButton = (ImageButton) findViewById(R.id.playPauseButton);
        mPlayPauseButtonMini = (FabButton) findViewById(R.id.playPauseButtonMini);
        ImageButton mNextButton = (ImageButton) findViewById(R.id.nextButton);
        ImageButton mPreviousButton = (ImageButton) findViewById(R.id.previousButton);
        mABRepeatButton = (ImageButton) findViewById(R.id.mABRepeatButton);
        mRepeatButton = (ImageButton) findViewById(R.id.repeatButton);
        mLikeButton = (ImageView) findViewById(R.id.likeButton);

        //Song info/seekbar elements.
        mSeekbar = (DiscreteSeekBar) findViewById(R.id.nowPlayingSeekBar);
        mPlayPauseButton.setImageResource(R.drawable.ic_pause_circle_outline_white);
        mPlayPauseButtonMini.setIcon(R.drawable.pause_light, R.drawable.pause_light);
        mNextButton.setImageResource(R.drawable.ic_skip_next_white);
        mPreviousButton.setImageResource(R.drawable.ic_skip_previous_white);

        //Set the click listeners.
        mSeekbar.setOnProgressChangeListener(seekBarChangeListener);
        mNextButton.setOnClickListener(mOnClickNextListener);
        mPreviousButton.setOnClickListener(mOnClickPreviousListener);
        mPlayPauseButton.setOnClickListener(playPauseClickListener);
        mPlayPauseButtonMini.setOnClickListener(playPauseClickListener);
        mPlayPauseButtonBackground.setOnClickListener(playPauseClickListener);
        mABRepeatButton.setOnClickListener(mABRepeatButtonClickListener);
        mRepeatButton.setOnClickListener(repeatButtonClickListener);
        mLikeButton.setOnClickListener(likeButtonClickListener);

        //Apply haptic feedback to the play/pause button.
        mPlayPauseButtonBackground.setHapticFeedbackEnabled(true);
        mPlayPauseButton.setHapticFeedbackEnabled(true);
        mPlayPauseButtonMini.setHapticFeedbackEnabled(true);

        //Set the control buttons and background.
        setPlayPauseButton();
        setRepeatButtonIcon();
    }

    /**
     * Initializes the view pager.
     */
    private void initViewPager() {
        try {
            mViewPagerAdapter = new PlaylistPagerAdapter(getSupportFragmentManager());
            mViewPager.setAdapter(mViewPagerAdapter);
            mViewPager.setOffscreenPageLimit(1);
            mViewPager.setOnPageChangeListener(mPageChangeListener);
            mViewPager.setCurrentItem(mApp.getService().getCurrentSongIndex(), false);
            FadeAnimation fadeAnimation = new FadeAnimation(mViewPager, 600, 0.0f,
                    1.0f, new DecelerateInterpolator(2.0f));
            fadeAnimation.animate();
            mViewPager.setOffscreenPageLimit(10);
            mApp.getService().getCurrentMediaPlayer().setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
                @Override
                public void onBufferingUpdate(MediaPlayer mediaPlayer, int i) {
                    if (i == 100) {
                        mPlayPauseButtonMini.showProgress(false);
                    }
                }
            });
            if (mApp.getService().getCurrentSong().getSongSourceUri().toString().startsWith("http")) {
                mPlayPauseButtonMini.showProgress(true);
            } else {
                mPlayPauseButtonMini.showProgress(false);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Scrolls the ViewPager programmatically. If dispatchToListener
     * is true, USER_SCROLL will be set to true.
     */
    private void scrollViewPager(int newPosition,
                                 boolean smoothScroll,
                                 int velocity,
                                 boolean dispatchToListener) {

        USER_SCROLL = dispatchToListener;
        mViewPager.scrollToItem(newPosition,
                smoothScroll,
                velocity,
                dispatchToListener);
    }

    /**
     * Sets the repeat button icon based on the current repeat mode.
     */
    private void setRepeatButtonIcon() {
        if (mApp.isServiceRunning()) {
            if (mApp.getService().getRepeatMode() == Common.REPEAT_PLAYLIST) {
                mRepeatButton.setImageResource(R.drawable.ic_repeat_white);
            } else if (mApp.getService().getRepeatMode() == Common.REPEAT_SONG) {
                mRepeatButton.setImageResource(R.drawable.ic_repeat_highlight);
            } else
                mRepeatButton.setImageResource(R.drawable.ic_repeat_white);
            if (mApp.getService().getRepeatMode() == Common.A_B_REPEAT) {
                mABRepeatButton.setImageResource(R.drawable.ic_ab_repeat_highlight);
            } else {
                mABRepeatButton.setImageResource(R.drawable.ic_ab_repeat);
            }
        }
    }

    /**
     * Sets the seekbar's duration. Also updates the
     * elapsed/remaining duration text.
     */

    private void setSeekbarDuration() {
        mSeekbar.setProgress(mApp.getService().getCurrentMediaPlayer().getCurrentPosition() / 1000);
        mHandler.postDelayed(seekbarUpdateRunnable, 100);
    }

    /**
     * Smoothly scrolls the seekbar to the indicated position.
     */
    private void smoothScrollSeekbar(int progress) {
        ObjectAnimator animation = ObjectAnimator.ofInt(mSeekbar, "progress", progress);
        animation.setDuration(200);
        animation.setInterpolator(new DecelerateInterpolator());
        animation.start();
    }

    /**
     * Initiates the strobe effect on the seekbar.
     */
    private void initSeekbarStrobeEffect() {
        mSeekbarStrobeAnim = new AlphaAnimation(1.0f, 0.0f);
        mSeekbarStrobeAnim.setRepeatCount(SEEKBAR_STROBE_ANIM_REPEAT);
        mSeekbarStrobeAnim.setDuration(700);
        mSeekbarStrobeAnim.setRepeatMode(Animation.REVERSE);
        mSeekbar.startAnimation(mSeekbarStrobeAnim);
    }

    /**
     * Stops the seekbar strobe effect.
     */
    private void stopSeekbarStrobeEffect() {
        mSeekbarStrobeAnim = new AlphaAnimation(mSeekbar.getAlpha(), 1.0f);
        mSeekbarStrobeAnim.setDuration(700);
        mSeekbar.startAnimation(mSeekbarStrobeAnim);
    }

    /**
     * Displays the "Resuming from xx:xx" toast.
     */
    public void displayAudiobookToast(long resumePlaybackPosition) {
        try {
            String resumingFrom = mContext.getResources().getString(R.string.resuming_from)
                    + " " + mApp.convertMillisToMinsSecs(resumePlaybackPosition) + ".";

            Toast.makeText(mContext, resumingFrom, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public NowPlayingActivityListener getNowPlayingActivityListener() {
        return mNowPlayingActivityListener;
    }

    public void setNowPlayingActivityListener(NowPlayingActivityListener listener) {
        mNowPlayingActivityListener = listener;
    }

    @Override
    public void onResume() {
        super.onResume();

        setPlayPauseButton();
        if (!mIsCreating) {
            mHandler.postDelayed(seekbarUpdateRunnable, 100);
            mIsCreating = false;
        }

        //Update the seekbar.
        if (mApp.isServiceRunning()) {
            try {
                setSeekbarDuration();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (getIntent().hasExtra(START_SERVICE) &&
                getNowPlayingActivityListener() != null) {
            getNowPlayingActivityListener().onNowPlayingActivityReady();
            getIntent().removeExtra(START_SERVICE);
        }
        if (getIntent().hasExtra(Common.SHOW_EXPANDEDVIEW)) {
            slidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
            getIntent().removeExtra(Common.SHOW_EXPANDEDVIEW);
        }
        checkServiceRunning();
        loadDrawerFragments();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean("CALLED_FROM_FOOTER", true);
        savedInstanceState.putBoolean("CALLED_FROM_NOTIF", true);
    }

    void setMiniPlayerDetails(int pagerPosition) {
        SongHelper mSongHelper = new SongHelper();

        mSongHelper.populateSongData(mContext, pagerPosition, new PicassoMirrorReflectionTransformer());
                /* Change tracks ONLY when the user has finished the swiping gesture (swipeVelocity will be zero).
                 * Also, don't skip tracks if the new pager position is the same as the current mCursor position (indicates
				 * that the starting and ending position of the pager is the same).
				 */
        mMiniSongName.setText(mSongHelper.getTitle());
        mMiniArtistAlbumName.setText(mSongHelper.getArtist() + " - " + mSongHelper.getAlbum());
    }

    public String likeSong(SongHelper songHelper) {
        String return_res;
        String songId = "";
        String artUrl = "";
        String source = songHelper.getSource();
        if (source != null) {
            if (source.equals(DBAccessHelper.SOUNDCLOUD)) {
                songId = songHelper.getId();
                artUrl = songHelper.getSoundCloudAlbumArtPath();
            }
        }
        if (artUrl == null) {
            artUrl = "";
        }
        long lastModified = Long.parseLong(songHelper.getmSongLastModified());
        long lastPlayed = Long.parseLong(songHelper.getmSongLastPlayed());
        Date lastModifiedDate = new Date(lastModified);
        Date lastPlayedDate = new Date(lastPlayed);
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

        //printing value of Date
        Uri.Builder builder = new Uri.Builder();
        builder.scheme("http")
                .authority("api.madbeeapp.com")
                .appendPath("api")
                .appendPath("saveplaylist")
                .appendQueryParameter("countryCode", Prefs.getCountryCode(mContext))
                .appendQueryParameter("numbers", Prefs.getMobileNumber(mContext))
                .appendQueryParameter("cloud_id", songId)
                .appendQueryParameter("title", songHelper.getTitle().replaceAll("[^a-zA-Z0-9\\s\\-\\(\\)\\[]", ""))
                .appendQueryParameter("album", songHelper.getAlbum().replaceAll("[^a-zA-Z0-9\\s\\-\\(\\)\\[]", ""))
                .appendQueryParameter("artist", songHelper.getArtist().replaceAll("[^a-zA-Z0-9\\s\\-\\(\\)\\[]", ""))
                .appendQueryParameter("genre", songHelper.getGenre().replaceAll("[^a-zA-Z0-9\\s\\-\\(\\)\\[]", ""))
                .appendQueryParameter("duration", songHelper.getDuration())
                .appendQueryParameter("play_count", songHelper.getPlayCount())
                .appendQueryParameter("last_played", df.format(lastPlayedDate))
                .appendQueryParameter("last_modified", df.format(lastModifiedDate))
                .appendQueryParameter("has_like", "1");

        if (artUrl != null && !artUrl.equals("null")) {
            builder.appendQueryParameter("art_url", artUrl);
        } else {
            builder.appendQueryParameter("art_url", "");
        }

        String url = builder.build().toString();
        return_res = getResponse(url);

        if (return_res != null) {
            String phoneNumber = "likes";
            String songFilePath = mContext.getFilesDir().getPath() + "/music/" + songId + ".mp3";
            if (!mApp.getDBAccessHelper().checkIfSongIsPresentInFriendList(phoneNumber, songFilePath)) {
                String songAlbumArtPath = "file:" + mContext.getFilesDir().getPath() + "/album_art/" + songId + ".jpg";

                ContentValues values = new ContentValues();
                values.put(DBAccessHelper.CONTACT_NUMBER, phoneNumber);
                values.put(DBAccessHelper.SONG_ID, songId);
                values.put(DBAccessHelper.SONG_TITLE, songHelper.getTitle());
                values.put(DBAccessHelper.SONG_ALBUM, songHelper.getAlbum());
                values.put(DBAccessHelper.SONG_ARTIST, songHelper.getArtist());
                values.put(DBAccessHelper.SONG_DURATION, songHelper.getDuration());
                values.put(DBAccessHelper.SONG_GENRE, songHelper.getGenre());
                values.put(DBAccessHelper.SONG_YEAR, "2015");
                values.put(DBAccessHelper.SONG_PLAY_COUNT, songHelper.getPlayCount());
                values.put(DBAccessHelper.SONG_SOUNDCLOUD_ALBUM_ART_PATH, songHelper.getSoundCloudAlbumArtPath());
                values.put(DBAccessHelper.SONG_FILE_PATH, songFilePath);
                values.put(DBAccessHelper.SONG_TRACK_NUMBER, "11");
                values.put(DBAccessHelper.SONG_ALBUM_ART_PATH, songAlbumArtPath);
                values.put(DBAccessHelper.SONG_SOURCE, DBAccessHelper.SOUNDCLOUD);
                values.put(DBAccessHelper.ADDED_TIMESTAMP, songHelper.getmSongLastModified());
                values.put(DBAccessHelper.SONG_LAST_MODIFIED, songHelper.getmSongLastModified());
                values.put(DBAccessHelper.LAST_PLAYED_TIMESTAMP, songHelper.getmSongLastPlayed());

                //Add all the entries to the database to build the songs library.
                mApp.getDBAccessHelper().getWritableDatabase().insertWithOnConflict(DBAccessHelper.FRIENDS_LIBRARY_TABLE,
                        null, values, SQLiteDatabase.CONFLICT_REPLACE);
            }
        }
        return return_res;
    }

    String getResponse(String url) {
        try {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(url)
                    .build();
            return client.newCall(request).execute().body().string();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Interface that provides callbacks once this activity is
     * up and running.
     */
    public interface NowPlayingActivityListener {

        /**
         * Called once this activity's onResume() method finishes
         * executing.
         */
        void onNowPlayingActivityReady();
    }

    public class LibraryAdapter extends FragmentStatePagerAdapter {

        public LibraryAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return complete.get(position).getTitle();
        }

        @Override
        public int getCount() {
            return complete.size();
        }

        @Override
        public Fragment getItem(int position) {
            return getLayoutFragment(position);
        }
    }

    public class PlaylistPagerAdapter extends FragmentStatePagerAdapter {

        public PlaylistPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            super.destroyItem(container, position, object);
        }

        //This method controls the layout that is shown on each screen.
        @Override
        public Fragment getItem(int position) {

        	/* PlaylistPagerFragment.java will be shown on every pager screen. However,
             * the fragment will check which screen (position) is being shown, and will
        	 * update its TextViews and ImageViews to match the song that's being played. */
            Fragment fragment;
            fragment = new PlaylistPagerFragment();
            Bundle bundle = new Bundle();
            bundle.putInt("POSITION", position);
            fragment.setArguments(bundle);
            return fragment;
        }

        @Override
        public int getCount() {
            try {
                if (mApp.isServiceRunning()) {
                    if (mApp.getService().getPlaybackIndecesList() != null) {
                        return mApp.getService().getPlaybackIndecesList().size();
                    } else {
                        mApp.getService().stopSelf();
                        return 0;
                    }
                } else {
                    return 0;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return 0;
            }
        }
    }
}