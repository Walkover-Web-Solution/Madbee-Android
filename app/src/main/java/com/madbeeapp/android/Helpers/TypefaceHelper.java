package com.madbeeapp.android.Helpers;

import android.content.Context;
import android.graphics.Typeface;

import java.util.Hashtable;

//Caches the custom fonts in memory to improve rendering performance.
public class TypefaceHelper {

    public static final String TYPEFACE_FOLDER = "fonts";
    public static final String TYPEFACE_EXTENSION = ".ttf";

    private static Hashtable<String, Typeface> sTypeFaces = new Hashtable<>(4);

    public static Typeface getTypeface(Context context, String fileName) {
        Typeface tempTypeface = sTypeFaces.get(fileName);

        if (tempTypeface == null) {
            String fontPath = TYPEFACE_FOLDER + '/' + fileName + TYPEFACE_EXTENSION;

            tempTypeface = Typeface.createFromAsset(context.getAssets(), fontPath);
            sTypeFaces.put(fileName, tempTypeface);
        }

        return tempTypeface;
    }

}
