package edu.umbc.covid19.ble;

import android.app.Service;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import edu.umbc.covid19.Constants;
import edu.umbc.covid19.utils.ScheduleUtil;

import static android.bluetooth.BluetoothDevice.BOND_BONDED;
import static android.bluetooth.BluetoothDevice.BOND_BONDING;
import static android.bluetooth.BluetoothDevice.BOND_NONE;
import static android.bluetooth.BluetoothDevice.TRANSPORT_LE;
import static android.bluetooth.BluetoothGatt.GATT_SUCCESS;

public class MyBleService extends JobService {
    private Map<String,BluetoothDevice> devices;
    Handler bleHandler = new Handler();
    Context context;

    public MyBleService() {
        devices = new HashMap();
        this.context = this;
    }

    private void startServer(){
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothGattServer server = null;
        BluetoothGattServerCallback gattServerCallback = new BluetoothGattServerCallback() {
            @Override
            public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
                super.onConnectionStateChange(device, status, newState);
            }

            @Override
            public void onServiceAdded(int status, BluetoothGattService service) {
                super.onServiceAdded(status, service);
            }

            @Override
            public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
                super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
                server.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, "OK");
            }

            @Override
            public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
                super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
            }

            @Override
            public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
                super.onDescriptorReadRequest(device, requestId, offset, descriptor);
            }

            @Override
            public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
                super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value);
            }

            @Override
            public void onExecuteWrite(BluetoothDevice device, int requestId, boolean execute) {
                super.onExecuteWrite(device, requestId, execute);
            }

            @Override
            public void onNotificationSent(BluetoothDevice device, int status) {
                super.onNotificationSent(device, status);
            }
        };
        server= bluetoothManager.openGattServer(context, gattServerCallback);

        BluetoothGattService service = new BluetoothGattService(UUID.fromString(Constants.UUID_SER), BluetoothGattService.SERVICE_TYPE_PRIMARY);
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(UUID.fromString(Constants.UUID_CHAR_EID),
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE |
                        BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);

        characteristic.addDescriptor(new BluetoothGattDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"), BluetoothGattCharacteristic.PERMISSION_WRITE));

        service.addCharacteristic(characteristic);

        server.addService(service);

    }


    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        Log.i("TAG", "************ sonStartJob: ");
        StartBluetoothScan();
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        return true;
    }

    private void StartBluetoothScan(){
        Log.i("TAG", "************* method called StartBluetoothScan: ");
         final ScanCallback scanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                BluetoothDevice device = result.getDevice();
                // ...do whatever you want with this found device
                if (device != null && null != device.getName())
                    if (!devices.containsKey(device.getName())) {
                        devices.put(device.getName(), device);
                        Log.i("TAG", "********* onScanResult: "+device.getName());
                        BluetoothGatt gatt = device.connectGatt(context, false, new BluetoothGattCallback() {
                            @Override
                            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                                Log.i("TAG", "onConnectionStateChange: "+status+" "+newState);
                                if (status == GATT_SUCCESS) {
                                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                                        int bondstate = device.getBondState();
                                        // Take action depending on the bond state
                                        if (bondstate == BOND_NONE || bondstate == BOND_BONDED) {

                                            // Connected to device, now proceed to discover it's services but delay a bit if needed
                                            int delayWhenBonded = 0;
                                            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N) {
                                                delayWhenBonded = 1000;
                                            }
                                            final int delay = bondstate == BOND_BONDED ? delayWhenBonded : 0;

                                            Runnable discoverServicesRunnable = new Runnable() {
                                                @Override
                                                public void run() {
                                                    Log.d("TAG", String.format(Locale.ENGLISH, "discovering services of '%s' with delay of %d ms", device.getName(), delay));
                                                    boolean result = gatt.discoverServices();
                                                    if (!result) {
                                                        Log.e("TAG", "discoverServices failed to start");
                                                    }
                                                    //discoverServicesRunnable = null;
                                                }
                                            };
                                            bleHandler.postDelayed(discoverServicesRunnable, delay);
                                        } else if (bondstate == BOND_BONDING) {
                                            // Bonding process in progress, let it complete
                                            Log.i("TAG", "waiting for bonding to complete");
                                        }
                                    }
                                } else {
                                    gatt.close();
                                }
                            }

                            @Override
                            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                                super.onServicesDiscovered(gatt, status);
                            }

                            @Override
                            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                                super.onCharacteristicRead(gatt, characteristic, status);
                            }

                            @Override
                            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                                super.onCharacteristicWrite(gatt, characteristic, status);
                            }

                            @Override
                            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                                super.onCharacteristicChanged(gatt, characteristic);
                            }

                            @Override
                            public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                                super.onDescriptorRead(gatt, descriptor, status);
                            }

                            @Override
                            public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                                super.onDescriptorWrite(gatt, descriptor, status);
                            }
                        }, TRANSPORT_LE);
                        gatt.connect();

                    }
            }


            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                // Ignore for now
            }

            @Override
            public void onScanFailed(int errorCode) {
                // Ignore for now
            }
        };

        ScanSettings scanSettings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                .setMatchMode(ScanSettings.MATCH_MODE_STICKY)
                .setNumOfMatches(ScanSettings.MATCH_NUM_FEW_ADVERTISEMENT)
                .setReportDelay(0L)
                .build();

        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothLeScanner scanner = adapter.getBluetoothLeScanner();



        UUID BLP_SERVICE_UUID = UUID.fromString(Constants.UUID_SER);
        UUID[] serviceUUIDs = new UUID[]{BLP_SERVICE_UUID};
        List<ScanFilter> filters = null;
        if(serviceUUIDs != null) {
            filters = new ArrayList<>();
            for (UUID serviceUUID : serviceUUIDs) {
                ScanFilter filter = new ScanFilter.Builder()
                        .setServiceUuid(new ParcelUuid(serviceUUID))
                        .build();
                filters.add(filter);
            }
        }
        if (scanner != null) {
            scanner.startScan(null, scanSettings, scanCallback);
            Log.d("TAG", "scan started");
        }  else {
            Log.e("TAG", "could not get scanner object");
        }


    }
}
