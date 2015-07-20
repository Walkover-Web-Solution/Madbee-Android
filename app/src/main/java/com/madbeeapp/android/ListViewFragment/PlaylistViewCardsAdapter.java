package com.madbeeapp.android.ListViewFragment;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.madbeeapp.android.DBHelpers.DBAccessHelper;
import com.madbeeapp.android.Helpers.TypefaceHelper;
import com.madbeeapp.android.R;
import com.madbeeapp.android.Utils.Common;
import com.madbeeapp.android.Views.RoundedTransformation;

public class PlaylistViewCardsAdapter extends SimpleCursorAdapter {

    public static ListViewHolder mHolder = null;
    int mPosition = 0;
    private Context mContext;
    private Common mApp;
    private PlaylistViewFragment mListViewFragment;
    private String mPlaylistName;

    /**
     * Click listener for overflow button.
     */
    private OnClickListener deleteClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            String mFilePath = (String) v.getTag(R.string.song_file_path);
            mApp.getDBAccessHelper().deleteSongFromPlaylist(mFilePath, mPlaylistName);
            mListViewFragment.refreshList(mPosition);
        }
    };

    public PlaylistViewCardsAdapter(Context context, PlaylistViewFragment listViewFragment, String playlistName) {

        super(context, -1, listViewFragment.getCursor(), new String[]{}, new int[]{}, 0);
        mContext = context;
        mListViewFragment = listViewFragment;
        mApp = (Common) mContext.getApplicationContext();
        mPlaylistName = playlistName;
    }

    /**
     * Returns the individual row/child in the list/grid.
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Cursor c = (Cursor) getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.playlist_view_item, parent, false);

            mHolder = new ListViewHolder();
            mHolder.leftImage = (ImageView) convertView.findViewById(R.id.listViewLeftIcon);
            mHolder.titleText = (TextView) convertView.findViewById(R.id.listViewTitleText);
            mHolder.subText = (TextView) convertView.findViewById(R.id.listViewSubText);
            mHolder.rightSubText = (TextView) convertView.findViewById(R.id.listViewRightSubText);
            mHolder.deleteIcon = (ImageButton) convertView.findViewById(R.id.listViewDelete);
            mHolder.subTextParent = (RelativeLayout) convertView.findViewById(R.id.listViewSubTextParent);

            mHolder.titleText.setTypeface(TypefaceHelper.getTypeface(mContext, "calibri"));
            mHolder.subText.setTypeface(TypefaceHelper.getTypeface(mContext, "calibri"));
            mHolder.rightSubText.setTypeface(TypefaceHelper.getTypeface(mContext, "calibri"));

            mHolder.deleteIcon.setOnClickListener(deleteClickListener);
            mHolder.deleteIcon.setFocusable(false);
            mHolder.deleteIcon.setFocusableInTouchMode(false);
            mHolder.deleteIcon.setClickable(true);
            if (mPlaylistName.equals("Trash")) {
                mHolder.deleteIcon.setImageResource(R.drawable.ic_undo);
            } else {
                mHolder.deleteIcon.setImageResource(R.drawable.ic_delete_black);
            }
            convertView.setTag(mHolder);
        } else {
            mHolder = (ListViewHolder) convertView.getTag();
        }

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
        mHolder.deleteIcon.setTag(R.string.song_file_path, filePath);

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
        public ImageButton deleteIcon;
        public RelativeLayout subTextParent;
    }
}
