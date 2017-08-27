package com.exampledemo.parsaniahardik.gpsdemocode;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by igalk on 8/24/2017.
 */

public class DatabaseManager extends HandlerThread {
    final private Handler mHandlerUi;
    private Handler mHandlerSelf;

    private FirebaseDatabase mDatabase;
    private DatabaseReference mDBRefHotzones;
    private DatabaseReference mDBRefUsers;

    public DatabaseManager(String name, Handler handlerUi) {
        super(name);

        mHandlerUi = handlerUi;
        intializeDb();
    }

    private void setDbRefTriggers(DatabaseReference dbRef, ValueEventListener dbNotifys) {
        dbRef.addValueEventListener(dbNotifys);
    }

    private void intializeDb() {
        mDatabase = FirebaseDatabase.getInstance();
        mDBRefHotzones =  mDatabase.getReference("hotZones");
        mDBRefUsers = mDatabase.getReference("users");
        setDbRefTriggers(mDBRefUsers, new UserLoggedInNotification(this));
        setDbRefTriggers(mDBRefHotzones, new HotZoneNotification(this));
    }

    @Override
    public void onLooperPrepared() {
        mHandlerSelf = new Handler(getLooper()) {

            @Override
            public void handleMessage(Message msg) {
                onMsg(msg);
            }
        };

        // not sure it's the right way
        Message msg = new Message();
        msg.what = ConstMessages.MSG_DB_HANDLER_CREATED;
        msg.obj = mHandlerSelf;
        mHandlerUi.sendMessage(msg);
    };

    public void SaveContent(DbRecord obj) {
        if(obj instanceof HotZoneRecord) {
            mDBRefHotzones.child(""). push().setValue((HotZoneRecord)obj);
        } else if (obj instanceof UserLoggedInRecord) {
            mDBRefUsers.child("").push().setValue((UserLoggedInRecord)obj);
        }
    }

    private void onMsg(Message msg) {
        switch(msg.what) {
            case ConstMessages.MSG_SAVE_NEW_RECORD:
                SaveContent((DbRecord)msg.obj);
        }
    }

    public Handler getHandler() {
        return mHandlerSelf;
    }

    public void onTableDataChange(DbChangeRcrd dbChangeInfo, int nEventType) {
        Message msg = mHandlerUi.obtainMessage();
        msg.what = nEventType;
        msg.obj = dbChangeInfo;
        mHandlerUi.sendMessage(msg);
    }
}

interface DbRecord {

}

interface DbChangeRcrd {

}

class UserLoggedInRecord implements DbRecord {
    String mstrName;
    String mstrId;
    String mstrLogTime;
    String mstrType;

    public UserLoggedInRecord(String strName, String strId, String strType){
        mstrName = strName;
        mstrType = strType;
        mstrLogTime = new SimpleDateFormat("yyyy:MM:dd::HH:mm:ss").format(new Date());
        mstrId = strId;
    }

    public UserLoggedInRecord() {}

    public String getMstrLogTime() {
        return mstrLogTime;
    }

    public String getMstrName() {
        return mstrName;
    }

    public String getMstrType() {
        return mstrType;
    }

    public String getMstrId() {
        return mstrId;
    }
}

class UserLoggedInNotification
        implements ValueEventListener {

    class UserLoggedInInfo
            implements DbChangeRcrd
    {
        public String mstrName;
        public String mstrId;

        public UserLoggedInInfo(
                String strName,
                String strId) {
            mstrName = strName;
            mstrId = strId;
        }
    }

    private final DatabaseManager mdbMngr;

    public UserLoggedInNotification(DatabaseManager db) {
        mdbMngr = db;
    }

    @Override
    public void onDataChange(DataSnapshot dataSnapshot) {
        UserLoggedInRecord p = dataSnapshot.getValue(UserLoggedInRecord.class);
        // add protection
        if(dataSnapshot.getValue() != null) {// means a new table was created
            Iterable<DataSnapshot> itData = dataSnapshot.getChildren();
            mdbMngr.onTableDataChange(new UserLoggedInInfo(
                            dataSnapshot.child("mstrName").getValue().toString(),
                            dataSnapshot.child("mstrId").getValue().toString())
                    , ConstMessages.MSG_DB_NEW_USER_CONNECTED);
        }
    }

    @Override
    public void onCancelled(DatabaseError databaseError) {

    }
}

class HotZoneRecord implements DbRecord {
    String mstrName;
    String mstrId;
    Double mdbLatitude;
    Double mdbLongtitude;

    public String getMstrName() {
        return mstrName;
    }

    public String getMstrId() {
        return mstrId;
    }

    public double getMdbLatitude() {
        return mdbLatitude;
    }

    public double getMdbLongtitude() {
        return mdbLongtitude;
    }

    public HotZoneRecord(
            String strName,
            String strId,
            Double dbLatitude,
            Double dbLongtitude) {
        mstrName = strName;
        mstrId = strId;
        mdbLatitude = dbLatitude;
        mdbLongtitude = dbLongtitude;
    }
}

class HotZoneNotification
        implements ValueEventListener {

    class HotZoneInfo
            implements DbChangeRcrd {

        public String mstrName;
        public String mstrId;
        public Double mdbLongtitude;
        public Double mdbLatitude;

        public HotZoneInfo(
                String strName,
                String strId,
                Double dbLatitude,
                Double dbLongtitude) {
            mstrName = strName;
            mstrId = strId;
            mdbLatitude = dbLatitude;
            mdbLongtitude = dbLongtitude;
        }
    }

    private final DatabaseManager mdbMngr;

    public HotZoneNotification (DatabaseManager db) {
        mdbMngr = db;
    }

    @Override
    public void onDataChange(DataSnapshot dataSnapshot) {
        mdbMngr.onTableDataChange(new HotZoneNotification.HotZoneInfo(
                dataSnapshot.child("mstrName").getValue().toString(),
                dataSnapshot.child("mstrId").getValue().toString(),
                (Double)dataSnapshot.child("mdbLongtitude").getValue(),
                (Double)dataSnapshot.child("mdbLatitude").getValue())
                , ConstMessages.MSG_NEW_HOT_ZONE_POINT_ARRIVED);
    }

    @Override
    public void onCancelled(DatabaseError databaseError) {

    }
}
