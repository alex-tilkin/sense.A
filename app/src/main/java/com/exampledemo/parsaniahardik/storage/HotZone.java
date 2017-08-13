package com.exampledemo.parsaniahardik.storage;

public class HotZone {
    private String m_Id;
    private double m_Latitude;

    public double getM_Latitude() {
        return m_Latitude;
    }

    public double getM_Longitude() {
        return m_Longitude;
    }

    public int getM_Danger() {
        return m_Danger;
    }

    public String getM_Intersection() {
        return m_Intersection;
    }

    private double m_Longitude;
    private int m_Danger;
    private String m_Intersection;

    public HotZone(){

    }
    public HotZone(String id, double latitude, double longitude, int danger, String intersection) {
        m_Id = id;
        m_Latitude = latitude;
        m_Longitude = longitude;
        m_Danger = danger;
        m_Intersection = intersection;
    }

    public String getM_Id() {
        return m_Id;
    }
}