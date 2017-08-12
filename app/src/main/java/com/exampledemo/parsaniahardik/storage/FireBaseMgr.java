package com.exampledemo.parsaniahardik.storage;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.osmdroid.util.GeoPoint;

public class FireBaseMgr {

    //region Fields
    private FirebaseDatabase m_Database = null;
    private static FireBaseMgr m_Instance = null;
    //endregion

    //region API
    public static FireBaseMgr getInstace(){
        return m_Instance == null ? m_Instance = new FireBaseMgr() : m_Instance;
    }

    public void sendIntersection(GeoPoint geoPoint){
        //mDatabase.child("intersections");
    }

    public void sendMsg(){
        DatabaseReference myRef = m_Database.getReference("message");

        myRef.setValue("Hello, World!");
    }
    //endregion

    //region Helpers
    private FireBaseMgr() {
        m_Database = FirebaseDatabase.getInstance();
    }
    //endregion
}
