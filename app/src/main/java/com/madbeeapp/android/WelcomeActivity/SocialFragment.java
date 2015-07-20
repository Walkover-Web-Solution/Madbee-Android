package com.madbeeapp.android.WelcomeActivity;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatButton;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.madbeeapp.android.R;
import com.madbeeapp.android.Utils.Common;
import com.madbeeapp.android.Utils.Prefs;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;

import java.io.IOException;

public class SocialFragment extends Fragment {
    Context mContext;
    // Logcat tag
    Common mApp;
    EditText numberET;
    EditText countryCode;
    AppCompatButton verifyButton;
    View verificationDialogView;
    AlertDialog alertDialog;
    EditText title;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mContext = getActivity();
        mApp = (Common) getActivity().getApplicationContext();
        View rootView = getActivity().getLayoutInflater().inflate(R.layout.fragment_welcome_screen_5, container, false);
        TelephonyManager telemamanger = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        String simNumber = telemamanger.getLine1Number();
        if (simNumber != null)
            if (simNumber.startsWith("+")) {
                simNumber = simNumber.substring(3);
            }
        numberET = (EditText) rootView.findViewById(R.id.mobileNumber);
        countryCode = (EditText) rootView.findViewById(R.id.countryCode);
        verifyButton = (AppCompatButton) rootView.findViewById(R.id.verifyButton);
        countryCode.setText(mApp.getCountryZipCode());
        numberET.setText(simNumber);

        if (Prefs.getMobileNumber(mContext) != null && !Prefs.getMobileNumber(mContext).equals("")) {
            numberET.setText(Prefs.getMobileNumber(mContext));
            numberET.setEnabled(false);
            verifyButton.setText("Verified");
            verifyButton.setBackgroundColor(mContext.getResources().getColor(R.color.green_500));
            verifyButton.setEnabled(false);
            countryCode.setEnabled(false);
        } else {
            numberET.setText(simNumber);
        }
        verificationDialogView = View.inflate(mContext, R.layout.verification_code_dialog, null);
        alertDialog = new AlertDialog.Builder(
                mContext).create();
        alertDialog.setView(verificationDialogView);
        alertDialog.setCancelable(false);
        title = (EditText) verificationDialogView.findViewById(R.id.edit);

        verificationDialogView.findViewById(R.id.cancel_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
            }
        });

        verifyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (numberET.getText() != null && countryCode.getText() != null) {
                    final String number = numberET.getText().toString();
                    final String cc = countryCode.getText().toString();
                    if (number.length() > 9) {
                        final String finalNumber = cc + number;
                        if (Common.isNetworkAvailable(mContext)) {
                            new AsyncTask<Void, Void, Void>() {
                                String response = null;

                                @Override
                                protected void onPreExecute() {
                                    super.onPreExecute();
                                    Common.show_PDialog(mContext, "Sending verification code.", false);
                                }

                                @Override
                                protected Void doInBackground(Void... voids) {
                                    int code = Common.generateVerificaitonCode();
                                    Prefs.setVerificationCode(mContext, String.valueOf(code));
                                    Uri.Builder builder = new Uri.Builder();
                                    builder.scheme("https")
                                            .authority("control.msg91.com")
                                            .appendPath("api")
                                            .appendPath("sendhttp.php")
                                            .appendQueryParameter("authkey", "87555AyeqlYXqFO1558d0ba5")
                                            .appendQueryParameter("mobiles", finalNumber)
                                            .appendQueryParameter("message", code + " is your madbee verification code.")
                                            .appendQueryParameter("sender", "madbee")
                                            .appendQueryParameter("route", "4");
                                    response = getResponse(builder.build().toString());
                                    return null;
                                }

                                @Override
                                protected void onPostExecute(Void aVoid) {
                                    super.onPostExecute(aVoid);
                                    if (response != null) {
                                        Log.d("sendotp Response ", "" + response);
                                        Common.dialog.dismiss();
                                        verificationDialogView.findViewById(R.id.ok_button).setOnClickListener(new View.OnClickListener() {
                                            public void onClick(View v) {
                                                String code = title.getText().toString();
                                                if (code.equals(Prefs.getVerificationCode(mContext))) {
                                                    alertDialog.dismiss();
                                                    numberET.setEnabled(false);
                                                    verifyButton.setText("Verified");
                                                    Prefs.setMobileNumber(mContext, finalNumber);
                                                    Prefs.setCountryCode(mContext, cc);
                                                    verifyButton.setBackgroundColor(mContext.getResources().getColor(R.color.green_500));
                                                    verifyButton.setEnabled(false);
                                                    countryCode.setEnabled(false);
                                                    ((WelcomeActivity) getActivity()).loadFinishFragment();
                                                } else {
                                                    Toast.makeText(mContext, "Invalid verification code", Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                        });
                                        // Showing Alert Message
                                        alertDialog.show();
                                    }
                                }
                            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                        } else {
                            Toast.makeText(mContext, "Internet connection required", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(mContext, "Invalid Number", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(mContext, "Fields cannot be black", Toast.LENGTH_SHORT).show();
                }
            }
        });
        return rootView;
    }

    public void recivedSms(String message) {
        try {
            title.setText(message);
        } catch (Exception ignored) {
        }
    }

    String getResponse(String url) {
        try {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(url)
                    .build();
            return client.newCall(request).execute().body().string();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}