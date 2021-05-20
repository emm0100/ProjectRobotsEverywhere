package com.example.projectrobotseverywhere;

import java.io.Serializable;

public class DamageMarker implements Serializable {
    private final Double severity;
    private final Double latitude;
    private final Double longitude;
    private final String comment;

    /**
     * Construct a new DamageMarker object.
     * @param severity Severity of the damage
     * @param latitude Latitude coordinate
     * @param longitude Longitude coordinate
     * @param comment Optional comment, empty string if not available
     */
    public DamageMarker(Double severity, Double latitude, Double longitude, String comment){
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
