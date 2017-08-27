package com.exampledemo.parsaniahardik.gpsdemocode;

import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.iid.FirebaseInstanceId;

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
};

class ConstMessages {
    public static final int MSG_NEW_GPS_POINT = 1;
    public static final int MSG_SAVE_NEW_RECORD = MSG_NEW_GPS_POINT + 1;
    public static final int MSG_DB_HANDLER_CREATED = MSG_SAVE_NEW_RECORD + 1;
    public static final int MSG_DB_NEW_USER_CONNECTED = MSG_DB_HANDLER_CREATED + 1;
    public static final int MSG_NEW_HOT_ZONE_POINT_ARRIVED = MSG_DB_NEW_USER_CONNECTED + 1;
}

public class MainActivity extends
        AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {
    //region Fields
    private static final String TAG = "MainActivity";
    private GoogleApiClient mGoogleApiClient;
    private Handler mHandlerUi;
    LocationModule mLocationModuleThread;

    private ExMyLocation mMyLocationOverlay;

    private MapView mMapView;
    private MapController mMapController;

    private HandlerThread mHttpThread;
    private Handler mHttpHandler;
    private DatabaseManager mHandlerThreadFireBase;
    private Handler mHandlerFireBase;
    private eType meUser;
    private String mstrUid;

    //endregion

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build();

        mHandlerUi = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                onMessageArrive(msg);
            }
        };
    }

    private void onMessageArrive(Message msg) {
        switch (msg.what) {
            case ConstMessages.MSG_NEW_GPS_POINT:
                updateActivityWithNewPoint(
                        (GeoPoint) msg.obj,
                        msg.arg1 == 1);
                break;
            case ConstMessages.MSG_DB_HANDLER_CREATED:
                mHandlerFireBase = (Handler) msg.obj;
                notifyConnection("koko", mstrUid, meUser.name());
                break;
            case ConstMessages.MSG_DB_NEW_USER_CONNECTED:
                onUserConnected((UserLoggedInNotification.UserLoggedInInfo)msg.obj);
                break;
        }
    }

    private void onUserConnected(UserLoggedInNotification.UserLoggedInInfo userUpdateInfo) {
        if(userUpdateInfo.mstrId == mstrUid)
            return;

        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle(userUpdateInfo.mstrName);
        dialog.show();
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
        //initializeHttpThread();
        IntializeDatabaseManager();
        initializeMarkersOverlay();
        initializeUser(eMoveable);
        initializeLocationManager();
    }

    private void addToDb(Object obj) {
        Message msg = mHandlerFireBase.obtainMessage();
        msg.obj = obj;
        msg.what = ConstMessages.MSG_SAVE_NEW_RECORD;
        mHandlerFireBase.sendMessage(msg);
    }

    private void notifyConnection(String strName, String strUid, String strMoveableType) {
        addToDb(new UserLoggedInRecord(strName, strUid, strMoveableType));
    }

    private void initializeLocationManager() {
        LocationManager locManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (!locManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) &&
                !locManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)) {
            return;
        }

        mLocationModuleThread = new LocationModule("LocationModuleThread", this, mHandlerUi);
        mLocationModuleThread.start();
    }

    private void IntializeDatabaseManager() {
        mHandlerThreadFireBase = new DatabaseManager("DatabaseThread", mHandlerUi);
        mHandlerThreadFireBase.start();
    }

    private void initializeUser(eType eMoveable) {
        meUser = eMoveable;
        mstrUid = FirebaseInstanceId.getInstance().getId();
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

    @Override
    public void onConnected(Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
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
        mMyLocationOverlay = new ExMyLocation(mHandlerUi, mMapView.getContext(), mMapView);
        mMapView.getOverlays().add(mMyLocationOverlay);
    }

    private void AddPointToOverlay(final GeoPoint gPt, final int index, int iDrawable) {
        final OverlayItem overlayItem = new OverlayItem("", "", gPt);
        if (Looper.myLooper() == Looper.getMainLooper() && mMapView.getOverlays().size() > 0) {
            ((ExMyLocation) mMapView.getOverlays().get(index)).AddItem(gPt);
        }
    }

    public GoogleApiClient getGoogleApi() {
        return mGoogleApiClient;
    }

    public void updateActivityWithNewPoint(GeoPoint gPt, boolean bSetCenter) {
        addToDb(new HotZoneRecord("koko", mstrUid, gPt.getLatitude(), gPt.getLongitude()));

        if (bSetCenter)
            mMapController.setCenter(gPt);

        AddPointToOverlay(gPt, 0, R.drawable.pin);
        mMapView.invalidate();
    }
}