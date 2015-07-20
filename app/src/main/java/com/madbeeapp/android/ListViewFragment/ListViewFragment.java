package com.madbeeapp.android.ListViewFragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
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
import com.madbeeapp.android.MainActivity.MainActivity;
import com.madbeeapp.android.R;
import com.madbeeapp.android.Utils.Common;

/**
 * Generic, multipurpose ListView fragment.
 *
 * @author Arpit Gandhi
 */
public class ListViewFragment extends Fragment {

    public Handler mHandler = new Handler();
    private Context mContext;
    private ListViewFragment mFragment;
    private Common mApp;
    private int mFragmentId;

    /**
     * Updates this activity's UI elements based on the passed intent's
     * update flag(s).
     */
    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            //Initializes the ViewPager.
            if (intent.hasExtra(Common.INIT_PAGER) ||
                    intent.hasExtra(Common.NEW_QUEUE_ORDER)) {
                if (mFragmentId != Common.TOP_25_PLAYED_FRAGMENT && mFragmentId != Common.RECENTLY_PLAYED_FRAGMENT)
                    onResume();
            }

            //Updates the ViewPager's current page/position.
            if (intent.hasExtra(Common.UPDATE_PAGER_POSTIION)) {
                if (mFragmentId != Common.TOP_25_PLAYED_FRAGMENT && mFragmentId != Common.RECENTLY_PLAYED_FRAGMENT)
                    onResume();
            }
        }
    };
    private ListView mListView;
    private Cursor mCursor;
    private String mQuerySelection = "";

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

            playlistView.setOnItemClickListener(new OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    String selectedPlaylist = arrayAdapter.getItem(i);
                    if (selectedPlaylist.equals("Move to Trash")) {
                        selectedPlaylist = "Trash";
                    }
                    if (mCursor.moveToFirst()) {
                        if (mCursor.move(position)) {
                            String filePath = mCursor.getString(mCursor.getColumnIndex(DBAccessHelper.SONG_FILE_PATH));
                            if (!mApp.getDBAccessHelper().checkIfSongIsPresentPlaylist(selectedPlaylist, filePath)) {
                                mApp.getDBAccessHelper().addPlaylistEntry(selectedPlaylist, mCursor);
                                ((MainActivity) getActivity()).loadDrawerFragments();
                            }
                        }
                    }
                    Toast.makeText(mContext, "Added to " + selectedPlaylist, Toast.LENGTH_SHORT).show();
                    alertDialog.dismiss();
                }
            });

            // Showing Alert Message
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

            mApp.getPlaybackKickstarter()
                    .initPlayback(
                            mQuerySelection,
                            mFragmentId,
                            index,
                            true,
                            true);

            if (mFragmentId == Common.RECENTLY_PLAYED_FRAGMENT) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        onResume();
                    }
                }, 600);
            } else if (mFragmentId == Common.TOP_25_PLAYED_FRAGMENT) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        onResume();
                    }
                }, 600);
            }
        }
    };

    /**
     * Query runnable.
     */
    public Runnable queryRunnable = new Runnable() {
        @Override
        public void run() {
            mCursor = mApp.getDBAccessHelper().getFragmentCursor(mQuerySelection, mFragmentId);
            ListViewCardsAdapter mListViewAdapter = new ListViewCardsAdapter(mContext, mFragment);
            mListView.setAdapter(mListViewAdapter);
            mListView.setOnItemClickListener(onItemClickListener);
            mListView.setOnItemLongClickListener(onItemLongClickListener);
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View mRootView = inflater.inflate(R.layout.fragment_list_view, container, false);
        mContext = getActivity();
        mFragment = this;
        mFragmentId = getArguments().getInt(Common.FRAGMENT_ID);
        mListView = (ListView) mRootView.findViewById(R.id.generalListView);
        mListView.setVerticalScrollBarEnabled(true);
        if (mFragmentId != Common.TOP_25_PLAYED_FRAGMENT && mFragmentId != Common.RECENTLY_PLAYED_FRAGMENT) {
            if (mHandler != null)
                mHandler.post(queryRunnable);
        }
        return mRootView;
    }

    @Override
    public void onResume() {
        try {
            if (mFragmentId == Common.TOP_25_PLAYED_FRAGMENT || mFragmentId == Common.RECENTLY_PLAYED_FRAGMENT) {
                if (mHandler != null)
                    mHandler.post(queryRunnable);
            }
        } catch (Exception ignored) {
        }
        super.onResume();
    }

    @Override
    public void onPause() {
        if (mHandler != null)
            mHandler.removeCallbacks(queryRunnable);
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        if (mCursor != null) {
            mCursor.close();
        }
        mHandler = null;
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(mReceiver);
        super.onDestroyView();
    }

    public Cursor getCursor() {
        return mCursor;
    }

    public void setCursor(Cursor cursor) {
        this.mCursor = cursor;
    }

    @Override
    public void onStart() {
        super.onStart();
        mApp = (Common) getActivity().getApplicationContext();
        LocalBroadcastManager.getInstance(mContext)
                .registerReceiver((mReceiver), new IntentFilter(Common.UPDATE_UI_BROADCAST));
        if (mApp.isServiceRunning() && mApp.getService().getCursor() != null) {
            String[] updateFlags = new String[]{Common.UPDATE_PAGER_POSTIION,
                    Common.UPDATE_SEEKBAR_DURATION,
                    Common.INIT_PAGER,
                    Common.UPDATE_PLAYBACK_CONTROLS};

            String[] flagValues = new String[]{"" + mApp.getService().getCurrentSongIndex(),
                    "" + mApp.getService().getCurrentMediaPlayer().getDuration(),
                    "", ""};
            mApp.broadcastUpdateUICommand(updateFlags, flagValues);
        }
    }

    @Override
    public void onStop() {
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(mReceiver);
        super.onStop();
    }
}
