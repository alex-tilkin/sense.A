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

    private KalmanApp m_KalmanApp;

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
        mGoogleApiClient = new GoogleApiClient.Builder(this).addConnectionCallbacks(this).addOnConnectionFailedListener(this).
                addApi(LocationServices.API).build();

        mLocationManager = (LocationManager)this.getSystemService(Context.LOCATION_SERVICE);
        mHttpRequest = new HttpRequestThread(this);
        mHttpRequest.start();
        try {
            m_KalmanApp = new KalmanApp();
        } catch (Exception e) {
            e.printStackTrace();
        }
        InitializeMarkersOverlay();
    }

    @Override
    public void onLocationChanged(Location location) {
        GeoPoint gPt = new GeoPoint(location.getLatitude(), location.getLongitude());
        mMapController.setCenter(gPt);
        AddPointToOverlay(gPt, 0, R.drawable.pin);
        mMapView.invalidate();
        gPt = m_KalmanApp.Evaluate(gPt);
        PostClosestIntersectionRequest(gPt);
    }

    @Override
    public void onConnected(Bundle bundle) {
        if (ActivityCompat.
                checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

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
            mMapView.invalidate();
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

    private void initKalmanFilter() {

        /*try {
            m_Jkalman = new JKalman(4, 2);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Random rand = new Random(System.currentTimeMillis() % 2011);
        double x = 0;
        double y = 0;
        // constant velocity
        double dx = rand.nextDouble();
        double dy = rand.nextDouble();

        // init
        Matrix s = new Matrix(4, 1); // state [x, y, dx, dy, dxy]
        Matrix c = new Matrix(4, 1); // corrected state [x, y, dx, dy, dxy]

        Matrix m = new Matrix(2, 1); // measurement [x]
        m.set(0, 0, x);
        m.set(1, 0, y);

        // transitions for x, y, dx, dy
        double[][] tr = { {1, 0, 1, 0},
                {0, 1, 0, 1},
                {0, 0, 1, 0},
                {0, 0, 0, 1} };

        m_Jkalman.setTransition_matrix(new Matrix(tr));

        // 1s somewhere?
        m_Jkalman.setError_cov_post(m_Jkalman.getError_cov_post().identity());*/

        /*Log.d("KalmanFilter", "first x:" + x + ", y:" + y + ", dx:" + dx + ", dy:" + dy);
        Log.d("KalmanFilter", "no; x; y; dx; dy; predictionX; predictionY; predictionDx; predictionDy; correctionX; correctionY; correctionDx; correctionDy;");

        // For debug only
        for (int i = 0; i < 200; ++i) {
            s = m_Jkalman.Predict();

            x = rand.nextGaussian();
            y = rand.nextGaussian();

            m.set(0, 0, m.get(0, 0) + dx + rand.nextGaussian());
            m.set(1, 0, m.get(1, 0) + dy + rand.nextGaussian());

            c = m_Jkalman.Correct(m);

            Log.d("KalmanFilter", "" + i + ";" +  m.get(0, 0) + ";" + m.get(1, 0) + ";" + x + ";" + y + ";"
                    + s.get(0, 0) + ";" + s.get(1, 0) + ";" + s.get(2, 0) + ";" + s.get(3, 0) + ";"
                    + c.get(0, 0) + ";" + c.get(1, 0) + ";" + c.get(2, 0) + ";" + c.get(3, 0) + ";");
        }*/
    }

    private void InitializeMarkersOverlay() {
        mOverlayItemArrayList = new ArrayList<>();
        mItemizedOverlayLocation = new ItemizedIconOverlay<>(this, mOverlayItemArrayList, null);
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
        // Request location updates
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            return;
        }

        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
    }

    private void PostClosestIntersectionRequest(GeoPoint gPt) {
        String strUrl = "http://api.geonames.org/findNearestIntersectionOSM?lat=%s&lng=%s&username=krinitsa";
        strUrl = String.format(strUrl,
                Double.toString(gPt.getLatitude()), // Should be reveredd!!! not sure why!!
                Double.toString(gPt.getLongitude()));
        mHttpRequest.AddRequest(strUrl);
    }

    private void AddPointToOverlay(GeoPoint gPt, int index, int iDrawable) {
        OverlayItem overlayItem = new OverlayItem("", "", gPt);
        //Drawable markerDrawable = ContextCompat.getDrawable(this, iDrawable);
        //overlayItem.setMarker(markerDrawable);

        if(mMapView.getOverlays().size() > 0) {
            ((ItemizedIconOverlay<OverlayItem>)mMapView.getOverlays().get(index)).removeAllItems();
            ((ItemizedIconOverlay<OverlayItem>)mMapView.getOverlays().get(index)).addItem(overlayItem);
        }
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
