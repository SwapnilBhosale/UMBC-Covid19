package edu.umbc.covid19.ble;

import android.app.AlarmManager;
import android.app.PendingIntent;
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
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
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
    private BluetoothManager mBluetoothManager;
    private BluetoothGattServer mBluetoothGattServer;
    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;
    private static List<byte[]> ephIds=null;

    public List<byte[]> getEphIds() {
        return ephIds;
    }

    public static void setEphIds(List<byte[]> newEphIds) {
        ephIds = newEphIds;
    }

    /* Collection of notification subscribers */
    private Set<BluetoothDevice> mRegisteredDevices = new HashSet<>();

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Constants.BUTTON_CLICKED_INTENT.equals(intent.getAction())) {

                boolean status = intent.getBooleanExtra(Constants.BUTTON_CLICKED_INTENT_STATUS, false);
                Log.i("TAG", "********Service onReceive: "+status);
                if(status){
                    //start
                }else{
                    //stop
                }
            }
        }
    };

    public MyBleService() {
        devices = new HashMap();
        this.context = this;

    }


    /**
     * Callback to receive information about the advertisement process.
     */
    private AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            Log.i("TAG", "LE Advertise Started.");
        }

        @Override
        public void onStartFailure(int errorCode) {
            Log.w("TAG", "LE Advertise Failed: "+errorCode);
        }
    };

    private void StartServerMode(){
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        // Register for system Bluetooth events
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mBluetoothReceiver, filter);
        if (!bluetoothAdapter.isEnabled()) {
            Log.d("TAG", "Bluetooth is currently disabled...enabling");
            bluetoothAdapter.enable();
        } else {
            Log.d("TAG", "Bluetooth enabled...starting services");
            startAdvertising();
            startServer();
        }

    }


    /**
     * Listens for Bluetooth adapter events to enable/disable
     * advertising and server functionality.
     */
    private BroadcastReceiver mBluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);

            switch (state) {
                case BluetoothAdapter.STATE_ON:
                    startAdvertising();
                    startServer();
                    break;
                case BluetoothAdapter.STATE_OFF:
                    stopServer();
                    stopAdvertising();
                    break;
                default:
                    // Do nothing
            }

        }
    };

    /**
     * Shut down the GATT server.
     */
    private void stopServer() {
        if (mBluetoothGattServer == null) return;

        mBluetoothGattServer.close();
    }

    /**
     * Stop Bluetooth advertisements.
     */
    private void stopAdvertising() {
        if (mBluetoothLeAdvertiser == null) return;

        mBluetoothLeAdvertiser.stopAdvertising(mAdvertiseCallback);
    }

    /**
     * Initialize the GATT server instance with the services/characteristics
     * from the Time Profile.
     */
    private void startServer() {
        mBluetoothGattServer = mBluetoothManager.openGattServer(this, mGattServerCallback);
        if (mBluetoothGattServer == null) {
            Log.w("TAG", "Unable to create GATT server");
            return;
        }

        BluetoothGattService bluetoothGattService = new BluetoothGattService(UUID.fromString(Constants.UUID_SERVICE_PRIMARY), BluetoothGattService.SERVICE_TYPE_PRIMARY);


        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(UUID.fromString(Constants.UUID_CHAR_EID),
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE |
                        BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);
        bluetoothGattService.addCharacteristic(characteristic);

        BluetoothGattCharacteristic characteristic1 = new BluetoothGattCharacteristic(UUID.fromString(Constants.UUID_CHAR_LAT),
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE |
                        BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);
        bluetoothGattService.addCharacteristic(characteristic1);

        BluetoothGattCharacteristic characteristic2 = new BluetoothGattCharacteristic(UUID.fromString(Constants.UUID_CHAR_LNG),
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE |
                        BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);

        bluetoothGattService.addCharacteristic(characteristic2);


        mBluetoothGattServer.addService(bluetoothGattService);

        // Initialize the local UI
        //updateLocalUi(System.currentTimeMillis());
    }

    /**
     * Begin advertising over Bluetooth that this device is connectable
     * and supports the Current Time Service.
     */
    private void startAdvertising() {
        BluetoothAdapter bluetoothAdapter = mBluetoothManager.getAdapter();
        mBluetoothLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
        if (mBluetoothLeAdvertiser == null) {
            Log.w("TAG", "Failed to create advertiser");
            return;
        }

        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setConnectable(true)
                .setTimeout(0)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .build();

        AdvertiseData data = new AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .setIncludeTxPowerLevel(false)
                .addServiceUuid(new ParcelUuid(UUID.fromString(Constants.UUID_SERVICE_PRIMARY)))
                .build();

        mBluetoothLeAdvertiser
                .startAdvertising(settings, data, mAdvertiseCallback);
    }

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        registerReceiver(receiver, new IntentFilter(Constants.BUTTON_CLICKED_INTENT));
        Log.i("TAG", "************ sonStartJob: ");
        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        StartBluetoothScan();
        //StartServerMode();
        scheduleAlarm();
        return true;
    }
    public void scheduleAlarm() {
        // time at which alarm will be scheduled here alarm is scheduled at 1 day from current time,
        // we fetch  the current time in milliseconds and added 1 day time
        // i.e. 24*60*60*1000= 86,400,000   milliseconds in a day
         Long time = new GregorianCalendar().getTimeInMillis()+60*1000;
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("EST"));
        calendar.set(Calendar.HOUR_OF_DAY, 13);
        calendar.set(Calendar.SECOND,0);
        calendar.set(Calendar.MINUTE,46);
        // create an Intent and set the class which will execute when Alarm triggers, here we have
        // given AlarmReciever in the Intent, the onRecieve() method of this class will execute when
        // alarm triggers and
        //we call the method inside onRecieve() method pf Alarmreciever class
        Intent intentAlarm = new Intent(this, AlarmReceiver.class);
        // create the object
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        //set the alarm for particular time
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP,time,AlarmManager.INTERVAL_FIFTEEN_MINUTES,  PendingIntent.getBroadcast(this,1,  intentAlarm, PendingIntent.FLAG_UPDATE_CURRENT));
        //Toast.makeText(this, "Alarm Scheduled ", Toast.LENGTH_LONG).show();
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
                        Toast.makeText(context, "Got device "+device.getName(),Toast.LENGTH_LONG);
                        Log.i("TAG", "********* onScanResult: "+device.getName());
                        BluetoothGatt gatt = device.connectGatt(getApplicationContext(), false, new BluetoothGattCallback() {
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

                                    List<BluetoothGattService> services = gatt.getServices();
                                    //Toast.makeText(getApplicationContext(), "Got service "+services.get(0).getCharacteristics().get(0).getUuid(), Toast.LENGTH_LONG);
                                    services.forEach(s -> s.getCharacteristics().forEach(c -> Log.i("TAG : ", "onServicesDiscovered: " + c.getUuid())));

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



        UUID BLP_SERVICE_UUID = UUID.fromString(Constants.UUID_SERVICE_PRIMARY);
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
            scanner.startScan(filters, scanSettings, scanCallback);
            Log.d("TAG", "scan started");
        }  else {
            Log.e("TAG", "could not get scanner object");
        }


    }


    /**
     * Callback to handle incoming requests to the GATT server.
     * All read/write requests for characteristics and descriptors are handled here.
     */
    private BluetoothGattServerCallback mGattServerCallback = new BluetoothGattServerCallback() {

        private int counter = 0;
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i("TAG", "BluetoothDevice CONNECTED: " + device);
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i("TAG", "BluetoothDevice DISCONNECTED: " + device);
                //Remove device from any active subscriptions
                mRegisteredDevices.remove(device);
            }
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset,
                                                BluetoothGattCharacteristic characteristic) {
            long now = System.currentTimeMillis();

            //super call
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
            String uuid = characteristic.getUuid().toString();

            if (Constants.UUID_CHAR_EID.equals(uuid)) {
                //sending final response
                counter++;
                counter = counter<24?counter:0;
                byte[] ephId = ephIds.get(counter);
                mBluetoothGattServer.sendResponse(device, requestId, GATT_SUCCESS, 0, ephId);
            }else if (Constants.UUID_CHAR_LAT.equals(uuid)){
                mBluetoothGattServer.sendResponse(device, requestId, GATT_SUCCESS, 0, "TEST_LAT".getBytes());
            }else if (Constants.UUID_CHAR_LNG.equals(uuid)){
                mBluetoothGattServer.sendResponse(device, requestId, GATT_SUCCESS, 0, "TEST_LNG".getBytes());
            }
        }

        @Override
        public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset,
                                            BluetoothGattDescriptor descriptor) {
            super.onDescriptorReadRequest(device, requestId,offset,  descriptor);
        }

        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId,
                                             BluetoothGattDescriptor descriptor,
                                             boolean preparedWrite, boolean responseNeeded,
                                             int offset, byte[] value) {
            super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value);
        }


    };
}
