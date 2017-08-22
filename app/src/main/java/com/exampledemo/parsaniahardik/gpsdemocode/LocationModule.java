package com.exampledemo.parsaniahardik.gpsdemocode;

import android.content.Context;
import android.location.Location;
import android.os.HandlerThread;

import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.osmdroid.util.GeoPoint;

/**
 * Created by igalk on 8/22/2017.
 */

public class LocationModule extends HandlerThread
        implements com.google.android.gms.location.LocationListener {

    private android.location.LocationManager mLocationManager;
    private LocationRequest mLocationRequest;
    private com.google.android.gms.location.LocationListener listener;
    private long UPDATE_INTERVAL = 2 * 1000;  /* 10 secs */
    private long FASTEST_INTERVAL = 2000; /* 2 sec */

    private MainActivity mActivity;
    private KalmanGPS m_KalmanGPS;

    public LocationModule(String name, MainActivity oSender) {
        super(name);
        mActivity = oSender;

        initializeKalman();
        initializeLocation();

    }

    protected void initializeLocation() {
        mLocationManager = (android.location.LocationManager)mActivity.getSystemService(Context.LOCATION_SERVICE);
        if(!mLocationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) &&
           !mLocationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)) {
            return;
        }

        // Create the location request
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(UPDATE_INTERVAL)
                .setFastestInterval(FASTEST_INTERVAL);

        // fix this getter
        LocationServices.FusedLocationApi.requestLocationUpdates(mActivity.getGoogleApi(), mLocationRequest, this);
    }

    private void initializeKalman() {
        try {
            m_KalmanGPS = new KalmanGPS();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        onLocationChanged(location, true);
    }

    public void onLocationChanged(Location location, boolean bSetCenter) {
        GeoPoint gPt = new GeoPoint(
                location.getLatitude(),
                location.getLongitude()
        );

        try {
            m_KalmanGPS.push(gPt.getLongitude(), gPt.getLatitude());
            double[] coordinate = m_KalmanGPS.getCoordinate();
            gPt = new GeoPoint(coordinate[1], coordinate[0]);
        } catch (Exception e) {
            e.printStackTrace();
        }

        mActivity.runOnUiThread(new Runnable() {
            private GeoPoint mGpt;
            public Runnable init(GeoPoint gPt) {
                mGpt = gPt;
                return this;
            }

            @Override
            public void run() {
                mActivity.updateActivityWithNewPoint(mGpt);
            }
        }.init(gPt));
    }
}
