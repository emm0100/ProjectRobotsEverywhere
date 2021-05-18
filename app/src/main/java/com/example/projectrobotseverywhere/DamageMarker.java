package com.example.projectrobotseverywhere;

import java.io.Serializable;

public class DamageMarker implements Serializable {
    private String id;
    private double severity;
    private double latitude;
    private double longitude;
    private String comment;

    public void DamageMarker(double severity, double latitude, double longitude, String comment){
        this.id = String.valueOf(java.lang.System.currentTimeMillis());
        this.severity = severity;
        this.latitude = latitude;
        this.longitude = longitude;
        this.comment = comment;
    }

    public String getId() {
        return id;
    }

    public double getSeverity(){
        return severity;
    }

    public double getLatitude(){
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getComment() {
        return comment;
    }
}
