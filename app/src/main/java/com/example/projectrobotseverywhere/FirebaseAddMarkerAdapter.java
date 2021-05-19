package com.example.projectrobotseverywhere;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.jetbrains.annotations.NotNull;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static android.content.ContentValues.TAG;

public class FirebaseAddMarkerAdapter extends FirebaseAdapter {
    private Map<String, DamageMarker> markers;
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
        markers.clear();
        firebaseFirestore.collection("markers")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                DamageMarker damageMarker = new DamageMarker(
                                        (Double) document.get("severity"),
                                        (Double) document.get("latitude"),
                                        (Double) document.get("longitude"),
                                        (String) document.get("comment"));
                                markers.put(document.getId(), damageMarker);
                            }
                        } else {
                            Log.w(TAG, "Error getting documents.", task.getException());
                        }
                    }
                });
        notifyObservers(FirebaseAddMarkerAdapter.this, markers);
    }

    /**
     * Get the list of all markers in the database.
     *
     * @return a map containing all markers in the database with ID as key
     */
    public Map getMarkers(){
        return markers;
    }

    /**
     * Push marker to the FireBase database.
     *
     * @param damageMarker the marker to be pushed
     */
    public void addDamageMarker(DamageMarker damageMarker) {
        firebaseFirestore.collection("markers")
                .document(String.valueOf(Calendar.getInstance().getTimeInMillis()))
                .set(damageMarker).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        System.out.println("Marker successfully added");
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull @NotNull Exception e) {
                        System.out.println("Failed to add marker, error: " + e);
                    }
                });
    }
}
