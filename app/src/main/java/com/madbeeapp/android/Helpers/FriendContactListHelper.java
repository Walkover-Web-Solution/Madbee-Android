package com.madbeeapp.android.Helpers;

public class FriendContactListHelper {
    public String name;
    public String subTitle;
    public String extra;
    public String number;
    boolean isPresent;

    public FriendContactListHelper(String name, String subTitle, String extra, String number, boolean isPresent) {
        this.name = name;
        this.subTitle = subTitle;
        this.extra = extra;

        this.number = number;
        this.isPresent = isPresent;
    }

    public boolean isPresent() {
        return isPresent;
    }

    public void setIsPresent(boolean isPresent) {
        this.isPresent = isPresent;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSubTitle() {
        return subTitle;
    }

    public void setSubTitle(String subTitle) {
        this.subTitle = subTitle;
    }

    public String getExtra() {
        return extra;
    }

    public void setExtra(String extra) {
        this.extra = extra;
    }
}
