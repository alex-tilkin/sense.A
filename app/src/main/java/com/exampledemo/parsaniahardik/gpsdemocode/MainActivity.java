package com.exampledemo.parsaniahardik.gpsdemocode;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
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

class ConstMessages {
    public static final int MSG_NEW_GPS_POINT = 1;
    public static final int MSG_SAVE_NEW_RECORD = MSG_NEW_GPS_POINT + 1;
    public static final int MSG_DB_HANDLER_CREATED = MSG_SAVE_NEW_RECORD + 1;
    public static final int MSG_DB_NEW_USER_CONNECTED = MSG_DB_HANDLER_CREATED + 1;
    public static final int MSG_NEW_HOT_ZONE_POINT_ARRIVED = MSG_DB_NEW_USER_CONNECTED + 1;
}

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    //region Fields
    private static final String TAG = "MainActivity";
    private GoogleApiClient mGoogleApiClient;
    private Handler mHandlerUi;
    LocationModule mLocationModuleThread;

    private ExMyLocation mMyLocationOverlay;

    private MapView mMapView;
    private MapController mMapController;

    private DatabaseManager mHandlerThreadFireBase;
    private Handler mHandlerFireBase;

    private MoveableObject mMoveable;

    private static final String PREFERENCES_NAME = "Preferences";
    //endregion

    //region Methods
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

    @Override
    protected void onStart() {
        super.onStart();

        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
        initializeAppContent(MoveableObject.eType.ePedestrian);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }

        // We need an Editor object to make preference changes.
        // All objects are from android.context.Context
        SharedPreferences settings = getSharedPreferences(PREFERENCES_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        //editor.putBoolean("silentMode", mSilentMode);

        // Commit the edits!
        editor.commit();
    }

    @Override
    public void onConnected(Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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
    //endregion

    //region Helpers
    private void initializeAppContent(MoveableObject.eType eMoveable) {
        initializeMap();
        IntializeDatabaseManager();
        initializeMarkersOverlay();
        initializeUser(eMoveable);
        initializeLocationManager();
    }

    private void initializeMap() {
        mMapView = (MapView) findViewById(R.id.mapview);
        mMapView.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE);
        mMapView.setBuiltInZoomControls(true);
        mMapController = (MapController) mMapView.getController();
        mMapController.setZoom(100);
    }

    private void IntializeDatabaseManager() {
        mHandlerThreadFireBase = new DatabaseManager("DatabaseThread", mHandlerUi);
        mHandlerThreadFireBase.start();
    }

    private void initializeMarkersOverlay() {
        mMyLocationOverlay = new ExMyLocation(mHandlerUi, mMapView.getContext(), mMapView);
        mMapView.getOverlays().add(mMyLocationOverlay);
    }

    private void initializeUser(MoveableObject.eType eMoveable) {
        switch (eMoveable) {
            case ePedestrian:
                mMoveable = new Pedestrian(FirebaseInstanceId.getInstance().getId());
                break;
            case eCar:
                mMoveable = new Car(FirebaseInstanceId.getInstance().getId());
                break;
        }
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

    private void updateActivityWithNewPoint(GeoPoint gPt, String strSenderUid, boolean bSetCenter, int iMarkerClr) {
        if (bSetCenter)
            mMapController.setCenter(gPt);

        AddPointToOverlay(gPt, strSenderUid, 0, R.drawable.pin, iMarkerClr);
        mMapView.invalidate();
    }

    private void onMessageArrive(Message msg) {
        switch (msg.what) {
            case ConstMessages.MSG_NEW_GPS_POINT:
                addToDb(new HotZoneRecord(
                        "koko",
                        mMoveable.mstrUid,
                        ((GeoPoint)msg.obj).getLatitude(),
                        ((GeoPoint)msg.obj).getLongitude()));

                updateActivityWithNewPoint(
                        (GeoPoint) msg.obj,
                        mMoveable.mstrUid,
                        msg.arg1 == 1,
                        Color.BLUE);

                mMoveable.addPoint(((GeoPoint)msg.obj));
                break;
            case ConstMessages.MSG_DB_HANDLER_CREATED:
                mHandlerFireBase = (Handler) msg.obj;
                notifyConnection("koko",
                        mMoveable.mstrUid,
                        mMoveable.meMoveableType.name());
                break;
            case ConstMessages.MSG_DB_NEW_USER_CONNECTED:
                onUserConnected((UserLoggedInRecord)msg.obj);
                break;
            case ConstMessages.MSG_NEW_HOT_ZONE_POINT_ARRIVED:
                onNewHotzonePntArrived((HotZoneRecord)msg.obj);
                break;
        }
    }

    private void onNewHotzonePntArrived(HotZoneRecord obj) {
        if(obj.mstrId.equals(mMoveable.mstrUid))
            return;

        updateActivityWithNewPoint(
                new GeoPoint(
                obj.mdbLatitude,
                obj.mdbLongtitude),
                obj.mstrId
                , false, Color.RED);
    }

    private void onUserConnected(UserLoggedInRecord userUpdateInfo) {
        if(userUpdateInfo.mstrId.equals(mMoveable.mstrUid))
            return;

        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle(userUpdateInfo.mstrName);
        dialog.show();
    }

    private void addToDb(Object obj) {
        Message msg = mHandlerFireBase.obtainMessage();
        msg.obj = obj;
        msg.what = ConstMessages.MSG_SAVE_NEW_RECORD;
        mHandlerFireBase.sendMessage(msg);
    }

    private void notifyConnection(String strName, String strUid, String strMoveableType) {
        addToDb(new UserLoggedInRecord(strName, strUid, strMoveableType, new HotZoneRecord("", "", 0.0, 0.0)));
    }

    private void AddPointToOverlay(final GeoPoint gPt, String strSenderUid, final int index, int iDrawable, int iMarkerClr) {
        if (Looper.myLooper() == Looper.getMainLooper() && mMapView.getOverlays().size() > 0) {
            ((ExMyLocation)mMapView
                    .getOverlays()
                    .get(index))
                    .AddItem(new ExMyLocationPoint(
                            gPt,
                            strSenderUid,
                            iMarkerClr));
        }
    }

    private void decideUserType() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setCancelable(false)
                .setPositiveButton("Vehicle", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                        initializeAppContent(MoveableObject.eType.eCar);
                    }
                })
                .setNegativeButton("Pedestrian", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                        initializeAppContent(MoveableObject.eType.ePedestrian);
                    }
                });
        dialog.show();
    }
    //endregion
}