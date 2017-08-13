package com.exampledemo.parsaniahardik.storage;

import android.util.Log;

import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.osmdroid.util.GeoPoint;

import java.text.MessageFormat;

public class FireBaseMgr {
    private static final String TAG = FireBaseMgr.class.getSimpleName();

    //region Fields
    private FirebaseDatabase m_Database = null;
    private static FireBaseMgr m_Instance = null;
    //endregion

    //region API
    public static FireBaseMgr getInstace(){
        return m_Instance == null ? m_Instance = new FireBaseMgr() : m_Instance;
    }

    public void sendIntersection(GeoPoint geoPoint){
        DatabaseReference myRef = m_Database.getReference(Keys.HOT_ZONES);

        double latitude = geoPoint.getLatitude();
        double longitude = geoPoint.getLongitude();
        String id = new StringBuilder(MessageFormat.format("{1,number,#.####},{0,number,#.####}", latitude, longitude)).toString();
        myRef.setValue(new HotZone(id, latitude, longitude, 100, Type.INTERSECTION), new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                Log.d(TAG, "onComplete: " + databaseError);
            }
        });
    }
    //endregion

    //region Helpers
    private FireBaseMgr() {
        m_Database = FirebaseDatabase.getInstance();
    }

    private class Keys {
        public final static String HOT_ZONES = "hotZones";
    }

    private class Type {
        private final static String INTERSECTION = "intersection";
    }


    //endregion
}
