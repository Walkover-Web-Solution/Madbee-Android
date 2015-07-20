package com.madbeeapp.android.Helpers;

public class AudioManagerHelper {

    private int mOriginalVolume;
    private boolean mHasAudioFocus = false;
    private boolean mAudioDucked = false;
    private int mTargetVolume;
    private int mCurrentVolume;
    private int mStepDownIncrement;
    private int mStepUpIncrement;

    public int getOriginalVolume() {
        return mOriginalVolume;
    }

    public void setOriginalVolume(int originalVolume) {
        this.mOriginalVolume = originalVolume;
    }

    public boolean hasAudioFocus() {
        return mHasAudioFocus;
    }

    public boolean isAudioDucked() {
        return mAudioDucked;
    }

    public void setAudioDucked(boolean audioDucked) {
        this.mAudioDucked = audioDucked;
    }

    public int getTargetVolume() {
        return mTargetVolume;
    }

    public void setTargetVolume(int targetVolume) {
        this.mTargetVolume = targetVolume;
    }

    public int getCurrentVolume() {
        return mCurrentVolume;
    }

    public void setCurrentVolume(int currentVolume) {
        this.mCurrentVolume = currentVolume;
    }

    public int getStepDownIncrement() {
        return mStepDownIncrement;
    }

    public void setStepDownIncrement(int stepDownIncrement) {
        this.mStepDownIncrement = stepDownIncrement;
    }

    public int getStepUpIncrement() {
        return mStepUpIncrement;
    }

    public void setStepUpIncrement(int stepUpIncrement) {
        this.mStepUpIncrement = stepUpIncrement;
    }

    public void setHasAudioFocus(boolean hasAudioFocus) {
        mHasAudioFocus = hasAudioFocus;
    }

}
