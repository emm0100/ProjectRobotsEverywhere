package com.example.projectrobotseverywhere;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.infowindow.MarkerInfoWindow;

import java.util.Map;

public class DamageMarkerInfoWindow extends MarkerInfoWindow {

    private final FirebaseAddMarkerAdapter firebaseAddMarkerAdapter;

    public DamageMarkerInfoWindow(MapView mapView, FirebaseAddMarkerAdapter firebaseAddMarkerAdapter) {
        super(R.layout.custom_info_window, mapView);
        this.firebaseAddMarkerAdapter = firebaseAddMarkerAdapter;
    }

    public void onOpen(Object item) {
        Marker marker = (Marker) item;
        String markerId = marker.getId();
        Map<String, DamageMarker> damageMarkers = firebaseAddMarkerAdapter.getMarkers();
        DamageMarker damageMarker = damageMarkers.get(markerId);

        // if something goes horribly wrong
        if (damageMarker == null) { return; }

        setTextValues(damageMarker);

        // Configure delete button
        Button deleteMarkerButton = mView.findViewById(R.id.deleteMarkerButton);
        deleteMarkerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Show alert to confirm delete action
                AlertDialog.Builder alertBuilder = new AlertDialog.Builder(mView.getContext());
                alertBuilder.setMessage("Are you sure you want to delete this marker?")
                        .setTitle("Confirmation")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                firebaseAddMarkerAdapter.deleteDamageMarker(markerId, mView);
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) { }
                        });
                // Show dialog
                alertBuilder.show();
            }
        });
    }

    public void onClose() { }

    // Change text values in InfoWindow to display correct values for the given DamageMarker
    private void setTextValues(DamageMarker damageMarker) {
        TextView severityValue = mView.findViewById(R.id.severityValue);
        TextView latitudeValue = mView.findViewById(R.id.latitudeValue);
        TextView longitudeValue = mView.findViewById(R.id.longitudeValue);
        TextView commentValue = mView.findViewById(R.id.commentValue);

        severityValue.setText(String.valueOf(damageMarker.getSeverity()));
        latitudeValue.setText(String.valueOf(damageMarker.getLatitude()));
        longitudeValue.setText(String.valueOf(damageMarker.getLongitude()));
        commentValue.setText(damageMarker.getComment());
    }
}
