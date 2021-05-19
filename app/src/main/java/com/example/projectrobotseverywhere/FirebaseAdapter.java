package com.example.projectrobotseverywhere;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public abstract class FirebaseAdapter {
    // declare list that keeps track of all observers who needs to be updated if something changes
    private final List<FirebaseObserver> observers;
    private final FirebaseFirestore firestoreReference;

    public FirebaseAdapter() {
        firestoreReference = FirebaseFirestore.getInstance();
        observers = new ArrayList<>();
    }

    public void attachObserver(FirebaseObserver observer) {
        observers.add(observer);
    }

    public void detachObserver(FirebaseObserver observer) {
        observers.remove(observer);
    }

    public FirebaseFirestore getFirestoreReference(){
        return firestoreReference;
    }

    /**
     * Notifies each observer that is in the observer list
     *
     * @param adapter the adapter that runs this method
     * @param arg     the data that needs to be transferred to to observer
     */
    public void notifyObservers(FirebaseAdapter adapter, Object arg) {
        for (FirebaseObserver observer : observers) {
            observer.firebaseUpdate(adapter, arg);
        }
    }
}

