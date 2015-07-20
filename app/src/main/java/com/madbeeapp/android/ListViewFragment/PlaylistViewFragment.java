package com.madbeeapp.android.ListViewFragment;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.madbeeapp.android.DBHelpers.DBAccessHelper;
import com.madbeeapp.android.FriendsListFragment.FriendsContactListViewCardsAdapter;
import com.madbeeapp.android.Helpers.FriendContactListHelper;
import com.madbeeapp.android.MainActivity.MainActivity;
import com.madbeeapp.android.R;
import com.madbeeapp.android.Utils.Common;

import java.util.ArrayList;

/**
 * Playlist fragment.
 *
 * @author Arpit Gandhi
 */
public class PlaylistViewFragment extends Fragment {

    public Handler mHandler = new Handler();
    /**
     * Query runnable.
     */
    public Runnable queryRunnable = new Runnable() {
        @Override
        public void run() {
            new AsyncRunQuery().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            ;
        }
    };
    TextView empty;
    ArrayList<FriendContactListHelper> contactListDto;
    private Context mContext;
    private PlaylistViewFragment mFragment;
    private Common mApp;
    private String mPlaylistName;
    private ListView mListView;
    private Cursor mCursor;
    private String mQuerySelection = "";
    private LinearLayout trashHeader;
    private TextView clearButton;

