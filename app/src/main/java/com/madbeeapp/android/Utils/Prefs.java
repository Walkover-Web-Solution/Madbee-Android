package com.madbeeapp.android.Utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.telephony.TelephonyManager;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.madbeeapp.android.R;

import java.lang.reflect.Type;
import java.util.HashMap;

public class Prefs {
    // ---------MY Social Profile --------------------------------
    private static String MOBILE_NUMBER = "mobile_number";
    private static String COUNTRY_CODE = "country_code";
    private static String VerificationCode = "verification_code";
    private static String SongFileSize = "song_file_size";
    private static String ISONCALL = "is_on_call";

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences("madbee_music", Context.MODE_MULTI_PROCESS);
    }

    // -----------------------------------------------------------------------------------------------------------------------------------

    // ---------Mobile Number --------------------------------
    public static String getMobileNumber(Context context) {
        return getPrefs(context).getString(MOBILE_NUMBER, "");
    }

    public static void setMobileNumber(Context context, String value) {
        getPrefs(context).edit().putString(MOBILE_NUMBER, value).commit();
    }

    public static String getCountryCode(Context context) {
        return getPrefs(context).getString(COUNTRY_CODE, getCountryZipCode(context));
    }

    public static void setCountryCode(Context context, String value) {
        getPrefs(context).edit().putString(COUNTRY_CODE, value).commit();
    }

    public static String getVerificationCode(Context context) {
        return getPrefs(context).getString(VerificationCode, "");
    }

    public static void setVerificationCode(Context context, String value) {
        getPrefs(context).edit().putString(VerificationCode, value).commit();
    }

    public static boolean getIsOnCall(Context context) {
        return getPrefs(context).getBoolean(ISONCALL, false);
    }

    public static void setIsOnCall(Context context, Boolean value) {
        getPrefs(context).edit().putBoolean(ISONCALL, value).commit();
    }
    // ---------Song File Size --------------------------------

    public static HashMap<String, Long> getSongFileSize(Context context) {

        String json = getPrefs(context).getString(SongFileSize, null);
        Type type = new TypeToken<HashMap<String, Long>>() {
        }.getType();
        return new Gson().fromJson(json, type);
    }

    public static void setSongFileSize(Context context, HashMap<String, Long> map) {
        getPrefs(context).edit().putString(SongFileSize, new Gson().toJson(map)).commit();
    }

    public static String getCountryZipCode(Context context) {
        String CountryID;
        String CountryZipCode = "";

        TelephonyManager manager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        CountryID = manager.getSimCountryIso().toUpperCase();
        String[] rl = context.getResources().getStringArray(R.array.CountryCodes);
        for (String aRl : rl) {
            String[] g = aRl.split(",");
            if (g[1].trim().equals(CountryID.trim())) {
                CountryZipCode = g[0];
                break;
            }
        }
        return CountryZipCode;
    }
}

