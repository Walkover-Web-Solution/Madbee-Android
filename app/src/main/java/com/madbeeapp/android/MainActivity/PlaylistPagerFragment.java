package com.madbeeapp.android.MainActivity;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.madbeeapp.android.Helpers.SongHelper;
import com.madbeeapp.android.Helpers.SongHelper.AlbumArtLoadedListener;
import com.madbeeapp.android.Helpers.TypefaceHelper;
import com.madbeeapp.android.ImageTransformers.PicassoMirrorReflectionTransformer;
import com.madbeeapp.android.R;
import com.madbeeapp.android.Utils.Common;

public class PlaylistPagerFragment extends Fragment implements AlbumArtLoadedListener {

    private Context mContext;
    private Common mApp;
    private SongHelper mSongHelper;
    private ImageView coverArt;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mContext = getActivity();
        mApp = (Common) mContext.getApplicationContext();
        ViewGroup mRootView = (ViewGroup) inflater.inflate(R.layout.fragment_playlist_pager_fill, container, false);
        int mPosition = getArguments().getInt("POSITION");
        coverArt = (ImageView) mRootView.findViewById(R.id.coverArt);
        TextView songNameTextView = (TextView) mRootView.findViewById(R.id.songName);
        TextView artistAlbumNameTextView = (TextView) mRootView.findViewById(R.id.artistAlbumName);
        songNameTextView.setTypeface(TypefaceHelper.getTypeface(mContext, "calibri"));
        artistAlbumNameTextView.setTypeface(TypefaceHelper.getTypeface(mContext, "calibri"));

        //Allow the TextViews to scroll if they extend beyond the layout margins.
        songNameTextView.setSelected(true);
        artistAlbumNameTextView.setSelected(true);

        mSongHelper = new SongHelper();
        mSongHelper.setAlbumArtLoadedListener(this);
        mSongHelper.populateSongData(mContext, mPosition, new PicassoMirrorReflectionTransformer());

        songNameTextView.setText(mSongHelper.getTitle());
        artistAlbumNameTextView.setText(mSongHelper.getAlbum() + " - " + mSongHelper.getArtist());

        return mRootView;
    }

    /**
     * Callback method for album art loading.
     */
    @Override
    public void albumArtLoaded() {
        if (mSongHelper.getAlbumArt() != null) {
            coverArt.setImageBitmap(mSongHelper.getAlbumArt());
        } else {
            coverArt.setImageResource(R.drawable.empty_music_raag_full);
        }
    }
}
