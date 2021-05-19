package com.example.projectrobotseverywhere;

import android.graphics.drawable.Drawable;
import android.location.Address;
import android.os.Bundle;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.StrictMode;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.osmdroid.api.IMapController;
import org.osmdroid.bonuspack.location.GeocoderNominatim;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.content.ContentValues.TAG;


public class MainActivity extends AppCompatActivity {

    private FirebaseFirestore firebaseFirestore;
    private FirebaseAddMarkerAdapter firebaseAddMarkerAdapter;

    private final int REQUEST_PERMISSIONS_REQUEST_CODE = 1;

    // Default location set to TU/e
    private final double DEFAULT_LATITUDE = 51.4484098;
    private final double DEFAULT_LONGITUDE = 5.4907148;

    private MapView map = null;
    private EditText searchInput;
    private IMapController mapController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // init firebase stuff
        firebaseFirestore = FirebaseFirestore.getInstance();
        firebaseAddMarkerAdapter = new FirebaseAddMarkerAdapter();

        // Allow network calls on main thread, possibly temp solution,
        // TODO: should be async (java.util.concurrent)
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        // osmdroid stuff
        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));

        //inflate and create the map
        setContentView(R.layout.activity_main);

        searchInput = findViewById(R.id.searchText);

        // search confirmed with done button on phone keyboard
        searchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                searchInputConfirmed();
                return true;
            }
            return false;
        });

        // Search button onclick
        ImageButton searchButton = findViewById(R.id.searchButton);
        searchButton.setOnClickListener(view -> searchInputConfirmed());

        ImageButton addMarkerButton = findViewById(R.id.addMarkerButton);
        addMarkerButton.setOnClickListener(view -> showAddMarkerPopup(view));

        // Request permissions for GPS and storage use
        requestPermissionsIfNecessary(new String[] {
                // if you need to show the current location, uncomment the line below
                Manifest.permission.ACCESS_FINE_LOCATION,
                // WRITE_EXTERNAL_STORAGE is required in order to show the map
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        });

        // Configure map
        map = findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.setTilesScaledToDpi(true); // makes font larger i.e. more readable

        mapController = map.getController();
        mapController.setZoom(15.0);
        GeoPoint startPoint = new GeoPoint(DEFAULT_LATITUDE, DEFAULT_LONGITUDE);
        mapController.setCenter(startPoint);

        getMarkers();
    }

    private void getMarkers() {
        map.getOverlays().clear();
        Map<String, DamageMarker> damageMarkers = new HashMap<>();
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
                                damageMarkers.put(document.getId(), damageMarker);
                            }
                            drawMarkers(damageMarkers);
                        } else {
                            Log.w(TAG, "Error getting documents.", task.getException());
                        }
                    }
                });
    }

    private void drawMarkers(Map<String, DamageMarker> damageMarkers) {
        for (DamageMarker damageMarker: damageMarkers.values()) {
            Double severity = damageMarker.getSeverity();
            Drawable markerIcon = severity > 7 ? this.getDrawable(R.drawable.damage_b) :
                    (severity > 3 ? this.getDrawable(R.drawable.damage_m) :
                            this.getDrawable(R.drawable.damage_l));
            GeoPoint location = new GeoPoint(damageMarker.getLatitude(), damageMarker.getLongitude());
            System.out.println("Severity: " + severity);
            System.out.println("Lat: " + damageMarker.getLatitude());
            System.out.println("Long: " + damageMarker.getLongitude());
            addMarkerToMapView(markerIcon, location);
            map.invalidate();
        }
    }

    private void addMarkerToMapView(Drawable markerIcon, GeoPoint startPoint) {
        Marker startMarker = new Marker(map);
        startMarker.setIcon(markerIcon);
        startMarker.setPosition(startPoint);
        startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        map.getOverlays().add(startMarker);
    }

    private void searchInputConfirmed() {
        String inputLocation = searchInput.getText().toString();
        List<Address> addressList;

        if (!inputLocation.equals("")) {
            GeocoderNominatim geocoder = new GeocoderNominatim("ProjectRobotsEverywhere");
            try {
                addressList = geocoder.getFromLocationName(inputLocation, 1);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
            Address address = addressList.get(0);
            double inputLatitude = address.getLatitude();
            double inputLongitude = address.getLongitude();
            GeoPoint inputPoint = new GeoPoint(inputLatitude, inputLongitude);
            mapController.setCenter(inputPoint);
        }
    }

    private void showAddMarkerPopup(View view) {
        // inflate the layout of the popup window
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.add_marker_popup, null);

        // create the popup window
        int width = LinearLayout.LayoutParams.WRAP_CONTENT;
        int height = LinearLayout.LayoutParams.WRAP_CONTENT;
        final PopupWindow popupWindow = new PopupWindow(popupView, width, height, true);

        // show the popup window
        popupWindow.showAtLocation(view, Gravity.TOP, 0, 200);

        Button closeButton = popupView.findViewById(R.id.closeButton);
        closeButton.setOnClickListener(view1 -> popupWindow.dismiss());

        EditText severityInput = popupView.findViewById(R.id.severityInput);
        EditText latitudeInput = popupView.findViewById(R.id.latitudeInput);
        EditText longitudeInput = popupView.findViewById(R.id.longitudeInput);
        EditText commentInput = popupView.findViewById(R.id.commentInput);

        Button confirmButton = popupView.findViewById(R.id.confirmButton);
        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String severityStr = severityInput.getText().toString();
                String latitudeStr = latitudeInput.getText().toString();
                String longitudeStr = longitudeInput.getText().toString();

                if (severityStr.equals("")) {
                    Toast.makeText(view.getContext(),
                            "Please enter a value for severity.",
                            Toast.LENGTH_LONG).show();
                    return;
                }
                if (latitudeStr.equals("")) {
                    Toast.makeText(view.getContext(),
                            "Please enter a value for latitude.",
                            Toast.LENGTH_LONG).show();
                    return;
                }
                if (longitudeStr.equals("")) {
                    Toast.makeText(view.getContext(),
                            "Please enter a value for longitude.",
                            Toast.LENGTH_LONG).show();
                    return;
                }

                Double severity = Double.parseDouble(severityStr);
                Double latitude = Double.parseDouble(latitudeStr);
                Double longitude = Double.parseDouble(longitudeStr);

                String comment = commentInput.getText().toString();

                DamageMarker damageMarker = new DamageMarker(severity, latitude, longitude, comment);
                Map<String, DamageMarker> newMarker = new HashMap<>();
                newMarker.put("", damageMarker);

                firebaseAddMarkerAdapter.addDamageMarker(damageMarker, view);
                drawMarkers(newMarker);
                popupWindow.dismiss();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        map.onResume(); //needed for compass, my location overlays, v6.0.0 and up
    }

    @Override
    public void onPause() {
        super.onPause();
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().save(this, prefs);
        map.onPause();  //needed for compass, my location overlays, v6.0.0 and up
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        ArrayList<String> permissionsToRequest = new ArrayList<>();
        for (int i = 0; i < grantResults.length; i++) {
            permissionsToRequest.add(permissions[i]);
        }
        if (permissionsToRequest.size() > 0) {
            ActivityCompat.requestPermissions(
                    this,
                    permissionsToRequest.toArray(new String[0]),
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

    private void requestPermissionsIfNecessary(String[] permissions) {
        ArrayList<String> permissionsToRequest = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                // Permission is not granted
                permissionsToRequest.add(permission);
            }
        }
        if (permissionsToRequest.size() > 0) {
            ActivityCompat.requestPermissions(
                    this,
                    permissionsToRequest.toArray(new String[0]),
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }
}