    /**
     * Item click listener for the ListView.
     */
    private OnItemClickListener onItemClickListener = new OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> arg0, View view, int index, long id) {
            mApp.getDBAccessHelper().updatePlaylistPlayCount(mPlaylistName);
            mApp.getPlaybackKickstarter()
                    .initPlayback(
                            mQuerySelection,
                            99,
                            index,
                            true,
                            false);
        }
    };

    /**
     * Contact Item click listener for the ListView.
     */
    private OnItemClickListener onContactItemClickListener = new OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> arg0, View view, int index, long id) {
            Intent sendIntent = new Intent(Intent.ACTION_SEND);
            sendIntent.setType("text/plain");
            sendIntent.setPackage("com.whatsapp");
            sendIntent.putExtra(android.content.Intent.EXTRA_TEXT, "​Hey! madbee app is crazily awesome. It can play any song in the world. Plus, you can listen to playlists of your friends.\n" +
                    "​#Madformusic.\n\n" +
                    "https://play.google.com/store/apps/details?id=" + getActivity().getPackageName());
            startActivity(sendIntent);
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View mRootView = inflater.inflate(R.layout.fragment_playlist_view, container, false);
        mContext = getActivity();
        mApp = (Common) getActivity().getApplicationContext();
        mFragment = this;
        contactListDto = new ArrayList<>();
        mPlaylistName = getArguments().getString(DBAccessHelper.PLAYLIST_NAME);
        mQuerySelection = DBAccessHelper.PLAYLIST_NAME + "='" + mPlaylistName + "'";
        mListView = (ListView) mRootView.findViewById(R.id.generalListView);
        empty = (TextView) mRootView.findViewById(R.id.empty_list);
        clearButton = (TextView) mRootView.findViewById(R.id.clearButton);
        trashHeader = (LinearLayout) mRootView.findViewById(R.id.trashHeader);

        if (mPlaylistName.equals("Send Invites")) {
            if (mHandler != null)
                mHandler.post(queryRunnable);
        }
        return mRootView;
    }

    @Override
    public void onResume() {
        if (!mPlaylistName.equals("Send Invites")) {
            if (mHandler != null)
                mHandler.post(queryRunnable);
        }
        super.onResume();
    }

    public void refreshList(int position) {
        if (mHandler != null)
            mHandler.post(queryRunnable);
        if (position != 0)
            position = position - 1;
        final int finalPosition = position;
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mListView.setSelection(finalPosition);
            }
        }, 400);
    }

    public void addWhatsappContacts() {
        if (appInstalledOrNot("com.whatsapp")) {
            ArrayList<String> numbers = new ArrayList<>();
            ArrayList<String> existingNumbers = new ArrayList<>();
            ArrayList<String> ids = new ArrayList<>();
            Cursor cursor = mContext.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    null,
                    ContactsContract.CommonDataKinds.Phone.HAS_PHONE_NUMBER + " > 0",
                    null,
                    ContactsContract.CommonDataKinds.Phone.TIMES_CONTACTED + " DESC LIMIT 20");
            existingNumbers = mApp.getDBAccessHelper().getAllFriendNumbers();
            if (cursor.moveToFirst()) {
                do {
                    String phoneNumber = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                    phoneNumber = phoneNumber.replaceAll("[^0-9]", "");
                    String id = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID));
                    if (!ids.contains(id)) {
                        Cursor c = mContext.getContentResolver().query(
                                ContactsContract.RawContacts.CONTENT_URI,
                                null,
                                ContactsContract.RawContacts.ACCOUNT_TYPE + "= ? AND " + ContactsContract.RawContacts.CONTACT_ID + "= ?",
                                new String[]{"com.whatsapp", id},
                                null);

                        int contactIdColumn = c.getColumnIndex(ContactsContract.RawContacts.DISPLAY_NAME_PRIMARY);
                        if (c.moveToFirst()) {
                            boolean shouldAdd = true;
                            for (FriendContactListHelper dto : contactListDto) {
                                if (phoneNumber.equals(dto.getNumber())) {
                                    shouldAdd = false;
                                    break;
                                }
                            }

                            for (String number : existingNumbers) {
                                if (phoneNumber.equals(number)) {
                                    shouldAdd = false;
                                    break;
                                }
                            }

                            if (shouldAdd) {
                                if (!numbers.contains(phoneNumber)) {
                                    contactListDto.add(new FriendContactListHelper(c.getString(contactIdColumn),
                                            cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI)),
                                            "0", phoneNumber, false));
                                }
                            }
                        }
                        c.close();
                        ids.add(id);
                        numbers.add(phoneNumber);
                    }
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mHandler != null)
            mHandler.removeCallbacks(queryRunnable);
    }

    @Override
    public void onDestroyView() {
        if (mCursor != null) {
            mCursor.close();
        }
        mHandler = null;
        super.onDestroyView();
    }

    public boolean appInstalledOrNot(String uri) {
        try {
            PackageManager pm = getActivity().getPackageManager();
            boolean app_installed;
            try {
                pm.getPackageInfo(uri, PackageManager.GET_ACTIVITIES);
                app_installed = true;
            } catch (PackageManager.NameNotFoundException e) {
                app_installed = false;
            }
            return app_installed;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public Cursor getCursor() {
        return mCursor;
    }

    public void setCursor(Cursor cursor) {
        this.mCursor = cursor;
    }

    public class AsyncRunQuery extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            if (mPlaylistName.equals("Send Invites")) {
                addWhatsappContacts();
            } else {
                mCursor = mApp.getDBAccessHelper().getAllPlaylistSongs(mQuerySelection);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            trashHeader.setVisibility(View.GONE);
            if (mPlaylistName.equals("Send Invites")) {
                try {
                    FriendsContactListViewCardsAdapter mListViewAdapter = new FriendsContactListViewCardsAdapter(mContext, (MainActivity) getActivity(), "friends", contactListDto);
                    mListView.setDivider(ContextCompat.getDrawable(mContext, R.drawable.icon_list_divider));
                    mListView.setAdapter(mListViewAdapter);
                    mListView.setOnItemClickListener(onContactItemClickListener);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                PlaylistViewCardsAdapter mListViewAdapter = new PlaylistViewCardsAdapter(mContext, mFragment, mPlaylistName);
                mListView.setAdapter(mListViewAdapter);
                mListView.setOnItemClickListener(onItemClickListener);
                if (mCursor.getCount() == 0) {
                    mListView.setVisibility(View.GONE);
                    empty.setVisibility(View.VISIBLE);
                    empty.setText(mPlaylistName + " is empty");
                } else {
                    if (mPlaylistName.equals("Trash")) {
                        trashHeader.setVisibility(View.VISIBLE);
                        clearButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                mApp.getDBAccessHelper().deleteAllTrashFiles();
                                onResume();
                            }
                        });
                    }
                    mListView.setVisibility(View.VISIBLE);
                    empty.setVisibility(View.GONE);
                }
            }
        }
    }
}
