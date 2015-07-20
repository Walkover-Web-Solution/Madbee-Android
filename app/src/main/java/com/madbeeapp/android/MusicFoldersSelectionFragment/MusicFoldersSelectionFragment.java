package com.madbeeapp.android.MusicFoldersSelectionFragment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.madbeeapp.android.DBHelpers.DBAccessHelper;
import com.madbeeapp.android.Helpers.TypefaceHelper;
import com.madbeeapp.android.R;
import com.madbeeapp.android.Utils.Common;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class MusicFoldersSelectionFragment extends Fragment {

    private static boolean CALLED_FROM_WELCOME = false;
    private TextView mCurrentFolderText;
    private ListView mFoldersListView;
    private Cursor mCursor;
    private String mCurrentDir;
    private List<String> mFileFolderNamesList;
    private List<String> mFileFolderPathsList;
    private List<String> mFileFolderSizesList;
    private HashMap<String, Boolean> mMusicFolders;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        Context mContext = getActivity().getApplicationContext();
        Common mApp = (Common) mContext;
        View rootView = getActivity().getLayoutInflater().inflate(R.layout.fragment_folders_selection, container, false);
        mMusicFolders = new HashMap<>();

        mFoldersListView = (ListView) rootView.findViewById(R.id.folders_list_view);

        RelativeLayout mUpLayout = (RelativeLayout) rootView.findViewById(R.id.folders_up_layout);
        ImageView mUpIcon = (ImageView) rootView.findViewById(R.id.folders_up_icon);
        TextView mUpText = (TextView) rootView.findViewById(R.id.folders_up_text);
        mCurrentFolderText = (TextView) rootView.findViewById(R.id.folders_current_directory_text);

        mUpText.setTypeface(TypefaceHelper.getTypeface(mContext, "calibri"));
        mCurrentFolderText.setTypeface(TypefaceHelper.getTypeface(mContext, "calibri"));

        mUpLayout.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                try {
                    getDir(new File(mCurrentDir).getParentFile().getCanonicalPath());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        });

        mFoldersListView.setDivider(ContextCompat.getDrawable(mContext, R.drawable.icon_list_divider_light));
        mUpIcon.setImageResource(R.drawable.up);

        mFoldersListView.setDividerHeight(1);
        String mRootDir = Environment.getExternalStorageDirectory().getAbsolutePath();
        mCurrentDir = mRootDir;

        //Get a mCursor with a list of all the current folder paths (will be empty if this is the first run).
        mCursor = mApp.getDBAccessHelper().getAllMusicFolderPaths();

        //Get a list of all the paths that are currently stored in the DB.
        for (int i = 0; i < mCursor.getCount(); i++) {
            mCursor.moveToPosition(i);

            //Filter out any double slashes.
            String path = mCursor.getString(mCursor.getColumnIndex(DBAccessHelper.FOLDER_PATH));
            if (path.contains("//")) {
                path = path.replace("//", "/");
            }
            mMusicFolders.put(path, true);
        }

        //Close the cursor.
        if (mCursor != null)
            mCursor.close();

        //Get the folder hierarchy of the selected folder.
        getDir(mRootDir);

        mFoldersListView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int index, long arg3) {
                String newPath = mFileFolderPathsList.get(index);
                getDir(newPath);

            }

        });
        return rootView;
    }

    /**
     * Sets the current directory's text.
     */
    private void setCurrentDirText() {
        mCurrentFolderText.setText(mCurrentDir);
    }

    /**
     * Retrieves the folder hierarchy for the specified folder
     * (this method is NOT recursive and doesn't go into the parent
     * folder's subfolders.
     */
    private void getDir(String dirPath) {
        mFileFolderNamesList = new ArrayList<>();
        mFileFolderPathsList = new ArrayList<>();
        mFileFolderSizesList = new ArrayList<>();

        File f = new File(dirPath);
        File[] files = f.listFiles();
        Arrays.sort(files);

        for (File file : files) {
            if (!file.isHidden() && file.canRead()) {
                if (file.isDirectory()) {
                    String filePath;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
                        filePath = getRealFilePath(file.getAbsolutePath());
                    else
                        filePath = file.getAbsolutePath();

                    mFileFolderPathsList.add(filePath);
                    mFileFolderNamesList.add(file.getName());

                    File[] listOfFiles = file.listFiles();

                    if (listOfFiles != null) {
                        if (listOfFiles.length == 1) {
                            mFileFolderSizesList.add("" + listOfFiles.length + " item");
                        } else {
                            mFileFolderSizesList.add("" + listOfFiles.length + " items");
                        }
                    }
                }
            }
        }

        boolean dirChecked = false;
        if (getMusicFoldersHashMap().get(dirPath) != null)
            dirChecked = getMusicFoldersHashMap().get(dirPath);

        MultiselectListViewAdapter mFoldersListViewAdapter = new MultiselectListViewAdapter(getActivity(),
                this,
                dirChecked);

        mFoldersListView.setAdapter(mFoldersListViewAdapter);
        mFoldersListViewAdapter.notifyDataSetChanged();

        mCurrentDir = dirPath;
        setCurrentDirText();
    }

    /**
     * Resolves the /storage/emulated/legacy paths to
     * their true folder path representations. Required
     * for Nexuses and other devices with no SD card.
     */
    @SuppressLint("SdCardPath")
    private String getRealFilePath(String filePath) {
        if (filePath.equals("/storage/emulated/0") ||
                filePath.equals("/storage/emulated/0/") ||
                filePath.equals("/storage/emulated/legacy") ||
                filePath.equals("/storage/emulated/legacy/") ||
                filePath.equals("/storage/sdcard0") ||
                filePath.equals("/storage/sdcard0/") ||
                filePath.equals("/sdcard") ||
                filePath.equals("/sdcard/") ||
                filePath.equals("/mnt/sdcard") ||
                filePath.equals("/mnt/sdcard/")) {
            return Environment.getExternalStorageDirectory().toString();
        }
        return filePath;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (!CALLED_FROM_WELCOME) {
            getActivity().finish();
        }

    }

    @Override
    public void onPause() {
        super.onPause();

        if (!CALLED_FROM_WELCOME) {
            getActivity().finish();
        }

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (isRemoving()) {
            mCursor.close();
            mCursor = null;
        }

    }

    public HashMap<String, Boolean> getMusicFoldersHashMap() {
        return mMusicFolders;
    }

    public List<String> getFileFolderNamesList() {
        return mFileFolderNamesList;
    }

    public List<String> getFileFolderSizesList() {
        return mFileFolderSizesList;
    }

    public List<String> getFileFolderPathsList() {
        return mFileFolderPathsList;
    }

}

