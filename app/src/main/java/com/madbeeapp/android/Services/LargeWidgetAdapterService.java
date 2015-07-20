package com.madbeeapp.android.Services;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.madbeeapp.android.DBHelpers.DBAccessHelper;
import com.madbeeapp.android.R;
import com.madbeeapp.android.Utils.Common;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.assist.ImageSize;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;

public class LargeWidgetAdapterService extends RemoteViewsService {

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new StackRemoteViewsFactory(this.getApplicationContext(), intent);
    }

}

class StackRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {

    private Context mContext;
    private SharedPreferences sharedPreferences;
    private Cursor cursor;
    private Common mApp;
    private DisplayImageOptions displayImageOptions;
    private String mWidgetColor = "DARK";

    public StackRemoteViewsFactory(Context context, Intent intent) {
        mContext = context;
        mApp = (Common) mContext.getApplicationContext();

        sharedPreferences = context.getSharedPreferences("com.madbeeapp.android", Context.MODE_PRIVATE);
        cursor = mApp.getService().getCursor();
        mWidgetColor = intent.getStringExtra("WIDGET_COLOR");

        //Create a set of options to optimize the bitmap memory usage.
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        options.inJustDecodeBounds = false;

        //Display Image Options.
        displayImageOptions = new DisplayImageOptions.Builder()
                .showImageForEmptyUri(R.drawable.empty_art_padding)
                .showImageOnFail(R.drawable.empty_art_padding)
                .cacheInMemory(true)
                .decodingOptions(options)
                .imageScaleType(ImageScaleType.EXACTLY)
                .bitmapConfig(Bitmap.Config.RGB_565)
                .displayer(new FadeInBitmapDisplayer(400))
                .build();

    }

    @Override
    public int getCount() {
        if (cursor != null) {
            return cursor.getCount();
        } else {
            return 0;
        }

    }

    @Override
    public RemoteViews getViewAt(int position) {
        RemoteViews rv = new RemoteViews(mContext.getPackageName(), R.layout.large_widget_listview_layout);
        if (position <= getCount()) {

            try {
                if (mApp.getService().getPlaybackIndecesList() != null && mApp.getService().getPlaybackIndecesList().size() != 0) {
                    if (cursor.getCount() > mApp.getService().getPlaybackIndecesList().get(position)) {
                        cursor.moveToPosition(mApp.getService().getPlaybackIndecesList().get(position));
                    } else {
                        return null;
                    }

                } else {
                    return null;
                }
            } catch (Exception e) {
                return null;
            }

            //Set the song name, album, and artist fields.
            String songTitle = cursor.getString(cursor.getColumnIndex(DBAccessHelper.SONG_TITLE));
            String songAlbumArtPath = cursor.getString(cursor.getColumnIndex(DBAccessHelper.SONG_ALBUM_ART_PATH));
            ImageSize imageSize = new ImageSize(100, 100);

            //Set the duration of the song.
            long songDurationInMillis;
            try {
                songDurationInMillis = Long.parseLong(cursor.getString(cursor.getColumnIndex(DBAccessHelper.SONG_DURATION)));
            } catch (Exception e) {
                songDurationInMillis = 0;
            }

            rv.setTextViewText(R.id.widget_listview_song_name, songTitle);
            rv.setTextViewText(R.id.widget_listview_duration, convertMillisToMinsSecs(songDurationInMillis));

            if (mWidgetColor.equals("LIGHT")) {
                rv.setTextColor(R.id.widget_listview_song_name, Color.BLACK);
                rv.setTextColor(R.id.widget_listview_duration, Color.BLACK);
            }

            Bitmap bitmap = mApp.getImageLoader().loadImageSync(songAlbumArtPath, imageSize, displayImageOptions);
            rv.setImageViewBitmap(R.id.widget_listview_thumbnail, bitmap);

        }
        
        /* This intent latches itself onto the pendingIntentTemplate from 
         * LargeWidgetProvider.java and adds the subTitle "INDEX" argument to it. */
        Intent fillInIntent = new Intent();
        fillInIntent.putExtra("INDEX", position);
        rv.setOnClickFillInIntent(R.id.widget_listview_layout, fillInIntent);

        return rv;
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public void onDataSetChanged() {

        if (sharedPreferences.getBoolean("SERVICE_RUNNING", false)) {
            cursor = mApp.getService().getCursor();
        }

    }

    //Convert millisseconds to hh:mm:ss format.
    private String convertMillisToMinsSecs(long milliseconds) {

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

        if (hoursValue < 10) {
            hours = "0" + hoursValue;
        } else {
            hours = "" + hoursValue;
        }

        String output;

        if (hoursValue != 0) {
            output = hours + ":" + minutes + ":" + seconds;
        } else {
            output = minutes + ":" + seconds;
        }

        return output;
    }

    @Override
    public void onDestroy() {

    }

    @Override
    public void onCreate() {
    }

}
