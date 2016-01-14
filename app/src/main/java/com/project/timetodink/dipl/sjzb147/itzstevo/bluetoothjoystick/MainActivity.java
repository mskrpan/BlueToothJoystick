package com.project.timetodink.dipl.sjzb147.itzstevo.bluetoothjoystick;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends Activity implements SensorEventListener {

    ToggleButton lazyToggle;

    private float checkY = -30;
    private float checkZ = -30;

    private float deltaX = 0;
    private float deltaY = 0;
    private float deltaZ = 0;

    SensorManager sensorManager;
    Sensor gyro;

    TextView tvLabel, tvF, tvB, tvR, tvL, tvS, tvA, tvX, tvY, tvZ;

    BluetoothAdapter mBluetoothAdapter;
    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice;

    OutputStream mmOutputStream;
    InputStream mmInputStream;
    Thread workerThread;

    byte[] readBuffer;
    int readBufferPosition;
    volatile boolean stopWorker;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        lazyToggle = (ToggleButton) findViewById(R.id.tbLazy);
        lazyToggle.setChecked(true);


        Button connectButton = (Button) findViewById(R.id.bConnect);
        Button autoButton = (Button) findViewById(R.id.bAuto);
        Button disconnectButton = (Button) findViewById(R.id.bDisconnect);

        Button fButton = (Button) findViewById(R.id.bNap);
        Button bButton = (Button) findViewById(R.id.bNaz);
        Button rButton = (Button) findViewById(R.id.bOkretD);
        Button lButton = (Button) findViewById(R.id.bOkretL);
        Button sButton = (Button) findViewById(R.id.bStop);

        tvLabel = (TextView) findViewById(R.id.tvLabel);
        tvF = (TextView) findViewById(R.id.tvF);
        tvB = (TextView) findViewById(R.id.tvB);
        tvR = (TextView) findViewById(R.id.tvR);
        tvL = (TextView) findViewById(R.id.tvL);
        tvS = (TextView) findViewById(R.id.tvS);
        tvA = (TextView) findViewById(R.id.tvAuto);

        tvX = (TextView) findViewById(R.id.tvX);
        tvY = (TextView) findViewById(R.id.tvY);
        tvZ = (TextView) findViewById(R.id.tvZ);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION) != null) {

            Toast.makeText(this, "Ima gyro!", Toast.LENGTH_SHORT).show();

            gyro = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);

        } else {
            Toast.makeText(this, "Nema gyro!", Toast.LENGTH_SHORT).show();
        }


        try {

            // pronalazi i otvara BT connect
            connectButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    try {
                        findBT();
                        openBT();
                    } catch (IOException ex) {
                    }
                }
            });

            // šalje string za naprijed
            fButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    try {
                        sendDataF();
                    } catch (IOException ex) {
                    }
                }
            });

            // šalje string za nazad
            bButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    try {
                        sendDataB();
                    } catch (IOException ex) {
                    }
                }
            });

            // šalje string za okret u desno
            rButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    try {
                        sendDataR();
                    } catch (IOException ex) {
                    }
                }
            });

            // šalje string za okret u lijevo
            lButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    try {
                        sendDataL();
                    } catch (IOException ex) {
                    }
                }
            });

            // šalje string za stop
            sButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    try {
                        sendDataS();
                    } catch (IOException ex) {
                    }
                }
            });
            // šalje string za auto pilot
            autoButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    try {
                        sendDataAuto();
                    } catch (IOException ex) {
                    }
                }
            });

            // zatvara BT connection
            disconnectButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    try {
                        closeBT();
                    } catch (IOException ex) {
                    }
                }
            });

        } catch (NullPointerException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    //onResume() registerira accelerometer za osluškivanje
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, gyro, SensorManager.SENSOR_DELAY_NORMAL);
    }

    //onPause() odregistrira gyro za zaustavljanje osluškivanja
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    // pronalazi BT
    void findBT() {

        try {
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

            if (mBluetoothAdapter == null) {
                tvLabel.setText("No bluetooth adapter available");
            }

            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBluetooth = new Intent(
                        BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBluetooth, 0);
            }

            Set<BluetoothDevice> pairedDevices = mBluetoothAdapter
                    .getBondedDevices();
            if (pairedDevices.size() > 0) {
                for (BluetoothDevice device : pairedDevices) {

                    // provjera dal je bluetooth HC-06
                    if (device.getName().equals("HC-06")) {
                        mmDevice = device;
                        break;
                    }
                }
            }
            tvLabel.setText("Bluetooth Device Found");
        } catch (NullPointerException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // pokušava otvoriti konekciju s BT arduina
    void openBT() throws IOException {
        try {
            // Standardni SerialPortService ID
            UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
            mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
            mmSocket.connect();
            mmOutputStream = mmSocket.getOutputStream();
            mmInputStream = mmSocket.getInputStream();

            beginListenForData();

            tvLabel.setText("Bluetooth Opened");
        } catch (NullPointerException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // nakon otvaranja konekcije s arduinom pregledava/preslušava jel što poslano
    void beginListenForData() {
        try {
            final Handler handler = new Handler();

            // ASCII kod za newline charactere
            final byte delimiter = 10;

            stopWorker = false;
            readBufferPosition = 0;
            readBuffer = new byte[1024];

            workerThread = new Thread(new Runnable() {
                public void run() {
                    while (!Thread.currentThread().isInterrupted()
                            && !stopWorker) {

                        try {

                            int bytesAvailable = mmInputStream.available();
                            if (bytesAvailable > 0) {
                                byte[] packetBytes = new byte[bytesAvailable];
                                mmInputStream.read(packetBytes);
                                for (int i = 0; i < bytesAvailable; i++) {
                                    byte b = packetBytes[i];
                                    if (b == delimiter) {
                                        byte[] encodedBytes = new byte[readBufferPosition];
                                        System.arraycopy(readBuffer, 0,
                                                encodedBytes, 0,
                                                encodedBytes.length);
                                        final String data = new String(
                                                encodedBytes, "US-ASCII");
                                        readBufferPosition = 0;

                                        handler.post(new Runnable() {
                                            public void run() {
                                                tvLabel.setText(data);
                                            }
                                        });
                                    } else {
                                        readBuffer[readBufferPosition++] = b;
                                    }
                                }
                            }

                        } catch (IOException ex) {
                            stopWorker = true;
                        }

                    }
                }
            });

            workerThread.start();
        } catch (NullPointerException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    //string za naprijed
    void sendDataF() throws IOException {
        try {
            String msg = tvF.getText().toString();
            msg += "\n";

            mmOutputStream.write(msg.getBytes());

        } catch (NullPointerException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //string za nazad
    void sendDataB() throws IOException {
        try {
            String msg = tvB.getText().toString();
            msg += "\n";

            mmOutputStream.write(msg.getBytes());

        } catch (NullPointerException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //string za okret u desno
    void sendDataR() throws IOException {
        try {
            String msg = tvR.getText().toString();
            msg += "\n";

            mmOutputStream.write(msg.getBytes());

        } catch (NullPointerException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //string za okret u lijevo
    void sendDataL() throws IOException {
        try {
            String msg = tvL.getText().toString();
            msg += "\n";

            mmOutputStream.write(msg.getBytes());

        } catch (NullPointerException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //string za stop
    void sendDataS() throws IOException {
        try {
            String msg = tvS.getText().toString();
            msg += "\n";

            mmOutputStream.write(msg.getBytes());

        } catch (NullPointerException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //string za autopilot
    void sendDataAuto() throws IOException {
        try {
            String msg = tvA.getText().toString();
            msg += "\n";

            mmOutputStream.write(msg.getBytes());

        } catch (NullPointerException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // zatvara connection s BT
    void closeBT() throws IOException {
        try {
            stopWorker = true;
            mmOutputStream.close();
            mmInputStream.close();
            mmSocket.close();
            tvLabel.setText("Bluetooth Closed");
        } catch (NullPointerException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        deltaX = event.values[0];
        deltaY = event.values[1];
        deltaZ = event.values[2];


        tvX.setText("X: " + Float.toString(deltaX));
        tvY.setText("Y: " + Float.toString(deltaY));
        tvZ.setText("Z: " + Float.toString(deltaZ));

        if (deltaY < 0 && deltaY > -15 && deltaZ > 0 && deltaZ < 15)
            try {
                sendDataS();
            } catch (IOException e) {
                e.printStackTrace();
            }


        else if (deltaY < checkY) // && deltaZ > 2
            try {
                sendDataR();
            } catch (IOException e) {
                e.printStackTrace();
            }
        else if (deltaY > 30)
            try {
                sendDataL();
            } catch (IOException e) {
                e.printStackTrace();
            }
        else if (deltaZ < checkZ) //&& deltaY < -2
            try {
                sendDataF();
            } catch (IOException e) {
                e.printStackTrace();
            }
        else if (deltaZ > 30)
            try {
                sendDataB();
            } catch (IOException e) {
                e.printStackTrace();
            }
        else{

        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void onToggle(View view) {
        if (lazyToggle.isChecked()){
            sensorManager.registerListener(this, gyro, SensorManager.SENSOR_DELAY_NORMAL);
        }
        else{
            sensorManager.unregisterListener(this);
        }

    }
}