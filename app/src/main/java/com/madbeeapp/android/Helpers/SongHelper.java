package com.madbeeapp.android.Helpers;

import android.content.ContentUris;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.MediaStore;

import com.madbeeapp.android.DBHelpers.DBAccessHelper;
import com.madbeeapp.android.R;
import com.madbeeapp.android.Utils.Common;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;
import com.squareup.picasso.Transformation;

public class SongHelper {

    private Uri mSongSourceUri;
    private SongHelper mSongHelper;
    private Common mApp;
    private boolean mIsCurrentSong = false;
    private boolean mIsAlbumArtLoaded = false;
    //Song parameters.
    private String mTitle;
    private String mArtist;
    private String mAlbum;
    private String mDuration;
    private String mFilePath;
    private String mGenre;
    private String mId;
    private String mAlbumArtPath;
    private String mSongLastPlayed;
    private String mSongLastModified;
    private String mSoundCloudAlbumArtPath;
    private String mPlayCount;
    private String mSource;
    private Bitmap mAlbumArt;
    private AlbumArtLoadedListener mAlbumArtLoadedListener;
    /**
     * Image loading listener to store the current song's album art.
     */
    Target imageLoadingTarget = new Target() {

        @Override
        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
            mIsAlbumArtLoaded = true;
            setAlbumArt(bitmap);
            if (getAlbumArtLoadedListener() != null)
                getAlbumArtLoadedListener().albumArtLoaded();

            if (mIsCurrentSong) {
                try {
                    mApp.getService().updateNotification(mSongHelper);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }

        @Override
        public void onBitmapFailed(Drawable errorDrawable) {
            setAlbumArt(null);
            onBitmapLoaded(mAlbumArt, null);
        }

        @Override
        public void onPrepareLoad(Drawable placeHolderDrawable) {
            mIsAlbumArtLoaded = false;
        }

    };

    public String getPlayCount() {
        return mPlayCount;
    }

    public void setPlayCount(String mPlayCount) {
        this.mPlayCount = mPlayCount;
    }

    public String getmSongLastModified() {
        return mSongLastModified;
    }

    public void setmSongLastModified(String mSongLastModified) {
        this.mSongLastModified = mSongLastModified;
    }

    public String getmSongLastPlayed() {
        return mSongLastPlayed;
    }

    public void setmSongLastPlayed(String mSongLastPlayed) {
        this.mSongLastPlayed = mSongLastPlayed;
    }

    public String getSoundCloudAlbumArtPath() {
        return mSoundCloudAlbumArtPath;
    }

    public void setSoundCloudAlbumArtPath(String mSoundCloudAlbumArtPath) {
        this.mSoundCloudAlbumArtPath = mSoundCloudAlbumArtPath;
    }

    public Uri getSongSourceUri() {
        return mSongSourceUri;
    }

    public void setSongSourceUri(Uri mSongSourceUri) {
        this.mSongSourceUri = mSongSourceUri;
    }

    /**
     * Moves the specified cursor to the specified index and populates this
     * helper object with new song data.
     *
     * @param context             Context used to get a new Common object.
     * @param index               The index of the song.
     * @param albumArtTransformer The transformer to apply to the album art bitmap;
     */
    public void populateSongData(Context context, int index, Transformation albumArtTransformer) {

        mSongHelper = this;
        mApp = (Common) context.getApplicationContext();
        if (mApp.isServiceRunning()) {
            if (mApp.getService().getCursor().moveToFirst()) {
                mApp.getService().getCursor().moveToPosition(mApp.getService().getPlaybackIndecesList().get(index));

                this.setId(mApp.getService().getCursor().getString(getIdColumnIndex()));
                this.setTitle(mApp.getService().getCursor().getString(getTitleColumnIndex()));
                this.setAlbum(mApp.getService().getCursor().getString(getAlbumColumnIndex()));
                this.setArtist(mApp.getService().getCursor().getString(getArtistColumnIndex()));
                this.setAlbumArtist();
                this.setGenre(determineGenreName());
                this.setDuration(determineDuration());
                this.setSoundCloudAlbumArtPath(determineSoundCloudAlbumArtPath());
                this.setmSongLastModified(determineSongLastModified());
                this.setmSongLastPlayed(determineSongLastPlayed());
                this.setFilePath(mApp.getService().getCursor().getString(getFilePathColumnIndex()));
                this.setAlbumArtPath(determineAlbumArtPath());
                this.setSource(determineSongSource());
                this.setPlayCount(determineSongPlayCount());
                mApp.getPicasso()
                        .load(getAlbumArtPath())
                        .error(R.drawable.empty_art_padding)
                        .transform(albumArtTransformer)
                        .placeholder(R.drawable.empty_art_padding)
                        .into(imageLoadingTarget);
            }
        }
    }

    /**
     * Moves the specified cursor to the specified index and populates this
     * helper object with new song data.
     *
     * @param context Context used to get a new Common object.
     * @param index   The index of the song.
     */
    public void populateSongData(Context context, int index) {

        mSongHelper = this;

        mApp = (Common) context.getApplicationContext();
        if (index == -1)
            index = 0;
        if (mApp.isServiceRunning()) {
            if (mApp.getService().getCursor().moveToFirst()) {
                mApp.getService().getCursor().moveToPosition(mApp.getService().getPlaybackIndecesList().get(index));

                this.setId(mApp.getService().getCursor().getString(getIdColumnIndex()));
                this.setTitle(mApp.getService().getCursor().getString(getTitleColumnIndex()));
                this.setAlbum(mApp.getService().getCursor().getString(getAlbumColumnIndex()));
                this.setArtist(mApp.getService().getCursor().getString(getArtistColumnIndex()));
                this.setAlbumArtist();
                this.setGenre(determineGenreName());
                this.setDuration(determineDuration());
                this.setmSongLastModified(determineSongLastModified());
                this.setmSongLastPlayed(determineSongLastPlayed());
                this.setSoundCloudAlbumArtPath(determineSoundCloudAlbumArtPath());
                this.setFilePath(mApp.getService().getCursor().getString(getFilePathColumnIndex()));
                this.setAlbumArtPath(determineAlbumArtPath());
                this.setSource(determineSongSource());
                this.setPlayCount(determineSongPlayCount());

                mApp.getPicasso()
                        .load(getAlbumArtPath())
                        .error(R.drawable.empty_art_padding)
                        .placeholder(R.drawable.empty_art_padding)
                        .into(imageLoadingTarget);
            }
        }
    }

    /**
     * Sets this helper object as the current song. This method
     * will check if the song's album art has already been loaded.
     * If so, the updateNotification() and updateWidget() methods
     * will be called. If not, they'll be called as soon as the
     * album art is loaded.
     */
    public void setIsCurrentSong() {
        mIsCurrentSong = true;
        //The album art has already been loaded.
        if (mIsAlbumArtLoaded) {
            mApp.getService().updateNotification(mSongHelper);
        }
    }

    private int getIdColumnIndex() {
        if (mApp.getService().getCursor().getColumnIndex(MediaStore.Audio.Media.IS_MUSIC) == -1) {
            //We're dealing with madbee' internal DB schema.
            return mApp.getService().getCursor().getColumnIndex(DBAccessHelper.SONG_ID);
        } else {
            String isMusicColName = MediaStore.Audio.Media.IS_MUSIC;
            int isMusicColumnIndex = mApp.getService().getCursor().getColumnIndex(isMusicColName);

            //Check if the current row is from madbee' internal DB schema or MediaStore.
            if (mApp.getService().getCursor().getString(isMusicColumnIndex).isEmpty())
                //We're dealing with madbee' internal DB schema.
                return mApp.getService().getCursor().getColumnIndex(DBAccessHelper.SONG_ID);
            else
                //The current row is from MediaStore's DB schema.
                return mApp.getService().getCursor().getColumnIndex(MediaStore.Audio.Media._ID);

        }

    }

    private int getFilePathColumnIndex() {
        if (mApp.getService().getCursor().getColumnIndex(MediaStore.Audio.Media.IS_MUSIC) == -1) {
            //We're dealing with madbee' internal DB schema.
            return mApp.getService().getCursor().getColumnIndex(DBAccessHelper.SONG_FILE_PATH);
        } else {
            String isMusicColName = MediaStore.Audio.Media.IS_MUSIC;
            int isMusicColumnIndex = mApp.getService().getCursor().getColumnIndex(isMusicColName);

            //Check if the current row is from madbee' internal DB schema or MediaStore.
            if (mApp.getService().getCursor().getString(isMusicColumnIndex).isEmpty())
                //We're dealing with madbee' internal DB schema.
                return mApp.getService().getCursor().getColumnIndex(DBAccessHelper.SONG_FILE_PATH);
            else
                //The current row is from MediaStore's DB schema.
                return mApp.getService().getCursor().getColumnIndex(MediaStore.Audio.Media.DATA);

        }

    }

    private int getTitleColumnIndex() {
        if (mApp.getService().getCursor().getColumnIndex(MediaStore.Audio.Media.IS_MUSIC) == -1) {
            //We're dealing with madbee' internal DB schema.
            return mApp.getService().getCursor().getColumnIndex(DBAccessHelper.SONG_TITLE);
        } else {
            String isMusicColName = MediaStore.Audio.Media.IS_MUSIC;
            int isMusicColumnIndex = mApp.getService().getCursor().getColumnIndex(isMusicColName);

            //Check if the current row is from madbee' internal DB schema or MediaStore.
            if (mApp.getService().getCursor().getString(isMusicColumnIndex).isEmpty())
                //We're dealing with madbee' internal DB schema.
                return mApp.getService().getCursor().getColumnIndex(DBAccessHelper.SONG_TITLE);
            else
                //The current row is from MediaStore's DB schema.
                return mApp.getService().getCursor().getColumnIndex(MediaStore.Audio.Media.TITLE);
        }

    }

    private int getArtistColumnIndex() {
        if (mApp.getService().getCursor().getColumnIndex(MediaStore.Audio.Media.IS_MUSIC) == -1) {
            //We're dealing with madbee' internal DB schema.
            return mApp.getService().getCursor().getColumnIndex(DBAccessHelper.SONG_ARTIST);
        } else {
            String isMusicColName = MediaStore.Audio.Media.IS_MUSIC;
            int isMusicColumnIndex = mApp.getService().getCursor().getColumnIndex(isMusicColName);

            //Check if the current row is from madbee' internal DB schema or MediaStore.
            if (mApp.getService().getCursor().getString(isMusicColumnIndex).isEmpty())
                //We're dealing with madbee' internal DB schema.
                return mApp.getService().getCursor().getColumnIndex(DBAccessHelper.SONG_ARTIST);
            else
                //The current row is from MediaStore's DB schema.
                return mApp.getService().getCursor().getColumnIndex(MediaStore.Audio.Media.ARTIST);
        }
    }

    private int getAlbumColumnIndex() {
        if (mApp.getService().getCursor().getColumnIndex(MediaStore.Audio.Media.IS_MUSIC) == -1) {
            //We're dealing with madbee' internal DB schema.
            return mApp.getService().getCursor().getColumnIndex(DBAccessHelper.SONG_ALBUM);
        } else {
            String isMusicColName = MediaStore.Audio.Media.IS_MUSIC;
            int isMusicColumnIndex = mApp.getService().getCursor().getColumnIndex(isMusicColName);

            //Check if the current row is from madbee' internal DB schema or MediaStore.
            if (mApp.getService().getCursor().getString(isMusicColumnIndex).isEmpty())
                //We're dealing with madbee' internal DB schema.
                return mApp.getService().getCursor().getColumnIndex(DBAccessHelper.SONG_ALBUM);
            else
                //The current row is from MediaStore's DB schema.
                return mApp.getService().getCursor().getColumnIndex(MediaStore.Audio.Media.ALBUM);
        }
    }

    private String determineGenreName() {
        if (mApp.getService().getCursor().getColumnIndex(MediaStore.Audio.Media.IS_MUSIC) == -1) {
            //We're dealing with madbee' internal DB schema.
            int colIndex = mApp.getService().getCursor().getColumnIndex(DBAccessHelper.SONG_GENRE);
            return mApp.getService().getCursor().getString(colIndex);
        } else {
            String isMusicColName = MediaStore.Audio.Media.IS_MUSIC;
            int isMusicColumnIndex = mApp.getService().getCursor().getColumnIndex(isMusicColName);

            //Check if the current row is from madbee' internal DB schema or MediaStore.
            if (mApp.getService().getCursor().getString(isMusicColumnIndex).isEmpty()) {
                //We're dealing with madbee' internal DB schema.
                int colIndex = mApp.getService().getCursor().getColumnIndex(DBAccessHelper.SONG_GENRE);
                return mApp.getService().getCursor().getString(colIndex);

            } else {
                //The current row is from MediaStore's DB schema.
                return ""; //We're not using the genres field for now, so we'll leave it blank.
            }
        }
    }

    private String determineAlbumArtPath() {
        if (mApp.getService().getCursor().getColumnIndex(MediaStore.Audio.Media.IS_MUSIC) == -1) {
            //We're dealing with madbee' internal DB schema.
            int colIndex = mApp.getService().getCursor().getColumnIndex(DBAccessHelper.SONG_ALBUM_ART_PATH);
            return mApp.getService().getCursor().getString(colIndex);
        } else {
            String isMusicColName = MediaStore.Audio.Media.IS_MUSIC;
            int isMusicColumnIndex = mApp.getService().getCursor().getColumnIndex(isMusicColName);
            //Check if the current row is from madbee' internal DB schema or MediaStore.
            if (mApp.getService().getCursor().getString(isMusicColumnIndex).isEmpty()) {
                //We're dealing with madbee' internal DB schema.
                int colIndex = mApp.getService().getCursor().getColumnIndex(DBAccessHelper.SONG_ALBUM_ART_PATH);
                return mApp.getService().getCursor().getString(colIndex);
            } else {
                //The current row is from MediaStore's DB schema.
                final Uri ART_CONTENT_URI = Uri.parse("content://media/external/audio/albumart");
                int albumIdColIndex = mApp.getService().getCursor().getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);
                long albumId = mApp.getService().getCursor().getLong(albumIdColIndex);
                return ContentUris.withAppendedId(ART_CONTENT_URI, albumId).toString();
            }
        }
    }

