package com.madbeeapp.android.AsyncTasks;

import android.content.ContentValues;
import android.content.Context;
import android.os.AsyncTask;

import com.madbeeapp.android.DBHelpers.DBAccessHelper;
import com.madbeeapp.android.Utils.Common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class AsyncSaveMusicFoldersTask extends AsyncTask<String, Void, Boolean> {

    private Context mContext;
    private Common mApp;
    private HashMap<String, Boolean> mMusicFolders;

    public AsyncSaveMusicFoldersTask(Context context, HashMap<String, Boolean> musicFolders) {
        mContext = context;
        mApp = (Common) mContext;
        mMusicFolders = musicFolders;

    }

    @Override
    protected Boolean doInBackground(String... params) {

        //Clear the DB and insert the new selections (along with the old ones).
        mApp.getDBAccessHelper().deleteAllMusicFolderPaths();
        try {
            mApp.getDBAccessHelper().getWritableDatabase().beginTransaction();

            //Retrieve a list of all keys in the hash map (key = music folder path).
            if (mMusicFolders != null) {
                List<String> mPathsList = new ArrayList<>(mMusicFolders.keySet());

                for (int i = 0; i < mMusicFolders.size(); i++) {
                    String path = mPathsList.get(i);
                    boolean include = mMusicFolders.get(path);

                    //Trim down the folder path to include only the folder and its parent.
                    int secondSlashIndex = path.lastIndexOf("/", path.lastIndexOf("/") - 1);
                    if ((secondSlashIndex < path.length()) && secondSlashIndex != -1)
                        path = path.substring(secondSlashIndex, path.length());

                    ContentValues values = new ContentValues();
                    values.put(DBAccessHelper.FOLDER_PATH, path);
                    values.put(DBAccessHelper.INCLUDE, include);
                    mApp.getDBAccessHelper().getWritableDatabase().insert(DBAccessHelper.MUSIC_FOLDERS_TABLE, null, values);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            mApp.getDBAccessHelper().getWritableDatabase().setTransactionSuccessful();
            mApp.getDBAccessHelper().getWritableDatabase().endTransaction();
        }
        return true;
    }

    @Override
    protected void onPostExecute(Boolean result) {
        super.onPostExecute(result);

    }

}
