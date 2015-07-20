package com.madbeeapp.android.Search;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.madbeeapp.android.DBHelpers.DBAccessHelper;
import com.madbeeapp.android.Helpers.TypefaceHelper;
import com.madbeeapp.android.R;
import com.madbeeapp.android.Utils.Common;

public class SearchViewCardsAdapter extends SimpleCursorAdapter {

    public static ListViewHolder mHolder = null;
    SearchActivity mSearchActivity;
    private Context mContext;
    private Common mApp;

    public SearchViewCardsAdapter(Context context, Cursor cursor, SearchActivity searchActivity) {
        super(context, -1, cursor, new String[]{}, new int[]{}, 0);
        mContext = context;
        mApp = (Common) mContext.getApplicationContext();
        mSearchActivity = searchActivity;
    }

    /**
     * Returns the individual row/child in the list/grid.
     */
    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        Cursor c = (Cursor) getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.list_view_item, parent, false);

            mHolder = new ListViewHolder();
            mHolder.leftImage = (ImageView) convertView.findViewById(R.id.listViewLeftIcon);
            mHolder.titleText = (TextView) convertView.findViewById(R.id.listViewTitleText);
            mHolder.subText = (TextView) convertView.findViewById(R.id.listViewSubText);
            mHolder.rightSubText = (TextView) convertView.findViewById(R.id.listViewRightSubText);
            mHolder.subTextParent = (RelativeLayout) convertView.findViewById(R.id.listViewSubTextParent);

            mHolder.titleText.setTypeface(TypefaceHelper.getTypeface(mContext, "calibri"));
            mHolder.subText.setTypeface(TypefaceHelper.getTypeface(mContext, "calibri"));
            mHolder.rightSubText.setTypeface(TypefaceHelper.getTypeface(mContext, "calibri"));

            convertView.setTag(mHolder);
        } else {
            mHolder = (ListViewHolder) convertView.getTag();
        }

        mHolder.leftImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                View playlistChooseDialog = View.inflate(mContext, R.layout.playlist_selection_dialog, null);
                final AlertDialog alertDialog = new AlertDialog.Builder(
                        mContext).create();
                alertDialog.setView(playlistChooseDialog);
                alertDialog.setCancelable(true);
                final EditText newPlaylist = (EditText) playlistChooseDialog.findViewById(R.id.new_playlist);
                final ListView playlistView = (ListView) playlistChooseDialog.findViewById(R.id.list);
                final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(mContext, R.layout.single_dialog_singlechoice, mApp.getDBAccessHelper().getPlaylistNames());
                arrayAdapter.remove("Trash");
                arrayAdapter.add("Trash");
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

                playlistView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                        String selectedPlaylist = arrayAdapter.getItem(i);
                        if (mCursor.moveToFirst()) {
                            if (mCursor.move(position)) {
                                String filePath = mCursor.getString(mCursor.getColumnIndex(DBAccessHelper.SONG_FILE_PATH));
                                if (!mApp.getDBAccessHelper().checkIfSongIsPresentPlaylist(selectedPlaylist, filePath)) {
                                    mApp.getDBAccessHelper().addPlaylistEntry(selectedPlaylist, mCursor);
                                }
                            }
                        }
                        Toast.makeText(mContext, "Added to " + selectedPlaylist, Toast.LENGTH_SHORT).show();
                        alertDialog.dismiss();
                    }
                });

                // Showing Alert Message
                alertDialog.show();
            }
        });
        //Retrieve data from the cursor.
        String titleText = "";
        String artist = "";
        String album = "";
        String albumArtist = "";
        String filePath = "";
        String genre = "";
        String year = "";
        String artworkPath = "";
        try {
            titleText = c.getString(c.getColumnIndex(DBAccessHelper.SONG_TITLE));
            artist = c.getString(c.getColumnIndex(DBAccessHelper.SONG_ARTIST));
            album = c.getString(c.getColumnIndex(DBAccessHelper.SONG_ALBUM));
            albumArtist = c.getString(c.getColumnIndex(DBAccessHelper.SONG_ALBUM_ARTIST));
            filePath = c.getString(c.getColumnIndex(DBAccessHelper.SONG_FILE_PATH));
            genre = c.getString(c.getColumnIndex(DBAccessHelper.SONG_GENRE));
            year = c.getString(c.getColumnIndex(DBAccessHelper.SONG_YEAR));
            artworkPath = c.getString(c.getColumnIndex(DBAccessHelper.SONG_ALBUM_ART_PATH));

        } catch (NullPointerException e) {
            //e.printStackTrace();
        }

        //Set the tags for this grid item.
        convertView.setTag(R.string.title_text, titleText);
        convertView.setTag(R.string.album, album);
        convertView.setTag(R.string.artist, artist);
        convertView.setTag(R.string.album_artist, albumArtist);
        convertView.setTag(R.string.file_path, filePath);
        convertView.setTag(R.string.genre, genre);
        convertView.setTag(R.string.year, year);
        convertView.setTag(R.string.artist_art_path, artworkPath);

        //Set the name text in the ListView.
        mHolder.titleText.setText(titleText);
        mHolder.subText.setText(artist);
        mHolder.rightSubText.setText("");

        //Load the album art.
        mApp.getPicasso().load(artworkPath)
                .placeholder(R.drawable.empty_art_padding)
                .resizeDimen(R.dimen.list_view_left_icon_size, R.dimen.list_view_left_icon_size)
                .into(mHolder.leftImage);

        return convertView;
    }

    /**
     * Holder subclass for SearchListViewCardsAdapter.
     *
     * @author Arpit Gandhi
     */
    static class ListViewHolder {
        public ImageView leftImage;
        public TextView titleText;
        public TextView subText;
        public TextView rightSubText;
        public RelativeLayout subTextParent;
    }
}
