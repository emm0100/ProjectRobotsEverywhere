package com.example.projectrobotseverywhere;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class FirebaseAdapter {
    // declare list that keeps track of all observers who needs to be updated if something changes
    private final List<FirebaseObserver> observers;
    private final FirebaseFirestore fireStoreReference;

    public FirebaseAdapter() {
        fireStoreReference = FirebaseFirestore.getInstance();
        observers = new ArrayList<>();
    }

    public void attachObserver(FirebaseObserver observer) {
        observers.add(observer);
    }

    public void detachObserver(FirebaseObserver observer) {
        observers.remove(observer);
    }

    public FirebaseFirestore getFirestoreReference(){
        return fireStoreReference;
    }

    /**
     * Notifies each observer that is in the observer list
     *
     * @param adapter the adapter that runs this method
     * @param damageMarkerMap  the data that needs to be transferred to to observer
     */
    public void notifyObservers(FirebaseAdapter adapter, Map<String, DamageMarker> damageMarkerMap) {
        for (FirebaseObserver observer : observers) {
            observer.firebaseUpdate(adapter, damageMarkerMap);
        }
    }
}

