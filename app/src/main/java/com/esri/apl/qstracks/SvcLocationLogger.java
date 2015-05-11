package com.esri.apl.qstracks;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import com.esri.apl.qstracks.data.QSFeature;

public class SvcLocationLogger
        extends     Service
        implements  GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
                    LocationListener {
    private static final int SVC_ID = 1;
    private static final int ACT_PI_REQ_CODE = 1;
    private static final String TAG = "SvcLocationLogger";
    private static final int MS_PER_S = 1000;

    private AtomicBoolean mIsCurrentlyLogging = new AtomicBoolean(false);

    private QSFeature mQSFeature = null;
    private GoogleApiClient mGoogleApiClient = null;
    private Location mLastLocation = null;
    private SharedPreferences mSharedPrefs = null;
    private LocationListener mLocationListener = null;
    private URL mUrlPostFeature = null;

    public SvcLocationLogger() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // This is not a bound service
        return null;
    }

    @Override
    public void onDestroy() {
        stopLogging();
        super.onDestroy();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        try {
            mUrlPostFeature = new URL(getString(R.string.http_svc_url));
        } catch (Exception e) {
            Log.e(TAG, "Exception creating feature-creation URL.", e);
        }

        // This creates the API client, but doesn't call connect.
        mGoogleApiClient = buildGoogleApiClient();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startLogging();
        return super.onStartCommand(intent, flags, startId);
    }


    /**
     * Performs various tasks to start logging, including:
     * <ul>
     *     <li>Getting user-entered values to be logged along with location</li>
     *     <li>Making the service foreground</li>
     *     <li>Creating a persistent notification</li>
     *     <li>Connecting to the API client and listening to location updates</li>
     * </ul>
     */
    private void startLogging() {
        if (!mIsCurrentlyLogging.get()) {
            // Get preferences info
            mQSFeature = (new QSFeature(this)).setPrefs(mSharedPrefs);


            // Create notification and bring to foreground
            Intent intent = new Intent(this, ActSettings.class);
            PendingIntent contentIntent =
                    PendingIntent.getActivity(this, ACT_PI_REQ_CODE, intent,
                            PendingIntent.FLAG_UPDATE_CURRENT);

            NotificationCompat.Builder nb = new NotificationCompat.Builder(this)
                    .setSmallIcon(android.R.drawable.ic_menu_compass)
                    .setTicker(getString(R.string.notif_ticker))
                    .setContentTitle(getString(R.string.notif_title))
                    .setContentText(getString(R.string.notif_content_text))
                    .setContentIntent(contentIntent);

            startForeground(SVC_ID, nb.build());

            // Connect; then set up location listening in onConnected()
            mGoogleApiClient.connect();

            mIsCurrentlyLogging.set(true);
        }
    }

    private void stopLogging() {
        if (mIsCurrentlyLogging.get()) {
            // Stop listening to location updates
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();

            // Stop the service since it's no longer needed
            stopForeground(true);

            mIsCurrentlyLogging.set(false);
        }
    }

    protected synchronized GoogleApiClient buildGoogleApiClient() {
        return new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    @Override
    public void onConnected(Bundle bundle) {
//        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        // Set up and start listening to location updates
        String sIntDef = getString(R.string.pref_default_tracking_interval);
        String sIntMin = getString(R.string.pref_min_tracking_interval);
        int iIntMin = Integer.parseInt(sIntMin);
        int iInt = Integer.parseInt(mSharedPrefs.getString(getString(R.string.pref_key_tracking_interval), sIntDef));
        LocationRequest locReq = (new LocationRequest())
                .setInterval(iInt * MS_PER_S)
                .setFastestInterval(iIntMin * MS_PER_S)
                .setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, locReq, this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        // Save info to feature service
        mQSFeature.setLocation(location);
        (new Thread(new Runnable() {
            @Override
            public synchronized void run() {
                HashMap<String, String> params = new HashMap<String, String>();
                params.put("f", "json");
                params.put("features", mQSFeature.getFeatureJSON());
                try {
                    HttpURLConnection conn = (HttpURLConnection) mUrlPostFeature.openConnection();

                    conn.setReadTimeout(10000);
                    conn.setConnectTimeout(15000);
                    conn.setRequestMethod("POST");
                    conn.setDoInput(true);
                    conn.setDoOutput(true);

                    OutputStream os = conn.getOutputStream();
                    BufferedWriter writer = new BufferedWriter(
                            new OutputStreamWriter(os, "UTF-8"));
                    String sPostData = getPostDataString(params);
                    writer.write(sPostData);
                    writer.flush();
                    writer.close();
                    os.close();

                    conn.connect();

                    int iResp = conn.getResponseCode();
                    Log.d(TAG, getString(R.string.log_create_success, iResp, sPostData));

                    conn.disconnect();
                } catch (Exception e) {
                    Log.e(TAG, "Problem creating new location log", e);
                    // Need to create a notification here
                }

            }
        })).start();
    }

    private String getPostDataString(HashMap<String, String> params) throws UnsupportedEncodingException{
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for(Map.Entry<String, String> entry : params.entrySet()){
            if (first)
                first = false;
            else
                result.append("&");

            result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
//            result.append(entry.getKey());
            result.append("=");
            result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
//            result.append(entry.getValue());
        }

        return result.toString();
    }
}
