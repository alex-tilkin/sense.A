package com.exampledemo.parsaniahardik.gpsdemocode;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.exampledemo.parsaniahardik.storage.FireBaseMgr;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.ItemizedOverlay;
import org.osmdroid.views.overlay.OverlayItem;

import java.util.ArrayList;

public class MainActivity extends
        AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener
{
    //region Fields
    private static final String TAG = "MainActivity";
    private GoogleApiClient mGoogleApiClient;
    private LocationManager mLocationManager;
    private LocationRequest mLocationRequest;
    private com.google.android.gms.location.LocationListener listener;
    private long UPDATE_INTERVAL = 2 * 1000;  /* 10 secs */
    private long FASTEST_INTERVAL = 2000; /* 2 sec */

    private ItemizedOverlay<OverlayItem> mItemizedOverlayLocation;
    private ArrayList<OverlayItem> mOverlayItemArrayList;

    private ExMyLocation mMyLocationOverlay;

    private MapView mMapView;
    private MapController mMapController;

    private HandlerThread mHtDb;
    private Handler mHandlerFireBase;

    private KalmanGPS m_KalmanGPS;

    private GeoPoint mGeoPointCurrentLocation;
    //endregion

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        if(!CheckLocation())
            return;

        mMapView = (MapView) findViewById(R.id.mapview);
        mMapView.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE);
        mMapView.setBuiltInZoomControls(true);
        mMapController = (MapController) mMapView.getController();
        mMapController.setZoom(100);
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build();

        mLocationManager = (LocationManager)this.getSystemService(Context.LOCATION_SERVICE);

        InitializeFireBaseThread();

        try {
            m_KalmanGPS = new KalmanGPS();
        } catch (Exception e) {
            e.printStackTrace();
        }

        InitializeMarkersOverlay();
    }

    private void InitializeFireBaseThread() {
        mHtDb = new HandlerThread("FireBaseThread");
        mHtDb.start();

        mHandlerFireBase = new Handler(mHtDb.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                GeoPoint gPtIntersection = new GeoPoint(
                        msg.getData().getDouble("latitude"),
                        msg.getData().getDouble("longitude")
                );

                FireBaseMgr.getInstace().sendIntersection(gPtIntersection);
            }
        };
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

        if(bSetCenter)
            mMapController.setCenter(gPt);

        AddPointToOverlay(gPt, 0, R.drawable.pin);
        mMapView.invalidate();

        mGeoPointCurrentLocation = gPt;
    }

    @Override
    public void onConnected(Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            return;
        }

        startLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "Connection Suspended");
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.i(TAG, "Connection failed. Error: " + connectionResult.getErrorCode());
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    private void addIntersectionContentToFireBaseDb(GeoPoint gPtIntersection) {
        Bundle bundleContet = new Bundle();
        bundleContet.putDouble("longitude", gPtIntersection.getLongitude());
        bundleContet.putDouble("latitude", gPtIntersection.getLatitude());

        Message msgFireBase = Message.obtain(mHandlerFireBase);
        msgFireBase.setData(bundleContet);
        mHandlerFireBase.sendMessage(msgFireBase);
    }

    private void InitializeMarkersOverlay() {
        mOverlayItemArrayList = new ArrayList<>();
        mItemizedOverlayLocation = new ExtendedItemizedIconOverlay<OverlayItem>(this, mOverlayItemArrayList, null);
        mMapView.getOverlays().add(mItemizedOverlayLocation);

        mMyLocationOverlay = new ExMyLocation(mMapView.getContext(), mMapView);
        mMapView.getOverlays().add(mMyLocationOverlay);
    }

    protected void startLocationUpdates() {
        // Create the location request
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(UPDATE_INTERVAL)
                .setFastestInterval(FASTEST_INTERVAL);

        LocationServices.
                FusedLocationApi
                .requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    private void AddPointToOverlay(final GeoPoint gPt, final int index, int iDrawable) {
        final OverlayItem overlayItem = new OverlayItem("", "", gPt);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //method which was problematic and was casing a problem
                if(mMapView.getOverlays().size() > 0) {

                    ((ItemizedIconOverlay<OverlayItem>)mMapView.getOverlays().get(index)).removeAllItems();
                    ((ItemizedIconOverlay<OverlayItem>)mMapView.getOverlays().get(index)).addItem(overlayItem);
                }
            }
        });
    }

    private boolean CheckLocation() {
        if(!isLocationEnabled())
            showAlert();
        return isLocationEnabled();
    }

    private void showAlert() {
        final AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle("Enable Location")
                .setMessage("Your Locations Settings is set to 'Off'.\nPlease Enable Location to " +
                        "use this app")
                .setPositiveButton("Location Settings", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface paramDialogInterface, int paramInt) {

                        Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(myIntent);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface paramDialogInterface, int paramInt) {

                    }
                });
        dialog.show();
    }

    private boolean isLocationEnabled() {
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }
}