    private String determineDuration() {
        if (mApp.getService().getCursor().getColumnIndex(MediaStore.Audio.Media.IS_MUSIC) == -1) {
            //We're dealing with madbee' internal DB schema.
            int colIndex = mApp.getService().getCursor().getColumnIndex(DBAccessHelper.SONG_DURATION);
            return mApp.getService().getCursor().getString(colIndex);
        } else {
            String isMusicColName = MediaStore.Audio.Media.IS_MUSIC;
            int isMusicColumnIndex = mApp.getService().getCursor().getColumnIndex(isMusicColName);

            //Check if the current row is from madbee' internal DB schema or MediaStore.
            if (mApp.getService().getCursor().getString(isMusicColumnIndex).isEmpty()) {
                //We're dealing with madbee' internal DB schema.
                int colIndex = mApp.getService().getCursor().getColumnIndex(DBAccessHelper.SONG_DURATION);
                return mApp.getService().getCursor().getString(colIndex);

            } else {
                //The current row is from MediaStore's DB schema.
                int durationColIndex = mApp.getService().getCursor().getColumnIndex(MediaStore.Audio.Media.DURATION);
                long duration = mApp.getService().getCursor().getLong(durationColIndex);

                return mApp.convertMillisToMinsSecs(duration);

            }

        }

    }

