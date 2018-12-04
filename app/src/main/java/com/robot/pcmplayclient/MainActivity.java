package com.robot.pcmplayclient;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.TextView;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "PcmPlayClient";
//    private int mSampleRate = 48000; //48HZ
//    private int mChannel = AudioFormat.CHANNEL_OUT_STEREO; //double channels
//    private int mEncoding = AudioFormat.ENCODING_PCM_16BIT; //16 bit
//    private int mBufferSize = 1024; // buffer size in bytes
    private static final int MUTILCAST_PORT = 9456;
    private static final String MUTILCAST_IP_ADDRESS = "239.10.10.10";

    private MulticastSocket mSocket;
    private InetAddress mAddress;
    private WifiManager.MulticastLock multicastLock;

    private File file;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                Log.d(TAG, "Send sample data");
                sendThread.start();
            }
        });

        TextView tv = findViewById(R.id.sample_text);
        requestExternalStoragePermission();
        file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "sample.pcm");
        if(file.exists()){
            tv.setText(file.getAbsolutePath());
        }else {
            tv.setText("No sample file found!");
        }
        requestMultcastPermission();

        new Thread(){
            @Override
            public void run() {
                try {
                    Log.d(TAG, "Create socket client");
                    mAddress = InetAddress.getByName(MUTILCAST_IP_ADDRESS);
                    mSocket = new MulticastSocket(MUTILCAST_PORT);
                    mSocket.setLoopbackMode(true);
                    mSocket.setTimeToLive(1);
                    mSocket.joinGroup(mAddress);
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
        }.start();
    }

    private void requestMultcastPermission(){
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        multicastLock = wifiManager.createMulticastLock("multicast");
        multicastLock.acquire();
    }

    Thread sendThread = new Thread(){
        @Override
        public void run() {
            try {
                byte[] data = new byte[1024*8];
                int size;
                DataInputStream inputStream = new DataInputStream(new FileInputStream(file));
                while (0 < (size = inputStream.read(data))){
                    DatagramPacket mPacket = new DatagramPacket(data, size, mAddress, MUTILCAST_PORT);
                    Log.d(TAG, "Send data here");
                    mSocket.send(mPacket);
                }
                Log.d(TAG, "Send data finished");
                multicastLock.release();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e){
                e.printStackTrace();
            }
        }
    };


    public void requestExternalStoragePermission(){
        try {
            int permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
            if (permission != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 200);
            }
        }catch (Exception e){
            e.printStackTrace();
        }

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

}
