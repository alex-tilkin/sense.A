package com.exampledemo.parsaniahardik.gpsdemocode;

import android.graphics.drawable.Drawable;
import android.location.Location;
import android.view.MotionEvent;

import org.osmdroid.ResourceProxy;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.OverlayItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by igalk on 8/9/2017.
 */

public class ExtendedItemizedIconOverlay <Item extends OverlayItem> extends ItemizedIconOverlay {
    private MainActivity oSender;

    public ExtendedItemizedIconOverlay(List pList, Drawable pDefaultMarker, OnItemGestureListener pOnItemGestureListener, ResourceProxy pResourceProxy) {
        super(pList, pDefaultMarker, pOnItemGestureListener, pResourceProxy);
    }

    public ExtendedItemizedIconOverlay(MainActivity mainActivity, ArrayList<OverlayItem> mOverlayItemArrayList, Object o) {
        super(mainActivity, mOverlayItemArrayList, (OnItemGestureListener) o);
        oSender = mainActivity;
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent event, final MapView mapView) {
        MapView.Projection proj = mapView.getProjection();

        GeoPoint loc = (GeoPoint)proj.fromPixels(
                (int)event.getX(),
                (int)event.getY()
        );

        Location l = new Location("Manual");
        l.setLatitude(((double)loc.getLatitudeE6())/1000000);
        l.setLongitude(((double)loc.getLongitudeE6())/1000000);

        //oSender.onLocationChanged(l, false);
        return true;
    }
}
