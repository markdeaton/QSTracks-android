package com.esri.apl.qstracks;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by mark4238 on 5/6/2015.
 */
public class StartOnBoot extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // Check to see whether the user wants the logger started on boot.
        // If so, start the logger on boot.
        // Get preferences info
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean bLoggingEnabled = prefs.getBoolean(
                context.getString(R.string.pref_key_tracking_enabled), false);
        if (bLoggingEnabled) {
            Intent intSvc = new Intent();
            intSvc.setClass(context, SvcLocationLogger.class);
            context.startService(intSvc);
        }
    }
}
