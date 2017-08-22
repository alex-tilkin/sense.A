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
import org.osmdroid.views.overlay.OverlayItem;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

enum eType {
    ePedestrian,
    eCar
}

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

    private ExMyLocation mMyLocationOverlay;

    private MapView mMapView;
    private MapController mMapController;

    private HandlerThread mHttpThread;
    private Handler mHttpHandler;
    private HandlerThread handlerThreadFireBase;
    private Handler mHandlerFireBase;

    private KalmanGPS m_KalmanGPS;

    //endregion

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build();
    }

    private void decideUserType() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setCancelable(false)
              .setPositiveButton("Car", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                initializeAppContent(eType.eCar);
            }
        })
        .setNegativeButton("Pedestrian", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                initializeAppContent(eType.ePedestrian);
            }
        });
        dialog.show();
    }

    private void initializeAppContent(eType eMoveable) {
        initializeMap();
        initializeHttpThread();
        initializeFireBaseThread();
        initializeKalman();
        initializeMarkersOverlay();
        initializeUser(eMoveable);
        initializeLocation();
    }

    private void initializeUser(eType eMoveable) {
    }

    private void initializeKalman() {
        try {
            m_KalmanGPS = new KalmanGPS();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initializeMap() {
        mMapView = (MapView) findViewById(R.id.mapview);
        mMapView.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE);
        mMapView.setBuiltInZoomControls(true);
        mMapController = (MapController) mMapView.getController();
        mMapController.setZoom(100);
    }

    private void initializeHttpThread() {
        mHttpThread = new HandlerThread("HttpThread");
        mHttpThread.start();

        mHttpHandler = new Handler(mHttpThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                int i;
                i = 1;
                int v = 0;
                v = 1;
            }
        };

    }

    private void initializeFireBaseThread() {
        handlerThreadFireBase = new HandlerThread("FireBaseThread");
        handlerThreadFireBase.start();

        mHandlerFireBase = new Handler(handlerThreadFireBase.getLooper()) {
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

        PostOnNewLocationArrive(gPt);
        AddPointToOverlay(gPt, 0, R.drawable.pin);
        mMapView.invalidate();
    }

    private void PostOnNewLocationArrive(GeoPoint gPt) {
        final String strUrl = String.format(
                "http://nominatim.openstreetmap.org/reverse?format=xml&lat=%s&lon=%s&zoom=18&email=krinitsa@gmail.com",
                Double.toString(gPt.getLatitude()),
                Double.toString(gPt.getLongitude()));
        
        mHttpHandler.post(new Runnable() {
            @Override
            public void run() {
                URL url = null;
                try {
                    url = new URL(strUrl);
                    HttpURLConnection con = (HttpURLConnection) url.openConnection();
                    con.setRequestMethod("GET");
                    con.setRequestProperty("User-Agent", "Mozilla/5.0");
                    con.setDoInput(true);
                    con.setDoOutput(true);

                   int status = con.getResponseCode();
                    BufferedReader in = new BufferedReader(
                            new InputStreamReader(con.getInputStream()));

                    String inputLine;
                    StringBuffer content = new StringBuffer();
                    while ((inputLine = in.readLine()) != null) {
                        content.append(inputLine);
                    }

                    in.close();
                    con.disconnect();
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void onConnected(Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            return;
        }
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

        decideUserType();
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

    private void initializeMarkersOverlay() {
        mMyLocationOverlay = new ExMyLocation(this, mMapView.getContext(), mMapView);
        mMapView.getOverlays().add(mMyLocationOverlay);
    }

    private boolean isLocationEnabled() {
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    protected void initializeLocation() {
        mLocationManager = (LocationManager)this.getSystemService(Context.LOCATION_SERVICE);
        if(!isLocationEnabled()) {
            return;
        }

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
                    ((ExMyLocation)mMapView.getOverlays().get(index)).AddItem(gPt);
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
}