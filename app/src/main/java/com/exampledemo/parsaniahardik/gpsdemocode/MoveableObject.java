package com.exampledemo.parsaniahardik.gpsdemocode;

import org.osmdroid.util.GeoPoint;

import java.util.Date;

/**
 * Created by igalk on 9/1/2017.
 */

public class MoveableObject {
    enum eType {
        ePedestrian,
        eCar
    };

    public GeoPoint mgptLocation; // will hold the real location, either from mgptA, or mgptB
    public GeoPoint mgptA; // the following 2 variables will be uplodated to DB
    public GeoPoint mgptB; // and help us make the direction vector
    public eType meMoveableType; // you can move this variable, you were right, dynamic_cast(instanceof) is a good solution
    public String mstrUid;
    public Long mnTimestamp; // will be sent each time to the DB, with local user time, to let know the DB that something
                             // happened

    public MoveableObject(eType eMoveableObj, String strUid) {
        mstrUid = strUid;
        meMoveableType = eMoveableObj;
        mnTimestamp = new Long(new Date().getTime());
    }

    public void addPoint(GeoPoint gpt) {
        if(null == mgptA) {
            mgptA = gpt;
        } else {
            mgptB = mgptA;
            mgptA = gpt;
        }
    }

    public boolean canCalculateTrajectory() {
        return mgptA != null && mgptB != null;
    }
}

class Car extends MoveableObject {

    public Car(String strUid) {
        super(eType.eCar, strUid);
    }
}

class Pedestrian extends  MoveableObject {
    public Pedestrian(String strUid) {
        super(eType.ePedestrian, strUid);
    }
}