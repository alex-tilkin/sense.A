package com.exampledemo.parsaniahardik.gpsdemocode;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;

import org.osmdroid.ResourceProxy;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.mylocation.IMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;

/**
 * Created by igalk on 8/12/2017.
 */

class ExMyLocation extends MyLocationNewOverlay {

    private ArrayList<GeoPoint> mList;

    @Override
    public void draw(Canvas canvas, MapView mapView, boolean shadow) {
        //super.draw(canvas, mapView, shadow);
        int radius = 10; // in meters
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

    public ExMyLocation(Context ctx/*, ArrayList<OverlayItem> mOverlayMylocationList*/, MapView mapView) {
        super(ctx, mapView);
        mList = new ArrayList<GeoPoint>();
    }

    public ExMyLocation(Context ctx, MapView mapView, ResourceProxy pResourceProxy) {
        super((IMyLocationProvider) ctx, mapView, pResourceProxy);
    }

    public void AddItem(final GeoPoint gPt) {
        mList.add(gPt);
    }
}