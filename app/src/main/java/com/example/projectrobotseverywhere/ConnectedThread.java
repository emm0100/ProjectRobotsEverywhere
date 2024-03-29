package com.example.projectrobotseverywhere;

import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ConnectedThread extends Thread {
    private final BluetoothSocket mmSocket;
    private final InputStream mmInStream;
    private final OutputStream mmOutStream;

    public Handler bluetoothHandler;

    private String lastMessage = "";

    private final static int MESSAGE_READ = 2; // used in bluetooth handler to identify message update

    public Context context;

    public ConnectedThread(BluetoothSocket socket, Handler bluetoothHandler, Context context) {
        this.context = context;
        this.bluetoothHandler = bluetoothHandler;
        mmSocket = socket;
        InputStream tmpIn = null;
        OutputStream tmpOut = null;

        // Get the input and output streams, using temp objects because
        // member streams are final
        try {
            tmpIn = socket.getInputStream();
            tmpOut = socket.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }

        mmInStream = tmpIn;
        mmOutStream = tmpOut;
    }

    public void run() {
        byte[] buffer = new byte[1024];  // buffer store for the stream
        int bytes = 0; // bytes returned from read()
        // Keep listening to the InputStream until an exception occurs
        while (true) {
            try {
                    /*
                    Read from the InputStream from Arduino until termination character is reached.
                    Then send the whole String message to GUI Handler.
                     */
                buffer[bytes] = (byte) mmInStream.read();
                String readMessage;
                if (buffer[bytes] == '\n'){ // termination character = \n

                    readMessage = new String(buffer,0, bytes);
                    Log.e("Arduino Message", readMessage);

                    // If the message is a copy of the previous one, ignore it
                    if (readMessage.equals(lastMessage)) {
                        continue;
                    }

                    lastMessage = readMessage;
                    bluetoothHandler.obtainMessage(MESSAGE_READ, readMessage).sendToTarget();

                    // for testing
                    Toast.makeText(context,
                            readMessage,
                            Toast.LENGTH_LONG).show();

                    bytes = 0;
                } else {
                    bytes++;
                }
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
        }
    }

    /* Call this from the main activity to send data to the remote device */
    public void write(String input) {
        byte[] bytes = input.getBytes(); //converts entered String into bytes
        try {
            mmOutStream.write(bytes);
        } catch (IOException e) {
            Log.e("Send Error","Unable to send message",e);
        }
    }

    /* Call this from the main activity to shutdown the connection */
    public void cancel() {
        try {
            mmSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
