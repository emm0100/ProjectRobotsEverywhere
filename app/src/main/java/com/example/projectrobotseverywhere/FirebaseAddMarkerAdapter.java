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
                            DamageMarker damageMarker = new DamageMarker(
                                    (Double) document.get("severity"),
                                    (Double) document.get("latitude"),
                                    (Double) document.get("longitude"),
                                    (String) document.get("comment"));
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
    public Map getMarkers(){
        return markers;
    }

    /**
     * Push marker to the FireBase database.
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
}