    private String determineSongLastPlayed() {
        return mApp.getService().getCursor().getString(mApp.getService().getCursor().getColumnIndex(DBAccessHelper.LAST_PLAYED_TIMESTAMP));
    }

    private String determineSongLastModified() {
        return mApp.getService().getCursor().getString(mApp.getService().getCursor().getColumnIndex(DBAccessHelper.SONG_LAST_MODIFIED));
    }

    private String determineSoundCloudAlbumArtPath() {
        return mApp.getService().getCursor().getString(mApp.getService().getCursor().getColumnIndex(DBAccessHelper.SONG_SOUNDCLOUD_ALBUM_ART_PATH));
    }

    private String determineSongSource() {
        return mApp.getService().getCursor().getString(mApp.getService().getCursor().getColumnIndex(DBAccessHelper.SONG_SOURCE));
    }

    private String determineSongPlayCount() {
        return mApp.getService().getCursor().getString(mApp.getService().getCursor().getColumnIndex(DBAccessHelper.SONG_PLAY_COUNT));
    }

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String title) {
        mTitle = title;
    }

    public String getArtist() {
        return mArtist;
    }

    public void setArtist(String artist) {
        mArtist = artist;
    }

    public String getAlbum() {
        return mAlbum;
    }

    public void setAlbum(String album) {
        mAlbum = album;
    }

    public void setAlbumArtist() {
    }

    public String getDuration() {
        return mDuration;
    }

    public void setDuration(String duration) {
        mDuration = duration;
    }

    public String getFilePath() {
        return mFilePath;
    }

    public void setFilePath(String filePath) {
        mFilePath = filePath;
    }

    public Bitmap getAlbumArt() {
        return mAlbumArt;
    }

    public void setAlbumArt(Bitmap albumArt) {
        mAlbumArt = albumArt;
    }

    public String getGenre() {
        return mGenre;
    }

    public void setGenre(String genre) {
        mGenre = genre;
    }

    public String getId() {
        return mId;
    }

    public void setId(String id) {
        mId = id;
    }

    public String getAlbumArtPath() {
        return mAlbumArtPath;
    }

    public void setAlbumArtPath(String albumArtPath) {
        mAlbumArtPath = albumArtPath;
    }

    public String getSource() {
        return mSource;
    }

    public void setSource(String source) {
        mSource = source;
    }

    public AlbumArtLoadedListener getAlbumArtLoadedListener() {
        return mAlbumArtLoadedListener;
    }

    public void setAlbumArtLoadedListener(AlbumArtLoadedListener listener) {
        mAlbumArtLoadedListener = listener;
    }

    /**
     * Interface that provides callbacks to the provided listener
     * once the song's album art has been loaded.
     */
    public interface AlbumArtLoadedListener {

        /**
         * Called once the album art bitmap is ready for use.
         */
        void albumArtLoaded();
    }

}
