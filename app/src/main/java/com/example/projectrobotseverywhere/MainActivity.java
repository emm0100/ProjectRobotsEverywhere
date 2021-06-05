package com.example.projectrobotseverywhere;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

import org.osmdroid.api.IMapController;
import org.osmdroid.bonuspack.location.GeocoderNominatim;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.infowindow.InfoWindow;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;


public class MainActivity extends AppCompatActivity implements FirebaseObserver, LifecycleObserver {

    private FirebaseAddMarkerAdapter firebaseAddMarkerAdapter;

    private final int REQUEST_PERMISSIONS_REQUEST_CODE = 1;

    // Default location set to TU/e
    private final double DEFAULT_LATITUDE = 51.4484098;
    private final double DEFAULT_LONGITUDE = 5.4907148;

    private MapView map = null;
    private EditText searchInput;
    private IMapController mapController;
    private Map<String, DamageMarker> damageMarkers;
    private final Drawable[] markerIcons = new Drawable[11];

    private CreateConnectThread createConnectThread;
    private Map<String, String> pairedDevicesMap;
    private static BluetoothAdapter bluetoothAdapter;
    private static Handler bluetoothHandler;

    private final static int CONNECTING_STATUS = 1; // used in bluetooth handler to identify message status
    private final static int MESSAGE_READ = 2; // used in bluetooth handler to identify message update

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);



        // init firebase adapter
        firebaseAddMarkerAdapter = new FirebaseAddMarkerAdapter();
        firebaseAddMarkerAdapter.attachObserver(this);

        // Allow actions upon backgrounding and foregrounding the app
        getLifecycle().addObserver(this);

        // init map of all DamageMarkers in the database
        damageMarkers = new HashMap<>();

        // osmdroid stuff
        Context context = getApplicationContext();
        Configuration.getInstance()
                .load(context, PreferenceManager.getDefaultSharedPreferences(context));

        //inflate and create the map
        setContentView(R.layout.activity_main);

        // Request permissions for GPS and storage use
        requestPermissions(new String[] {
                // if you need to show the current location, uncomment the line below
                Manifest.permission.ACCESS_FINE_LOCATION,
                // WRITE_EXTERNAL_STORAGE is required in order to show the map
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN
                //Manifest.permission.ACCESS_BACKGROUND_LOCATION
        });

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

        // Add marker button onclick
        ImageButton addMarkerButton = findViewById(R.id.addMarkerButton);
        addMarkerButton.setOnClickListener(this::showAddMarkerPopup);

        // Configure map
        initializeMap();

        // retrieve markers from database
        initializeMarkerIcons();
        getMarkers();

        startBluetoothHandling();
    }

    /**
     * Should resume bluetooth message handling
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    protected void onStart() {
        super.onStart();
        if (createConnectThread != null) {
            createConnectThread.start();
        } else {
            startBluetoothHandling();
        }
    }

    /**
     * Should cancel bluetooth message handling
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    protected void onStop() {
        super.onStop();
        if (createConnectThread != null) {
            createConnectThread.cancel();
        }
    }

    /**
     * Initialize bluetoothAdapter,
     * turn on bluetooth if off
     * get bonded devices
     */
    private void startBluetoothHandling() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled()) {
            requestTurnOnBluetooth();
        }
        getBluetoothBondedDevices();
        initializeBluetoothHandler();
        setCreateConnectThreadHC05();
    }

    private void setCreateConnectThreadHC05() {
        if (pairedDevicesMap != null) {
            if (pairedDevicesMap.containsKey("HC-05")) {
                createConnectThread = new CreateConnectThread(
                        bluetoothAdapter,
                        pairedDevicesMap.get("HC-05"),
                        bluetoothHandler,
                        this.getApplicationContext());
                createConnectThread.start();
            } else {
                Toast.makeText(this, "Please connect to HC-05 bluetooth module", Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(this, "Please connect to HC-05 bluetooth module", Toast.LENGTH_LONG).show();
        }
    }

    private void initializeBluetoothHandler() {
        bluetoothHandler = new Handler(Looper.getMainLooper()) {
            //private String lastMessage = "";

            @Override
            public void handleMessage(Message message){
                if (message.what == MESSAGE_READ){
                        String arduinoMsg = message.obj.toString(); // Read message from Arduino
                        arduinoMsg = arduinoMsg.toLowerCase();

                        /*if (arduinoMsg.equals(lastMessage)) {
                            return;
                        } else {
                            lastMessage = arduinoMsg;
                        }*/

                        String[] values = arduinoMsg.split(":");

                        // message is in the wrong format, break
                        if (values.length != 3) {
                            return;
                        }

                        // Extract values
                        String latitude = values[0];
                        String longitude = values[1];
                        String severity = values[2];

                        if (severity.contains("\n")) {
                            severity = severity.replace("\n", "");
                        }

                        // TODO: remove this, just for testing
                        Toast.makeText(getApplicationContext(),
                                "Message: " + arduinoMsg,
                                Toast.LENGTH_LONG)
                                .show();

                        double latitudeDouble;
                        double longitudeDouble;
                        double severityDouble;

                        try {
                            latitudeDouble = Double.parseDouble(latitude);
                            longitudeDouble = Double.parseDouble(longitude);
                            severityDouble = Double.parseDouble(severity);
                        }
                        // One of the values is not a double, break
                        catch (NumberFormatException e) {
                            e.printStackTrace();
                            return;
                        }

                        // Add marker to database
                        DamageMarker dmgMarker = new DamageMarker(
                                latitudeDouble,
                                longitudeDouble,
                                severityDouble,
                                "");
                        addMarkerToFirebase(dmgMarker);
                        }
                }
        };
    }

    private void addMarkerToFirebase(DamageMarker damageMarker) {
        firebaseAddMarkerAdapter.addDamageMarker(damageMarker, this.findViewById(android.R.id.content).getRootView());
    }

    /**
     * Get Bluetooth connected devices
     */
    private void getBluetoothBondedDevices() {
        pairedDevicesMap = new HashMap<>();
        if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

            if (pairedDevices.size() > 0) {

                // There are paired devices. Get the name and address of each paired device.
                for (BluetoothDevice device : pairedDevices) {
                    String deviceName = device.getName();
                    String deviceHardwareAddress = device.getAddress(); // MAC address
                    pairedDevicesMap.put(deviceName, deviceHardwareAddress);
                }
            }
        }
    }

    // Request to turn on bluetooth if currently off
    private void requestTurnOnBluetooth() {
        Intent turnOnBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(turnOnBluetooth, 0);
    }

    /**
     * Handles setting all values for the MapView as required.
     */
    private void initializeMap() {
        map = findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.setTilesScaledToDpi(true); // makes font larger i.e. more readable

        mapController = map.getController();
        mapController.setZoom(15.0);
        GeoPoint startPoint = new GeoPoint(DEFAULT_LATITUDE, DEFAULT_LONGITUDE);
        mapController.setCenter(startPoint);
    }

    private void initializeMarkerIcons() {
        markerIcons[0] = this.getDrawable(R.drawable.damage_0);
        markerIcons[1] = this.getDrawable(R.drawable.damage_1);
        markerIcons[2] = this.getDrawable(R.drawable.damage_2);
        markerIcons[3] = this.getDrawable(R.drawable.damage_3);
        markerIcons[4] = this.getDrawable(R.drawable.damage_4);
        markerIcons[5] = this.getDrawable(R.drawable.damage_5);
        markerIcons[6] = this.getDrawable(R.drawable.damage_6);
        markerIcons[7] = this.getDrawable(R.drawable.damage_7);
        markerIcons[8] = this.getDrawable(R.drawable.damage_8);
        markerIcons[9] = this.getDrawable(R.drawable.damage_9);
        markerIcons[10] = this.getDrawable(R.drawable.damage_10);
    }

    /**
     * Retrieve the list of all markers in the database from the FirebaseAdapter.
     */
    private void getMarkers() {
        damageMarkers.clear();
        damageMarkers = firebaseAddMarkerAdapter.getMarkers();
        updateMarkers();
    }

    /**
     * Draws all markers in the database on the map.
     * Clears all markers currently on the map.
     * Marker colour depends on severity.
     */
    private void updateMarkers() {
        map.getOverlays().clear();
        for (Map.Entry<String, DamageMarker> entry: damageMarkers.entrySet()) {
            DamageMarker damageMarker = entry.getValue();
            String markerID = entry.getKey();

            Double severity = damageMarker.getSeverity();
            Drawable markerIcon = markerIcons[(int)Math.round(severity)];
            GeoPoint location = new GeoPoint(damageMarker.getLatitude(), damageMarker.getLongitude());
            addMarkerToMapView(markerIcon, location, damageMarker, markerID);
            map.invalidate();
        }
    }

    /**
     * Draws a marker on the map.
     *
     * @param markerIcon The Drawable that should be used for this marker.
     * @param location The geolocation of the marker, needed for positioning on the map.
     */
    private void addMarkerToMapView(Drawable markerIcon, GeoPoint location, DamageMarker damageMarker, String markerID) {
        Marker marker = new Marker(map);
        marker.setIcon(markerIcon);
        marker.setPosition(location);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);

        InfoWindow infoWindow = new DamageMarkerInfoWindow(map, firebaseAddMarkerAdapter);
        marker.setInfoWindow(infoWindow);
        marker.setInfoWindowAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_TOP);
        marker.setId(markerID);
        marker.setOnMarkerClickListener(new Marker.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker, MapView mapView) {
                marker.showInfoWindow();
                return true;
            }
        });
        map.getOverlays().add(marker);
    }

    /**
     * Handles searching for a specific location.
     * Uses GeocoderNominatim to convert address or postal code input to a geolocation.
     * Focuses the map on the found coordinates.
     */
    private void searchInputConfirmed() {
        String inputLocation = searchInput.getText().toString();

        if (!inputLocation.equals("")) {
            Callable<GeoPoint> task = () -> {
                List<Address> addressList;

                GeocoderNominatim geocoder = new GeocoderNominatim("ProjectRobotsEverywhere");
                try {
                    addressList = geocoder.getFromLocationName(inputLocation, 1);
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
                Address address = addressList.get(0);
                double inputLatitude = address.getLatitude();
                double inputLongitude = address.getLongitude();
                GeoPoint inputPoint = new GeoPoint(inputLatitude, inputLongitude);
                return inputPoint;
            };
            ExecutorService executor = Executors.newFixedThreadPool(1);
            Future<GeoPoint> future = executor.submit(task);

            // wait for data fetch to complete
            while (!future.isDone()) {
                try {
                    TimeUnit.MILLISECONDS.sleep(100);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            // move map to searched location
            if (future.isDone()) {
                try {
                    GeoPoint inputPoint = future.get();
                    mapController.setCenter(inputPoint);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Handles creating and showing the popup window used to add new markers to the database.
     *
     * @param view reference to current interface component
     */
    private void showAddMarkerPopup(View view) {
        // inflate the layout of the popup window
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.add_marker_popup,
                                          (ViewGroup) findViewById(android.R.id.content), false);

        // create the popup window
        int width = LinearLayout.LayoutParams.WRAP_CONTENT;
        int height = LinearLayout.LayoutParams.WRAP_CONTENT;
        final PopupWindow popupWindow = new PopupWindow(popupView, width, height, true);

        // show the popup window
        popupWindow.showAtLocation(view, Gravity.TOP, 0, 200); // TODO: offset shouldn't be hardcoded

        // [X]-button dismisses the window
        Button closeButton = popupView.findViewById(R.id.closeButton);
        closeButton.setOnClickListener(view1 -> popupWindow.dismiss());

        // Get all input elements
        EditText severityInput = popupView.findViewById(R.id.severityInput);
        EditText latitudeInput = popupView.findViewById(R.id.latitudeInput);
        EditText longitudeInput = popupView.findViewById(R.id.longitudeInput);
        EditText commentInput = popupView.findViewById(R.id.commentInput);

        // Handle confirming the addition
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

                // Create new DamageMarker and add to database
                DamageMarker damageMarker = new DamageMarker(severity, latitude, longitude, comment);
                firebaseAddMarkerAdapter.addDamageMarker(damageMarker, view);

                popupWindow.dismiss();
            }
        });
    }

    @Override
    public void firebaseUpdate(FirebaseAdapter adapter, Map<String, DamageMarker> damageMarkerMap) {
        if (!(adapter instanceof FirebaseAddMarkerAdapter)) { return; }

        damageMarkers = damageMarkerMap;

        // make sure the data was correctly retrieved
        if (damageMarkers != null) { updateMarkers(); }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
        map.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        map.onPause();
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

    /**
     * Request permission from the user to access GPS data and write to storage
     *
     * @param permissions Array of permissions to be requested
     */
    private void requestPermissions(String[] permissions) {
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
