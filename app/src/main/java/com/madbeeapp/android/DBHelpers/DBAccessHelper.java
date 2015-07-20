package com.madbeeapp.android.DBHelpers;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.madbeeapp.android.Utils.Common;

import java.io.File;
import java.util.ArrayList;

public class DBAccessHelper extends SQLiteOpenHelper {

    //Common fields.
    public static final String _ID = "_id";
    //Music folders table.
    public static final String MUSIC_FOLDERS_TABLE = "MusicFoldersTable";
    public static final String FOLDER_PATH = "folder_path";
    public static final String INCLUDE = "include";
    //Music library table.
    public static final String MUSIC_LIBRARY_TABLE = "MusicLibraryTable";
    public static final String SONG_ID = "song_id";
    public static final String SONG_TITLE = "title";
    public static final String SONG_ARTIST = "artist";
    public static final String SONG_ALBUM = "album";
    public static final String SONG_ALBUM_ARTIST = "album_artist";
    public static final String SONG_DURATION = "duration";
    public static final String SONG_FILE_PATH = "file_path";
    public static final String SONG_TRACK_NUMBER = "track_number";
    public static final String SONG_GENRE = "genre";
    public static final String SONG_PLAY_COUNT = "play_count";
    public static final String SONG_YEAR = "year";
    public static final String SONG_LAST_MODIFIED = "last_modified";
    public static final String ADDED_TIMESTAMP = "added_timestamp";
    public static final String LAST_PLAYED_TIMESTAMP = "last_played_timestamp";
    public static final String SONG_SOURCE = "source";
    public static final String SONG_ALBUM_ART_PATH = "album_art_path";
    public static final String SONG_SOUNDCLOUD_ALBUM_ART_PATH = "soundcloud_album_art_path";
    public static final String ARTIST_ART_LOCATION = "artist_art_location";
    //Firends library table..
    public static final String FRIENDS_LIBRARY_TABLE = "FriendsLibraryTable";
    public static final String CONTACT_NUMBER = "contact_number";
    //Playlist fields.
    public static final String PLAYLIST_TABLE = "PlaylistTable";
    public static final String PLAYLIST_NAME = "playlist_name";
    public static final String PLAYLIST_ADDED_TIME = "playlist_added_time";
    public static final String PLAYLIST_PLAY_COUNT = "playlist_play_count";

    //Song source values.
    public static final String SOUNDCLOUD = "soundcloud";
    public static final String LOCAL = "local";
    //Database Version.
    private static final int DATABASE_VERSION = 1;
    //Database Name.
    private static final String DATABASE_NAME = "madbee.db";
    //Database instance. Will last for the lifetime of the application.
    private static DBAccessHelper sInstance;
    //Writable database instance.
    private SQLiteDatabase mDatabase;
    //Commmon utils object.
    private Common mApp;

