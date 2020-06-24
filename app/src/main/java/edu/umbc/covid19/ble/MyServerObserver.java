package edu.umbc.covid19.ble;

import android.bluetooth.BluetoothDevice;
import android.content.Context;

import androidx.annotation.NonNull;

import no.nordicsemi.android.ble.observer.ServerObserver;

public class MyServerObserver implements ServerObserver {

    private MyBleServer myBleServer;


    public MyServerObserver(Context context){
        init(context);
    }

    private void init(Context context){
        myBleServer = new MyBleServer(context);
        myBleServer.setServerObserver(this);
    }


    @Override
    public void onServerReady() {

    }

    @Override
    public void onDeviceConnectedToServer(@NonNull BluetoothDevice device) {

    }

    @Override
    public void onDeviceDisconnectedFromServer(@NonNull BluetoothDevice device) {

    }
}
