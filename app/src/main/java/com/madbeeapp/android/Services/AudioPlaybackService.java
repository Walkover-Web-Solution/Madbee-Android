package com.madbeeapp.android.Services;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.MergeCursor;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.RemoteControlClient;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.madbeeapp.android.BroadcastReceivers.HeadsetButtonsReceiver;
import com.madbeeapp.android.BroadcastReceivers.HeadsetPlugBroadcastReceiver;
import com.madbeeapp.android.DBHelpers.DBAccessHelper;
import com.madbeeapp.android.Helpers.AudioManagerHelper;
import com.madbeeapp.android.Helpers.SongHelper;
import com.madbeeapp.android.PlaybackKickstarter.PlaybackKickstarter.BuildCursorListener;
import com.madbeeapp.android.R;
import com.madbeeapp.android.RemoteControlClient.RemoteControlClientCompat;
import com.madbeeapp.android.RemoteControlClient.RemoteControlHelper;
import com.madbeeapp.android.Utils.Common;
import com.madbeeapp.android.Utils.Prefs;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Random;

/**
 * The meat and potatoes of the entire app. Manages
 * playback, equalizer effects, and all other audio
 * related operations.
 *
 * @author Arpit Gandhi
 */
@SuppressWarnings("ResultOfMethodCallIgnored")
public class AudioPlaybackService extends Service {

    public static final int mNotificationId = 1080; //NOTE: Using 0 as a notification ID causes Android to ignore the notification call.
    //Custom actions for media player controls via the notification bar.
    public static final String LAUNCH_NOW_PLAYING_ACTION = "com.madbeeapp.android.LAUNCH_NOW_PLAYING_ACTION";
    public static final String PREVIOUS_ACTION = "com.madbeeapp.android.PREVIOUS_ACTION";
    public static final String PLAY_PAUSE_ACTION = "com.madbeeapp.android.PLAY_PAUSE_ACTION";
    public static final String NEXT_ACTION = "com.madbeeapp.android.NEXT_ACTION";
    public static final String STOP_SERVICE = "com.madbeeapp.android.STOP_SERVICE";

