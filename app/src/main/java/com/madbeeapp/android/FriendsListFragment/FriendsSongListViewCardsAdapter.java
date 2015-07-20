package com.madbeeapp.android.FriendsListFragment;

import android.content.Context;
import android.database.Cursor;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.madbeeapp.android.DBHelpers.DBAccessHelper;
import com.madbeeapp.android.Helpers.TypefaceHelper;
import com.madbeeapp.android.MainActivity.MainActivity;
import com.madbeeapp.android.R;
import com.madbeeapp.android.Search.ImageLoader;
import com.madbeeapp.android.Utils.Common;
import com.madbeeapp.android.Views.RoundedTransformation;

public class FriendsSongListViewCardsAdapter extends SimpleCursorAdapter {

    public static ListViewHolder mHolder = null;
    Cursor mCursor;
    ImageLoader imageLoader;
    String mNumber;
    Common mApp;
    FriendsListViewFragment mFragment;
    private Context mContext;

    public FriendsSongListViewCardsAdapter(FriendsListViewFragment fragment, Context context, Cursor cursor, String number) {
        super(context, -1, cursor, new String[]{}, new int[]{}, 0);
        mContext = context;
        mFragment = fragment;
        mCursor = cursor;
        mNumber = number;
        imageLoader = new com.madbeeapp.android.Search.ImageLoader(context);
        mApp = (Common) mContext.getApplicationContext();
    }

    @Override
    public int getCount() {
        return mCursor.getCount();
    }

    /**
     * Returns the individual row/child in the list/grid.
     */
    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.friends_songs_list_view_item, parent, false);
            mHolder = new ListViewHolder();
            mHolder.titleText = (TextView) convertView.findViewById(R.id.listViewTitleText);
            mHolder.subText = (TextView) convertView.findViewById(R.id.listViewSubText);
            mHolder.leftImage = (ImageView) convertView.findViewById(R.id.listViewLeftIcon);

            mHolder.titleText.setTypeface(TypefaceHelper.getTypeface(mContext, "calibri"));
            mHolder.subText.setTypeface(TypefaceHelper.getTypeface(mContext, "calibri"));

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
                                    try {
                                        ((MainActivity) mFragment.getActivity()).loadDrawerFragments();
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
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
        mHolder.titleText.setSelected(true);
        mHolder.subText.setSelected(true);
        if (mCursor.moveToFirst()) {
            if (mCursor.move(position)) {
                mHolder.titleText.setText(mCursor.getString(mCursor.getColumnIndex(DBAccessHelper.SONG_TITLE)));
                mHolder.subText.setText(mCursor.getString(mCursor.getColumnIndex(DBAccessHelper.SONG_ARTIST)));
                //Load the album art.
                String albumArt = mCursor.getString(mCursor.getColumnIndex(DBAccessHelper.SONG_ALBUM_ART_PATH));
                mApp.getPicasso().load(albumArt)
                        .transform(new RoundedTransformation(5, 0))
                        .placeholder(R.drawable.empty_art_padding)
                        .resizeDimen(R.dimen.list_view_left_icon_size, R.dimen.list_view_left_icon_size)
                        .into(mHolder.leftImage);
            }
        }
        return convertView;
    }

    /**
     * Holder subclass for FriendsSongListViewCardsAdapter.
     *
     * @author Arpit Gandhi
     */
    static class ListViewHolder {
        public TextView titleText;
        public TextView subText;
        public ImageView leftImage;
    }
}
