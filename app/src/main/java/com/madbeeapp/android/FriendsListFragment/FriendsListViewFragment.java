package com.madbeeapp.android.FriendsListFragment;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.madbeeapp.android.DBHelpers.DBAccessHelper;
import com.madbeeapp.android.R;
import com.madbeeapp.android.Utils.Common;

/**
 * @author Arpit Gandhi
 */
public class FriendsListViewFragment extends Fragment {

    String number;
    Cursor mCursor;
    private Context mContext;
    private Common mApp;
    private ListView mListView;

    private AdapterView.OnItemLongClickListener onItemLongClickListener = new AdapterView.OnItemLongClickListener() {
        @Override
        public boolean onItemLongClick(AdapterView<?> adapterView, View view, final int position, long l) {

            View playlistChooseDialog = View.inflate(mContext, R.layout.playlist_selection_dialog, null);
            final AlertDialog alertDialog = new AlertDialog.Builder(
                    mContext).create();
            alertDialog.setView(playlistChooseDialog);
            alertDialog.setCancelable(true);
            final EditText newPlaylist = (EditText) playlistChooseDialog.findViewById(R.id.new_playlist);
            final ListView playlistView = (ListView) playlistChooseDialog.findViewById(R.id.list);
            final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(mContext, R.layout.single_dialog_singlechoice, mApp.getDBAccessHelper().getPlaylistNames());
            arrayAdapter.remove("Trash");
            playlistView.setAdapter(arrayAdapter);
            playlistView.setDrawSelectorOnTop(true);

            if (arrayAdapter.getCount() == 0) {
                playlistView.setVisibility(View.GONE);
            }

            playlistChooseDialog.findViewById(R.id.create_playlist_button).setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    String code = newPlaylist.getText().toString();
                    if (!code.equals("")) {
                        if (!mApp.getDBAccessHelper().checkIfPlaylistExists(code)) {
                            arrayAdapter.insert(code, 0);
                            Toast.makeText(mContext, code, Toast.LENGTH_SHORT).show();
                            playlistView.setVisibility(View.VISIBLE);
                            newPlaylist.setText("");
                        }
                    }
                }
            });

            playlistView.setOnItemClickListener(new OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    final String selectedPlaylist = arrayAdapter.getItem(i);
                    new AsyncTask<Void, Void, Void>() {

                        @Override
                        protected Void doInBackground(Void... voids) {
                            if (mCursor.moveToFirst()) {
                                if (mCursor.move(position)) {
                                    String songFilePath = mCursor.getString(mCursor.getColumnIndex(DBAccessHelper.SONG_FILE_PATH));
                                    String songId = mCursor.getString(mCursor.getColumnIndex(DBAccessHelper.SONG_ID));

                                    ContentValues values = new ContentValues();
                                    values.put(DBAccessHelper.SONG_TITLE, mCursor.getString(mCursor.getColumnIndex(DBAccessHelper.SONG_TITLE)));
                                    values.put(DBAccessHelper.SONG_ARTIST, mCursor.getString(mCursor.getColumnIndex(DBAccessHelper.SONG_ARTIST)));
                                    values.put(DBAccessHelper.SONG_ALBUM, mCursor.getString(mCursor.getColumnIndex(DBAccessHelper.SONG_ALBUM)));
                                    values.put(DBAccessHelper.SONG_ALBUM_ARTIST, mCursor.getString(mCursor.getColumnIndex(DBAccessHelper.SONG_ALBUM_ARTIST)));
                                    values.put(DBAccessHelper.SONG_DURATION, mCursor.getString(mCursor.getColumnIndex(DBAccessHelper.SONG_DURATION)));
                                    values.put(DBAccessHelper.SONG_FILE_PATH, songFilePath);
                                    values.put(DBAccessHelper.SONG_TRACK_NUMBER, mCursor.getString(mCursor.getColumnIndex(DBAccessHelper.SONG_TRACK_NUMBER)));
                                    values.put(DBAccessHelper.SONG_GENRE, mCursor.getString(mCursor.getColumnIndex(DBAccessHelper.SONG_GENRE)));
                                    values.put(DBAccessHelper.SONG_YEAR, mCursor.getString(mCursor.getColumnIndex(DBAccessHelper.SONG_YEAR)));
                                    values.put(DBAccessHelper.SONG_ALBUM_ART_PATH, mCursor.getString(mCursor.getColumnIndex(DBAccessHelper.SONG_ALBUM_ART_PATH)));
                                    values.put(DBAccessHelper.SONG_LAST_MODIFIED, mCursor.getString(mCursor.getColumnIndex(DBAccessHelper.SONG_LAST_MODIFIED)));
                                    values.put(DBAccessHelper.SONG_SOUNDCLOUD_ALBUM_ART_PATH, mCursor.getString(mCursor.getColumnIndex(DBAccessHelper.SONG_SOUNDCLOUD_ALBUM_ART_PATH)));
                                    values.put(DBAccessHelper.ADDED_TIMESTAMP, mCursor.getString(mCursor.getColumnIndex(DBAccessHelper.ADDED_TIMESTAMP)));
                                    values.put(DBAccessHelper.LAST_PLAYED_TIMESTAMP, mCursor.getString(mCursor.getColumnIndex(DBAccessHelper.LAST_PLAYED_TIMESTAMP)));
                                    values.put(DBAccessHelper.SONG_SOURCE, mCursor.getString(mCursor.getColumnIndex(DBAccessHelper.SONG_SOURCE)));
                                    values.put(DBAccessHelper.SONG_ID, songId);
                                    values.put(DBAccessHelper.SONG_PLAY_COUNT, mCursor.getString(mCursor.getColumnIndex(DBAccessHelper.SONG_PLAY_COUNT)));

                                    if (mApp.getDBAccessHelper().getCount(DBAccessHelper.SONG_ID, songId) == 0) {
                                        //Add all the entries to the database to build the songs library.
                                        mApp.getDBAccessHelper().getWritableDatabase().insert(DBAccessHelper.MUSIC_LIBRARY_TABLE,
                                                null,
                                                values);
                                    }

                                    if (!mApp.getDBAccessHelper().checkIfSongIsPresentPlaylist(selectedPlaylist, songFilePath)) {
                                        values.put(DBAccessHelper.PLAYLIST_NAME, selectedPlaylist);
                                        values.put(DBAccessHelper.PLAYLIST_PLAY_COUNT, "0");
                                        mApp.getDBAccessHelper().getWritableDatabase().insert(DBAccessHelper.PLAYLIST_TABLE,
                                                null,
                                                values);
                                    }
                                }
                            }
                            return null;
                        }

                        @Override
                        protected void onPostExecute(Void aVoid) {
                            super.onPostExecute(aVoid);
                            alertDialog.dismiss();
                            Toast.makeText(mContext, "Added to " + selectedPlaylist, Toast.LENGTH_SHORT).show();
                        }
                    }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    ;
                }
            });

            alertDialog.show();
            return true;
        }
    };

    /**
     * Item click listener for the ListView.
     */
    private OnItemClickListener onItemClickListener = new OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> arg0, View view, final int index, long id) {
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... voids) {
                    if (mCursor.moveToFirst()) {
                        do {
                            String songFilePath = mCursor.getString(mCursor.getColumnIndex(DBAccessHelper.SONG_FILE_PATH));
                            String songId = mCursor.getString(mCursor.getColumnIndex(DBAccessHelper.SONG_ID));

                            ContentValues values = new ContentValues();
                            values.put(DBAccessHelper.SONG_TITLE, mCursor.getString(mCursor.getColumnIndex(DBAccessHelper.SONG_TITLE)));
                            values.put(DBAccessHelper.SONG_ARTIST, mCursor.getString(mCursor.getColumnIndex(DBAccessHelper.SONG_ARTIST)));
                            values.put(DBAccessHelper.SONG_ALBUM, mCursor.getString(mCursor.getColumnIndex(DBAccessHelper.SONG_ALBUM)));
                            values.put(DBAccessHelper.SONG_ALBUM_ARTIST, mCursor.getString(mCursor.getColumnIndex(DBAccessHelper.SONG_ALBUM_ARTIST)));
                            values.put(DBAccessHelper.SONG_DURATION, mCursor.getString(mCursor.getColumnIndex(DBAccessHelper.SONG_DURATION)));
                            values.put(DBAccessHelper.SONG_FILE_PATH, songFilePath);
                            values.put(DBAccessHelper.SONG_TRACK_NUMBER, mCursor.getString(mCursor.getColumnIndex(DBAccessHelper.SONG_TRACK_NUMBER)));
                            values.put(DBAccessHelper.SONG_GENRE, mCursor.getString(mCursor.getColumnIndex(DBAccessHelper.SONG_GENRE)));
                            values.put(DBAccessHelper.SONG_YEAR, mCursor.getString(mCursor.getColumnIndex(DBAccessHelper.SONG_YEAR)));
                            values.put(DBAccessHelper.SONG_ALBUM_ART_PATH, mCursor.getString(mCursor.getColumnIndex(DBAccessHelper.SONG_ALBUM_ART_PATH)));
                            values.put(DBAccessHelper.SONG_LAST_MODIFIED, mCursor.getString(mCursor.getColumnIndex(DBAccessHelper.SONG_LAST_MODIFIED)));
                            values.put(DBAccessHelper.SONG_SOUNDCLOUD_ALBUM_ART_PATH, mCursor.getString(mCursor.getColumnIndex(DBAccessHelper.SONG_SOUNDCLOUD_ALBUM_ART_PATH)));
                            values.put(DBAccessHelper.ADDED_TIMESTAMP, mCursor.getString(mCursor.getColumnIndex(DBAccessHelper.ADDED_TIMESTAMP)));
                            values.put(DBAccessHelper.LAST_PLAYED_TIMESTAMP, mCursor.getString(mCursor.getColumnIndex(DBAccessHelper.LAST_PLAYED_TIMESTAMP)));
                            values.put(DBAccessHelper.SONG_SOURCE, DBAccessHelper.SOUNDCLOUD);
                            values.put(DBAccessHelper.SONG_ID, songId);
                            values.put(DBAccessHelper.SONG_PLAY_COUNT, "0");

                            if (mApp.getDBAccessHelper().getCount(DBAccessHelper.SONG_ID, songId) == 0) {
                                //Add all the entries to the database to build the songs library.
                                mApp.getDBAccessHelper().getWritableDatabase().insert(DBAccessHelper.MUSIC_LIBRARY_TABLE,
                                        null,
                                        values);
                            }
                        } while (mCursor.moveToNext());
                    }
                    return null;
                }

                @Override
                protected void onPostExecute(Void aVoid) {
                    super.onPostExecute(aVoid);
                    mApp.getPlaybackKickstarter()
                            .initPlayback(
                                    number,
                                    100,
                                    index,
                                    true,
                                    false);
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    };

    public Cursor getCursor() {
        return mCursor;
    }

    public void setCursor(Cursor mCursor) {
        this.mCursor = mCursor;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View mRootView = inflater.inflate(R.layout.fragment_friends_list_view, container, false);
        mContext = getActivity();
        mApp = (Common) getActivity().getApplicationContext();
        number = getArguments().getString(Common.CONTACT_NUMBER);
        mListView = (ListView) mRootView.findViewById(R.id.generalListView);
        mListView.setVerticalScrollBarEnabled(false);

        new Handler().post(new Runnable() {
            @Override
            public void run() {
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... voids) {
                        getSongsList(number);
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void aVoid) {
                        super.onPostExecute(aVoid);
                        initSongsList();
                    }
                }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        });
        return mRootView;
    }

    public void initSongsList() {
        FriendsSongListViewCardsAdapter mSListViewAdapter = new FriendsSongListViewCardsAdapter(this, mContext, mCursor, number);
        mListView.setAdapter(mSListViewAdapter);
        mListView.setOnItemClickListener(onItemClickListener);
        mListView.setOnItemLongClickListener(onItemLongClickListener);
    }

    public void getSongsList(String number) {
        mCursor = mApp.getDBAccessHelper().getFriendSongs(number);
    }
}
