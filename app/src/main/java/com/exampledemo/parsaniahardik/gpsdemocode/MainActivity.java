package com.exampledemo.parsaniahardik.gpsdemocode;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class MainActivity extends
        AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener,
        IHttpRequestResponse
{
    //region Fields
    private static final String TAG = "MainActivity";
    private GoogleApiClient mGoogleApiClient;
    private LocationManager mLocationManager;
    private LocationRequest mLocationRequest;
    private com.google.android.gms.location.LocationListener listener;
    private long UPDATE_INTERVAL = 2 * 1000;  /* 10 secs */
    private long FASTEST_INTERVAL = 2000; /* 2 sec */
    private Location mLocation;

    private ItemizedOverlay<OverlayItem> mItemizedOverlayLocation;
    private ArrayList<OverlayItem> mOverlayItemArrayList;

    private ItemizedOverlay<OverlayItem> mItemizedOverlayNextIntersection;
    private ArrayList<OverlayItem> mOverlayItemNextIntersectionList;

    private MapView         mMapView;
    private MapController   mMapController;

    private HttpRequestThread mHttpRequest;

    private KalmanGPS m_KalmanGPS;
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
        mHttpRequest = new HttpRequestThread(this);
        mHttpRequest.start();

        try {
            m_KalmanGPS = new KalmanGPS();
        } catch (Exception e) {
            e.printStackTrace();
        }

        InitializeMarkersOverlay();
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

        if(bSetCenter)
            mMapController.setCenter(gPt);

        AddPointToOverlay(gPt, 0, R.drawable.pin);
        mMapView.invalidate();

        try {
            m_KalmanGPS.push(gPt.getLongitude(), gPt.getLatitude());
            double[] coordinate = m_KalmanGPS.getCoordinate();
            gPt = new GeoPoint(coordinate[1], coordinate[0]);
        } catch (Exception e) {
            e.printStackTrace();
        }
        PostClosestIntersectionRequest(gPt);
    }

    @Override
    public void onConnected(Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            return;
        }

        startLocationUpdates();
        mLocation = LocationServices.
                FusedLocationApi.
                getLastLocation(mGoogleApiClient);
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

    @Override
    public void OnHttpResponse(String strResponse) {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        Document doc = null;

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            doc =  builder.parse(new InputSource(new StringReader(strResponse)));
            NodeList e = doc.getElementsByTagName("intersection");
            if (((Element)e.item(0)).getElementsByTagName("lat") != null &&
                ((Element)e.item(0)).getElementsByTagName("lng") != null) {

                GeoPoint gPt = new GeoPoint(
                        Double.parseDouble(((Element) e.item(0)).getElementsByTagName("lat").item(0).getTextContent()),
                        Double.parseDouble(((Element) e.item(0)).getElementsByTagName("lng").item(0).getTextContent())
                );
                AddPointToOverlay(gPt, 1, R.drawable.pininter);
            }
            mMapView.post(new Runnable() {
                @Override
                public void run() {
                    mMapView.invalidate();
                }
            });
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void InitializeMarkersOverlay() {
        mOverlayItemArrayList = new ArrayList<>();
        mItemizedOverlayLocation = new ExtendedItemizedIconOverlay<OverlayItem>(this, mOverlayItemArrayList, null);
        mMapView.getOverlays().add(mItemizedOverlayLocation);

        mOverlayItemNextIntersectionList = new ArrayList<>();
        mItemizedOverlayNextIntersection = new ItemizedIconOverlay<>(this, mOverlayItemNextIntersectionList, null);
        mMapView.getOverlays().add(mItemizedOverlayNextIntersection);
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

    private void PostClosestIntersectionRequest(GeoPoint gPt) {
        String strUrl = "http://api.geonames.org/findNearestIntersectionOSM?lat=%s&lng=%s&username=krinitsa";
        strUrl = String.format(strUrl,
                Double.toString(gPt.getLatitude()), // Should be reveredd!!! not sure why!!
                Double.toString(gPt.getLongitude()));
        mHttpRequest.AddRequest(strUrl);
    }

    private void AddPointToOverlay(final GeoPoint gPt, final int index, int iDrawable) {
        final OverlayItem overlayItem = new OverlayItem("", "", gPt);
        //Drawable markerDrawable = ContextCompat.getDrawable(this, iDrawable);
        //overlayItem.setMarker(markerDrawable);

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
//        if(mMapView.getOverlays().size() > 0) {
//
//            ((ItemizedIconOverlay<OverlayItem>)mMapView.getOverlays().get(index)).removeAllItems();
//            ((ItemizedIconOverlay<OverlayItem>)mMapView.getOverlays().get(index)).addItem(overlayItem);
//        }
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