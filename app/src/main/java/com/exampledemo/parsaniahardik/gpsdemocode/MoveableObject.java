package com.exampledemo.parsaniahardik.gpsdemocode;

import org.osmdroid.util.GeoPoint;

/**
 * Created by igalk on 9/1/2017.
 */

public class MoveableObject {
    enum eType {
        ePedestrian,
        eCar
    };

    protected GeoPoint mgptA;
    protected GeoPoint mgptB;
    protected eType meMoveableType;
    protected String mstrUid;

    public MoveableObject(eType eMoveableObj, String strUid) {
        mstrUid = strUid;
        meMoveableType = eMoveableObj;
    }

    public void addPoint(GeoPoint gpt) {
        if(null == mgptA) {
            mgptA = gpt;
        } else {
            mgptB = mgptA;
            mgptA = gpt;
        }
    }

    public GeoPoint getMgptA() {
        return mgptA;
    }

    public void setMgptA(GeoPoint mgptA) {
        this.mgptA = mgptA;
    }

    public GeoPoint getMgptB() {
        return mgptB;
    }

    public void setMgptB(GeoPoint mgptB) {
        this.mgptB = mgptB;
    }

    public eType getMeMoveableType() {
        return meMoveableType;
    }

    public void setMeMoveableType(eType meMoveableType) {
        this.meMoveableType = meMoveableType;
    }

    public String getMstrUid() {
        return mstrUid;
    }

    public void setMstrUid(String mstrUid) {
        this.mstrUid = mstrUid;
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