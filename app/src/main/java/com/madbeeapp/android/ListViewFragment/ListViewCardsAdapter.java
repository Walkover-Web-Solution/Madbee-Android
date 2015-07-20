package com.madbeeapp.android.ListViewFragment;

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
import com.madbeeapp.android.MainActivity.MainActivity;
import com.madbeeapp.android.R;
import com.madbeeapp.android.Utils.Common;
import com.madbeeapp.android.Views.RoundedTransformation;

public class ListViewCardsAdapter extends SimpleCursorAdapter {

    public static ListViewHolder mHolder = null;
    private Context mContext;
    private Common mApp;
    private ListViewFragment mListViewFragment;

    public ListViewCardsAdapter(Context context, ListViewFragment listViewFragment) {

        super(context, -1, listViewFragment.getCursor(), new String[]{}, new int[]{}, 0);
        mContext = context;
        mListViewFragment = listViewFragment;
        mApp = (Common) mContext.getApplicationContext();
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
                                    try {
                                        ((MainActivity) mListViewFragment.getActivity()).loadDrawerFragments();
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
        //Retrieve data from the cursor.
        String titleText = "";
        String filePath = "";
        String artworkPath = "";
        String field1 = "";
        String field2 = "";
        try {
            titleText = c.getString(c.getColumnIndex(DBAccessHelper.SONG_TITLE));
            filePath = c.getString(c.getColumnIndex(DBAccessHelper.SONG_FILE_PATH));
            artworkPath = c.getString(c.getColumnIndex(DBAccessHelper.SONG_ALBUM_ART_PATH));
            field1 = c.getString(c.getColumnIndex(DBAccessHelper.SONG_DURATION));
            field2 = c.getString(c.getColumnIndex(DBAccessHelper.SONG_ARTIST));
        } catch (NullPointerException e) {
            e.printStackTrace();
        }

        //Set the tags for this grid item.
        convertView.setTag(R.string.title_text, titleText);
        convertView.setTag(R.string.song_file_path, filePath);
        convertView.setTag(R.string.album_art, artworkPath);
        convertView.setTag(R.string.field_1, field1);
        convertView.setTag(R.string.field_2, field2);

        //Set the name text in the ListView.
        mHolder.titleText.setText(titleText);
        mHolder.subText.setText(field2);
        mHolder.rightSubText.setText(field1);

        //Load the album art.
        mApp.getPicasso().load(artworkPath)
                .transform(new RoundedTransformation(5, 0))
                .placeholder(R.drawable.empty_art_padding)
                .resizeDimen(R.dimen.list_view_left_icon_size, R.dimen.list_view_left_icon_size)
                .into(mHolder.leftImage);
        return convertView;
    }

    /**
     * Holder subclass for FriendsSongListViewCardsAdapter.
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