    public DBAccessHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        mApp = (Common) context.getApplicationContext();
    }

    /**
     * Returns a singleton instance for the database.
     */
    public static synchronized DBAccessHelper getInstance(Context context) {
        if (sInstance == null)
            sInstance = new DBAccessHelper(context.getApplicationContext());
        return sInstance;
    }

    /**
     * Returns a writable instance of the database. Provides an additional
     * null check for additional stability.
     */
    private synchronized SQLiteDatabase getDatabase() {
        if (mDatabase == null)
            mDatabase = getWritableDatabase();
        return mDatabase;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        //Music folders table.
        String[] musicFoldersTableCols = {FOLDER_PATH, INCLUDE};
        String[] musicFoldersTableColTypes = {"TEXT", "TEXT"};
        String createMusicFoldersTable = buildCreateStatement(MUSIC_FOLDERS_TABLE,
                musicFoldersTableCols,
                musicFoldersTableColTypes);

        //Music library table.
        String[] musicLibraryTableCols = {SONG_ID, SONG_TITLE, SONG_ARTIST,
                SONG_ALBUM, SONG_ALBUM_ARTIST,
                SONG_DURATION, SONG_FILE_PATH,
                SONG_TRACK_NUMBER, SONG_GENRE,
                SONG_PLAY_COUNT, SONG_YEAR,
                SONG_SOUNDCLOUD_ALBUM_ART_PATH,
                SONG_LAST_MODIFIED, ADDED_TIMESTAMP, LAST_PLAYED_TIMESTAMP,
                SONG_SOURCE, SONG_ALBUM_ART_PATH,
                ARTIST_ART_LOCATION};

        String[] musicLibraryTableColTypes = new String[musicLibraryTableCols.length];
        for (int i = 0; i < musicLibraryTableCols.length; i++)
            musicLibraryTableColTypes[i] = "TEXT";

        String createMusicLibraryTable = buildCreateStatement(MUSIC_LIBRARY_TABLE,
                musicLibraryTableCols,
                musicLibraryTableColTypes);

        //Friends library table.
        String[] friendsLibraryTableCols = {CONTACT_NUMBER, SONG_ID, SONG_TITLE, SONG_ARTIST,
                SONG_ALBUM, SONG_ALBUM_ARTIST,
                SONG_DURATION, SONG_FILE_PATH,
                SONG_TRACK_NUMBER, SONG_GENRE,
                SONG_PLAY_COUNT, SONG_YEAR,
                SONG_SOUNDCLOUD_ALBUM_ART_PATH,
                SONG_LAST_MODIFIED, ADDED_TIMESTAMP, LAST_PLAYED_TIMESTAMP,
                SONG_SOURCE, SONG_ALBUM_ART_PATH,
                ARTIST_ART_LOCATION};

        String[] friendsLibraryTableColTypes = new String[friendsLibraryTableCols.length];
        for (int i = 0; i < friendsLibraryTableCols.length; i++)
            friendsLibraryTableColTypes[i] = "TEXT";

        String createFriendsLibraryTable = buildCreateFriendStatement(FRIENDS_LIBRARY_TABLE,
                friendsLibraryTableCols,
                friendsLibraryTableColTypes);

        //Friends library table.
        String[] playlistTableCols = {PLAYLIST_NAME, PLAYLIST_ADDED_TIME, PLAYLIST_PLAY_COUNT, SONG_ID, SONG_TITLE, SONG_ARTIST,
                SONG_ALBUM, SONG_ALBUM_ARTIST,
                SONG_DURATION, SONG_FILE_PATH,
                SONG_TRACK_NUMBER, SONG_GENRE,
                SONG_PLAY_COUNT, SONG_YEAR, SONG_SOUNDCLOUD_ALBUM_ART_PATH,
                SONG_LAST_MODIFIED, ADDED_TIMESTAMP,
                LAST_PLAYED_TIMESTAMP, SONG_SOURCE, SONG_ALBUM_ART_PATH,
                ARTIST_ART_LOCATION};

        String[] playlistTableColTypes = new String[playlistTableCols.length];
        for (int i = 0; i < playlistTableCols.length; i++)
            playlistTableColTypes[i] = "TEXT";

        String createPlaylistLibraryTable = buildCreateStatement(PLAYLIST_TABLE,
                playlistTableCols,
                playlistTableColTypes);
        //Execute the CREATE statements.
        db.execSQL(createMusicFoldersTable);
        db.execSQL(createMusicLibraryTable);
        db.execSQL(createFriendsLibraryTable);
        db.execSQL(createPlaylistLibraryTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    @Override
    protected void finalize() {
        try {
            getDatabase().close();
            super.finalize();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }

    }

    /**
     * Constructs a fully formed CREATE statement using the input
     * parameters.
     */

    private String buildCreateFriendStatement(String tableName, String[] columnNames, String[] columnTypes) {
        String createStatement = "";
        if (columnNames.length == columnTypes.length) {
            createStatement += "CREATE TABLE IF NOT EXISTS " + tableName + "("
                    + _ID + " INTEGER PRIMARY KEY, ";

            for (int i = 0; i < columnNames.length; i++) {

                if (i == columnNames.length - 1) {
                    createStatement += columnNames[i]
                            + " "
                            + columnTypes[i]
                            + ", UNIQUE (" + SONG_ID + "," + CONTACT_NUMBER + ") ON CONFLICT REPLACE)";
                } else {
                    createStatement += columnNames[i]
                            + " "
                            + columnTypes[i]
                            + ", ";
                }
            }
        }
        Log.e("", "" + createStatement);
        return createStatement;
    }

    /**
     * Constructs a fully formed CREATE statement using the input
     * parameters.
     */

    private String buildCreateStatement(String tableName, String[] columnNames, String[] columnTypes) {
        String createStatement = "";
        if (columnNames.length == columnTypes.length) {
            createStatement += "CREATE TABLE IF NOT EXISTS " + tableName + "("
                    + _ID + " INTEGER PRIMARY KEY, ";

            for (int i = 0; i < columnNames.length; i++) {

                if (i == columnNames.length - 1) {
                    createStatement += columnNames[i]
                            + " "
                            + columnTypes[i]
                            + ")";
                } else {
                    createStatement += columnNames[i]
                            + " "
                            + columnTypes[i]
                            + ", ";
                }
            }
        }
        return createStatement;
    }
    /***********************************************************
     * PLAYLIST TABLE METHODS.
     ***********************************************************/
    /**
     * Returns a cursor with all songs of playlist in the table.
     */
    public Cursor getAllPlaylistSongs(String selection) {
        String selectQuery;
        selectQuery = "SELECT  * FROM " + PLAYLIST_TABLE + " WHERE " + selection;
        return getDatabase().rawQuery(selectQuery, null);
    }

    /**
     * Returns a arraylist of strings with all distinct playlist names in the table.
     */
    public ArrayList<String> getPlaylistNames() {
        ArrayList<String> names = new ArrayList<>();
        String selectQuery = "SELECT  DISTINCT " + PLAYLIST_NAME + " AS " + PLAYLIST_NAME + " FROM " + PLAYLIST_TABLE;
        Cursor c = getDatabase().rawQuery(selectQuery, null);
        if (c.moveToFirst()) {
            do {
                names.add(c.getString(c.getColumnIndex(PLAYLIST_NAME)));
            } while (c.moveToNext());
        }

        if (!names.contains("Trash")) {
            names.add("Trash");
        } else {
            names.remove("Trash");
            names.add("Trash");
        }
        return names;
    }

    /**
     * Returns a cursor with all songs of playlist in the table.
     */
    public Cursor getAllFriendsSongs(String selection) {
        String selectQuery;
        selectQuery = "SELECT  * FROM " + PLAYLIST_TABLE + " WHERE " + selection;
        return getDatabase().rawQuery(selectQuery, null);
    }

    /**
     * Returns a boolean value with result if playlist exists in the table.
     */
    public boolean checkIfPlaylistExists(String playlistName) {
        String selectQuery = "SELECT  *  FROM " + PLAYLIST_TABLE + " WHERE " + PLAYLIST_NAME + "='" + playlistName.replace("'", "''") + "'";
        Cursor c = getDatabase().rawQuery(selectQuery, null);
        return c.getCount() > 0;
    }

    /**
     * Returns a boolean value with result if playlist exists in the table.
     */
    public boolean checkIfSongIsPresentPlaylist(String playlistName, String filePath) {
        String selectQuery = "SELECT  *  FROM " + PLAYLIST_TABLE + " WHERE " + PLAYLIST_NAME + "='" + playlistName.replace("'", "''")
                + "' AND " + SONG_FILE_PATH + "='" + filePath.replace("'", "''") + "'";
        Cursor c = getDatabase().rawQuery(selectQuery, null);
        return c.getCount() > 0;
    }

    /**
     * Returns a boolean value with result if friend exists in the table.
     */
    public boolean checkIfSongIsPresentInFriendList(String contactNumber, String filePath) {
        String selectQuery = "SELECT  *  FROM " + FRIENDS_LIBRARY_TABLE + " WHERE " + CONTACT_NUMBER + "='" + contactNumber
                + "' AND " + SONG_FILE_PATH + "='" + filePath.replace("'", "''") + "'";
        Cursor c = getDatabase().rawQuery(selectQuery, null);
        return c.getCount() > 0;
    }

    /**
     * Deletes all old trash files .
     */
    public void deleteAllOldTrashFiles() {
        long daysInMiliSec = System.currentTimeMillis() - 2 * (24L * 60L * 60L * 1000L);
        String query = "SELECT * FROM " + PLAYLIST_TABLE + " WHERE " + PLAYLIST_ADDED_TIME + "<='" + daysInMiliSec + "' AND " + PLAYLIST_NAME + "='Trash'";
        Cursor c = getDatabase().rawQuery(query, null);
        if (c.moveToFirst()) {
            do {
                String filePath = c.getString(c.getColumnIndex(SONG_FILE_PATH));
                File file = new File(filePath);
                if (file.exists()) {
                    file.delete();
                }
                deleteSong(filePath);
            } while (c.moveToNext());
        }
    }

    /**
     * Deletes all old trash files .
     */
    public void deleteAllTrashFiles() {
        String query = "SELECT * FROM " + PLAYLIST_TABLE + " WHERE " + PLAYLIST_NAME + "='Trash'";
        Cursor c = getDatabase().rawQuery(query, null);
        if (c.moveToFirst()) {
            do {
                String filePath = c.getString(c.getColumnIndex(SONG_FILE_PATH));
                File file = new File(filePath);
                if (file.exists()) {
                    file.delete();
                }
                deleteSong(filePath);
                deleteSongFromPlaylist(filePath, "Trash");
            } while (c.moveToNext());
        }
    }

    /**
     * Adds playlist song entry to the database.
     */
    public void addPlaylistEntry(String playlistName,
                                 Cursor cursor) {
        ContentValues values = new ContentValues();
        values.put(SONG_TITLE, cursor.getString(cursor.getColumnIndex(SONG_TITLE)));
        values.put(SONG_ARTIST, cursor.getString(cursor.getColumnIndex(SONG_ARTIST)));
        values.put(SONG_ALBUM, cursor.getString(cursor.getColumnIndex(SONG_ALBUM)));
        values.put(SONG_ALBUM_ARTIST, cursor.getString(cursor.getColumnIndex(SONG_ALBUM_ARTIST)));
        values.put(SONG_DURATION, cursor.getString(cursor.getColumnIndex(SONG_DURATION)));
        values.put(SONG_FILE_PATH, cursor.getString(cursor.getColumnIndex(SONG_FILE_PATH)));
        values.put(SONG_TRACK_NUMBER, cursor.getString(cursor.getColumnIndex(SONG_TRACK_NUMBER)));
        values.put(SONG_GENRE, cursor.getString(cursor.getColumnIndex(SONG_GENRE)));
        values.put(SONG_YEAR, cursor.getString(cursor.getColumnIndex(SONG_YEAR)));
        values.put(SONG_ALBUM_ART_PATH, cursor.getString(cursor.getColumnIndex(SONG_ALBUM_ART_PATH)));
        values.put(SONG_LAST_MODIFIED, cursor.getString(cursor.getColumnIndex(SONG_LAST_MODIFIED)));
        values.put(SONG_ALBUM_ART_PATH, cursor.getString(cursor.getColumnIndex(SONG_ALBUM_ART_PATH)));
        values.put(ADDED_TIMESTAMP, cursor.getString(cursor.getColumnIndex(ADDED_TIMESTAMP)));
        values.put(LAST_PLAYED_TIMESTAMP, cursor.getString(cursor.getColumnIndex(LAST_PLAYED_TIMESTAMP)));
        values.put(SONG_SOURCE, cursor.getString(cursor.getColumnIndex(SONG_SOURCE)));
        values.put(SONG_ID, cursor.getString(cursor.getColumnIndex(SONG_ID)));
        values.put(SONG_PLAY_COUNT, cursor.getString(cursor.getColumnIndex(SONG_PLAY_COUNT)));

        values.put(PLAYLIST_NAME, playlistName.replace("'", "''"));
        values.put(PLAYLIST_ADDED_TIME, System.currentTimeMillis());
        values.put(PLAYLIST_PLAY_COUNT, "0");
        getDatabase().insert(PLAYLIST_TABLE, null, values);
    }

    /**
     * Convert millisseconds to hh:mm:ss format.
     *
     * @param milliseconds The input time in milliseconds to format.
     * @return The formatted time string.
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

        String output;
        if (hoursValue != 0) {
            minutes = "0" + minutesValue;
            hours = "" + hoursValue;
            output = hours + ":" + minutes + ":" + seconds;
        } else {
            minutes = "" + minutesValue;
            output = minutes + ":" + seconds;
        }
        return output;
    }

    /***********************************************************
     * MUSIC FOLDERS TABLE METHODS.
     ***********************************************************/

    /**
     * Deletes all music folders from the table.
     */
    public void deleteAllMusicFolderPaths() {
        getDatabase().delete(MUSIC_FOLDERS_TABLE, null, null);
    }

    /**
     * Deletes all old soundcloud files .
     */
    public void deleteAllOldSoundcloudFiles() {
        long daysInMiliSec = System.currentTimeMillis() - 15 * (24L * 60L * 60L * 1000L);
        String query = "SELECT * FROM " + MUSIC_LIBRARY_TABLE + " WHERE " + SONG_SOURCE + "='" + SOUNDCLOUD
                + "' AND " + LAST_PLAYED_TIMESTAMP + "<='" + daysInMiliSec + "' AND " + LAST_PLAYED_TIMESTAMP + " != " + SONG_LAST_MODIFIED;
        Cursor c = getDatabase().rawQuery(query, null);
        if (c.moveToFirst()) {
            do {
                String filePath = c.getString(c.getColumnIndex(SONG_FILE_PATH));
                File file = new File(filePath);
                if (file.exists()) {
                    file.delete();
                }
            } while (c.moveToNext());
        }
    }

    /**
     * Deletes all 5 days and 1 count soundcloud files .
     */
    public void deleteAllNonPlayedSoundcloudFiles() {
        long daysInMiliSec = System.currentTimeMillis() - 5 * (24L * 60L * 60L * 1000L);
        String query = "SELECT * FROM " + MUSIC_LIBRARY_TABLE + " WHERE " + SONG_SOURCE + "='" + SOUNDCLOUD
                + "' AND " + LAST_PLAYED_TIMESTAMP + "<='" + daysInMiliSec + "' AND " + LAST_PLAYED_TIMESTAMP + " != " + SONG_LAST_MODIFIED;
        Cursor c = getDatabase().rawQuery(query, null);
        if (c.moveToFirst()) {
            do {
                String filePath = c.getString(c.getColumnIndex(SONG_FILE_PATH));
                String songCount = c.getString(c.getColumnIndex(SONG_PLAY_COUNT));
                int count = Integer.parseInt(songCount);
                if (filePath != null)
                    if (count < 2) {
                        File file = new File(filePath);
                        if (file.exists()) {
                            file.delete();
                        }
                    }
            } while (c.moveToNext());
        }
    }

    /**
     * Returns a cursor with all music folder paths in the table.
     */
    public Cursor getAllMusicFolderPaths() {
        String selectQuery = "SELECT  * FROM " + MUSIC_FOLDERS_TABLE
                + " ORDER BY " + INCLUDE + "*1 DESC";
        return getDatabase().rawQuery(selectQuery, null);
    }

    /**
     * Returns a cursor with all distinct friends number in the table.
     */
    public Cursor getDistinctFriends() {
        String selectQuery = "SELECT  DISTINCT " + CONTACT_NUMBER + " AS " + CONTACT_NUMBER + " FROM " + FRIENDS_LIBRARY_TABLE;
        return getDatabase().rawQuery(selectQuery, null);
    }

    /**
     * Returns a int with song count of friend's number in the table.
     */
    public int getFriendSongsCount(String phoneNumber) {
        String selectQuery = "SELECT  *  FROM " + FRIENDS_LIBRARY_TABLE + " WHERE " + CONTACT_NUMBER + "='" + phoneNumber + "'";
        return getDatabase().rawQuery(selectQuery, null).getCount();
    }

    /**
     * Returns a Cursor with all songs of friend's number in the table.
     */
    public Cursor getFriendSongs(String phoneNumber) {
        if (phoneNumber.equals("trending")) {
            String selectQuery = "SELECT  * FROM " + FRIENDS_LIBRARY_TABLE + " GROUP BY " + SONG_ID + " ORDER BY " + SONG_PLAY_COUNT + " LIMIT 40";
            return getDatabase().rawQuery(selectQuery, null);
        } else {
            String selectQuery = "SELECT  *  FROM " + FRIENDS_LIBRARY_TABLE + " WHERE " + CONTACT_NUMBER + "='" + phoneNumber + "'";
            return getDatabase().rawQuery(selectQuery, null);
        }
    }

    //Deletes the friends data
    public void deleteAllFriends() {
        getDatabase().delete(FRIENDS_LIBRARY_TABLE, null, null);
    }

    //Deletes the specified friend list.
    public void deleteFriendsSongs(String number) {
        String condition = CONTACT_NUMBER + " = " + "'" + number + "'";
        getDatabase().delete(FRIENDS_LIBRARY_TABLE, condition, null);
    }
    /***********************************************************
     * MUSIC LIBRARY TABLE METHODS.
     ***********************************************************/

    /**
     * Returns the cursor based on the specified fragment.
     */
    public Cursor getFragmentCursor(String querySelection, int fragmentId) {

        if (querySelection.equals("")) {
            return getFragmentCursorHelper(querySelection, fragmentId);
        } else {
            return getFragmentCursorInLibraryHelper(querySelection, fragmentId);
        }
    }

    /**
     * Helper method for getFragmentCursor(). Returns the correct
     * cursor retrieval method for the specified fragment.
     */
    private Cursor getFragmentCursorHelper(String querySelection, int fragmentId) {
        switch (fragmentId) {
            case Common.ARTISTS_FRAGMENT:
                return getAllUniqueArtists(querySelection);
            case Common.ALBUMS_FRAGMENT:
                return getAllUniqueAlbums(querySelection);
            case Common.SONGS_FRAGMENT:
                querySelection = " " + SONG_TITLE + " IS NOT NULL ORDER BY " + SONG_TITLE + " COLLATE NOCASE ASC";
                return getAllSongsSearchable(querySelection);
            case Common.GENRES_FRAGMENT:
                return getAllUniqueGenres(querySelection);
            case Common.ARTISTS_FLIPPED_FRAGMENT:
                return getAllUniqueAlbumsByArtist(querySelection);
            case Common.ARTISTS_FLIPPED_SONGS_FRAGMENT:
                return getAllSongsInAlbumByArtist(querySelection);
            case Common.ALBUM_ARTISTS_FLIPPED_SONGS_FRAGMENT:
                return getAllSongsInAlbumByAlbumArtist(querySelection);
            case Common.ALBUMS_FLIPPED_FRAGMENT:
                return getAllSongsInAlbumByArtist(querySelection);
            case Common.GENRES_FLIPPED_FRAGMENT:
                return getAllUniqueAlbumsInGenre(querySelection);
            case Common.GENRES_FLIPPED_SONGS_FRAGMENT:
                return getAllSongsInAlbumInGenre(querySelection);
            case Common.TOP_25_PLAYED_FRAGMENT:
                return getTop25PlayedTracks();
            case Common.RECENTLY_PLAYED_FRAGMENT:
                return getRecentlyPlayedSongs();
            default:
                return null;
        }
    }

    /**
     * Helper method for getFragmentCursor(). Returns the correct
     * cursor retrieval method for the specified fragment in the
     * specified library.
     */
    private Cursor getFragmentCursorInLibraryHelper(String querySelection, int fragmentId) {
        switch (fragmentId) {
            case Common.ARTISTS_FRAGMENT:
                return getAllUniqueArtistsInLibrary(querySelection);
            case Common.ALBUMS_FRAGMENT:
                return getAllUniqueAlbumsInLibrary(querySelection);
            case Common.SONGS_FRAGMENT:
                querySelection += " ORDER BY " + SONG_TITLE + " COLLATE NOCASE ASC";
                return getAllSongsInLibrarySearchable(querySelection);
            case Common.GENRES_FRAGMENT:
                return getAllUniqueGenresInLibrary(querySelection);
            case Common.ARTISTS_FLIPPED_FRAGMENT:
                return getAllUniqueAlbumsByArtistInLibrary(querySelection);
            case Common.ARTISTS_FLIPPED_SONGS_FRAGMENT:
                return getAllSongsInAlbumByArtistInLibrary(querySelection);
            case Common.ALBUM_ARTISTS_FLIPPED_SONGS_FRAGMENT:
                return getAllSongsInAlbumByAlbumArtistInLibrary(querySelection);
            case Common.ALBUMS_FLIPPED_FRAGMENT:
                return getAllSongsInAlbumByArtistInLibrary(querySelection);
            case Common.GENRES_FLIPPED_FRAGMENT:
                return getAllUniqueAlbumsInGenreInLibrary(querySelection);
            case Common.GENRES_FLIPPED_SONGS_FRAGMENT:
                return getAllSongsByInAlbumInGenreInLibrary(querySelection);
            default:
                return null;
        }
    }

    /**
     * Returns the playback cursor based on the specified query selection.
     */
    public Cursor getPlaybackCursor(String querySelection, int fragmentId) {
        if (fragmentId == 99) {
            return getAllPlaylistSongs(querySelection);
        } else if (fragmentId == 100) {
            return getFriendSongs(querySelection);
        } else {
            if (querySelection == null || querySelection.equals("")) {
                querySelection = " " + SONG_TITLE + " IS NOT NULL";
            }
            return getPlaybackCursorHelper(querySelection, fragmentId);
        }

    }

    /**
     * Helper method for getPlaybackCursor(). Returns the correct
     * cursor retrieval method for the specified playback/fragment route.
     */
    private Cursor getPlaybackCursorHelper(String querySelection, int fragmentId) {
        switch (fragmentId) {
            case Common.TOP_25_PLAYED_FRAGMENT:
                querySelection += " ORDER BY " + SONG_PLAY_COUNT + "*1 DESC LIMIT 25";
                break;
            case Common.RECENTLY_PLAYED_FRAGMENT:
                querySelection += " ORDER BY " + LAST_PLAYED_TIMESTAMP + "*1 DESC LIMIT 25";
                break;
            case Common.PLAY_ALL_SONGS:
                querySelection += " ORDER BY " + SONG_TITLE + " COLLATE NOCASE ASC";
                break;
            case Common.SONGS_FRAGMENT:
                querySelection += " ORDER BY " + SONG_TITLE + " COLLATE NOCASE ASC";
                break;
        }
        return getAllSongsInLibrarySearchable(querySelection);
    }

    //Deletes the specified song.
    public void deleteSong(String songFilePath) {
        String condition = SONG_FILE_PATH + " = " + "'" + songFilePath.replace("'", "''") + "'";
        getDatabase().delete(MUSIC_LIBRARY_TABLE, condition, null);
    }

    //Deletes the specified song.
    public void deleteSongFromPlaylist(String songFilePath, String playlistName) {
        String condition = SONG_FILE_PATH + " = " + "'" + songFilePath.replace("'", "''") + "' AND " + PLAYLIST_NAME + "='" + playlistName.replace("'", "''") + "'";
        getDatabase().delete(PLAYLIST_TABLE, condition, null);
    }

    /**
     * Returns a selection cursor of all unique artists.
     */
    public Cursor getAllUniqueArtists(String selection) {

        String selectDistinctQuery;
        if (selection.equals("")) {
            selectDistinctQuery = "SELECT DISTINCT(" + SONG_ARTIST + "), "
                    + _ID + ", " + SONG_FILE_PATH + ", " + ARTIST_ART_LOCATION + ", "
                    + SONG_SOURCE + ", " + SONG_ALBUM_ART_PATH + ", "
                    + SONG_DURATION + " FROM " + MUSIC_LIBRARY_TABLE
                    + " GROUP BY "
                    + SONG_ARTIST + " ORDER BY " + SONG_ARTIST
                    + " COLLATE NOCASE ASC";
        } else {
            selectDistinctQuery = "SELECT DISTINCT(" + SONG_ARTIST + "), "
                    + _ID + ", " + SONG_FILE_PATH + ", " + ARTIST_ART_LOCATION + ", "
                    + SONG_SOURCE + ", " + SONG_ALBUM_ART_PATH + ", "
                    + SONG_DURATION + " FROM " + MUSIC_LIBRARY_TABLE
                    + " WHERE " + selection + " GROUP BY "
                    + SONG_ARTIST + " ORDER BY " + SONG_ARTIST
                    + " COLLATE NOCASE ASC";
        }
        return getDatabase().rawQuery(selectDistinctQuery, null);
    }

    /**
     * Returns a selection cursor of all unique artists in the
     * specified library. The library should be specified in the
     * selection parameter.
     */
    public Cursor getAllUniqueArtistsInLibrary(String selection) {
        String selectDistinctQuery = "SELECT DISTINCT(" + SONG_ARTIST + "), "
                + MUSIC_LIBRARY_TABLE + "." + _ID + ", "
                + SONG_FILE_PATH + ", " + ARTIST_ART_LOCATION + ", "
                + SONG_SOURCE + ", " + SONG_DURATION + ", "
                + SONG_ALBUM_ART_PATH + " FROM " + MUSIC_LIBRARY_TABLE + " WHERE "
                + MUSIC_LIBRARY_TABLE + "." + selection + " GROUP BY "
                + MUSIC_LIBRARY_TABLE + "." + SONG_ARTIST + " ORDER BY "
                + MUSIC_LIBRARY_TABLE + "." + SONG_ARTIST
                + " COLLATE NOCASE ASC";

        return getDatabase().rawQuery(selectDistinctQuery, null);
    }

    /**
     * Returns a cursor of all songs by the specified artist.
     */
    public Cursor getAllSongsByArtist(String artistName) {
        String selection = SONG_ARTIST + "=" + "'" + artistName.replace("'", "''") + "'";
        return getDatabase().query(MUSIC_LIBRARY_TABLE, null, selection, null, null, null, null);

    }

    /**
     * Returns a ArrayList<String> of all friend's numbers.
     */
    public ArrayList<String> getAllFriendNumbers() {
        ArrayList<String> number = new ArrayList<>();
        String selectDistinctQuery = "SELECT DISTINCT(" + CONTACT_NUMBER + ") AS "
                + CONTACT_NUMBER + " FROM " + FRIENDS_LIBRARY_TABLE;
        Cursor c = getDatabase().rawQuery(selectDistinctQuery, null);
        if (c.moveToFirst()) {
            do {
                number.add(c.getString(c.getColumnIndex(CONTACT_NUMBER)));
            } while (c.moveToNext());
        }
        return number;
    }

    /**
     * Returns a selection cursor of all unique albums.
     */
    public Cursor getAllUniqueAlbums(String selection) {
        String selectDistinctQuery;
        if (selection.equals("")) {
            selectDistinctQuery = "SELECT DISTINCT(" + SONG_ALBUM + "), " +
                    _ID + ", " + SONG_ARTIST + ", " + SONG_FILE_PATH + ", " +
                    SONG_ALBUM_ART_PATH + ", " + SONG_SOURCE + ", " + SONG_ALBUM_ARTIST + ", " + SONG_DURATION +
                    " FROM " + MUSIC_LIBRARY_TABLE
                    + " GROUP BY " + SONG_ALBUM + " ORDER BY " + SONG_ALBUM
                    + " COLLATE NOCASE ASC";
        } else {
            selectDistinctQuery = "SELECT DISTINCT(" + SONG_ALBUM + "), " +
                    _ID + ", " + SONG_ARTIST + ", " + SONG_FILE_PATH + ", " +
                    SONG_ALBUM_ART_PATH + ", " + SONG_SOURCE + ", " + SONG_ALBUM_ARTIST + ", " + SONG_DURATION +
                    " FROM " + MUSIC_LIBRARY_TABLE + " WHERE " +
                    selection + " GROUP BY " +
                    SONG_ALBUM + " ORDER BY " + SONG_ALBUM
                    + " COLLATE NOCASE ASC";
        }
        return getDatabase().rawQuery(selectDistinctQuery, null);

    }

    public int getCount(String key, String value) {
        String selectDistinctQuery;
        selectDistinctQuery = "SELECT " + key + " FROM " + MUSIC_LIBRARY_TABLE
                + " WHERE " + key + " = '" + value + "'";
        Cursor c = getDatabase().rawQuery(selectDistinctQuery, null);
        int count = 0;
        if (c != null) {
            count = c.getCount();
        }
        if (c != null)
            c.close();
        return count;
    }

    public int getCountofAlbumArtists(String album) {
        String selectDistinctQuery;
        selectDistinctQuery = "SELECT DISTINCT (" + DBAccessHelper.SONG_ARTIST + "), " + DBAccessHelper.SONG_ARTIST + " FROM " + MUSIC_LIBRARY_TABLE
                + " WHERE " + DBAccessHelper.SONG_ALBUM + " = '" + album + "'";
        Cursor c = getDatabase().rawQuery(selectDistinctQuery, null);
        int count = 0;
        if (c != null) {
            count = c.getCount();
        }
        if (c != null)
            c.close();
        return count;
    }

    public int getCountofAlbumAndArtists(String album, String artist) {
        String selectDistinctQuery;
        selectDistinctQuery = "SELECT " + DBAccessHelper.SONG_ARTIST + " FROM " + MUSIC_LIBRARY_TABLE
                + " WHERE " + DBAccessHelper.SONG_ALBUM + " = '" + album + "' AND " + DBAccessHelper.SONG_ARTIST + " = '" + artist + "'";
        Cursor c = getDatabase().rawQuery(selectDistinctQuery, null);
        int count = 0;
        if (c != null) {
            count = c.getCount();
        }
        if (c != null)
            c.close();
        return count;
    }

    /**
     * Returns a selection cursor of all unique albums in the
     * specified library. The library should be specified in the
     * selection parameter.
     */
    public Cursor getAllUniqueAlbumsInLibrary(String selection) {
        String selectDistinctQuery = "SELECT DISTINCT(" + SONG_ALBUM + "), " +
                MUSIC_LIBRARY_TABLE + "." + _ID + ", " + SONG_FILE_PATH + ", " + SONG_ALBUM_ARTIST + ", "
                + SONG_SOURCE + ", " + SONG_DURATION + ", " + SONG_ALBUM_ART_PATH + ", " + SONG_ARTIST + " FROM " + MUSIC_LIBRARY_TABLE
                + " WHERE " + MUSIC_LIBRARY_TABLE + "." + selection + " GROUP BY "
                + MUSIC_LIBRARY_TABLE + "." + SONG_ALBUM + " ORDER BY " + MUSIC_LIBRARY_TABLE + "." + SONG_ALBUM
                + " COLLATE NOCASE ASC";

        return getDatabase().rawQuery(selectDistinctQuery, null);
    }

    /**
     * Returns a selection cursor of all songs in the database.
     * This method can also be used to search all songs if a
     * valid selection parameter is passed.
     */
    public Cursor getAllSongsSearchable(String selection) {
        if (selection == null)
            selection = "";
        String selectQuery;
        if (selection.equals("")) {
            selectQuery = "SELECT  * FROM " + MUSIC_LIBRARY_TABLE;
        } else {
            selectQuery = "SELECT  * FROM " + MUSIC_LIBRARY_TABLE + " WHERE " +
                    selection;
        }
        return getDatabase().rawQuery(selectQuery, null);
    }

    /**
     * Returns a selection cursor of all songs in the
     * specified library. The library should be specified in the
     * selection parameter.
     */
    public Cursor getAllSongsInLibrarySearchable(String selection) {
        String selectQuery;
        if (selection != null && !selection.equals("")) {
            selectQuery = "SELECT * FROM " + MUSIC_LIBRARY_TABLE + " WHERE " +
                    selection;
        } else {
            selectQuery = "SELECT * FROM " + MUSIC_LIBRARY_TABLE;
        }
        return getDatabase().rawQuery(selectQuery, null);
    }

    /**
     * Returns a cursor of all songs in the specified album by the
     * specified artist.
     */
    public Cursor getAllSongsInAlbum(String albumName, String artistName) {
        String selection = SONG_ALBUM + "=" + "'"
                + albumName.replace("'", "''")
                + "'" + " AND " + SONG_ARTIST
                + "=" + "'" + artistName.replace("'", "''")
                + "'";

        return getDatabase().query(MUSIC_LIBRARY_TABLE, null, selection, null, null, null, null);

    }

    /**
     * Returns a selection cursor of all unique genres.
     */
    public Cursor getAllUniqueGenres(String selection) {
        String selectDistinctQuery;
        if (selection.equals("")) {
            selectDistinctQuery = "SELECT DISTINCT(" + SONG_GENRE + "), " +
                    _ID + ", " + SONG_FILE_PATH + ", " + SONG_ALBUM_ART_PATH
                    + ", " + SONG_DURATION + ", " + SONG_SOURCE + " FROM " +
                    MUSIC_LIBRARY_TABLE + " GROUP BY " +
                    SONG_GENRE + " ORDER BY " + SONG_GENRE
                    + " COLLATE NOCASE ASC";
        } else {
            selectDistinctQuery = "SELECT DISTINCT(" + SONG_GENRE + "), " +
                    _ID + ", " + SONG_FILE_PATH + ", " + SONG_ALBUM_ART_PATH
                    + ", " + SONG_DURATION + ", " + SONG_SOURCE + " FROM " +
                    MUSIC_LIBRARY_TABLE + " WHERE " +
                    selection + " GROUP BY " +
                    SONG_GENRE + " ORDER BY " + SONG_GENRE
                    + " COLLATE NOCASE ASC";
        }
        return getDatabase().rawQuery(selectDistinctQuery, null);

    }

    /**
     * Returns a selection cursor of all unique genres in the
     * specified library. The library should be specified in the
     * selection parameter.
     */
    public Cursor getAllUniqueGenresInLibrary(String selection) {
        String selectDistinctQuery = "SELECT DISTINCT(" + SONG_GENRE + "), " + MUSIC_LIBRARY_TABLE + "." +
                _ID + ", " + SONG_FILE_PATH + ", " + SONG_ALBUM_ART_PATH + ", " + SONG_DURATION
                + ", " + SONG_SOURCE + " FROM " + MUSIC_LIBRARY_TABLE
                + " WHERE " +
                selection + " GROUP BY " +
                SONG_GENRE + " ORDER BY " + SONG_GENRE
                + " COLLATE NOCASE ASC";

        return getDatabase().rawQuery(selectDistinctQuery, null);

    }

    /**
     * Returns a cursor with all the songs in the specified genre.
     */
    public Cursor getAllSongsInGenre(String selection) {
        String selectQuery;
        if (selection.equals("")) {
            selectQuery = "SELECT * FROM " + MUSIC_LIBRARY_TABLE
                    + " ORDER BY " + SONG_ALBUM + " ASC, "
                    + SONG_TRACK_NUMBER + "*1 COLLATE NOCASE ASC";
        } else {
            selectQuery = "SELECT * FROM " + MUSIC_LIBRARY_TABLE
                    + " WHERE " + selection + " ORDER BY " + SONG_ALBUM + " ASC, "
                    + SONG_TRACK_NUMBER + "*1 ASC";
        }
        return getDatabase().rawQuery(selectQuery, null);

    }

    /**
     * Returns a cursor of all the songs in an album by a specific artist.
     */
    public Cursor getAllSongsInAlbumByArtist(String selection) {
        String selectQuery = "SELECT  * FROM " + MUSIC_LIBRARY_TABLE + " WHERE " +
                selection + " ORDER BY " + SONG_TRACK_NUMBER + "*1 ASC";
        return getDatabase().rawQuery(selectQuery, null);
    }

    /**
     * Returns a cursor of all the songs in an album by a specific artist, within the specified library.
     */
    public Cursor getAllSongsInAlbumByArtistInLibrary(String selection) {
        String selectQuery = "SELECT  * FROM " + MUSIC_LIBRARY_TABLE + " WHERE " +
                selection + " ORDER BY " + SONG_TRACK_NUMBER + "*1 ASC";
        return getDatabase().rawQuery(selectQuery, null);

    }

    /**
     * Returns a list of all the songs in an album within a specific genre.
     */
    public Cursor getAllSongsInAlbumInGenre(String selection) {
        String selectQuery = "SELECT  * FROM " + MUSIC_LIBRARY_TABLE + " WHERE " +
                selection + " ORDER BY " + SONG_TRACK_NUMBER + "*1 ASC";
        return getDatabase().rawQuery(selectQuery, null);
    }

    /**
     * Returns a list of all the songs in an album in a genre, within the specified library.
     */
    public Cursor getAllSongsByInAlbumInGenreInLibrary(String selection) {

        String selectQuery = "SELECT  * FROM " + MUSIC_LIBRARY_TABLE + " WHERE " +
                selection +
                " ORDER BY " + SONG_TRACK_NUMBER + "*1 ASC";

        return getDatabase().rawQuery(selectQuery, null);
    }

    /**
     * Returns a list of all the songs in an album by a specific album artist.
     */
    public Cursor getAllSongsInAlbumByAlbumArtist(String selection) {

        String selectQuery = "SELECT  * FROM " + MUSIC_LIBRARY_TABLE + " WHERE " +
                selection +
                " ORDER BY " + SONG_TRACK_NUMBER + "*1 ASC";

        return getDatabase().rawQuery(selectQuery, null);

    }

    /**
     * Returns a cursor of all the songs in an album by a specific artist, within the specified library.
     */
    public Cursor getAllSongsInAlbumByAlbumArtistInLibrary(String selection) {

        String selectQuery = "SELECT  * FROM " + MUSIC_LIBRARY_TABLE + " WHERE " +
                selection +
                " ORDER BY " + SONG_TRACK_NUMBER + "*1 ASC";

        return getDatabase().rawQuery(selectQuery, null);

    }

    /**
     * Returns a cursor of all locally stored files on the device.
     */
    public Cursor getAllLocalSongs() {
        String where = SONG_SOURCE + "='local'";
        String[] columns = {SONG_FILE_PATH};

        return getDatabase().query(MUSIC_LIBRARY_TABLE, columns, where, null, null, null, null);

    }

    /**
     * Returns the song play count.
     */
    public String getSongPlayCount(String filePath) {
        String selection = SONG_FILE_PATH + "=" + "'" + filePath + "'";
        Cursor cursor = getDatabase().query(MUSIC_LIBRARY_TABLE, null, selection, null, null, null, null);
        if (cursor.moveToFirst()) {
            String songCount = cursor.getString(cursor.getColumnIndex(SONG_PLAY_COUNT));
            cursor.close();
            return songCount;
        }
        return "0";
    }

    /**
     * Update the play count of song.
     */
    public void updateSongPlayCount(String filePath) {
        filePath = filePath.replaceAll("'", "''");
        ContentValues values = new ContentValues();
        int count = Integer.parseInt(getSongPlayCount(filePath));
        int newCount = count + 1;
        values.put(DBAccessHelper.SONG_PLAY_COUNT, newCount);
        String where = DBAccessHelper.SONG_FILE_PATH + "=" + "'" + filePath + "'";

        getDatabase().update(MUSIC_LIBRARY_TABLE,
                values,
                where,
                null);
    }

    /**
     * get play count of playlist.
     */
    public String getPlaylistPlayCount(String playlistName) {
        String selection = PLAYLIST_NAME + "=" + "'" + playlistName + "'";
        Cursor cursor = getDatabase().query(PLAYLIST_TABLE, null, selection, null, null, null, null);
        if (cursor.moveToFirst()) {
            String songCount = cursor.getString(cursor.getColumnIndex(PLAYLIST_PLAY_COUNT));
            cursor.close();
            return songCount;
        }
        return "0";
    }

    /**
     * Update the play count of playlist.
     */
    public void updatePlaylistPlayCount(String playlistName) {
        ContentValues values = new ContentValues();
        int count = Integer.parseInt(getPlaylistPlayCount(playlistName));
        int newCount = count + 1;
        values.put(DBAccessHelper.PLAYLIST_PLAY_COUNT, newCount);
        String where = DBAccessHelper.PLAYLIST_PLAY_COUNT + "=" + "'" + playlistName + "'";

        getDatabase().update(PLAYLIST_TABLE,
                values,
                where,
                null);
    }

    /**
     * Update the timestamp of song.
     */
    public void updateSongTimestamp(String filePath) {
        filePath = filePath.replaceAll("'", "''");
        ContentValues values = new ContentValues();
        values.put(DBAccessHelper.LAST_PLAYED_TIMESTAMP, System.currentTimeMillis());
        String where = DBAccessHelper.SONG_FILE_PATH + "=" + "'" + filePath + "'";

        getDatabase().update(MUSIC_LIBRARY_TABLE,
                values,
                where,
                null);
    }

    /**
     * Returns a cursor with the top 25 played tracks in the library.
     */
    public Cursor getTop25PlayedTracks() {
        return getDatabase().query(MUSIC_LIBRARY_TABLE,
                null,
                null,
                null,
                null,
                null,
                SONG_PLAY_COUNT + "*1 DESC",
                "25");
    }

    /**
     * Returns a cursor with the top 30 played tracks in the library.
     */
    public Cursor getTop30PlayedTracks() {
        long sevenDays = 3 * 24 * 60 * 60 * 1000;
        String time = String.valueOf(System.currentTimeMillis() - sevenDays);
        return getDatabase().query(MUSIC_LIBRARY_TABLE,
                null,
                LAST_PLAYED_TIMESTAMP + " > " + time,
                null,
                null,
                null,
                SONG_PLAY_COUNT + "*1 DESC",
                "30");
    }

    /**
     * Returns a cursor with all songs, ordered by their add date.
     */
    public Cursor getRecentlyAddedSongs() {
        return getDatabase().query(MUSIC_LIBRARY_TABLE,
                null,
                null,
                null,
                null,
                null,
                SONG_LAST_MODIFIED + "*1 DESC",
                "25");

    }

    /**
     * Returns a cursor with all songs, ordered by their last played timestamp.
     */
    public Cursor getRecentlyPlayedSongs() {
        return getDatabase().query(MUSIC_LIBRARY_TABLE,
                null,
                null,
                null,
                null,
                null,
                LAST_PLAYED_TIMESTAMP + "*1 DESC",
                "25");

    }

    /**
     * Returns a cursor of all the songs in the current table.
     */
    public Cursor getAllSongs() {
        String selectQuery = "SELECT  * FROM " + MUSIC_LIBRARY_TABLE + " ORDER BY " + SONG_TITLE + " COLLATE NOCASE ASC";
        return getDatabase().rawQuery(selectQuery, null);
    }

    /**
     * Returns a cursor with all the albums in the specified genre.
     */
    public Cursor getAllUniqueAlbumsInGenre(String selection) {
        String selectDistinctQuery = "SELECT DISTINCT(" + SONG_ALBUM + "), " +
                _ID + ", " + SONG_ARTIST + ", " + SONG_FILE_PATH + ", " +
                SONG_GENRE + ", " + SONG_YEAR + ", " +
                SONG_ALBUM_ART_PATH + ", " + SONG_SOURCE + ", " +
                SONG_ALBUM_ARTIST + ", " + SONG_DURATION
                + " FROM " + MUSIC_LIBRARY_TABLE + " WHERE " +
                selection + " GROUP BY " +
                SONG_ALBUM + " ORDER BY " + SONG_ALBUM
                + " COLLATE NOCASE ASC";

        return getDatabase().rawQuery(selectDistinctQuery, null);

    }

    /**
     * Returns a cursor with unique albums in the specified genre, within the specified library.
     */
    public Cursor getAllUniqueAlbumsInGenreInLibrary(String selection) {
        String selectDistinctQuery = "SELECT DISTINCT(" + SONG_ALBUM + "), " +
                MUSIC_LIBRARY_TABLE + "." + _ID + ", " + SONG_FILE_PATH + ", " + SONG_ALBUM_ARTIST + ", "
                + SONG_SOURCE + ", " + SONG_DURATION + ", " + SONG_ALBUM_ART_PATH + ", " + SONG_ARTIST
                + ", " + SONG_GENRE + ", " + SONG_YEAR + " FROM " + MUSIC_LIBRARY_TABLE
                + " WHERE " + MUSIC_LIBRARY_TABLE + "." +
                selection + " GROUP BY " +
                MUSIC_LIBRARY_TABLE + "." + SONG_ALBUM + " ORDER BY " + MUSIC_LIBRARY_TABLE + "." + SONG_ALBUM
                + " COLLATE NOCASE ASC";

        return getDatabase().rawQuery(selectDistinctQuery, null);

    }

    /**
     * Returns a list of all the albums sorted by title.
     */
    public Cursor getAllAlbumsOrderByName() {
        String selectQuery = "SELECT DISTINCT(" + SONG_ALBUM + "), " +
                _ID + ", " + SONG_ARTIST + ", " + SONG_FILE_PATH + ", " +
                SONG_ALBUM_ART_PATH + ", " + SONG_SOURCE + ", " + SONG_ALBUM_ARTIST +
                " FROM " + MUSIC_LIBRARY_TABLE + " GROUP BY " +
                SONG_ALBUM + " ORDER BY " + SONG_ALBUM
                + " COLLATE NOCASE ASC";

        return getDatabase().rawQuery(selectQuery, null);

    }

    /**
     * Returns a list of all the artists sorted by title.
     */
    public Cursor getAllArtistsOrderByName() {

        String selectQuery = "SELECT DISTINCT(" + SONG_ARTIST + "), " +
                _ID + ", " + SONG_ARTIST + ", " + SONG_FILE_PATH + ", " +
                SONG_ALBUM_ART_PATH + ", " + SONG_SOURCE + ", " + SONG_ALBUM_ARTIST +
                " FROM " + MUSIC_LIBRARY_TABLE + " GROUP BY " +
                SONG_ARTIST + " ORDER BY " + SONG_ARTIST
                + " COLLATE NOCASE ASC";

        return getDatabase().rawQuery(selectQuery, null);

    }

    /**
     * Returns a cursor with the specified song.
     */
    public Cursor getSongById(String songID) {
        String selection = SONG_ID + "=" + "'" + songID + "'";
        return getDatabase().query(MUSIC_LIBRARY_TABLE, null, selection, null, null, null, null);

    }

    /**
     * Returns a cursor with unique albums by an artist.
     */
    public Cursor getAllUniqueAlbumsByArtist(String selection) {
        String selectDistinctQuery = "SELECT DISTINCT(" + SONG_ALBUM + "), " +
                _ID + ", " + SONG_ARTIST + ", " + SONG_FILE_PATH +
                ", " + SONG_YEAR + ", " + SONG_SOURCE + ", " + SONG_DURATION + ", " +
                SONG_ALBUM_ART_PATH + ", " + SONG_TITLE +
                ", " + SONG_ALBUM + ", " + SONG_GENRE + " FROM " +
                MUSIC_LIBRARY_TABLE + " WHERE " + selection + " GROUP BY " +
                SONG_ALBUM + " ORDER BY " + SONG_YEAR
                + "*1 ASC";

        return getDatabase().rawQuery(selectDistinctQuery, null);

    }

    /**
     * Returns a cursor with unique albums by an artist within the specified library.
     */
    public Cursor getAllUniqueAlbumsByArtistInLibrary(String selection) {
        String selectDistinctQuery = "SELECT DISTINCT(" + SONG_ALBUM + "), " + MUSIC_LIBRARY_TABLE + "." +
                _ID + ", " + SONG_ARTIST + ", " + SONG_FILE_PATH +
                ", " + SONG_YEAR + ", " + SONG_SOURCE + ", " + SONG_DURATION + ", " +
                SONG_ALBUM_ART_PATH + ", " + SONG_TITLE + ", " + SONG_ALBUM + ", " + SONG_GENRE + " FROM " +
                MUSIC_LIBRARY_TABLE + " WHERE " + selection + " GROUP BY " + SONG_ALBUM + " ORDER BY " + SONG_YEAR
                + "*1 ASC";

        return getDatabase().rawQuery(selectDistinctQuery, null);
    }

}
