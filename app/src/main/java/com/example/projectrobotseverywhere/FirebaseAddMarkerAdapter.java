package com.example.projectrobotseverywhere;

import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.annotations.Nullable;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.jetbrains.annotations.NotNull;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import static android.content.ContentValues.TAG;

public class FirebaseAddMarkerAdapter extends FirebaseAdapter {
    private final Map<String, DamageMarker> markers;
    private final FirebaseFirestore firebaseFirestore;

    public FirebaseAddMarkerAdapter() {
        super();
        markers = new HashMap<>();
        firebaseFirestore = FirebaseFirestore.getInstance();

        syncMarkerEntries();
    }

    /**
     * Notifies the observers when a data change occurs.
     */
    private void syncMarkerEntries() {
        firebaseFirestore.collection("markers")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value,
                                        @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.w(TAG, "Listen failed.", e);
                            return;
                        }
                        markers.clear();
                        for (QueryDocumentSnapshot document : value) {
                            Object severityObj = document.get("severity");
                            Object latitudeObj = document.get("latitude");
                            Object longitudeObj = document.get("longitude");
                            Object commentObj = document.get("comment");

                            // Skip the document if any of these values are non-existent
                            if (severityObj == null ||
                                latitudeObj == null ||
                                longitudeObj == null)
                            {
                                Log.w(TAG, "Marker document is invalid");
                                continue;
                            }

                            String severity = severityObj.toString();
                            String latitude = latitudeObj.toString();
                            String longitude = longitudeObj.toString();
                            String comment = "";
                            if (commentObj != null) {
                                comment = commentObj.toString();
                            }

                            DamageMarker damageMarker = new DamageMarker(
                                    Double.parseDouble(severity),
                                    Double.parseDouble(latitude),
                                    Double.parseDouble(longitude),
                                    comment);
                            markers.put(document.getId(), damageMarker);
                        }
                        // notify attached observers that something has changed
                        notifyObservers(FirebaseAddMarkerAdapter.this, markers);
                    }
                });
    }

    /**
     * Get the list of all markers in the database.
     *
     * @return a map containing all markers in the database with ID as key
     */
    public Map<String, DamageMarker> getMarkers(){
        return markers;
    }

    /**
     * Push marker to the Firebase FireStore.
     *
     * @param damageMarker the marker to be pushed
     */
    public void addDamageMarker(DamageMarker damageMarker, View view) {
        firebaseFirestore.collection("markers")
                .document(String.valueOf(Calendar.getInstance().getTimeInMillis()))
                .set(damageMarker).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Toast.makeText(view.getContext(),
                                  "New marker successfully added",
                                       Toast.LENGTH_LONG).show();
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull @NotNull Exception e) {
                        Toast.makeText(view.getContext(),
                                  "Something went wrong",
                                       Toast.LENGTH_LONG).show();
                    }
                });
    }

    /**
     * Delete the DamageMarker with the given ID from the database
     *
     * @param markerId The ID of the marker to be deleted
     */
    public void deleteDamageMarker(String markerId, View view) {
        firebaseFirestore.collection("markers")
                .document(markerId)
                .delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Toast.makeText(view.getContext(),
                                "Marker successfully deleted",
                                Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull @NotNull Exception e) {
                        Toast.makeText(view.getContext(),
                                "Something went wrong",
                                Toast.LENGTH_LONG).show();
                    }
                });
    }
}
