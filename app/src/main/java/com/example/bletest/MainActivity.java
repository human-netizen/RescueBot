package com.example.bletest;

import static java.lang.Math.abs;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import de.hdodenhof.circleimageview.CircleImageView;
import android.os.Bundle;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {

    BluetoothAdapter bluetoothAdapter;
    BluetoothDevice bluetoothDevice;
    BluetoothSocket bluetoothSocket;
    OutputStream outputStream;
    // Write a message to the database
    FirebaseDatabase database;
    DatabaseReference myRef;
    TextView textView;



    private String macAddress = "";
    UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    Button send , forward , backward , left , right ;

    SeekBar xseek , yseek , zseek;
    EditText cmd ;

    TextView xval , yval , zval , textViewx , textViewy , textViewAngel
            ;
    CircleImageView imageView;
    float xDown = 0 , yDown = 0;
    float initialX , initialY;
    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        database = FirebaseDatabase.getInstance();
        myRef = database.getReference("move");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        send = findViewById(R.id.button);
        cmd = findViewById(R.id.cmd);
//        //forward = findViewById(R.id.forward);
//        //backward = findViewById(R.id.backward);
//        //left = findViewById(R.id.left);
        //right = findViewById(R.id.right);
        //xseek = findViewById(R.id.xseek);
        yseek = findViewById(R.id.yseek);
        zseek = findViewById(R.id.zseek);
        //xval = findViewById(R.id.xval);
        yval = findViewById(R.id.yval);
        zval = findViewById(R.id.zval);
        imageView = findViewById(R.id.imageView2);
        textViewx = findViewById(R.id.textViewx);
        textViewy = findViewById(R.id.textViewy);
        textViewAngel = findViewById(R.id.textViewAngel);
        textView = findViewById(R.id.textView);


        System.out.println(xDown);
        System.out.println(yDown);
        imageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch(event.getActionMasked()){
                    case MotionEvent.ACTION_DOWN:
                        initialX = imageView.getX();
                        initialY = imageView.getY();

                        xDown = event.getX();
                        yDown = event.getY();
                        break;
                    case MotionEvent.ACTION_UP:
                        imageView.setX(initialX);
                        imageView.setY(initialY);
                        sendCommand("l0");
                        sendCommand("r0");
                        break;
                    case MotionEvent.ACTION_MOVE:
                        float movedX , movedY;
                        movedX = event.getX();
                        movedY = event.getY();

                        float distancX = movedX - xDown;
                        float distancY = movedY - yDown;
                        float newX = imageView.getX() + distancX;
                        float newY = imageView.getY() + distancY;
                        imageView.setX(newX);
                        imageView.setY(newY);
                        textViewx.setText("x: " + newX);
                        textViewy.setText("y: " + newY);

                        float deltaX = newX - initialX;
                        float deltaY = initialY - newY;

                        // Calculate angle in radians
                        double angleRadians = Math.atan2(deltaY, deltaX);

                        // Convert to degrees
                        double angleDegrees = Math.toDegrees(angleRadians);

                            // Normalize the angle to be between 0 and 360
                        if (angleDegrees < 0) {
                            angleDegrees += 360;
                        }
                        String str = new DecimalFormat("#.0#").format(angleDegrees);
                        textViewAngel.setText("angle: " + str);
                        String[] motorValues = calculateMotorValuesAsString((float) angleDegrees);
                        String motorString1 = "l" + motorValues[0];
                        if(motorValues[0].charAt(0) == '-'){
                            motorString1 = "a" + motorValues[0].substring(1);
                        }
                        String motorString2 = "r" + motorValues[1];
                        if(motorValues[1].charAt(0) == '-'){
                            motorString2 = "b" + motorValues[1].substring(1);
                        }
                        //int motor1 = Integer.parseInt(motorValues[0]);
                        //int motor2 = Integer.parseInt(motorValues[1]);
                        textViewx.setText("Motor 1: " + motorValues[0]);
                        textViewy.setText("Motor 2: " + motorValues[1]);
                        //sendCommands(motorValues);
                        sendCommand(motorString1);
                        sendCommand(motorString2);
                        break;
                }
                return true;
            }
        });

        if (ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_DENIED) {
                Toast.makeText(this, "Permission granted", Toast.LENGTH_LONG).show();
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.BLUETOOTH_CONNECT}, 2);
        }

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null) {
            System.out.println("Bluetooth not supported");
            Toast.makeText(this, "Not supported", Toast.LENGTH_LONG).show();
            return;
        } else {
            System.out.println("Bluetooth supported");
            Toast.makeText(this, "Supported", Toast.LENGTH_LONG).show();
            if(!bluetoothAdapter.isEnabled()) {
                Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableIntent, 0);
            }
            else{
                System.out.println("Bluetooth is enabled");
                Toast.makeText(this, "Bluetooth is enabled", Toast.LENGTH_LONG).show();
            }
        }
        
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

        String pairedDeviceNumber = String.valueOf(pairedDevices.size());
        Toast.makeText(this, pairedDeviceNumber, Toast.LENGTH_LONG).show();
        for (BluetoothDevice device : pairedDevices) {


            System.out.println("Name->" + device.getName() + "    " + "MAC->" + device.getAddress());
            if (device.getName().equals("espAND")) {
                macAddress = device.getAddress();
                System.out.println("MAC - > " + macAddress);
                TextView textView = findViewById(R.id.dName);
                textView.setText(macAddress + "  " + device.getName() + " Paired" );
                Toast.makeText(this, "Connected", Toast.LENGTH_SHORT).show();
                break;
            }
        }
        Toast.makeText(this, "hehe", Toast.LENGTH_SHORT).show();
        //Toast.makeText(this, pairedDevices.size(), Toast.LENGTH_SHORT).show();




        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothDevice = bluetoothAdapter.getRemoteDevice(macAddress);
        Toast.makeText(this, "ekhane elo", Toast.LENGTH_LONG).show();

        // Read from the database
        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                String value = dataSnapshot.getValue(String.class);
                //Log.d(TAG, "Value is: " + value);
                Toast.makeText(MainActivity.this, value, Toast.LENGTH_SHORT).show();
                String motorString1;// = "l" + motorValues[0];
                String motorString2;// = "r" + motorValues[1];
                if(Objects.equals(value, "f")){
                    System.out.println("hi");
                    //motorString1 = "l255";
                    //motorString2 = "r255";
                    sendCommand("ff");
                    textView.setText("forward");
                }
                else if(Objects.equals(value, "s")){
                    //motorString1 = "l0";
                    //motorString2 = "r0";
                    sendCommand("ss");
                    textView.setText("Stop");
                }
                else if(Objects.equals(value, "b")){
                    //motorString1 = "a255";
                    //motorString2 = "b255";
                    sendCommand("bb");
                    textView.setText("back");
                }
                else if(Objects.equals(value, "l")){
                    //motorString1 = "l0";
                    //motorString2 = "r255";
                    sendCommand("rl");
                    textView.setText("left");
                }
                else if(Objects.equals(value, "r")){
                    motorString1 = "l255";
                    motorString2 = "r0";
                    sendCommand("rr");
                    textView.setText("right");
                }

                //int motor1 = Integer.parseInt(motorValues[0]);
                //int motor2 = Integer.parseInt(motorValues[1]);
                //textViewx.setText("Motor 1: " + motorValues[0]);
                //textViewy.setText("Motor 2: " + motorValues[1]);
                //sendCommands(motorValues);
                //sendCommand(motorString1);
                //sendCommand(motorString2);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Failed to read value
                //Log.w(TAG, "Failed to read value.", error.toException());
            }
        });


        new Thread(new Runnable() {
            @Override
            public void run() {
                if (ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_DENIED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.BLUETOOTH_CONNECT}, 2);
                }

                try{
                    bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(uuid);
                    bluetoothAdapter.cancelDiscovery();
                    bluetoothSocket.connect();
                    outputStream = bluetoothSocket.getOutputStream();
                    Log.d("Bluetooth", "Connected") ;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //Toast.makeText(MainActivity.this, "Connected", Toast.LENGTH_LONG).show();
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();

        send.setOnClickListener(v -> {
            String command = cmd.getText().toString();
            sendCommand(command);
        });

//        forward.setOnClickListener(v -> {
//            sendCommand("f");
//        });
//
//        backward.setOnClickListener(v -> {
//            sendCommand("b");
//        });
//
//        left.setOnClickListener(v -> {
//            sendCommand("l");
//        });
//
//        right.setOnClickListener(v -> {
//            sendCommand("r");
//        });
//
//        forward.setOnLongClickListener(v -> {
//            sendCommand("ff");
//            return true;
//        });

//        forward.setOnTouchListener((v, event) -> {
//            if(event.getAction() == android.view.MotionEvent.ACTION_UP) {
//                sendCommand("ss");
//            }
//            return false;
//        });
//
//        backward.setOnLongClickListener(v -> {
//            sendCommand("bb");
//            return true;
//        });
//
//        backward.setOnTouchListener((v, event) -> {
//            if(event.getAction() == android.view.MotionEvent.ACTION_UP) {
//                sendCommand("ss");
//            }
//            return false;
//        });
//
//        left.setOnLongClickListener(v -> {
//            sendCommand("ll");
//            return true;
//        });
//
//        left.setOnTouchListener((v, event) -> {
//            if(event.getAction() == android.view.MotionEvent.ACTION_UP) {
//                sendCommand("ss");
//            }
//            return false;
//        });
//
//        right.setOnLongClickListener(v -> {
//            sendCommand("rr");
//            return true;
//        });
//
//        right.setOnTouchListener((v, event) -> {
//            if(event.getAction() == android.view.MotionEvent.ACTION_UP) {
//                sendCommand("ss");
//            }
//            return false;
//        });

        getAllValue();
    }
    private static final int MAX_VALUE = 255; // Assuming MAX_VALUE is defined

    private  String[] calculateMotorValuesAsString(float angle) {
        int motor1 = 0;
        int motor2 = 0;

        if (angle >= 0 && angle <= 90) {
            motor1 = MAX_VALUE;
            motor2 = (int) (MAX_VALUE * (angle / 90));
        } else if (angle >= 91 && angle <= 180) {
            motor2 = MAX_VALUE;
            motor1 = (int) (MAX_VALUE * (1 - (angle - 90) / 90));
        }
        else if (angle >= 181 && angle <= 270) {
            motor1 = -MAX_VALUE;
            motor2 = -(int) (MAX_VALUE * (angle - 180) / 90);
        } else if (angle >= 271 && angle <= 360) {
            motor2 = -MAX_VALUE;
            motor1 = -(int) (MAX_VALUE * (1 - (angle - 270) / 90));
        }

        // Convert integer values to string
        String motor1String = String.valueOf(motor1);
        String motor2String = String.valueOf(motor2);

        // Return the string array
        return new String[] {motor1String, motor2String};

    }


    private void sendCommand(String command) {
        if(outputStream == null) {
            Log.d("Bluetooth", "Output Stream is null") ;
            return;
        }
        try {
            command += "\n";
            outputStream.write(command.getBytes());
        } catch (Exception e) {
            e.printStackTrace();

        }
    }

    public void sendCommands(String[] commands) {
        if (outputStream == null) {
            Log.d("Bluetooth", "Output Stream is null");
            return;
        }

        try {
            for (String command : commands) {
                String fullCommand = command + "\n";  // Append newline to each command
                outputStream.write(fullCommand.getBytes());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void getAllValue(){
//        xseek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
//            @Override
//            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
////                sendCommand("x" + progress);
//                // map progress to 10 - 99
//
//                xval.setText("X : " + progress);
//            }
//
//            @Override
//            public void onStartTrackingTouch(SeekBar seekBar) {
////                sendCommand("x" + seekBar.getProgress());
//                xval.setText("X : " + seekBar.getProgress());
//            }
//
//            @Override
//            public void onStopTrackingTouch(SeekBar seekBar) {
//                // map progress from 0 - 100 to 10 - 99
//                int val = seekBar.getProgress();
//                val = (int) ((val * 0.8) + 10);
//                sendCommand("x" + val);
//                xval.setText("X : " + seekBar.getProgress());
//            }
//        });

        yseek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
//                sendCommand("y" + progress);
                yval.setText("Y : " + progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
//                sendCommand("y" + seekBar.getProgress());
                yval.setText("Y : " + seekBar.getProgress());
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int val = seekBar.getProgress();
                val = (int) ((val * 0.8) + 10);
                sendCommand("y" + val);
                yval.setText("Y : " + seekBar.getProgress());
            }
        });

        zseek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
//                sendCommand("z" + progress);
                zval.setText("Z : " + progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
//                sendCommand("z" + seekBar.getProgress());
                zval.setText("Z : " + seekBar.getProgress());
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int val = seekBar.getProgress();
                val = (int) ((val * 0.8) + 10);
                sendCommand("z" + val);
                zval.setText("Z : " + seekBar.getProgress());
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(bluetoothSocket == null) {
            return;
        }
        try {
            bluetoothSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}