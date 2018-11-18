package com.stuffaboutcode.bluematrix;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.*;

import java.util.UUID;


public class Matrix extends AppCompatActivity {


    /**
     * Name of the connected device
     */
    private String mConnectedDeviceName = null;

    /**
     * String buffer for outgoing messages
     */
    private StringBuffer mOutStringBuffer;

    /**
     * Local Bluetooth adapter
     */
    private BluetoothAdapter mBluetoothAdapter = null;

    /**
     * Member object for the chat services
     */
    private BluetoothChatService mChatService = null;

    String address = null;
    String deviceName = null;
    StringBuffer dataBuffer = new StringBuffer();

    // static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    static final UUID myUUID = UUID.fromString("5c464d54-bb29-4f1e-bcf8-caa0860fb48e");


    TextView statusView;
    TextView debugText;
    DynamicMatrix matrix;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_matrix);

        statusView = (TextView)findViewById(R.id.status);

        debugText = (TextView)findViewById(R.id.debugText);

        matrix = findViewById(R.id.matrix);

        Intent newint = getIntent();

        deviceName = newint.getStringExtra(Connect.EXTRA_NAME);
        address = newint.getStringExtra(Connect.EXTRA_ADDRESS);

        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "Bluetooth is not available", Toast.LENGTH_LONG).show();
            this.finish();
        }

        // Initialize the BluetoothChatService to perform bluetooth connections
        mChatService = new BluetoothChatService(this, mHandler);

        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");

        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        mChatService.connect(device, true);

        // Once connected setup the listener
        matrix.setOnUseListener(new DynamicMatrix.DynamicMatrixListener() {
            @Override
            public void onPress(DynamicMatrix.MatrixCell cell, int pointerId, float x, float y) {
                sendMessage("1" + "," + cell.getCol() + "," + cell.getRow() + "," + String.valueOf(pointerId) + "," + x + "," + y + "\n");
            }

            @Override
            public void onMove(DynamicMatrix.MatrixCell cell, int pointerId, float x, float y) {

            }

            @Override
            public void onRelease(DynamicMatrix.MatrixCell cell, int pointerId, float x, float y) {

            }
        });
    }

    public void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
            Toast.makeText(this, "cant send message - not connected", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            mChatService.write(send);

            // Reset out string buffer to zero and clear the edit text field
            mOutStringBuffer.setLength(0);
        }
    }

    private void disconnect() {
        if (mChatService != null) {
            mChatService.stop();
        };
        finish();
    }

    private void parseData(String data) {
        //statusView.setText(data);

        // add the message to the buffer
        dataBuffer.append(data);

        // debug - log data and buffer
        //Log.d("data", data);
        //Log.d("databuffer", dataBuffer.toString());

        // find any complete messages
        String[] messages = dataBuffer.toString().split("\\n");
        int noOfMessages = messages.length;
        // does the last message end in a \n, if not its incomplete and should be ignored
        if (!dataBuffer.toString().endsWith("\n")) {
            noOfMessages = noOfMessages - 1;
        }

        // debug - write data to screen
        debugText.setText(Integer.toString(noOfMessages) + ":" + dataBuffer.toString());

        // clean the data buffer of any processed messages
        if (dataBuffer.lastIndexOf("\n") > -1)
            dataBuffer.delete(0, dataBuffer.lastIndexOf("\n") + 1);

        // process messages
        for (int messageNo = 0; messageNo < noOfMessages; messageNo++) {
            processMessage(messages[messageNo]);
        }

    }

    private void processMessage(String message) {
        // sendMessage("processing " + message + "\n");
        String parameters[] = message.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
        boolean invalidMessage = false;

        // Check the message
        if (parameters.length > 0) {
            // check length
            if (parameters.length == 6) {

                // set matrix
                if (parameters[0].equals("1")) {
                    matrix.setSize(Integer.parseInt(parameters[1]), Integer.parseInt(parameters[2]));
                    if (!parameters[3].equals("")) {
                        try {
                            matrix.setColor(Color.parseColor(parameters[3]));
                        }
                        catch(IllegalArgumentException i){
                            invalidMessage = true;
                        }
                    }
                    if (!parameters[4].equals(""))
                        matrix.setBorder(parameters[4].equals("1") ? true : false);
                    if (!parameters[5].equals(""))
                        matrix.setVisible(parameters[5].equals("1") ? true : false);
                    matrix.update();

                // set cell
                } else if (parameters[0].equals("2")) {
                    DynamicMatrix.MatrixCell cell = matrix.getCell(
                            Integer.parseInt(parameters[1]), Integer.parseInt(parameters[2]));
                    if (!parameters[3].equals("")) {
                        try {
                            matrix.setColor(Color.parseColor(parameters[3]));
                        }
                        catch(IllegalArgumentException i){
                            invalidMessage = true;
                        }
                    }
                    cell.setColor(Color.parseColor(parameters[3]));
                    if (!parameters[4].equals(""))
                        cell.setBorder(parameters[4].equals("1") ? true : false);
                    if (!parameters[5].equals(""))
                        cell.setVisible(parameters[5].equals("1") ? true : false);
                    matrix.update();

                // op not recognised
                } else {
                    invalidMessage = true;
                }
            } else {
                invalidMessage = true;
            }
        } else {
            invalidMessage = true;
        }

        if (invalidMessage) {
            statusView.setText("Error - Invalid message received");
        }
    }

    @Override
    public void onBackPressed() {
        disconnect();
    }

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothChatService.STATE_CONNECTED:
                            statusView.setText("Connected to " + deviceName);
                            matrix.setVisibility(View.VISIBLE);
                            break;
                        case BluetoothChatService.STATE_CONNECTING:
                            statusView.setText("Connecting to " + deviceName);
                            matrix.setVisibility(View.INVISIBLE);
                            break;
                        case BluetoothChatService.STATE_LISTEN:
                        case BluetoothChatService.STATE_NONE:
                            statusView.setText("Not connected");
                            disconnect();
                            break;
                    }
                    break;
                case Constants.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    // message sent
                    //mConversationArrayAdapter.add("Me:  " + writeMessage);
                    break;
                case Constants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readData = new String(readBuf, 0, msg.arg1);
                    // message received
                    parseData(readData);
                    //mConversationArrayAdapter.add(mConnectedDeviceName + ":  " + readMessage);
                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    if (null != this) {
                        Toast.makeText(getApplicationContext(), "Connected to "
                                + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    }
                    break;
                case Constants.MESSAGE_TOAST:
                    if (null != this) {
                        Toast.makeText(getApplicationContext(), msg.getData().getString(Constants.TOAST),
                                Toast.LENGTH_SHORT).show();
                    }
                    break;
            }

        }
    };

}


/* potential messages the matrix might receive

messages:
- matrix
    - cols
    - rows
- cell [array]
    - col
    - row
    - colour
    - border
    - visible

- all - reconfiguration of the matrix and updated values for all the cells


 */