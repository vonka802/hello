package com.example.blutoothcompanion;

import androidx.appcompat.app.AppCompatActivity;

import static android.content.ContentValues.TAG;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private String deviceAddress = null;
    public static Handler handler;
    public static BluetoothSocket mmSocket;
    public static CreateConnectThread createConnectThread;
    public static ConnectedThread connectedThread;
    private final static int CONNECTION_STATUS = 1;
    private final static int MESSAGE_READ = 2;

    @Override
    public void onBackPressed()  {
       //  super.onBackPressed();
        Toast.makeText(MainActivity.this,"There is no back action",Toast.LENGTH_LONG).show();
        return;
    }
   
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Instantiate UI
        final TextView bluetoothStatus = findViewById(R.id.textBluetoothStatus);
        final TextView ledStatus = findViewById(R.id.textledstatus);
        final Button buttonDisConnect = findViewById(R.id.buttonDisconnect);
        final Button buttonConnect = findViewById(R.id.buttonConnect);
        final Button LedOn = findViewById(R.id.buttonOn);
        final Button LedOff = findViewById(R.id.buttonOff);
        final Switch switchBulb1 =(Switch) findViewById(R.id.switchBulb1);
        final Switch switchBulb2 =(Switch) findViewById(R.id.switchBulb2);
        final Switch switchSocket =(Switch) findViewById(R.id.switchSocket);




        buttonConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // This is the code to move to another screen
                    Intent intent = new Intent(MainActivity.this, SelectDeviceActivity.class);
                    startActivity(intent);
            }
        });

        // Get Device Address from SelectDeviceActivity.java to create connection
        deviceAddress = getIntent().getStringExtra("deviceAddress");
        if (deviceAddress != null){
            bluetoothStatus.setText("Connecting...");
            /*
            This is the most important piece of code.
            When "deviceAddress" is found, the code will call the create connection thread
            to create bluetooth connection to the selected device using the device Address
             */
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            createConnectThread = new CreateConnectThread(bluetoothAdapter,deviceAddress);
            createConnectThread.start();
        }

        // Code for the disconnect button
        buttonDisConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createConnectThread.cancel();
                bluetoothStatus.setText("Bluetooth is Disconnected");
            }
        });

        //Code for the button on

        LedOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            String androidCmd = "A";
            connectedThread.write(androidCmd);
            LedOn.setTextColor(Color.GREEN);
            LedOff.setTextColor(Color.BLACK);
            }
        });
        //Code for the Switch light

        switchBulb1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            if(switchBulb1.isChecked()) {
                String androidCmd = "B";
                switchBulb1.setTextColor(Color.GREEN);
                connectedThread.write(androidCmd);
            }
            else
            {
                String androidCmd = "b";
                switchBulb1.setTextColor(Color.RED);
                connectedThread.write(androidCmd);
            }
            }
        });

        switchBulb2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(switchBulb2.isChecked()) {
                    String androidCmd = "C";
                    switchBulb2.setTextColor(Color.GREEN);
                    connectedThread.write(androidCmd);
                }
                else
                {
                    String androidCmd = "c";
                    switchBulb2.setTextColor(Color.RED);
                    connectedThread.write(androidCmd);
                }
            }
        });

        switchSocket.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(switchSocket.isChecked()) {
                    String androidCmd = "D";
                    switchSocket.setTextColor(Color.GREEN);
                    connectedThread.write(androidCmd);
                }
                else
                {
                    String androidCmd = "d";
                    switchSocket.setTextColor(Color.RED);
                    connectedThread.write(androidCmd);
                }
            }
        });

        LedOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            String androidCmd ="a";
            connectedThread.write(androidCmd);
            LedOff.setTextColor(Color.RED);
            LedOn.setTextColor(Color.BLACK);
            }
        });



        handler = new Handler(Looper.getMainLooper()){
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what){
                    case CONNECTION_STATUS: switch (msg.arg1){
                        case 1: bluetoothStatus.setText("Blutooth Connected");
                        break;
                        case -1: bluetoothStatus.setText("Connection Failed");
                        break;
                    }
                    break;
                    //if the message contains data from arduino board
                    case MESSAGE_READ:
                        String statusText = msg.obj.toString().replace("/n","");
                        ledStatus.setText(statusText);
                    break;
                }
            }
        };
    }

    //Handler



    /* ============================ Thread to Create Connection ================================= */
    public static class CreateConnectThread extends Thread {

        public CreateConnectThread(BluetoothAdapter bluetoothAdapter, String address) {
            // Opening connection socket with the Arduino board
            BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(address);
            BluetoothSocket tmp = null;
            UUID uuid = bluetoothDevice.getUuids()[0].getUuid();
            try {
                // Get a BluetoothSocket to connect with the given BluetoothDevice.
                // MY_UUID is the app's UUID string, also used in the server code.
                tmp = bluetoothDevice.createInsecureRfcommSocketToServiceRecord(uuid);
            } catch (IOException e) {
                Log.e(TAG, "Socket's create() method failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it otherwise slows down the connection.
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            bluetoothAdapter.cancelDiscovery();
            try {
                // Connect to the Arduino board through the socket. This call blocks
                // until it succeeds or throws an exception.
                mmSocket.connect();
                handler.obtainMessage(CONNECTION_STATUS, 1, -1).sendToTarget();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and return.
                try {
                    mmSocket.close();
                    handler.obtainMessage(CONNECTION_STATUS, -1, -1).sendToTarget();
                } catch (IOException closeException) { }
                return;
            }

            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
            // Calling for the Thread for Data Exchange (see below)
            connectedThread = new ConnectedThread(mmSocket);
            connectedThread.run();
        }

        // Closes the client socket and causes the thread to finish.
        // Disconnect from Arduino board
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }
    /* =============================== Thread for Data Exchange ================================= */
    public static class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        // Getting Input and Output Stream when connected to Arduino Board
        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        // Send command to Arduino Board
        // This method must be called from Main Thread
        public void write(String input) {
            byte[] bytes = input.getBytes(); //converts entered String into bytes
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) { }
        }


        // Read message from Arduino device and send it to handler in the Main Thread
        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes = 0; // bytes returned from read()
            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    buffer[bytes] = (byte) mmInStream.read();
                    String arduinoMsg = null;

                    // Parsing the incoming data stream
                    if (buffer[bytes] == '\n'){
                        arduinoMsg = new String(buffer,0,bytes);
                        Log.e("Arduino Message",arduinoMsg);
                        handler.obtainMessage(MESSAGE_READ,arduinoMsg).sendToTarget();
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


    }
}