    /**
     * Error listener for mMediaPlayer.
     */
    public OnErrorListener onErrorListener = new OnErrorListener() {

        @Override
        public boolean onError(MediaPlayer mMediaPlayer, int what, int extra) {
            /* This error listener might seem like it's not doing anything.
             * However, removing this will cause the mMediaPlayer object to go crazy
			 * and skip around. The key here is to make this method return true. This
			 * notifies the mMediaPlayer object that we've handled all errors and that
			 * it shouldn't do anything else to try and remedy the situation.
			 *
			 * TL;DR: Don't touch this interface. Ever.
			 */
            return true;
        }

    };
    //Context and Intent.
    private Context mContext;
    private Service mService;
    //Global Objects Provider.
    private Common mApp;
    //PrepareServiceListener instance.
    private PrepareServiceListener mPrepareServiceListener;
    //MediaPlayer objects and flags.
    private MediaPlayer mMediaPlayer;
    private MediaPlayer mMediaPlayer2;
    private int mCurrentMediaPlayer = 1;
    private boolean mFirstRun = true;
    //AudioManager.
    private AudioManager mAudioManager;
    private AudioManagerHelper mAudioManagerHelper;
    //Flags that indicate whether the mediaPlayers have been initialized.
    private boolean mMediaPlayerPrepared = false;
    private boolean mMediaPlayer2Prepared = false;
    //Cursor object(s) that will guide the rest of this queue.
    private Cursor mCursor;
    //Holds the indeces of the current cursor, in the order that they'll be played.
    private ArrayList<Integer> mPlaybackIndecesList = new ArrayList<>();
    //Holds the indeces of songs that were unplayable.
    private ArrayList<Integer> mFailedIndecesList = new ArrayList<>();
    //Song data helpers for each MediaPlayer object.
    private SongHelper mMediaPlayerSongHelper;
    private SongHelper mMediaPlayer2SongHelper;
    //Pointer variable.
    private int mCurrentSongIndex;
    //Notification elements.
    private NotificationCompat.Builder mNotificationBuilder;
    /**
     * Interface implementation to listen for service cursor events.
     */
    public BuildCursorListener buildCursorListener = new BuildCursorListener() {

        @Override
        public void onServiceCursorReady(Cursor cursor, int currentSongIndex, boolean playAll) {

            if (cursor.getCount() == 0) {
                return;
            }

            setCursor(cursor);
            setCurrentSongIndex(currentSongIndex);
            getFailedIndecesList().clear();
            initPlaybackIndecesList(playAll);
            mFirstRun = true;
            prepareMediaPlayer(currentSongIndex);

            //Notify NowPlayingActivity to initialize its ViewPager.
            mApp.broadcastUpdateUICommand(new String[]{Common.INIT_PAGER},
                    new String[]{""});
        }

        @Override
        public void onServiceCursorFailed(String exceptionMessage) {
            //We don't have a valid cursor, so stop the service.
            stopSelf();
        }
    };
    //Handler object.
    private Handler mHandler;
    //Volume variables that handle the crossfade effect.
    private float mFadeOutVolume = 1.0f;
    private float mFadeInVolume = 0.0f;
    //Headset plug receiver.
    private HeadsetPlugBroadcastReceiver mHeadsetPlugReceiver;
    //Crossfade.
    private int mCrossfadeDuration;
    /**
     * First runnable that handles the cross fade operation between two tracks.
     */
    public Runnable startCrossFadeRunnable = new Runnable() {

        @Override
        public void run() {
            //Check if we're in the last part of the current song.
            try {
                if (getCurrentMediaPlayer().isPlaying()) {
                    int currentTrackDuration = getCurrentMediaPlayer().getDuration();
                    int currentTrackFadePosition = currentTrackDuration - (mCrossfadeDuration * 1000);
                    if (getCurrentMediaPlayer().getCurrentPosition() >= currentTrackFadePosition) {
                        //Launch the next runnable that will handle the cross fade effect.
                        mHandler.postDelayed(crossFadeRunnable, 100);
                    } else {
                        mHandler.postDelayed(startCrossFadeRunnable, 1000);
                    }
                } else {
                    mHandler.postDelayed(startCrossFadeRunnable, 1000);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    };
    //A-B Repeat variables.
    private int mRepeatSongRangePointA = 0;
    private int mRepeatSongRangePointB = 0;
    //Indicates if the user changed the track manually.
    //RemoteControlClient for use with remote controls and ICS+ lockscreen controls.
    private RemoteControlClientCompat mRemoteControlClientCompat;
    private ComponentName mMediaButtonReceiverComponent;
    /**
     * Fades out volume before a duck operation.
     */
    private Runnable duckDownVolumeRunnable = new Runnable() {

        @Override
        public void run() {
            if (mAudioManagerHelper.getCurrentVolume() > mAudioManagerHelper.getTargetVolume()) {
                mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
                        (mAudioManagerHelper.getCurrentVolume() - mAudioManagerHelper.getStepDownIncrement()),
                        0);

                mAudioManagerHelper.setCurrentVolume(mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
                mHandler.postDelayed(this, 50);
            }

        }

    };
    /**
     * Fades in volume after a duck operation.
     */
    private Runnable duckUpVolumeRunnable = new Runnable() {

        @Override
        public void run() {
            if (mAudioManagerHelper.getCurrentVolume() < mAudioManagerHelper.getTargetVolume()) {
                mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
                        (mAudioManagerHelper.getCurrentVolume() + mAudioManagerHelper.getStepUpIncrement()),
                        0);

                mAudioManagerHelper.setCurrentVolume(mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
                mHandler.postDelayed(this, 50);
            }
        }

    };
    /**
     * Starts mMediaPlayer if it is prepared and ready for playback.
     * Otherwise, continues checking every 100ms if mMediaPlayer is prepared.
     */
    private Runnable startMediaPlayerIfPrepared = new Runnable() {

        @Override
        public void run() {
            if (isMediaPlayerPrepared())
                startMediaPlayer();
            else
                mHandler.postDelayed(this, 100);

        }

    };
    /**
     * Starts mMediaPlayer if it is prepared and ready for playback.
     * Otherwise, continues checking every 100ms if mMediaPlayer2 is prepared.
     */
    private Runnable startMediaPlayer2IfPrepared = new Runnable() {

        @Override
        public void run() {
            if (isMediaPlayer2Prepared())
                startMediaPlayer2();
            else
                mHandler.postDelayed(this, 100);
        }

    };
    /**
     * Completion listener for mMediaPlayer.
     */
    private OnCompletionListener onMediaPlayerCompleted = new OnCompletionListener() {

        @Override
        public void onCompletion(MediaPlayer mp) {

            //Remove the crossfade playback.
            mHandler.removeCallbacks(startCrossFadeRunnable);
            mHandler.removeCallbacks(crossFadeRunnable);

            //Set the track position handler (notifies the handler when the track should start being faded).
            if (mHandler != null && mApp.isCrossfadeEnabled()) {
                mHandler.post(startCrossFadeRunnable);
            }

            //Reset the fadeVolume variables.
            mFadeInVolume = 0.0f;
            mFadeOutVolume = 1.0f;

            //Reset the volumes for both mediaPlayers.
            getMediaPlayer().setVolume(1.0f, 1.0f);
            getMediaPlayer2().setVolume(1.0f, 1.0f);

            try {
                if (isAtEndOfQueue() && getRepeatMode() != Common.REPEAT_PLAYLIST) {
                    stopSelf();
                } else if (isMediaPlayer2Prepared()) {
                    startMediaPlayer2();
                } else {
                    //Check every 100ms if mMediaPlayer2 is prepared.
                    mHandler.post(startMediaPlayer2IfPrepared);
                }
            } catch (IllegalStateException e) {
                mHandler.post(startMediaPlayer2IfPrepared);
            }
        }

    };
    /**
     * Completion listener for mMediaPlayer2.
     */
    private OnCompletionListener onMediaPlayer2Completed = new OnCompletionListener() {

        @Override
        public void onCompletion(MediaPlayer mp) {

            //Remove the crossfade playback.
            mHandler.removeCallbacks(startCrossFadeRunnable);
            mHandler.removeCallbacks(crossFadeRunnable);

            //Set the track position handler (notifies the handler when the track should start being faded).
            if (mHandler != null && mApp.isCrossfadeEnabled()) {
                mHandler.post(startCrossFadeRunnable);
            }

            //Reset the fadeVolume variables.
            mFadeInVolume = 0.0f;
            mFadeOutVolume = 1.0f;

            //Reset the volumes for both mediaPlayers.
            getMediaPlayer().setVolume(1.0f, 1.0f);
            getMediaPlayer2().setVolume(1.0f, 1.0f);

            try {
                if (isAtEndOfQueue() && getRepeatMode() != Common.REPEAT_PLAYLIST) {
                    stopSelf();
                } else if (isMediaPlayerPrepared()) {
                    startMediaPlayer();
                } else {
                    //Check every 100ms if mMediaPlayer is prepared.
                    mHandler.post(startMediaPlayerIfPrepared);
                }

            } catch (IllegalStateException e) {
                //mMediaPlayer isn't prepared yet.
                mHandler.post(startMediaPlayerIfPrepared);
            }

        }

    };
    /**
     * Called once mMediaPlayer2 is prepared.
     */
    public OnPreparedListener mediaPlayer2Prepared = new OnPreparedListener() {

        @Override
        public void onPrepared(MediaPlayer mediaPlayer) {

            //Update the prepared flag.
            setIsMediaPlayer2Prepared(true);

            //Set the completion listener for mMediaPlayer2.
            getMediaPlayer2().setOnCompletionListener(onMediaPlayer2Completed);
        }

    };
    /**
     * Called repetitively to check for A-B repeat markers.
     */
    private Runnable checkABRepeatRange = new Runnable() {

        @Override
        public void run() {
            try {
                if (getCurrentMediaPlayer().isPlaying()) {
                    if (getCurrentMediaPlayer().getCurrentPosition() >= (mRepeatSongRangePointB)) {
                        getCurrentMediaPlayer().seekTo(mRepeatSongRangePointA);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (mApp.getSharedPreferences().getInt(Common.REPEAT_MODE, Common.REPEAT_PLAYLIST) == Common.A_B_REPEAT) {
                mHandler.postDelayed(checkABRepeatRange, 100);
            }
        }
    };
    /**
     * Listens for audio focus changes and reacts accordingly.
     */
    private OnAudioFocusChangeListener audioFocusChangeListener = new OnAudioFocusChangeListener() {

        @Override
        public void onAudioFocusChange(int focusChange) {
            if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                //We've temporarily lost focus, so pause the mMediaPlayer, wherever it's at.
                try {
                    getCurrentMediaPlayer().pause();
                    updateNotification(mApp.getService().getCurrentSong());
                    mAudioManagerHelper.setHasAudioFocus(false);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
                //Lower the current mMediaPlayer volume.
                mAudioManagerHelper.setAudioDucked(true);
                mAudioManagerHelper.setTargetVolume(5);
                mAudioManagerHelper.setStepDownIncrement(1);
                mAudioManagerHelper.setCurrentVolume(mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
                mAudioManagerHelper.setOriginalVolume(mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
                mHandler.post(duckDownVolumeRunnable);

            } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {

                if (mAudioManagerHelper.isAudioDucked()) {
                    //Crank the volume back up again.
                    mAudioManagerHelper.setTargetVolume(mAudioManagerHelper.getOriginalVolume());
                    mAudioManagerHelper.setStepUpIncrement(1);
                    mAudioManagerHelper.setCurrentVolume(mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC));

                    mHandler.post(duckUpVolumeRunnable);
                    mAudioManagerHelper.setAudioDucked(false);
                } else {
                    //We've regained focus. Update the audioFocus tag, but don't start the mMediaPlayer.
                    mAudioManagerHelper.setHasAudioFocus(true);

                }

            } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                //We've lost focus permanently so pause the service. We'll have to request focus again later.
                getCurrentMediaPlayer().pause();
                updateNotification(mApp.getService().getCurrentSong());
                mAudioManagerHelper.setHasAudioFocus(false);
            }
        }
    };
    /**
     * Crossfade runnable.
     */
    public Runnable crossFadeRunnable = new Runnable() {

        @Override
        public void run() {
            try {
                if (getRepeatMode() != Common.REPEAT_SONG) {
                    if (getCursor().getCount() > (mCurrentSongIndex + 1)) {
                        if (getCurrentMediaPlayer() == getMediaPlayer()) {
                            getMediaPlayer2().setVolume(mFadeInVolume, mFadeInVolume);
                            getMediaPlayer().setVolume(mFadeOutVolume, mFadeOutVolume);
                            //If the mMediaPlayer is already playing or it hasn't been prepared yet, we can't use crossfade.
                            if (!getMediaPlayer2().isPlaying()) {
                                if (mMediaPlayer2Prepared) {
                                    if (checkAndRequestAudioFocus()) {
                                        getMediaPlayer2().start();
                                    } else {
                                        return;
                                    }
                                }
                            }
                        } else {
                            getMediaPlayer().setVolume(mFadeInVolume, mFadeInVolume);
                            getMediaPlayer2().setVolume(mFadeOutVolume, mFadeOutVolume);

                            //If the mMediaPlayer is already playing or it hasn't been prepared yet, we can't use crossfade.
                            if (!getMediaPlayer().isPlaying()) {
                                if (mMediaPlayerPrepared) {
                                    if (checkAndRequestAudioFocus()) {
                                        getMediaPlayer().start();
                                    } else {
                                        return;
                                    }
                                }
                            }
                        }
                        mFadeInVolume = mFadeInVolume + 1.0f / (((float) mCrossfadeDuration) * 10.0f);
                        mFadeOutVolume = mFadeOutVolume - 1.0f / (((float) mCrossfadeDuration) * 10.0f);
                        mHandler.postDelayed(crossFadeRunnable, 100);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    };
    /**
     * Called once mMediaPlayer is prepared.
     */
    public OnPreparedListener mediaPlayerPrepared = new OnPreparedListener() {

        @Override
        public void onPrepared(MediaPlayer mediaPlayer) {

            //Update the prepared flag.
            setIsMediaPlayerPrepared(true);

            //Set the completion listener for mMediaPlayer.
            getMediaPlayer().setOnCompletionListener(onMediaPlayerCompleted);

            //Check to make sure we have AudioFocus.
            if (checkAndRequestAudioFocus()) {
                if (mFirstRun) {
                    startMediaPlayer();
                    mFirstRun = false;
                }
            }
        }
    };

    /**
     * Constructor that should be used whenever this
     * service is being explictly created.
     *
     * @param context The context being passed in.
     */
    public AudioPlaybackService(Context context) {
        mContext = context;
    }

    /**
     * Empty constructor. Required if a custom constructor
     * was explicitly declared (see above).
     */
    public AudioPlaybackService() {
        super();
    }

    /**
     * Prepares the MediaPlayer objects for first use
     * and starts the service. The workflow of the entire
     * service starts here.
     *
     * @param intent  Calling intent.
     * @param flags   Service flags.
     * @param startId Service start ID.
     */
    @SuppressLint("NewApi")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        //Context.
        mContext = getApplicationContext();
        mService = this;
        mHandler = new Handler();

        mApp = (Common) getApplicationContext();
        mApp.setService(this);

        //Initialize the MediaPlayer objects.
        initMediaPlayers();

        //Time to play nice with other music players (and audio apps) and request audio focus.
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mAudioManagerHelper = new AudioManagerHelper();

        mCrossfadeDuration = mApp.getCrossfadeDuration();

        mMediaButtonReceiverComponent = new ComponentName(this.getPackageName(), HeadsetButtonsReceiver.class.getName());
        mAudioManager.registerMediaButtonEventReceiver(mMediaButtonReceiverComponent);

        initRemoteControlClient();
        registerHeadsetPlugReceiver();

        mApp.getPlaybackKickstarter().setBuildCursorListener(buildCursorListener);

        //The service has been successfully started.
        setPrepareServiceListener(mApp.getPlaybackKickstarter());
        getPrepareServiceListener().onServiceRunning(this);

        return START_NOT_STICKY;
    }

    /**
     * Initializes remote control clients for this service session.
     * Currently used for lockscreen controls.
     */
    public void initRemoteControlClient() {
        if (mRemoteControlClientCompat == null) {
            Intent remoteControlIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
            remoteControlIntent.setComponent(mMediaButtonReceiverComponent);

            mRemoteControlClientCompat = new RemoteControlClientCompat(PendingIntent.getBroadcast(mContext, 0, remoteControlIntent, 0));
            RemoteControlHelper.registerRemoteControlClient(mAudioManager, mRemoteControlClientCompat);
        }

        mRemoteControlClientCompat.setPlaybackState(RemoteControlClient.PLAYSTATE_PLAYING);
        mRemoteControlClientCompat.setTransportControlFlags(RemoteControlClient.FLAG_KEY_MEDIA_PLAY |
                RemoteControlClient.FLAG_KEY_MEDIA_PAUSE |
                RemoteControlClient.FLAG_KEY_MEDIA_NEXT |
                RemoteControlClient.FLAG_KEY_MEDIA_STOP |
                RemoteControlClient.FLAG_KEY_MEDIA_PREVIOUS);

    }

    /**
     * Initializes the MediaPlayer objects for this service session.
     */
    private void initMediaPlayers() {

		/*
         * Release the MediaPlayer objects if they are still valid.
		 */
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
        }

        if (mMediaPlayer2 != null) {
            mMediaPlayer2.release();
        }

        mMediaPlayer = new MediaPlayer();
        mMediaPlayer2 = new MediaPlayer();

        setCurrentMediaPlayer(1);

        //Loop the players if the repeat mode is set to repeat the current song.
        if (getRepeatMode() == Common.REPEAT_SONG) {
            getMediaPlayer().setLooping(true);
            getMediaPlayer2().setLooping(true);
        }

        try {
            getMediaPlayer().setWakeMode(mContext, PowerManager.PARTIAL_WAKE_LOCK);
            getMediaPlayer2().setWakeMode(mContext, PowerManager.PARTIAL_WAKE_LOCK);
        } catch (Exception e) {
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer2 = new MediaPlayer();
            setCurrentMediaPlayer(1);
        }

        //Set the mediaPlayers' stream sources.
        getMediaPlayer().setAudioStreamType(AudioManager.STREAM_MUSIC);
        getMediaPlayer2().setAudioStreamType(AudioManager.STREAM_MUSIC);
    }

    /**
     * Initializes the list of pointers to each cursor row.
     */
    private void initPlaybackIndecesList(boolean playAll) {
        if (getCursor() != null && getPlaybackIndecesList() != null) {
            getPlaybackIndecesList().clear();
            for (int i = 0; i < getCursor().getCount(); i++) {
                getPlaybackIndecesList().add(i);
            }

            if (isShuffleOn() && !playAll) {
                //Build a new list that doesn't include the current song index.
                ArrayList<Integer> newList = new ArrayList<>(getPlaybackIndecesList());
                newList.remove(getCurrentSongIndex());

                //Shuffle the new list.
                Collections.shuffle(newList, new Random(System.nanoTime()));

                //Plug in the current song index back into the new list.
                newList.add(getCurrentSongIndex(), getCurrentSongIndex());
                mPlaybackIndecesList = newList;

            } else if (isShuffleOn()) {
                Collections.shuffle(getPlaybackIndecesList(), new Random(System.nanoTime()));
            }
        } else {
            stopSelf();
        }
    }

    /**
     * Requests AudioFocus from the OS.
     *
     * @return True if AudioFocus was gained. False, otherwise.
     */
    private boolean requestAudioFocus() {
        int result = mAudioManager.requestAudioFocus(audioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN);

        if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            //Stop the service.
            mService.stopSelf();
            return false;
        } else {
            return true;
        }
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    /**
     * Builds and returns a fully constructed Notification for devices
     * on Lollipop and above (API 21+).
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private Notification buildLPNotification(SongHelper songHelper) {

        //Initialize the notification layout buttons.
        Intent previousTrackIntent = new Intent();
        previousTrackIntent.setAction(AudioPlaybackService.PREVIOUS_ACTION);
        PendingIntent previousTrackPendingIntent = PendingIntent.getBroadcast(mContext.getApplicationContext(), 0, previousTrackIntent, 0);

        Intent playPauseTrackIntent = new Intent();
        playPauseTrackIntent.setAction(AudioPlaybackService.PLAY_PAUSE_ACTION);
        PendingIntent playPauseTrackPendingIntent = PendingIntent.getBroadcast(mContext.getApplicationContext(), 0, playPauseTrackIntent, 0);

        Intent nextTrackIntent = new Intent();
        nextTrackIntent.setAction(AudioPlaybackService.NEXT_ACTION);
        PendingIntent nextTrackPendingIntent = PendingIntent.getBroadcast(mContext.getApplicationContext(), 0, nextTrackIntent, 0);

        Intent stopServiceIntent = new Intent();
        stopServiceIntent.setAction(AudioPlaybackService.STOP_SERVICE);
        PendingIntent stopServicePendingIntent = PendingIntent.getBroadcast(mContext.getApplicationContext(), 0, stopServiceIntent, 0);

        //Open up the player screen when the user taps on the notification.
        Intent launchNowPlayingIntent = new Intent();
        launchNowPlayingIntent.setAction(AudioPlaybackService.LAUNCH_NOW_PLAYING_ACTION);
        PendingIntent launchNowPlayingPendingIntent = PendingIntent.getBroadcast(mContext.getApplicationContext(), 0, launchNowPlayingIntent, 0);

        Notification.MediaStyle style = new Notification.MediaStyle();

        Notification.Builder builder = new Notification.Builder(this)
                .setSmallIcon(R.drawable.ic_action_music_raag)
                .setContentTitle(songHelper.getTitle())
                .setContentText(songHelper.getArtist())
                .setSubText(songHelper.getAlbum())
                .setContentIntent(launchNowPlayingPendingIntent)
                .setLargeIcon(songHelper.getAlbumArt())
                .setShowWhen(false)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setStyle(style);

        builder.addAction(new Notification.Action.Builder(R.drawable.ic_skip_previous, "previous", previousTrackPendingIntent).build());
        if (isPlayingMusic()) {
            builder.addAction(new Notification.Action.Builder(R.drawable.pause, "playpause", playPauseTrackPendingIntent).build());
        } else {
            builder.addAction(new Notification.Action.Builder(R.drawable.play, "playpause", playPauseTrackPendingIntent).build());
        }
        builder.addAction(new Notification.Action.Builder(R.drawable.ic_skip_next, "next", nextTrackPendingIntent).build());
        style.setShowActionsInCompactView(0, 1, 2);
        return builder.build();
    }

    /**
     * Builds and returns a fully constructed Notification for devices
     * on Jelly Bean and above (API 16+).
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private Notification buildJBNotification(SongHelper songHelper) {
        mNotificationBuilder = new NotificationCompat.Builder(mContext);
        mNotificationBuilder.setOngoing(true);
        mNotificationBuilder.setVisibility(Notification.VISIBILITY_PUBLIC);
        mNotificationBuilder.setAutoCancel(false);
        mNotificationBuilder.setSmallIcon(R.drawable.ic_action_music_raag);

        //Open up the player screen when the user taps on the notification.
        Intent launchNowPlayingIntent = new Intent();
        launchNowPlayingIntent.setAction(AudioPlaybackService.LAUNCH_NOW_PLAYING_ACTION);
        PendingIntent launchNowPlayingPendingIntent = PendingIntent.getBroadcast(mContext.getApplicationContext(), 0, launchNowPlayingIntent, 0);
        mNotificationBuilder.setContentIntent(launchNowPlayingPendingIntent);

        //Grab the notification layouts.
        RemoteViews notificationView = new RemoteViews(mContext.getPackageName(), R.layout.notification_custom_layout);
        RemoteViews expNotificationView = new RemoteViews(mContext.getPackageName(), R.layout.notification_custom_expanded_layout);
        //Initialize the notification layout buttons.
        Intent previousTrackIntent = new Intent();
        previousTrackIntent.setAction(AudioPlaybackService.PREVIOUS_ACTION);
        PendingIntent previousTrackPendingIntent = PendingIntent.getBroadcast(mContext.getApplicationContext(), 0, previousTrackIntent, 0);

        Intent playPauseTrackIntent = new Intent();
        playPauseTrackIntent.setAction(AudioPlaybackService.PLAY_PAUSE_ACTION);
        PendingIntent playPauseTrackPendingIntent = PendingIntent.getBroadcast(mContext.getApplicationContext(), 0, playPauseTrackIntent, 0);

        Intent nextTrackIntent = new Intent();
        nextTrackIntent.setAction(AudioPlaybackService.NEXT_ACTION);
        PendingIntent nextTrackPendingIntent = PendingIntent.getBroadcast(mContext.getApplicationContext(), 0, nextTrackIntent, 0);

        Intent stopServiceIntent = new Intent();
        stopServiceIntent.setAction(AudioPlaybackService.STOP_SERVICE);
        PendingIntent stopServicePendingIntent = PendingIntent.getBroadcast(mContext.getApplicationContext(), 0, stopServiceIntent, 0);

        //Check if audio is playing and set the appropriate play/pause button.
        if (mApp.getService().isPlayingMusic()) {
            notificationView.setImageViewResource(R.id.notification_base_play, R.drawable.pause);
            expNotificationView.setImageViewResource(R.id.notification_expanded_base_play, R.drawable.pause);
        } else {
            notificationView.setImageViewResource(R.id.notification_base_play, R.drawable.play);
            expNotificationView.setImageViewResource(R.id.notification_expanded_base_play, R.drawable.play);
        }

        //Set the notification content.
        expNotificationView.setTextViewText(R.id.notification_expanded_base_line_one, songHelper.getTitle());
        expNotificationView.setTextViewText(R.id.notification_expanded_base_line_two, songHelper.getArtist());
        expNotificationView.setTextViewText(R.id.notification_expanded_base_line_three, songHelper.getAlbum());
        notificationView.setViewVisibility(R.id.notification_base_previous, View.GONE);
        notificationView.setTextViewText(R.id.notification_base_line_one, songHelper.getTitle());
        notificationView.setTextViewText(R.id.notification_base_line_two, songHelper.getArtist());

        //Set the states of the next/previous buttons and their pending intents.
        if (mApp.getService().isOnlySongInQueue()) {
            //This is the only song in the queue, so disable the previous/next buttons.
            expNotificationView.setViewVisibility(R.id.notification_expanded_base_next, View.INVISIBLE);
            expNotificationView.setViewVisibility(R.id.notification_expanded_base_previous, View.INVISIBLE);
            expNotificationView.setOnClickPendingIntent(R.id.notification_expanded_base_play, playPauseTrackPendingIntent);

            notificationView.setViewVisibility(R.id.notification_base_next, View.INVISIBLE);
            notificationView.setOnClickPendingIntent(R.id.notification_base_play, playPauseTrackPendingIntent);

        } else if (mApp.getService().isFirstSongInQueue()) {
            //This is the the first song in the queue, so disable the previous button.
            expNotificationView.setViewVisibility(R.id.notification_expanded_base_previous, View.INVISIBLE);
            expNotificationView.setViewVisibility(R.id.notification_expanded_base_next, View.VISIBLE);
            expNotificationView.setOnClickPendingIntent(R.id.notification_expanded_base_play, playPauseTrackPendingIntent);
            expNotificationView.setOnClickPendingIntent(R.id.notification_expanded_base_next, nextTrackPendingIntent);

            notificationView.setViewVisibility(R.id.notification_base_next, View.VISIBLE);
            notificationView.setOnClickPendingIntent(R.id.notification_base_play, playPauseTrackPendingIntent);
            notificationView.setOnClickPendingIntent(R.id.notification_base_next, nextTrackPendingIntent);

        } else if (mApp.getService().isLastSongInQueue()) {
            //This is the last song in the cursor, so disable the next button.
            expNotificationView.setViewVisibility(R.id.notification_expanded_base_previous, View.VISIBLE);
            expNotificationView.setViewVisibility(R.id.notification_expanded_base_next, View.INVISIBLE);
            expNotificationView.setOnClickPendingIntent(R.id.notification_expanded_base_play, playPauseTrackPendingIntent);
            expNotificationView.setOnClickPendingIntent(R.id.notification_expanded_base_next, nextTrackPendingIntent);

            notificationView.setViewVisibility(R.id.notification_base_next, View.INVISIBLE);
            notificationView.setOnClickPendingIntent(R.id.notification_base_play, playPauseTrackPendingIntent);
            notificationView.setOnClickPendingIntent(R.id.notification_base_next, nextTrackPendingIntent);
        } else {
            //We're smack dab in the middle of the queue, so keep the previous and next buttons enabled.
            expNotificationView.setViewVisibility(R.id.notification_expanded_base_previous, View.VISIBLE);
            expNotificationView.setViewVisibility(R.id.notification_expanded_base_next, View.VISIBLE);
            expNotificationView.setOnClickPendingIntent(R.id.notification_expanded_base_play, playPauseTrackPendingIntent);
            expNotificationView.setOnClickPendingIntent(R.id.notification_expanded_base_next, nextTrackPendingIntent);
            expNotificationView.setOnClickPendingIntent(R.id.notification_expanded_base_previous, previousTrackPendingIntent);

            notificationView.setViewVisibility(R.id.notification_base_next, View.VISIBLE);
            notificationView.setOnClickPendingIntent(R.id.notification_base_play, playPauseTrackPendingIntent);
            notificationView.setOnClickPendingIntent(R.id.notification_base_next, nextTrackPendingIntent);
            notificationView.setOnClickPendingIntent(R.id.notification_base_previous, previousTrackPendingIntent);

        }
        //Set the "Stop Service" pending intents.
//        notificationView.setViewVisibility(R.id.notification_base_image, View.GONE);
        expNotificationView.setOnClickPendingIntent(R.id.notification_expanded_base_collapse, stopServicePendingIntent);
        notificationView.setOnClickPendingIntent(R.id.notification_base_collapse, stopServicePendingIntent);

        //Set the album art.
        expNotificationView.setImageViewBitmap(R.id.notification_expanded_base_image, songHelper.getAlbumArt());
        notificationView.setImageViewBitmap(R.id.notification_base_image, songHelper.getAlbumArt());

        //Attach the shrunken layout to the notification.
        mNotificationBuilder.setContent(notificationView);

        //Build the notification object.
        Notification notification = mNotificationBuilder.build();

        //Attach the expanded layout to the notification and set its flags.
        notification.bigContentView = expNotificationView;
        notification.flags = Notification.FLAG_FOREGROUND_SERVICE |
                Notification.FLAG_NO_CLEAR |
                Notification.FLAG_ONGOING_EVENT;

        return notification;
    }

    /**
     * Builds and returns a fully constructed Notification for devices
     * on Ice Cream Sandwich (APIs 14 & 15).
     */
    private Notification buildICSNotification(SongHelper songHelper) {
        mNotificationBuilder = new NotificationCompat.Builder(mContext);
        mNotificationBuilder.setOngoing(true);
        mNotificationBuilder.setAutoCancel(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            mNotificationBuilder.setVisibility(Notification.VISIBILITY_PUBLIC);
        mNotificationBuilder.setSmallIcon(R.drawable.ic_action_music_raag);

        //Open up the player screen when the user taps on the notification.
        Intent launchNowPlayingIntent = new Intent();
        launchNowPlayingIntent.setAction(AudioPlaybackService.LAUNCH_NOW_PLAYING_ACTION);
        PendingIntent launchNowPlayingPendingIntent = PendingIntent.getBroadcast(mContext.getApplicationContext(), 0, launchNowPlayingIntent, 0);
        mNotificationBuilder.setContentIntent(launchNowPlayingPendingIntent);

        //Grab the notification layout.
        RemoteViews notificationView = new RemoteViews(mContext.getPackageName(), R.layout.notification_custom_layout);

        //Initialize the notification layout buttons.
        Intent previousTrackIntent = new Intent();
        previousTrackIntent.setAction(AudioPlaybackService.PREVIOUS_ACTION);
        PendingIntent previousTrackPendingIntent = PendingIntent.getBroadcast(mContext.getApplicationContext(), 0, previousTrackIntent, 0);

        Intent playPauseTrackIntent = new Intent();
        playPauseTrackIntent.setAction(AudioPlaybackService.PLAY_PAUSE_ACTION);
        PendingIntent playPauseTrackPendingIntent = PendingIntent.getBroadcast(mContext.getApplicationContext(), 0, playPauseTrackIntent, 0);

        Intent nextTrackIntent = new Intent();
        nextTrackIntent.setAction(AudioPlaybackService.NEXT_ACTION);
        PendingIntent nextTrackPendingIntent = PendingIntent.getBroadcast(mContext.getApplicationContext(), 0, nextTrackIntent, 0);

        Intent stopServiceIntent = new Intent();
        stopServiceIntent.setAction(AudioPlaybackService.STOP_SERVICE);
        PendingIntent stopServicePendingIntent = PendingIntent.getBroadcast(mContext.getApplicationContext(), 0, stopServiceIntent, 0);

        //Check if audio is playing and set the appropriate play/pause button.
        if (mApp.getService().isPlayingMusic()) {
            notificationView.setImageViewResource(R.id.notification_base_play, R.drawable.pause);
        } else {
            notificationView.setImageViewResource(R.id.notification_base_play, R.drawable.play);
        }

        //Set the notification content.
        notificationView.setTextViewText(R.id.notification_base_line_one, songHelper.getTitle());
        notificationView.setTextViewText(R.id.notification_base_line_two, songHelper.getArtist());
        notificationView.setViewVisibility(R.id.notification_base_previous, View.GONE);

        //Set the states of the next/previous buttons and their pending intents.
        if (mApp.getService().isOnlySongInQueue()) {
            //This is the only song in the queue, so disable the previous/next buttons.
            notificationView.setViewVisibility(R.id.notification_base_next, View.INVISIBLE);
            notificationView.setOnClickPendingIntent(R.id.notification_base_play, playPauseTrackPendingIntent);

        } else if (mApp.getService().isFirstSongInQueue()) {
            //This is the the first song in the queue, so disable the previous button.
            notificationView.setViewVisibility(R.id.notification_base_next, View.VISIBLE);
            notificationView.setOnClickPendingIntent(R.id.notification_base_play, playPauseTrackPendingIntent);
            notificationView.setOnClickPendingIntent(R.id.notification_base_next, nextTrackPendingIntent);

        } else if (mApp.getService().isLastSongInQueue()) {
            //This is the last song in the cursor, so disable the next button.
            notificationView.setViewVisibility(R.id.notification_base_next, View.INVISIBLE);
            notificationView.setOnClickPendingIntent(R.id.notification_base_play, playPauseTrackPendingIntent);
            notificationView.setOnClickPendingIntent(R.id.notification_base_next, nextTrackPendingIntent);

        } else {
            //We're smack dab in the middle of the queue, so keep the previous and next buttons enabled.
            notificationView.setViewVisibility(R.id.notification_base_next, View.VISIBLE);
            notificationView.setOnClickPendingIntent(R.id.notification_base_play, playPauseTrackPendingIntent);
            notificationView.setOnClickPendingIntent(R.id.notification_base_next, nextTrackPendingIntent);
            notificationView.setOnClickPendingIntent(R.id.notification_base_previous, previousTrackPendingIntent);
        }

        //Set the "Stop Service" pending intent.
        notificationView.setOnClickPendingIntent(R.id.notification_base_collapse, stopServicePendingIntent);

        //Set the album art.
        notificationView.setImageViewBitmap(R.id.notification_base_image, songHelper.getAlbumArt());

        //Attach the shrunken layout to the notification.
        mNotificationBuilder.setContent(notificationView);

        //Build the notification object and set its flags.
        Notification notification = mNotificationBuilder.build();
        notification.flags = Notification.FLAG_FOREGROUND_SERVICE |
                Notification.FLAG_NO_CLEAR |
                Notification.FLAG_ONGOING_EVENT;

        return notification;
    }

    /**
     * Returns the appropriate notification based on the device's
     * API level.
     */
    private Notification buildNotification(SongHelper songHelper) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return buildLPNotification(songHelper);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            return buildJBNotification(songHelper);
        } else {
            return buildICSNotification(songHelper);
        }
    }

    /**
     * Updates the current notification with info from the specified
     * SongHelper object.
     */
    public void updateNotification(SongHelper songHelper) {
        Notification notification;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            notification = buildLPNotification(songHelper);
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
            notification = buildJBNotification(songHelper);
        else
            notification = buildICSNotification(songHelper);

        //Update the current notification.
        NotificationManager notifManager = (NotificationManager) mApp.getSystemService(Context.NOTIFICATION_SERVICE);
        notifManager.notify(mNotificationId, notification);

//        RemoteControlClientCompat.MetadataEditorCompat ed = mRemoteControlClientCompat.editMetadata(true);
//        ed.putString(MediaMetadataRetriever.METADATA_KEY_TITLE, getCurrentSong().getTitle());
//        ed.putString(MediaMetadataRetriever.METADATA_KEY_ALBUM, getCurrentSong().getAlbum());
//        ed.putString(MediaMetadataRetriever.METADATA_KEY_ARTIST, getCurrentSong().getArtist());
//        ed.putLong(MediaMetadataRetriever.METADATA_KEY_DURATION, Long.parseLong(getCurrentSong().getDuration()));
//        Bitmap b = getCurrentSong().getAlbumArt();
//        if (b != null) {
//            ed.putBitmap(RemoteControlClient.MetadataEditor.BITMAP_KEY_ARTWORK, b);
//        }
//        ed.apply();
    }

    /**
     * This method combines mCursor with the specified newCursor.
     *
     * @param newCursor The new cursor to append to mCursor.
     * @param playNext  Pass true if newCursor should be appeneded after the current song.
     */
    public void enqueueCursor(Cursor newCursor, boolean playNext) {

        Cursor[] cursorArray = {getCursor(), newCursor};
        MergeCursor mMergeCursor = new MergeCursor(cursorArray);
        setCursor(mMergeCursor);
        getCursor().moveToPosition(mPlaybackIndecesList.get(mCurrentSongIndex));

        if (playNext) {
            //Check which mMediaPlayer is currently playing, and prepare the other mediaPlayer.
            prepareAlternateMediaPlayer();
        }

    }

    /**
     * Grabs the song parameters at the specified index, retrieves its
     * data source, and beings to asynchronously prepare mMediaPlayer.
     * Once mMediaPlayer is prepared, mediaPlayerPrepared is called.
     *
     * @return True if the method completed with no exceptions. False, otherwise.
     */
    public boolean prepareMediaPlayer(int songIndex) {
        try {
            //Stop here if we're at the end of the queue.
            if (songIndex == -1)
                return true;
            //Reset mMediaPlayer to it's uninitialized state.
            getMediaPlayer().reset();

            //Loop the player if the repeat mode is set to repeat the current song.
            if (getRepeatMode() == Common.REPEAT_SONG) {
                getMediaPlayer().setLooping(true);
            }

            //Set mMediaPlayer's song data.
            SongHelper songHelper = new SongHelper();
            songHelper.populateSongData(mContext, songIndex);

            if (mFirstRun) {
                startForeground(mNotificationId, buildNotification(songHelper));
            }
            final Uri songMediaPlayerUri = getSongDataSource(songHelper);
            songHelper.setSongSourceUri(songMediaPlayerUri);
            setMediaPlayerSongHelper(songHelper);

//            if (songMediaPlayerUri != null) {
//                if (songMediaPlayerUri.toString().startsWith("http")) {
//                    new DownloadTask(songHelper).execute(songMediaPlayerUri.toString());
//                }
//            }
            getMediaPlayer().setDataSource(mContext, songMediaPlayerUri);
            getMediaPlayer().setOnPreparedListener(mediaPlayerPrepared);
            getMediaPlayer().setOnErrorListener(onErrorListener);
            getMediaPlayer().prepareAsync();
        } catch (Exception e) {
            e.printStackTrace();

            //Add the current song index to the list of failed indeces.
            getFailedIndecesList().add(songIndex);

            //Start preparing the next song.
//            if (!isAtEndOfQueue())
//                prepareMediaPlayer(songIndex + 1);
//            else
//                return false;
            return false;
        }
        return true;
    }

    /**
     * Grabs the song parameters at the specified index, retrieves its
     * data source, and beings to asynchronously prepare mMediaPlayer2.
     * Once mMediaPlayer2 is prepared, mediaPlayer2Prepared is called.
     *
     * @return True if the method completed with no exceptions. False, otherwise.
     */

    public boolean prepareMediaPlayer2(int songIndex) {
        try {
            //Stop here if we're at the end of the queue.
            if (songIndex == -1)
                return true;
            //Reset mMediaPlayer2 to its uninitialized state.
            getMediaPlayer2().reset();

            //Loop the player if the repeat mode is set to repeat the current song.
            if (getRepeatMode() == Common.REPEAT_SONG) {
                getMediaPlayer2().setLooping(true);
            }

            //Set mMediaPlayer2's song data.
            SongHelper songHelper = new SongHelper();
            songHelper.populateSongData(mContext, songIndex);

            final Uri songMediaPlayer2Uri = getSongDataSource(songHelper);
            songHelper.setSongSourceUri(songMediaPlayer2Uri);
            setMediaPlayer2SongHelper(songHelper);

//            if (songMediaPlayer2Uri != null) {
//                if (songMediaPlayer2Uri.toString().startsWith("http")) {
//                    new DownloadTask(songHelper).execute(songMediaPlayer2Uri.toString());
//                }
//            }

            getMediaPlayer2().setDataSource(mContext, songMediaPlayer2Uri);
            getMediaPlayer2().setOnPreparedListener(mediaPlayer2Prepared);
            getMediaPlayer2().setOnErrorListener(onErrorListener);
            getMediaPlayer2().prepareAsync();
        } catch (Exception e) {
            e.printStackTrace();
            //Add the current song index to the list of failed indeces.
            getFailedIndecesList().add(songIndex);
            //Start preparing the next song.
//                prepareMediaPlayer2(songIndex + 1);
//            else
//                return false;
            return false;
        }
        return true;
    }

    /**
     * Returns the Uri of a song's data source.
     * If the song is a local file, its file path is
     * returned. If the song is from soundcloud, its local
     * copy path is returned (if it exists). If no local
     * copy exists, the song's remote URL is requested
     * from Google's servers and a temporary placeholder
     * (URI_BEING_LOADED) is returned.
     */
    private Uri getSongDataSource(SongHelper songHelper) {
        try {
            File file = new File(songHelper.getFilePath());
            if (songHelper.getSource().equals(DBAccessHelper.SOUNDCLOUD)) {
                //Check if a local copy of the song exists.
                if (songHelper.getFilePath() != null &&
                        songHelper.getFilePath().length() > 2) {
                    //Double check to make sure that the local copy file exists.
                    if (file.exists()) {
                        if (Common.isNetworkAvailable(mContext)) {
                            HashMap<String, Long> map = new HashMap<>();
                            if (Prefs.getSongFileSize(mContext) != null)
                                map = Prefs.getSongFileSize(mContext);
                            if (map.get(songHelper.getId()) - file.length() < 5000) {
                                return Uri.fromFile(file);
                            } else {
                                file.delete();
                                return Uri.parse(Common.getSongStream(songHelper.getId()));
                            }
                        } else {
                            //The local copy exists. Return its path.
                            return Uri.fromFile(file);
                        }
                    } else {
                        //The local copy doesn't exist. Request the remote URL and return a placeholder Uri.
                        return Uri.parse(Common.getSongStream(songHelper.getId()));
                    }
                } else {
                    //Request the remote URL and return a placeholder Uri.
                    return Uri.parse(Common.getSongStream(songHelper.getId()));
                }
            } else {
                //Return the song's file path.
                return Uri.fromFile(file);
            }
        } catch (Exception e) {
            e.printStackTrace();
            mApp.getService().stopSelf();
        }
        return null;
    }

    /**
     * Sets the A-B Repeat song markers.
     *
     * @param pointA The duration to repeat from (in millis).
     * @param pointB The duration to repeat until (in millis).
     */
    public void setRepeatSongRange(int pointA, int pointB) {
        mRepeatSongRangePointA = pointA;
        mRepeatSongRangePointB = pointB;
        getCurrentMediaPlayer().seekTo(pointA);
        mHandler.postDelayed(checkABRepeatRange, 100);
    }

    /**
     * Clears the A-B Repeat song markers.
     */
    public void clearABRepeatRange() {
        mHandler.removeCallbacks(checkABRepeatRange);
        mRepeatSongRangePointA = 0;
        mRepeatSongRangePointB = 0;
        mApp.getSharedPreferences().edit().putInt(Common.REPEAT_MODE, Common.REPEAT_PLAYLIST);
    }

    /**
     * Fix for KitKat error where the service is killed as soon
     * as the app is swiped away from the Recents menu.
     */
    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Intent intent = new Intent(this, KitKatFixActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    /**
     * Checks if we have AudioFocus. If not, it explicitly requests it.
     *
     * @return True if we have AudioFocus. False, otherwise.
     */
    private boolean checkAndRequestAudioFocus() {
        return mAudioManagerHelper.hasAudioFocus() || requestAudioFocus();
    }

    /**
     * Registers the headset plug receiver.
     */
    public void registerHeadsetPlugReceiver() {
        //Register the headset plug receiver.
        IntentFilter filter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
        mHeadsetPlugReceiver = new HeadsetPlugBroadcastReceiver();
        mService.registerReceiver(mHeadsetPlugReceiver, filter);
    }

    /**
     * Increments mCurrentSongIndex based on mErrorCount.
     * Returns the new value of mCurrentSongIndex.
     */
    public int incrementCurrentSongIndex() {
        if ((getCurrentSongIndex() + 1) < getCursor().getCount())
            mCurrentSongIndex++;

        return mCurrentSongIndex;
    }

    /**
     * Decrements mCurrentSongIndex by one. Returns the new value
     * of mCurrentSongIndex.
     */
    public int decrementCurrentSongIndex() {
        if ((getCurrentSongIndex() - 1) > -1)
            mCurrentSongIndex--;

        return mCurrentSongIndex;
    }

    /**
     * Starts playing mMediaPlayer and sends out the update UI broadcast,
     * and updates the notification and any open widgets.
     * <p/>
     * Do NOT call this method before mMediaPlayer has been prepared.
     */
    private void startMediaPlayer() throws IllegalStateException {

        //Aaaaand let the show begin!
        setCurrentMediaPlayer(1);
        getMediaPlayer().start();

        //Set the new value for mCurrentSongIndex.
        if (!mFirstRun) {
            do {
                setCurrentSongIndex(determineNextSongIndex());
            } while (getFailedIndecesList().contains(getCurrentSongIndex()));
            getFailedIndecesList().clear();
        } else {
            while (getFailedIndecesList().contains(getCurrentSongIndex())) {
                setCurrentSongIndex(determineNextSongIndex());
            }
            //Initialize the crossfade runnable.
            if (mHandler != null && mApp.isCrossfadeEnabled()) {
                mHandler.post(startCrossFadeRunnable);
            }
        }

        //Update the UI.
        String[] updateFlags = new String[]{Common.UPDATE_PAGER_POSTIION,
                Common.UPDATE_PLAYBACK_CONTROLS,
                Common.UPDATE_SEEKBAR_DURATION};

        String[] flagValues = new String[]{getCurrentSongIndex() + "",
                "",
                getMediaPlayer().getDuration() + ""};

        mApp.broadcastUpdateUICommand(updateFlags, flagValues);
        setCurrentSong(getCurrentSong());
        if (getCurrentSong().getSongSourceUri() != null) {
            if (getCurrentSong().getSongSourceUri().toString().startsWith("http")) {
                new DownloadTask(getCurrentSong()).execute(getCurrentSong().getSongSourceUri().toString());
            }
        }
        //Start preparing the next song.
        prepareMediaPlayer2(determineNextSongIndex());
    }

    /**
     * Starts playing mMediaPlayer2, sends out the update UI broadcast,
     * and updates the notification and any open widgets.
     * <p/>
     * Do NOT call this method before mMediaPlayer2 has been prepared.
     */
    private void startMediaPlayer2() throws IllegalStateException {

        //Aaaaaand let the show begin!
        setCurrentMediaPlayer(2);
        getMediaPlayer2().start();

        //Set the new value for mCurrentSongIndex.
        do {
            setCurrentSongIndex(determineNextSongIndex());
        } while (getFailedIndecesList().contains(getCurrentSongIndex()));

        getFailedIndecesList().clear();

        //Update the UI.
        String[] updateFlags = new String[]{Common.UPDATE_PAGER_POSTIION,
                Common.UPDATE_PLAYBACK_CONTROLS,
                Common.UPDATE_SEEKBAR_DURATION};

        String[] flagValues = new String[]{getCurrentSongIndex() + "",
                "",
                getMediaPlayer2().getDuration() + ""};

        mApp.broadcastUpdateUICommand(updateFlags, flagValues);
        setCurrentSong(getCurrentSong());

        if (getCurrentSong().getSongSourceUri() != null) {
            if (getCurrentSong().getSongSourceUri().toString().startsWith("http")) {
                new DownloadTask(getCurrentSong()).execute(getCurrentSong().getSongSourceUri().toString());
            }
        }
        //Start preparing the next song.
        prepareMediaPlayer(determineNextSongIndex());
    }

    /**
     * Starts/resumes the current media player. Returns true if
     * the operation succeeded. False, otherwise.
     */
    public boolean startPlayback() {
        try {
            //Check to make sure we have audio focus.
            if (checkAndRequestAudioFocus()) {
                getCurrentMediaPlayer().start();
                String[] updateFlags = new String[]{Common.UPDATE_PLAYBACK_CONTROLS};
                String[] flagValues = new String[]{""};
                mApp.broadcastUpdateUICommand(updateFlags, flagValues);
                updateNotification(mApp.getService().getCurrentSong());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Pauses the current media player. Returns true if
     * the operation succeeded. False, otherwise.
     */
    public boolean pausePlayback() {
        try {
            getCurrentMediaPlayer().pause();
            String[] updateFlags = new String[]{Common.UPDATE_PLAYBACK_CONTROLS};
            String[] flagValues = new String[]{""};

            mApp.broadcastUpdateUICommand(updateFlags, flagValues);
            updateNotification(mApp.getService().getCurrentSong());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Skips to the next track (if there is one) and starts
     * playing it. Returns true if the operation succeeded.
     * False, otherwise.
     */
    public boolean skipToNextTrack() {
        try {
            //Reset both MediaPlayer objects.
            getMediaPlayer().reset();
            getMediaPlayer2().reset();
            clearCrossfadeCallbacks();

            //Loop the players if the repeat mode is set to repeat the current song.
            if (getRepeatMode() == Common.REPEAT_SONG) {
                getMediaPlayer().setLooping(true);
                getMediaPlayer2().setLooping(true);
            }

            //Remove crossfade runnables and reset all volume levels.
            getHandler().removeCallbacks(crossFadeRunnable);
            getMediaPlayer().setVolume(1.0f, 1.0f);
            getMediaPlayer2().setVolume(1.0f, 1.0f);

            //Increment the song index.
            incrementCurrentSongIndex();

            //Update the UI.
            String[] updateFlags = new String[]{Common.UPDATE_PAGER_POSTIION};
            String[] flagValues = new String[]{getCurrentSongIndex() + ""};
            mApp.broadcastUpdateUICommand(updateFlags, flagValues);

            //Start the playback process.
            mFirstRun = true;
            prepareMediaPlayer(getCurrentSongIndex());

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    /**
     * Skips to the previous track (if there is one) and starts
     * playing it. Returns true if the operation succeeded.
     * False, otherwise.
     */
    public boolean skipToPreviousTrack() {

        /*
         * If the current track is not within the first three seconds,
         * reset it. If it IS within the first three seconds, skip to the
         * previous track.
         */
        try {
            if (getCurrentMediaPlayer().getCurrentPosition() > 3000) {
                getCurrentMediaPlayer().seekTo(0);
                return true;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        try {
            //Reset both MediaPlayer objects.
            getMediaPlayer().reset();
            getMediaPlayer2().reset();
            clearCrossfadeCallbacks();

            //Loop the players if the repeat mode is set to repeat the current song.
            if (getRepeatMode() == Common.REPEAT_SONG) {
                getMediaPlayer().setLooping(true);
                getMediaPlayer2().setLooping(true);
            }

            //Remove crossfade runnables and reset all volume levels.
            getHandler().removeCallbacks(crossFadeRunnable);
            getMediaPlayer().setVolume(1.0f, 1.0f);
            getMediaPlayer2().setVolume(1.0f, 1.0f);

            //Decrement the song index.
            decrementCurrentSongIndex();

            //Update the UI.
            String[] updateFlags = new String[]{Common.UPDATE_PAGER_POSTIION};
            String[] flagValues = new String[]{getCurrentSongIndex() + ""};
            mApp.broadcastUpdateUICommand(updateFlags, flagValues);

            //Start the playback process.
            mFirstRun = true;
            prepareMediaPlayer(getCurrentSongIndex());

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    /**
     * Skips to the specified track index (if there is one) and starts
     * playing it. Returns true if the operation succeeded.
     * False, otherwise.
     */
    public boolean skipToTrack(int trackIndex) {
        try {
            //Reset both MediaPlayer objects.
            getMediaPlayer().reset();
            getMediaPlayer2().reset();
            clearCrossfadeCallbacks();

            //Loop the players if the repeat mode is set to repeat the current song.
            if (getRepeatMode() == Common.REPEAT_SONG) {
                getMediaPlayer().setLooping(true);
                getMediaPlayer2().setLooping(true);
            }

            //Remove crossfade runnables and reset all volume levels.
            getHandler().removeCallbacks(crossFadeRunnable);
            getMediaPlayer().setVolume(1.0f, 1.0f);
            getMediaPlayer2().setVolume(1.0f, 1.0f);

            //Update the song index.
            setCurrentSongIndex(trackIndex);

            //Update the UI.
            String[] updateFlags = new String[]{Common.UPDATE_PAGER_POSTIION};
            String[] flagValues = new String[]{getCurrentSongIndex() + ""};
            mApp.broadcastUpdateUICommand(updateFlags, flagValues);

            //Start the playback process.
            mFirstRun = true;
            prepareMediaPlayer(trackIndex);

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    /**
     * Toggles the playback state between playing and paused and
     * returns whether the current media player is now playing
     * music or not.
     */
    public boolean togglePlaybackState() {
        if (isPlayingMusic())
            pausePlayback();
        else
            startPlayback();

        return isPlayingMusic();
    }

    /**
     * Determines the next song's index based on the repeat
     * mode and current song index. Returns -1 if we're at
     * the end of the queue.
     */
    private int determineNextSongIndex() {
        if (isAtEndOfQueue() && getRepeatMode() == Common.REPEAT_PLAYLIST)
            return 0;
        else if (!isAtEndOfQueue() && getRepeatMode() == Common.REPEAT_SONG)
            return getCurrentSongIndex();
        else if (isAtEndOfQueue())
            return -1;
        else
            return (getCurrentSongIndex() + 1);

    }

    /**
     * Checks which MediaPlayer object is currently in use, and
     * starts preparing the other one.
     */
    public void prepareAlternateMediaPlayer() {
        if (mCurrentMediaPlayer == 1)
            prepareMediaPlayer2(determineNextSongIndex());
        else
            prepareMediaPlayer(determineNextSongIndex());

    }

    /**
     * Toggles shuffle mode and returns whether shuffle is now on or off.
     */
    public boolean toggleShuffleMode() {
        if (isShuffleOn()) {
            //Set shuffle off.
            mApp.getSharedPreferences().edit().putBoolean(Common.SHUFFLE_ON, false).commit();
            int currentElement = getPlaybackIndecesList().get(getCurrentSongIndex());
            Collections.sort(getPlaybackIndecesList());
            setCurrentSongIndex(getPlaybackIndecesList().indexOf(currentElement));
        } else {
            //Set shuffle on.
            mApp.getSharedPreferences().edit().putBoolean(Common.SHUFFLE_ON, true).commit();
            ArrayList<Integer> newList = new ArrayList<>(getPlaybackIndecesList());
            newList.remove(getCurrentSongIndex());
            Collections.shuffle(newList, new Random(System.nanoTime()));
            newList.add(getCurrentSongIndex(), getCurrentSongIndex());
            mPlaybackIndecesList = newList;
        }

    	/* Since the queue changed, we're gonna have to update the
         * next MediaPlayer object with the new song info.
    	 */
        prepareAlternateMediaPlayer();

        //Update all UI elements with the new queue order.
        mApp.broadcastUpdateUICommand(new String[]{Common.NEW_QUEUE_ORDER}, new String[]{""});
        return isShuffleOn();
    }

    /**
     * Returns the current active MediaPlayer object.
     */
    public MediaPlayer getCurrentMediaPlayer() {
        if (mCurrentMediaPlayer == 1)
            return mMediaPlayer;
        else
            return mMediaPlayer2;
    }

    /**
     * Sets the current active media player. Note that this
     * method does not modify the MediaPlayer objects in any
     * way. It simply changes the int variable that points to
     * the new current MediaPlayer object.
     */
    public void setCurrentMediaPlayer(int currentMediaPlayer) {
        mCurrentMediaPlayer = currentMediaPlayer;
    }

    /**
     * Returns the primary MediaPlayer object. Don't
     * use this method directly unless you have a good
     * reason to explicitly call mMediaPlayer. Use
     * getCurrentMediaPlayer() whenever possible.
     */
    public MediaPlayer getMediaPlayer() {
        return mMediaPlayer;
    }

    /**
     * Returns the secondary MediaPlayer object. Don't
     * use this method directly unless you have a good
     * reason to explicitly call mMediaPlayer2. Use
     * getCurrentMediaPlayer() whenever possible.
     */
    public MediaPlayer getMediaPlayer2() {
        return mMediaPlayer2;
    }

    /**
     * Indicates if mMediaPlayer is prepared and
     * ready for playback.
     */
    public boolean isMediaPlayerPrepared() {
        return mMediaPlayerPrepared;
    }

    /**
     * Indicates if mMediaPlayer2 is prepared and
     * ready for playback.
     */
    public boolean isMediaPlayer2Prepared() {
        return mMediaPlayer2Prepared;
    }

    /**
     * Indicates if music is currently playing.
     */
    public boolean isPlayingMusic() {
        try {
            return getCurrentMediaPlayer().isPlaying();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Returns an instance of SongHelper. This
     * object can be used to pull details about
     * the current song.
     */
    public SongHelper getCurrentSong() {
        if (getCurrentMediaPlayer() == mMediaPlayer) {
            return mMediaPlayerSongHelper;
        } else {
            return mMediaPlayer2SongHelper;
        }
    }

    /**
     * Sets the current MediaPlayer's SongHelper object. Also
     * indirectly calls the updateNotification()
     * methods via the [CURRENT SONG HELPER].setIsCurrentSong() method.
     */
    private void setCurrentSong(SongHelper songHelper) {
        if (getCurrentMediaPlayer() == mMediaPlayer) {
            mMediaPlayerSongHelper = songHelper;
            mMediaPlayerSongHelper.setIsCurrentSong();
        } else {
            mMediaPlayer2SongHelper = songHelper;
            mMediaPlayer2SongHelper.setIsCurrentSong();
        }
    }

    /**
     * Removes all crossfade callbacks on the current
     * Handler object. Also resets the volumes of the
     * MediaPlayer objects to 1.0f.
     */
    private void clearCrossfadeCallbacks() {
        if (mHandler == null)
            return;

        mHandler.removeCallbacks(startCrossFadeRunnable);
        mHandler.removeCallbacks(crossFadeRunnable);

        try {
            getMediaPlayer().setVolume(1.0f, 1.0f);
            getMediaPlayer2().setVolume(1.0f, 1.0f);
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }

    }

    /**
     * Sets mMediaPlayerSongHelper.
     */
    public void setMediaPlayerSongHelper(SongHelper songHelper) {
        mMediaPlayerSongHelper = songHelper;
    }

    /**
     * Sets mMediaPlayer2SongHelper.
     */
    public void setMediaPlayer2SongHelper(SongHelper songHelper) {
        mMediaPlayer2SongHelper = songHelper;
    }

    /**
     * Returns the service's cursor object.
     */
    public Cursor getCursor() {
        return mCursor;
    }

    /**
     * Replaces the current cursor object with the new one.
     */
    public void setCursor(Cursor cursor) {
        mCursor = cursor;
    }

    /**
     * Returns the list of playback indeces that are used
     * to traverse the cursor object.
     */
    public ArrayList<Integer> getPlaybackIndecesList() {
        return mPlaybackIndecesList;
    }

    /**
     * Returns the list of playback indeces that could
     * not be played.
     */
    public ArrayList<Integer> getFailedIndecesList() {
        return mFailedIndecesList;
    }

    /**
     * Returns the current value of mCurrentSongIndex.
     */
    public int getCurrentSongIndex() {
        return mCurrentSongIndex;
    }

    /**
     * Changes the value of mCurrentSongIndex.
     */
    public void setCurrentSongIndex(int currentSongIndex) {
        mCurrentSongIndex = currentSongIndex;
    }

    /**
     * Returns the mHandler object.
     */
    public Handler getHandler() {
        return mHandler;
    }

    /**
     * Returns point A in milliseconds for A-B repeat.
     */
    public int getRepeatSongRangePointA() {
        return mRepeatSongRangePointA;
    }

    /**
     * Returns point B in milliseconds for A-B repeat.
     */
    public int getRepeatSongRangePointB() {
        return mRepeatSongRangePointB;
    }

    /**
     * Returns the current repeat mode. The repeat mode
     * is determined based on the value that is saved in
     * SharedPreferences.
     */
    public int getRepeatMode() {
        return mApp.getSharedPreferences().getInt(Common.REPEAT_MODE, Common.REPEAT_PLAYLIST);
    }

    /**
     * Applies the specified repeat mode.
     */
    public void setRepeatMode(int repeatMode) {
        if (repeatMode == Common.REPEAT_PLAYLIST ||
                repeatMode == Common.REPEAT_SONG || repeatMode == Common.A_B_REPEAT) {
            //Save the repeat mode.
            mApp.getSharedPreferences().edit().putInt(Common.REPEAT_MODE, repeatMode).commit();
        } else {
            //Just in case a bogus value is passed in.
            mApp.getSharedPreferences().edit().putInt(Common.REPEAT_MODE, Common.REPEAT_PLAYLIST).commit();
        }

    	/*
         * Set the both MediaPlayer objects to loop if the repeat mode
    	 * is Common.REPEAT_SONG.
    	 */
        try {
            if (repeatMode == Common.REPEAT_SONG) {
                getMediaPlayer().setLooping(true);
                getMediaPlayer2().setLooping(true);
            } else {
                getMediaPlayer().setLooping(false);
                getMediaPlayer2().setLooping(false);
            }

            //Prepare the appropriate next song.
            prepareAlternateMediaPlayer();

        } catch (Exception e) {
            e.printStackTrace();
        }

        if (repeatMode != Common.A_B_REPEAT) {
            if (mHandler != null && mApp.isCrossfadeEnabled())
                mHandler.post(startCrossFadeRunnable);
        } else {
            clearCrossfadeCallbacks();
        }
    }

    /**
     * Indicates if shuffle mode is turned on or off.
     */
    public boolean isShuffleOn() {
        return mApp.getSharedPreferences().getBoolean(Common.SHUFFLE_ON, true);
    }

    /**
     * Indicates if mCurrentSongIndex points to the last
     * song in the current queue.
     */
    public boolean isAtEndOfQueue() {
        return (getCurrentSongIndex() == (getPlaybackIndecesList().size() - 1));
    }

    /**
     * Sets the prepared flag for mMediaPlayer.
     */
    public void setIsMediaPlayerPrepared(boolean prepared) {
        mMediaPlayerPrepared = prepared;
    }

    /**
     * Sets the prepared flag for mMediaPlayer2.
     */
    public void setIsMediaPlayer2Prepared(boolean prepared) {
        mMediaPlayer2Prepared = prepared;
    }

    /**
     * Returns true if there's only one song in the current queue.
     * False, otherwise.
     */
    public boolean isOnlySongInQueue() {
        return getCurrentSongIndex() == 0 && getCursor().getCount() == 1;

    }

    /**
     * Returns true if mCurrentSongIndex is pointing at the first
     * song in the queue and there is more than one song in the
     * queue. False, otherwise.
     */
    public boolean isFirstSongInQueue() {
        return getCurrentSongIndex() == 0 && getCursor().getCount() > 1;

    }

    /**
     * Returns true if mCurrentSongIndex is pointing at the last
     * song in the queue. False, otherwise.
     */
    public boolean isLastSongInQueue() {
        return getCurrentSongIndex() == (getCursor().getCount() - 1);
    }

    /**
     * Returns an instance of the PrepareServiceListener.
     */
    public PrepareServiceListener getPrepareServiceListener() {
        return mPrepareServiceListener;
    }

    /**
     * Sets the mPrepareServiceListener object.
     */
    public void setPrepareServiceListener(PrepareServiceListener listener) {
        mPrepareServiceListener = listener;
    }

    /**
     * (non-Javadoc)
     *
     * @see Service#onDestroy()
     */
    @Override
    public void onDestroy() {

        //Notify the UI that the service is about to stop.
        mApp.broadcastUpdateUICommand(new String[]{Common.SERVICE_STOPPING},
                new String[]{""});

        //Save the last track's info within the current queue.
        try {
            mApp.getSharedPreferences().edit().putLong("LAST_SONG_TRACK_POSITION", getCurrentMediaPlayer().getCurrentPosition());
        } catch (Exception ignored) {
            mApp.getSharedPreferences().edit().putLong("LAST_SONG_TRACK_POSITION", 0);
        }

        //If the current song is repeating a specific range, reset the repeat option.
        if (getRepeatMode() == Common.REPEAT_SONG || getRepeatMode() == Common.A_B_REPEAT) {
            setRepeatMode(Common.REPEAT_PLAYLIST);
        }

        mFadeInVolume = 0.0f;
        mFadeOutVolume = 1.0f;

        //Unregister the headset plug receiver and RemoteControlClient.
        try {
            RemoteControlHelper.unregisterRemoteControlClient(mAudioManager, mRemoteControlClientCompat);
            unregisterReceiver(mHeadsetPlugReceiver);
        } catch (Exception e) {
            //Just null out the receiver if it hasn't been registered yet.
            mHeadsetPlugReceiver = null;
        }

        //Remove the notification.
        NotificationManager notificationManager = (NotificationManager) this.getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancel(mNotificationId);

        if (mMediaPlayer != null)
            mMediaPlayer.release();

        if (mMediaPlayer2 != null)
            getMediaPlayer2().release();

        mMediaPlayer = null;
        mMediaPlayer2 = null;

        //Close the cursor(s).
        try {
            getCursor().close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        //Remove audio focus and unregister the audio buttons receiver.
        mAudioManagerHelper.setHasAudioFocus(false);
        mAudioManager.abandonAudioFocus(audioFocusChangeListener);
        mAudioManager.unregisterMediaButtonEventReceiver(new ComponentName(getPackageName(), HeadsetButtonsReceiver.class.getName()));
        mAudioManager = null;
        mMediaButtonReceiverComponent = null;
        mRemoteControlClientCompat = null;

        //Nullify the service object.
        mApp.setService(null);
        mApp.setIsServiceRunning(false);
        mApp = null;
    }

    /**
     * Public interface that provides access to
     * major events during the service startup
     * process.
     *
     * @author Arpit Gandhi
     */
    public interface PrepareServiceListener {
        /**
         * Called when the service is up and running.
         */
        void onServiceRunning(AudioPlaybackService service);
    }

    private class DownloadTask extends AsyncTask<String, Integer, Integer> {
        SongHelper songHelper;

        private DownloadTask(SongHelper songHelper) {
            this.songHelper = songHelper;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Log.d("", "Song Download Start: " + getCurrentSong().getTitle());
        }

        @Override
        protected void onPostExecute(Integer integer) {
            super.onPostExecute(integer);
            Log.d("", "Song Download Complete: " + getCurrentSong().getTitle());
        }

        @Override
        protected Integer doInBackground(String... sUrl) {
            InputStream input = null;
            OutputStream output = null;
            HttpURLConnection connection = null;
            try {
                URL url = new URL(sUrl[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();
                long fileLength = connection.getContentLength();
                HashMap<String, Long> map = new HashMap<>();
                if (Prefs.getSongFileSize(mContext) != null)
                    map = Prefs.getSongFileSize(mContext);
                map.put(songHelper.getId(), fileLength);
                Prefs.setSongFileSize(mContext, map);
                // download the file
                input = connection.getInputStream();
                output = new FileOutputStream(mContext.getFilesDir().getPath() + "/music/" + songHelper.getId() + ".mp3");

                byte data[] = new byte[4096];
                long total = 0;
                int count;
                while ((count = input.read(data)) != -1) {
                    // allow canceling with back button
                    if (isCancelled()) {
                        input.close();
                        return null;
                    }
                    total += count;
                    if (fileLength > 0) // only if total length is known
                        publishProgress((int) (total * 100 / fileLength));
                    output.write(data, 0, count);
                }
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            } finally {
                try {
                    if (output != null)
                        output.close();
                    if (input != null)
                        input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (connection != null)
                    connection.disconnect();
            }
            return null;
        }
    }
}