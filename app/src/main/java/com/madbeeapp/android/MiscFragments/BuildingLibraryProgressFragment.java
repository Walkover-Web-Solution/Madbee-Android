package com.madbeeapp.android.MiscFragments;

import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.madbeeapp.android.AsyncTasks.AsyncBuildLibraryTask;
import com.madbeeapp.android.AsyncTasks.AsyncBuildLibraryTask.OnBuildLibraryProgressUpdate;
import com.madbeeapp.android.Helpers.TypefaceHelper;
import com.madbeeapp.android.MainActivity.MainActivity;
import com.madbeeapp.android.R;

public class BuildingLibraryProgressFragment extends Fragment implements OnBuildLibraryProgressUpdate {

    TextView mCurrentTaskText;
    private Context mContext;
    private RelativeLayout mProgressElementsContainer;
    private ProgressBar mProgressBar, syncprogress;
    private Animation mFadeInAnimation;
    /**
     * Fade in animation listener.
     */
    private AnimationListener fadeInListener = new AnimationListener() {

        @Override
        public void onAnimationEnd(Animation arg0) {
            mProgressElementsContainer.setVisibility(View.VISIBLE);

        }

        @Override
        public void onAnimationRepeat(Animation arg0) {

        }

        @Override
        public void onAnimationStart(Animation arg0) {

        }

    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mContext = getActivity().getApplicationContext();
        View mRootView = getActivity().getLayoutInflater().inflate(R.layout.fragment_building_library_progress, null);

        mProgressElementsContainer = (RelativeLayout) mRootView.findViewById(R.id.progress_elements_container);
        mCurrentTaskText = (TextView) mRootView.findViewById(R.id.building_library_task);
        mProgressBar = (ProgressBar) mRootView.findViewById(R.id.building_library_progress);
        syncprogress = (ProgressBar) mRootView.findViewById(R.id.datasync_library_progress);

        mProgressElementsContainer.setVisibility(View.INVISIBLE);

        mCurrentTaskText.setTypeface(TypefaceHelper.getTypeface(mContext, "calibri"));
        mCurrentTaskText.setPaintFlags(mCurrentTaskText.getPaintFlags()
                | Paint.ANTI_ALIAS_FLAG
                | Paint.SUBPIXEL_TEXT_FLAG);

        syncprogress.setIndeterminate(true);
        mProgressBar.setMax(1000000);

        mFadeInAnimation = AnimationUtils.loadAnimation(mContext, R.anim.fade_in);
        mFadeInAnimation.setAnimationListener(fadeInListener);
        mFadeInAnimation.setDuration(700);

        return mRootView;
    }

    @Override
    public void onStartBuildingLibrary() {
        mProgressElementsContainer.startAnimation(mFadeInAnimation);

    }

    @Override
    public void onProgressUpdate(AsyncBuildLibraryTask task, String mCurrentTask,
                                 int overallProgress, int maxProgress,
                                 boolean mediaStoreTransferDone, boolean dataTransferComplete) {
        /**
         * overallProgress refers to the progress that the service's notification
         * progress bar will display. Since this fragment will only show the progress
         * of building the library (and not scanning the album art), we need to
         * multiply the overallProgress by 4 (the building library task only takes
         * up a quarter of the overall progress bar).
         */
        mProgressBar.setProgress(overallProgress * 4);

        //This fragment only shows the MediaStore transfer progress.
        if (mediaStoreTransferDone && !dataTransferComplete) {
            mCurrentTaskText.setText("Do not turn off the Internet\n Friend list is being loaded.");
            mProgressBar.setVisibility(View.GONE);
            syncprogress.setVisibility(View.VISIBLE);
        }

        if (mediaStoreTransferDone && dataTransferComplete) {
            mCurrentTaskText.setText("Finishing..");
            onFinishBuildingLibrary(task);
        }
    }

    @Override
    public void onFinishBuildingLibrary(AsyncBuildLibraryTask task) {
        task.mBuildLibraryProgressUpdate.remove(0);
        Intent intent = new Intent(mContext, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        mContext.startActivity(intent);
    }
}

