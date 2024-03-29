package com.esri.apl.qstracks;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import java.util.UUID;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p/>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class ActSettings extends PreferenceActivity {
    public final static String TAG = "ActSettings";
    /**
     * Determines whether to always show the simplified settings UI, where
     * settings are presented in a single list. When false, settings are shown
     * as a master/detail two-pane view on tablets. When true, a single pane is
     * shown on tablets.
     */
//    private static final boolean ALWAYS_SIMPLE_PREFS = false;


    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        setupSimplePreferencesScreen();
    }

    /**
     * Shows the simplified settings UI if the device configuration if the
     * device configuration dictates that a simplified, single-pane UI should be
     * shown.
     */
    private void setupSimplePreferencesScreen() {
/*        if (!isSimplePreferences(this)) {
            return;
        }*/

        // In the simplified UI, fragments are not used at all and we instead
        // use the older PreferenceActivity APIs.

        // Add 'general' preferences.
        addPreferencesFromResource(R.xml.pref_general);

        // Compute a first-time default for user id
        Preference prefId = findPreference(getString(R.string.pref_key_user_id));
        if (TextUtils.isEmpty(
               prefId.getSharedPreferences().getString(prefId.getKey(), "")
        ))
        {

            String sDeviceId =
                    Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
            if (TextUtils.isEmpty(sDeviceId)) sDeviceId = UUID.randomUUID().toString();

            SharedPreferences.Editor edPrefId = prefId.getEditor();
            edPrefId.putString(prefId.getKey(), sDeviceId).commit();
        }

/*        // Add 'notifications' preferences, and a corresponding header.
        PreferenceCategory fakeHeader = new PreferenceCategory(this);
        fakeHeader.setTitle(R.string.pref_header_notifications);
        getPreferenceScreen().addPreference(fakeHeader);
        addPreferencesFromResource(R.xml.pref_notification);

        // Add 'data and sync' preferences, and a corresponding header.
        fakeHeader = new PreferenceCategory(this);
        fakeHeader.setTitle(R.string.pref_header_data_sync);
        getPreferenceScreen().addPreference(fakeHeader);
        addPreferencesFromResource(R.xml.pref_data_sync);*/

        // Bind the summaries of EditText/List/Dialog/Ringtone preferences to
        // their values. When their values change, their summaries are updated
        // to reflect the new value, per the Android Design guidelines.
/*        bindPreferenceSummaryToValue(findPreference(
            getString(R.string.pref_key_tracking_enabled)));*/
        // Ordinarily wouldn't bind on/off but need to start/stop service when changed
        bindPreferenceSummaryToValue(findPreference(
           getString(R.string.pref_key_tracking_enabled)));
        bindPreferenceSummaryToValue(findPreference(
                getString(R.string.pref_key_user_id)));
        bindPreferenceSummaryToValue(findPreference(
            getString(R.string.pref_key_sex)));
        bindPreferenceSummaryToValue(findPreference(
            getString(R.string.pref_key_mood)));
        bindPreferenceSummaryToValue(findPreference(
            getString(R.string.pref_key_age)));
        bindPreferenceSummaryToValue(findPreference(
            getString(R.string.pref_key_tracking_interval)));

/*        bindPreferenceSummaryToValue(findPreference("notifications_new_message_ringtone"));
        bindPreferenceSummaryToValue(findPreference("sync_frequency"));*/
    }

    /**
     * {@inheritDoc}
     */
/*    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this) && !isSimplePreferences(this);
    }*/

    /**
     * Helper method to determine if the device has an extra-large screen. For
     * example, 10" tablets are extra-large.
     */
 /*   private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }*/

    /**
     * Determines whether the simplified settings UI should be shown. This is
     * true if this is forced via {@link #ALWAYS_SIMPLE_PREFS}, or the device
     * doesn't have newer APIs like {@link PreferenceFragment}, or the device
     * doesn't have an extra-large screen. In these cases, a single-pane
     * "simplified" settings UI should be shown.
     */
/*    private static boolean isSimplePreferences(Context context) {
        return ALWAYS_SIMPLE_PREFS
                || Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB
                || !isXLargeTablet(context);
    }*/

    /**
     * {@inheritDoc}
     */
/*    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(List<Header> target) {
        if (!isSimplePreferences(this)) {
            loadHeadersFromResource(R.xml.pref_headers, target);
        }
    }*/

    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private Preference.OnPreferenceChangeListener mBindPreferenceSummaryToValueListener =
            new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = TextUtils.isEmpty(String.valueOf(value))
                    ? "<Unspecified>" : String.valueOf(value);

            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);
            } else if (preference.getKey().equals(getString(
                     R.string.pref_key_tracking_enabled))
                    && value != null && value instanceof Boolean) {
                // Start/stop the tracking service
                boolean isTrackingEnabled = (boolean) value;
                if (isTrackingEnabled) startLogging();
                else stopLogging();
            } else if (preference.getKey().equals(getString(R.string.pref_key_tracking_interval))) {
                // Validate tracking interval
                String sMinInterval = getString(R.string.pref_min_tracking_interval);
                String sMaxInterval = getString(R.string.pref_max_tracking_interval);
                int iMinInterval, iMaxInterval;
                try {
                    iMinInterval = Integer.parseInt(sMinInterval);
                    iMaxInterval = Integer.parseInt(sMaxInterval);
                } catch (Exception e) {
                    Log.e(TAG, "Error decoding stored min/max intervals)", e);
                    Toast.makeText(ActSettings.this, "Error retrieving min/max intervals:\n"
                            + e.getLocalizedMessage(),
                            Toast.LENGTH_LONG)
                            .show();
                    return false;
                }

                boolean bIsValid = true;

                if (TextUtils.isEmpty(stringValue) || !TextUtils.isDigitsOnly(stringValue)) {
                    bIsValid = false;
                } else {
                    int iInterval = Integer.parseInt(stringValue);
                    if (iInterval < iMinInterval || iInterval > iMaxInterval) {
                        bIsValid = false;
                    }
                }
                if (!bIsValid) {
                    Toast toast = Toast.makeText(
                            ActSettings.this,
                            getString(R.string.validation_interval, sMinInterval, sMaxInterval),
                            Toast.LENGTH_LONG);
//                    toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
                    toast.show();
                    return false;
                } else {
                    preference.setSummary(stringValue);
                }

            } else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.setSummary(stringValue);
            }
            return true;
        }
    };

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see #mBindPreferenceSummaryToValueListener
     */
    private void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(mBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        mBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getAll()
                        .get(preference.getKey()));
    }



    private void startLogging() {
        Intent intent = new Intent();
        intent.setClass(this, SvcLocationLogger.class);
        startService(intent);
    }

    private void stopLogging() {
        Intent intent = new Intent();
        intent.setClass(this, SvcLocationLogger.class);
        stopService(intent);
    }
}
