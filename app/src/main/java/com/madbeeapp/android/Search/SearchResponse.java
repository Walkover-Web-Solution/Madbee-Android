package com.madbeeapp.android.Search;

public class SearchResponse {

    String SONG_ID;
    String SONG_TITLE;
    String SONG_ARTIST;
    String SONG_ALBUM;
    String SONG_ALBUM_ARTIST;
    String SONG_DURATION;
    String SONG_FILE_PATH;
    String SONG_TRACK_NUMBER;
    String SONG_GENRE;
    String SONG_PLAY_COUNT;
    String SONG_YEAR;
    String ALBUMS_COUNT;
    String SONGS_COUNT;
    String GENRES_SONG_COUNT;
    String SONG_LAST_MODIFIED;
    String SONG_SCANNED;
    String ADDED_TIMESTAMP;
    String RATING;
    String LAST_PLAYED_TIMESTAMP;
    String SONG_SOURCE;
    String SONG_ALBUM_ART_PATH;
    String SONG_DELETED;
    String ARTIST_ART_LOCATION;
    String ALBUM_ID;
    String ARTIST_ID;
    String GENRE_ID;
    String GENRE_SONG_COUNT;
    String LIBRARIES;
    String SONG_LIKES;
    String SOUNDCLOUD_SONG_PLAY_COUNT;

    public SearchResponse(String SONG_ID, String SONG_TITLE, String SONG_ARTIST, String SONG_ALBUM, String SONG_ALBUM_ARTIST, String SONG_DURATION, String SONG_FILE_PATH, String SONG_TRACK_NUMBER, String SONG_GENRE, String SONG_PLAY_COUNT, String SONG_YEAR, String ALBUMS_COUNT, String SONGS_COUNT, String GENRES_SONG_COUNT, String SONG_LAST_MODIFIED, String SONG_SCANNED, String ADDED_TIMESTAMP, String RATING, String LAST_PLAYED_TIMESTAMP, String SONG_SOURCE, String SONG_ALBUM_ART_PATH, String SONG_DELETED, String ARTIST_ART_LOCATION, String ALBUM_ID, String ARTIST_ID, String GENRE_ID, String GENRE_SONG_COUNT, String LIBRARIES, String SONG_LIKES, String SOUNDCLOUD_SONG_PLAY_COUNT) {
        this.SONG_ID = SONG_ID;
        this.SONG_TITLE = SONG_TITLE;
        this.SONG_ARTIST = SONG_ARTIST;
        this.SONG_ALBUM = SONG_ALBUM;
        this.SONG_ALBUM_ARTIST = SONG_ALBUM_ARTIST;
        this.SONG_DURATION = SONG_DURATION;
        this.SONG_FILE_PATH = SONG_FILE_PATH;
        this.SONG_TRACK_NUMBER = SONG_TRACK_NUMBER;
        this.SONG_GENRE = SONG_GENRE;
        this.SONG_PLAY_COUNT = SONG_PLAY_COUNT;
        this.SONG_YEAR = SONG_YEAR;
        this.ALBUMS_COUNT = ALBUMS_COUNT;
        this.SONGS_COUNT = SONGS_COUNT;
        this.GENRES_SONG_COUNT = GENRES_SONG_COUNT;
        this.SONG_LAST_MODIFIED = SONG_LAST_MODIFIED;
        this.SONG_SCANNED = SONG_SCANNED;
        this.ADDED_TIMESTAMP = ADDED_TIMESTAMP;
        this.RATING = RATING;
        this.LAST_PLAYED_TIMESTAMP = LAST_PLAYED_TIMESTAMP;
        this.SONG_SOURCE = SONG_SOURCE;
        this.SONG_ALBUM_ART_PATH = SONG_ALBUM_ART_PATH;
        this.SONG_DELETED = SONG_DELETED;
        this.ARTIST_ART_LOCATION = ARTIST_ART_LOCATION;
        this.ALBUM_ID = ALBUM_ID;
        this.ARTIST_ID = ARTIST_ID;
        this.GENRE_ID = GENRE_ID;
        this.GENRE_SONG_COUNT = GENRE_SONG_COUNT;
        this.LIBRARIES = LIBRARIES;
        this.SONG_LIKES = SONG_LIKES;
        this.SOUNDCLOUD_SONG_PLAY_COUNT = SOUNDCLOUD_SONG_PLAY_COUNT;
    }

    public String getSONG_ID() {
        return SONG_ID;
    }

    public String getSONG_TITLE() {
        return SONG_TITLE;
    }

    public void setSONG_TITLE(String SONG_TITLE) {
        this.SONG_TITLE = SONG_TITLE;
    }

    public String getSONG_ARTIST() {
        return SONG_ARTIST;
    }

    public void setSONG_ARTIST(String SONG_ARTIST) {
        this.SONG_ARTIST = SONG_ARTIST;
    }

    public String getSONG_ALBUM() {
        return SONG_ALBUM;
    }

    public void setSONG_ALBUM(String SONG_ALBUM) {
        this.SONG_ALBUM = SONG_ALBUM;
    }

    public String getSONG_ALBUM_ARTIST() {
        return SONG_ALBUM_ARTIST;
    }

    public String getSONG_DURATION() {
        return SONG_DURATION;
    }

    public void setSONG_DURATION(String SONG_DURATION) {
        this.SONG_DURATION = SONG_DURATION;
    }

    public String getSONG_FILE_PATH() {
        return SONG_FILE_PATH;
    }

    public void setSONG_FILE_PATH(String SONG_FILE_PATH) {
        this.SONG_FILE_PATH = SONG_FILE_PATH;
    }

    public String getSONG_TRACK_NUMBER() {
        return SONG_TRACK_NUMBER;
    }

    public String getSONG_GENRE() {
        return SONG_GENRE;
    }

    public String getSONG_YEAR() {
        return SONG_YEAR;
    }

    public String getSONG_LAST_MODIFIED() {
        return SONG_LAST_MODIFIED;
    }

    public String getADDED_TIMESTAMP() {
        return ADDED_TIMESTAMP;
    }

    public String getSONG_ALBUM_ART_PATH() {
        return SONG_ALBUM_ART_PATH;
    }

    public void setSONG_ALBUM_ART_PATH(String SONG_ALBUM_ART_PATH) {
        this.SONG_ALBUM_ART_PATH = SONG_ALBUM_ART_PATH;
    }

}
