package com.madbeeapp.android.DBHelpers;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

/**
 * Helper class that contains methods that access
 * Android's MediaStore. For methods that access
 * madbee' private database, see DBAccessHelper.
 *
 * @author Arpit Gandhi
 */
public class MediaStoreAccessHelper {

    /* Hidden album artist field. See: http://stackoverflow.com/questions/20710542/
     * why-doesnt-mediastore-audio-albums-external-content-uri-provide-an-accurate-al
     */
    public static final String ALBUM_ARTIST = "album_artist";

    /**
     * Queries MediaStore and returns a cursor with songs limited
     * by the selection parameter.
     */
    public static Cursor getAllSongsWithSelection(Context context,
                                                  String selection,
                                                  String[] projection,
                                                  String sortOrder) {

        ContentResolver contentResolver = context.getContentResolver();
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        return contentResolver.query(uri, projection, selection, null, sortOrder);

    }

    /**
     * Queries MediaStore and returns a cursor with all songs.
     */
    public static Cursor getAllSongs(Context context, String projection[],
                                     String sortOrder) {

        ContentResolver contentResolver = context.getContentResolver();
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String selection = MediaStore.Audio.Media.IS_MUSIC + "!= 0";

        return contentResolver.query(uri, projection, selection, null, sortOrder);

    }
}
