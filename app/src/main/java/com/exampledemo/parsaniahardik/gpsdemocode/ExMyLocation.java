package com.exampledemo.parsaniahardik.gpsdemocode;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.os.Handler;
import android.os.Message;
import android.view.MotionEvent;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;

/**
 * Created by igalk on 8/12/2017.
 */

class ExMyLocationPoint {
    public int miColor;
    public GeoPoint mGpt;

    public ExMyLocationPoint(GeoPoint gPt, int iMarkerColor) {
        miColor = iMarkerColor;
        mGpt = gPt;
    }
}

class ExMyLocation extends MyLocationNewOverlay {


    private final Handler mHandlerUi;
    private ArrayList<ExMyLocationPoint> mList;

    @Override
    public void draw(Canvas canvas, MapView mapView, boolean shadow) {
        int radius = 3; // in meters
        MapView.Projection projection = mapView.getProjection();
        float actualRadius = projection.metersToEquatorPixels(radius) *
                mapView.getResources().getDisplayMetrics().density;

        Paint paint = new Paint();
        paint.setColor(Color.BLUE);
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        paint.setAlpha(50);

        for(final ExMyLocationPoint exLocPoint : mList) {
            Point p = new Point();
            paint.setColor(exLocPoint.miColor);
            projection.toMapPixels(new GeoPoint(
                    exLocPoint.mGpt.getLatitude(),
                    exLocPoint.mGpt.getLongitude()), p);

            canvas.drawCircle(p.x, p.y, actualRadius, paint);
        }
    }

    @Override
    public boolean onLongPress(MotionEvent e, MapView pMapView) {
        return true;
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent event, MapView map) {
        MapView.Projection proj = map.getProjection();

        GeoPoint loc = (GeoPoint)proj.fromPixels(
                event.getX(),
                event.getY()
        );

        Message msg = mHandlerUi.obtainMessage();
        msg.what = ConstMessages.MSG_NEW_GPS_POINT;
        msg.obj = loc;
        return mHandlerUi.sendMessage(msg);
    }

    public ExMyLocation(Handler handlerUi, Context ctx, MapView mapView) {
        super(ctx, mapView);
        mList = new ArrayList<ExMyLocationPoint>();
        mHandlerUi = handlerUi;
    }

    public void AddItem(final ExMyLocationPoint gPt) {
        mList.add(gPt);
    }
}