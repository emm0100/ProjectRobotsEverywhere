package com.example.projectrobotseverywhere;

import java.io.Serializable;

public class DamageMarker implements Serializable {
    //private String id;
    private Double severity;
    private Double latitude;
    private Double longitude;
    private String comment;

    public DamageMarker(Double severity, Double latitude, Double longitude, String comment){
        //this.id = String.valueOf(java.lang.System.currentTimeMillis());
        this.severity = severity;
        this.latitude = latitude;
        this.longitude = longitude;
        this.comment = comment;
    }

    public Double getSeverity(){
        return severity;
    }

    public Double getLatitude(){
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public String getComment() {
        return comment;
    }
}
