package apl.esri.com.qstracks.data;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.BatteryManager;
import android.os.Build;
import android.text.TextUtils;

import java.security.InvalidParameterException;

import apl.esri.com.qstracks.R;

/**
 * Created by mark4238 on 4/29/2015.
 */
public class QSFeature {
    private Context context;

    private String userId = null;
    private String sex = null;
    private String mood = null;
    private String age = null;

    private SharedPreferences prefs = null;
    private Location location = null;


    public QSFeature(Context context) {
        this.context = context;
    }

    public QSFeature setLocation(Location location) {
        this.location = location;
        return this;
    }

    public QSFeature setPrefs(SharedPreferences prefs) {
        this.prefs = prefs;

        this.userId = prefs.getString(context.getString(R.string.pref_key_user_id),
                context.getString(R.string.pref_default_user_id));
        this.sex = prefs.getString(context.getString(R.string.pref_key_sex),
                context.getString(R.string.pref_default_sex));
        this.mood = prefs.getString(context.getString(R.string.pref_key_mood),
                context.getString(R.string.pref_default_mood));
        this.age = prefs.getString(context.getString(R.string.pref_key_age),
                context.getString(R.string.pref_default_age));

        return this;
    }

    private double batteryPercent() {
        Intent batteryIntent =
                context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        if(level == -1 || scale == -1) {
            return Double.NaN;
        }

        return ((double)level / (float)scale) * 100.0f;
    }

    private String phoneHardware() {
        return Build.MANUFACTURER + " " + Build.MODEL;
    }

    private String formatParamForJSON(int param) {
        if (param == Integer.MIN_VALUE) return "null";
        else return String.format("%d", param);
    }
    private String formatParamForJSON(String param) {
        if (TextUtils.isEmpty(param)) return "null";
        else return param;
    }
    private String formatParamForJSON(double param) {
        if (param == Double.NaN) return "null";
        else return String.format("%f", param);
    }
    public String getFeatureJSON() {
        if (this.location == null)
            throw new InvalidParameterException(context.getString(R.string.exc_bad_QSFeat_params));
        double battery = batteryPercent();
        String os = android.os.Build.VERSION.RELEASE;

        String sJSON = context.getString(R.string.http_add_feature_json,
                formatParamForJSON(this.location.getLongitude()),
                formatParamForJSON(this.location.getLatitude()),
                TextUtils.isEmpty(this.userId)
                        ? "null" : "\"" + formatParamForJSON(this.userId) + "\"",
                formatParamForJSON(location.getTime()),
                formatParamForJSON(this.sex),
                formatParamForJSON(this.mood),
                formatParamForJSON(this.age),
                location.hasAccuracy() ? formatParamForJSON(location.getAccuracy()) : "null",
                Double.isNaN(battery) ? "null" : battery,
                formatParamForJSON(os),
                formatParamForJSON(phoneHardware()),
                location.hasBearing() ? formatParamForJSON(location.getBearing()) : "null",
                location.hasSpeed() ? formatParamForJSON(location.getSpeed()) : "null",
                location.hasAltitude() ? formatParamForJSON(location.getAltitude()) : "null"
        );

        return sJSON;
    }
}
