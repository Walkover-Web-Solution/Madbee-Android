package com.madbeeapp.android.MainActivity;

public class DrawerHelper {

    String title;
    String type;
    String number;
    String icon;

    public DrawerHelper(String title, String type, String number, String icon) {
        this.title = title;
        this.type = type;
        this.number = number;
        this.icon = icon;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
