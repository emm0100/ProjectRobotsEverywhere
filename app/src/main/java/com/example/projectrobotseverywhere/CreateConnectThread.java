package com.example.projectrobotseverywhere;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;

import static android.content.ContentValues.TAG;

public class CreateConnectThread extends Thread {

    //private String deviceName = null;
    //private String deviceAddress;

    // https://medium.com/swlh/create-custom-android-app-to-control-arduino-board-using-bluetooth-ff878e998aa8

    public Handler bluetoothHandler;
    public static BluetoothSocket mmSocket;
    public static ConnectedThread connectedThread;

    private BluetoothAdapter bluetoothAdapter;
    public Context context;


    private final static int CONNECTING_STATUS = 1; // used in bluetooth handler to identify message status


    public CreateConnectThread(BluetoothAdapter bluetoothAdapter, String address, Handler bluetoothHandler, Context context) {
        if (!bluetoothAdapter.checkBluetoothAddress(address)) { return; }

        this.bluetoothAdapter = bluetoothAdapter;
        this.bluetoothHandler = bluetoothHandler;
        this.context = context;

        // Get device by its address
        BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(address);
        BluetoothSocket tmpSocket = null;
        UUID uuid = bluetoothDevice.getUuids()[0].getUuid();

        try {
                /*
                Get a BluetoothSocket to connect with the given BluetoothDevice.
                Due to Android device varieties,the method below may not work fo different devices.
                You should try using other methods i.e. :
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
                 */
            tmpSocket = bluetoothDevice.createInsecureRfcommSocketToServiceRecord(uuid);

        } catch (IOException e) {
            Log.e(TAG, "Socket's create() method failed", e);
        }
        mmSocket = tmpSocket;
    }

    public void run() {
        // Cancel discovery because it otherwise slows down the connection.
        bluetoothAdapter.cancelDiscovery();

        try {
            // Connect to the remote device through the socket. This call blocks
            // until it succeeds or throws an exception.
            mmSocket.connect();
            Log.e("Status", "Device connected");
            bluetoothHandler.obtainMessage(CONNECTING_STATUS, 1, -1).sendToTarget();
        } catch (IOException connectException) {
            // Unable to connect; close the socket and return.
            try {
                mmSocket.close();
                Log.e("Status", "Cannot connect to device");
                bluetoothHandler.obtainMessage(CONNECTING_STATUS, -1, -1).sendToTarget();
            } catch (IOException closeException) {
                Log.e(TAG, "Could not close the client socket", closeException);
            }
            return;
        }

        // The connection attempt succeeded. Perform work associated with
        // the connection in a separate thread.
        connectedThread = new ConnectedThread(mmSocket, bluetoothHandler, context);
        connectedThread.run();
    }

    // Closes the client socket and causes the thread to finish.
    public void cancel() {
        try {
            mmSocket.close();
        } catch (IOException e) {
            Log.e(TAG, "Could not close the client socket", e);
        }
    }
}

