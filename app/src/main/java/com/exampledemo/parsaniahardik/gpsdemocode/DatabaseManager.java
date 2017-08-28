package com.exampledemo.parsaniahardik.gpsdemocode;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Vector;

/**
 * Created by igalk on 8/24/2017.
 */

interface DbRecord {

}

public class DatabaseManager extends HandlerThread {
    final private Handler mHandlerUi;
    private Handler mHandlerSelf;

    private FirebaseDatabase mDatabase;
    private DatabaseReference mDBRefHotzones;
    private DatabaseReference mDBRefUsers;
    private List<Query> mListQueries;

    public DatabaseManager(String name, Handler handlerUi) {
        super(name);

        mHandlerUi = handlerUi;
        mListQueries = new Vector<Query>();

        intializeDb();
    }

    private void intializeDb() {
        mDatabase = FirebaseDatabase.getInstance();

        initializeDbReferences();
        initializeQueries();
    }

    private void initializeQueries() {
        mListQueries.add(getQueryLastInsertedRecordByKey(mDBRefUsers, new UserLoggedInNotification(this)));
        mListQueries.add(getQueryLastInsertedRecordByKey(mDBRefHotzones, new HotZoneNotification(this)));
    }

    private Query getQueryLastInsertedRecordByKey(DatabaseReference db, EventGetLastChildrenAdded events) {
        Query query = db.orderByKey().limitToLast(1);
        query.addChildEventListener((ChildEventListener)events);
        query.addListenerForSingleValueEvent((ValueEventListener)events);
        return query;
    }

    private void initializeDbReferences() {
        mDBRefHotzones =  mDatabase.getReference("/hotZones/");
        mDBRefUsers = mDatabase.getReference("/users/");
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

    public void onTableDataChange(DbRecord dbChangeInfo, int nEventType) {
        Message msg = mHandlerUi.obtainMessage();
        msg.what = nEventType;
        msg.obj = dbChangeInfo;
        mHandlerUi.sendMessage(msg);
    }
}

    abstract class EventGetLastChildrenAdded implements
        ValueEventListener, ChildEventListener {

        protected boolean mbInitialized;

        public EventGetLastChildrenAdded() {
            mbInitialized = false;
        }

        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            mbInitialized = true;
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {

        }

        @Override
        public void onChildChanged(DataSnapshot dataSnapshot, String s) {

        }

        @Override
        public void onChildRemoved(DataSnapshot dataSnapshot) {

        }

        @Override
        public void onChildMoved(DataSnapshot dataSnapshot, String s) {

        }
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
        extends EventGetLastChildrenAdded {

    private final DatabaseManager mdbMngr;

    public UserLoggedInNotification(DatabaseManager db) {
        super();
        mdbMngr = db;
    }

    @Override
    public void onChildAdded(DataSnapshot dataSnapshot, String s) {
        if(mbInitialized) {
            mdbMngr.onTableDataChange(dataSnapshot.getValue(UserLoggedInRecord.class)
                    , ConstMessages.MSG_DB_NEW_USER_CONNECTED);
        }
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
        extends EventGetLastChildrenAdded {

    private final DatabaseManager mdbMngr;

    public HotZoneNotification (DatabaseManager db) {
        mdbMngr = db;
    }

    @Override
    public void onChildAdded(DataSnapshot dataSnapshot, String s) {
        if(mbInitialized) {
            mdbMngr.onTableDataChange(dataSnapshot.getValue(HotZoneRecord.class)
                    , ConstMessages.MSG_NEW_HOT_ZONE_POINT_ARRIVED);
        }
    }
}
