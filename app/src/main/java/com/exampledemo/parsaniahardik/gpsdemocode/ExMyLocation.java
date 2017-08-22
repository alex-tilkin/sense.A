package com.exampledemo.parsaniahardik.gpsdemocode;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.location.Location;
import android.view.MotionEvent;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;

/**
 * Created by igalk on 8/12/2017.
 */

class ExMyLocation extends MyLocationNewOverlay {

    private ArrayList<GeoPoint> mList;
    private MainActivity mSender;

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

        for(final GeoPoint gPt : mList) {
            Point p = new Point();
            projection.toMapPixels(new GeoPoint(gPt.getLatitude(),
                    gPt.getLongitude()), p);

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
                (int)event.getX(),
                (int)event.getY()
        );

        Location l = new Location("Manual");
        l.setLatitude(((double)loc.getLatitudeE6())/1000000);
        l.setLongitude(((double)loc.getLongitudeE6())/1000000);

        //mSender.onLocationChanged(l, false);
        return true;
    }

    public ExMyLocation(MainActivity oSender, Context ctx, MapView mapView) {
        super(ctx, mapView);
        mSender = oSender;
        mList = new ArrayList<GeoPoint>();
    }

    public void AddItem(final GeoPoint gPt) {
        mList.add(gPt);
    }
}