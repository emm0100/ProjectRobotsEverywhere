package com.example.projectrobotseverywhere;

import java.util.Map;

/**
 * Interface for a FirebaseObserver object.
 */
public interface FirebaseObserver {

    void firebaseUpdate(FirebaseAdapter adapter, Map<String, DamageMarker> damageMarkerMap);

}
