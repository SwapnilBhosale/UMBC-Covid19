package edu.umbc.covid19.ble;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
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
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.CursorTreeAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import edu.umbc.covid19.Constants;
import edu.umbc.covid19.PrefManager;
import edu.umbc.covid19.database.DBManager;
import edu.umbc.covid19.database.DatabaseHelper;
import edu.umbc.covid19.main.InfectStatus;
import edu.umbc.covid19.utils.ScheduleUtil;

import static android.bluetooth.BluetoothDevice.BOND_BONDED;
import static android.bluetooth.BluetoothDevice.BOND_BONDING;
import static android.bluetooth.BluetoothDevice.BOND_NONE;
import static android.bluetooth.BluetoothDevice.TRANSPORT_LE;
import static android.bluetooth.BluetoothGatt.GATT_SUCCESS;

public class MyBleService extends JobService  implements LocationListener {
    private Map<String,BluetoothDevice> devices;
    Handler bleHandler = new Handler();
    Context context;
    private BluetoothManager mBluetoothManager;
    private BluetoothGattServer mBluetoothGattServer;
    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;
    private static List<byte[]> ephIds=null;
    LocationManager locationManager;
    // flag for GPS status
    boolean isGPSEnabled = false;

    // flag for network status
    boolean isNetworkEnabled = false;

    // flag for GPS status
    boolean canGetLocation = false;

    Location location; // location
    double latitude; // latitude
    double longitude; // longitude

    // The minimum distance to change Updates in meters
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 10; // 10 meters

    // The minimum time between updates in milliseconds
    private static final long MIN_TIME_BW_UPDATES = 1000 * 60 * 1; // 1 minute

    BluetoothLeScanner scanner;
    ScanCallback scanCallback;


    String RECV_EID;
    String RECV_LAT;
    String RECV_LNG;
    String RECV_RSSI;

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
                    startTheAlternateJob();

                }else{
                    stopTheAlternateJob();
                }
            }
        }
    };

    public MyBleService() {
        devices = new HashMap();
        this.context = this;

    }


    private void stopTheAlternateJob(){
        stopService(new Intent(this, MyBleService.class));
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
    class DataPojo {
        private String public_key;
        private Long time_stamp;
        private List<byte[]> eids;

        public String getPublic_key() {
            return public_key;
        }

        public void setPublic_key(String public_key) {
            this.public_key = public_key;
        }

        public Long getTime_stamp() {
            return time_stamp;
        }

        public void setTime_stamp(Long time_stamp) {
            this.time_stamp = time_stamp;
        }

        public List<byte[]> getEids() {
            return eids;
        }

        public void setEids(List<byte[]> eids) {
            this.eids = eids;
        }
    }

    private void checkIfInfected(JSONArray res) {
        List<DataPojo> list = new ArrayList<>();
        DBManager dbManager = new DBManager(getApplicationContext());
        Cursor c = dbManager.fetchKeys();
        List<InfectStatus> statuses = new ArrayList<>();
        while (c.moveToNext()){
            InfectStatus status = new InfectStatus();
            status.setEid(c.getString(c.getColumnIndex(DatabaseHelper.EID)));
            status.setLat(c.getString(c.getColumnIndex(DatabaseHelper.LAT)));
            status.setLng(c.getString(c.getColumnIndex(DatabaseHelper.LNG)));
            status.setTimestamp(Long.valueOf(c.getString(c.getColumnIndex(DatabaseHelper.TIMESTAMP))));
            Log.i("TAG", "checkIfInfected: in first");
            statuses.add(status);

        }
        try {
            for (int i = 0; i < res.length(); i++) {

                JSONObject obj = res.getJSONObject(i);
                DataPojo pojo = new DataPojo();
                pojo.setPublic_key(obj.getString("public_key"));
                pojo.setTime_stamp(obj.getLong("time_stamp"));
                pojo.setEids(AlarmReceiver.getEphIDsForDay(pojo.getPublic_key().getBytes()));
                Log.i("TAG", "checkIfInfected: in second");
                list.add(pojo);
            }


        }catch(Exception e){
            Log.i("TAG", "checkIfInfected: error "+e.getMessage());
        }

        Log.i("TAG", "checkIfInfected: got from server: "+res.toString());
    }


    private void startTheAlternateJob(){
        StartServerMode();
        final Timer timer = new Timer();
        RequestQueue queue = Volley.newRequestQueue(this);
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {

                        JsonArrayRequest request = new JsonArrayRequest(
                                Constants.DP3T_SERVER_URL+"allInfectedOneDay", new Response.Listener<JSONArray>() {
                            @Override
                            public void onResponse(JSONArray response) {
                                checkIfInfected(response);
                            }
                        }, new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {

                            }
                        });
                        queue.add(request);
                        queue.start();
                        Thread.sleep(30000);
                    }catch (Exception e){
                        Log.i("TAG", "run: volley thread inteerupted: "+e.getMessage());
                    }
                }

            }
        }).start();

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                stopServer();
                stopAdvertising();
                StartBluetoothScan();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        scanner.stopScan(scanCallback);
                    }
                },300000);
            }
        }, 300000);

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
                    startTheAlternateJob();
                    break;
                case BluetoothAdapter.STATE_OFF:
                    stopTheAlternateJob();
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
        Log.i("TAG", "******** startServer called ");
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

        BluetoothGattCharacteristic characteristic3 = new BluetoothGattCharacteristic(UUID.fromString(Constants.UUID_CHAR_RSSI),
                BluetoothGattCharacteristic.PROPERTY_WRITE |
                        BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_WRITE);

        bluetoothGattService.addCharacteristic(characteristic3);


        mBluetoothGattServer.addService(bluetoothGattService);

        Log.i("TAG", "********* SstartServer: added services to ble");
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
                .addServiceUuid(new ParcelUuid(UUID.fromString(Constants.UUID_SERVICE_PRIMARY)))
                .build();

        mBluetoothLeAdvertiser
                .startAdvertising(settings, data, mAdvertiseCallback);
    }

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        registerReceiver(receiver, new IntentFilter(Constants.BUTTON_CLICKED_INTENT));
        scheduleAlarm();
        getLocation();
        Log.i("TAG", "************ sonStartJob: ");
        scheduleAlarm();
        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);

        startTheAlternateJob();

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
        scanCallback = new ScanCallback() {
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
                                            gatt.readRemoteRssi();
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
                                    gatt.readCharacteristic(gatt.getService(UUID.fromString(Constants.UUID_SERVICE_PRIMARY)).getCharacteristic(UUID.fromString(Constants.UUID_CHAR_EID)));

                            }

                            @Override
                            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                                String uuid = characteristic.getUuid().toString();
                                super.onCharacteristicRead(gatt, characteristic, status);
                                DBManager dbManager = new DBManager(getApplicationContext());
                                if (Constants.UUID_CHAR_EID.equals(uuid)){
                                    RECV_EID = bytesToHex(characteristic.getValue());
                                    gatt.readCharacteristic(gatt.getService(UUID.fromString(Constants.UUID_SERVICE_PRIMARY)).getCharacteristic(UUID.fromString(Constants.UUID_CHAR_LAT)));
                                }else if (Constants.UUID_CHAR_LAT.equals(uuid)){
                                    RECV_LAT = String.valueOf(bytesToLong(characteristic.getValue()));
                                    gatt.readCharacteristic(gatt.getService(UUID.fromString(Constants.UUID_SERVICE_PRIMARY)).getCharacteristic(UUID.fromString(Constants.UUID_CHAR_LAT)));
                                }else if(Constants.UUID_CHAR_LNG.equals(uuid)){
                                    RECV_LNG = String.valueOf(bytesToLong(characteristic.getValue()));
                                    BluetoothGattCharacteristic c =
                                            gatt.getService(UUID.fromString(Constants.UUID_SERVICE_PRIMARY))
                                                    .getCharacteristic(UUID.fromString(Constants.UUID_CHAR_EID));
                                    Cursor cu = dbManager.getEphIds();
                                    if (null!=cu && cu.getCount() > 0) {
                                        String db_eid[] = cu.getString(cu.getColumnIndex(Constants.UUID_CHAR_EID)).split(" ");
                                        int randomNum = ThreadLocalRandom.current().nextInt(0, 23);
                                        c.setValue(db_eid[randomNum]);
                                        gatt.writeCharacteristic(c);
                                    }

                                }
                            }

                            @Override
                            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                                super.onCharacteristicWrite(gatt, characteristic, status);
                                DBManager manager = new DBManager(getApplicationContext());
                                String uuid = characteristic.getUuid().toString();
                                if (Constants.UUID_CHAR_EID.equals(uuid)) {
                                    BluetoothGattCharacteristic c =
                                            gatt.getService(UUID.fromString(Constants.UUID_SERVICE_PRIMARY))
                                                    .getCharacteristic(UUID.fromString(Constants.UUID_CHAR_LAT));
                                    c.setValue(String.valueOf(latitude));
                                    gatt.writeCharacteristic(c);
                                } else if (Constants.UUID_CHAR_LAT.equals(uuid)){
                                    BluetoothGattCharacteristic c =
                                            gatt.getService(UUID.fromString(Constants.UUID_SERVICE_PRIMARY))
                                                    .getCharacteristic(UUID.fromString(Constants.UUID_CHAR_LNG));
                                    c.setValue(String.valueOf(longitude));
                                    gatt.writeCharacteristic(c);
                                } else if(Constants.UUID_CHAR_LNG.equals(uuid)){
                                    BluetoothGattCharacteristic c =
                                            gatt.getService(UUID.fromString(Constants.UUID_SERVICE_PRIMARY))
                                                    .getCharacteristic(UUID.fromString(Constants.UUID_CHAR_RSSI));
                                    c.setValue(String.valueOf(longitude));
                                    gatt.writeCharacteristic(c);
                                    manager.insert(RECV_EID, RECV_LAT, RECV_LNG, RECV_RSSI);
                                    gatt.disconnect();
                                }
                            }

                            @Override
                            public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
                                super.onReadRemoteRssi(gatt, rssi, status);
                                RECV_RSSI = String.valueOf(rssi);
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
        scanner = adapter.getBluetoothLeScanner();



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

        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i("TAG", "BluetoothDevice CONNECTED: " + device);
                if (!mRegisteredDevices.contains(device)){
                    mRegisteredDevices.add(device);
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i("TAG", "BluetoothDevice DISCONNECTED: " + device);
                //Remove device from any active subscriptions
                mRegisteredDevices.remove(device);
                DBManager manager = new DBManager(getApplicationContext());
                manager.insert(RECV_EID, RECV_LAT, RECV_LNG, RECV_RSSI);
            }
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset,
                                                BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
            DBManager dbManager = new DBManager(getApplicationContext());
            Cursor c = dbManager.getEphIds();
            if(null != c && c.getCount() > 0) {

                //super call
                String db_eid[] = c.getString(c.getColumnIndex(Constants.UUID_CHAR_EID)).split(" ");

                String uuid = characteristic.getUuid().toString();

                String lat = String.valueOf(getLatitude());
                String lng = String.valueOf(getLongitude());
                if (Constants.UUID_CHAR_EID.equals(uuid)) {
                    //sending final response
                    int randomNum = ThreadLocalRandom.current().nextInt(0, 23);
                    byte[] ephId = db_eid[randomNum].getBytes();
                    mBluetoothGattServer.sendResponse(device, requestId, GATT_SUCCESS, 0, ephId);
                } else if (Constants.UUID_CHAR_LAT.equals(uuid)) {
                    mBluetoothGattServer.sendResponse(device, requestId, GATT_SUCCESS, 0, lat.getBytes());
                } else if (Constants.UUID_CHAR_LNG.equals(uuid)) {
                    mBluetoothGattServer.sendResponse(device, requestId, GATT_SUCCESS, 0, lng.getBytes());
                }
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

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            String uuid = characteristic.getUuid().toString();
            DBManager dbManager = new DBManager(getApplicationContext());
            if (Constants.UUID_CHAR_EID.equals(uuid)) {
                RECV_EID = bytesToHex(value);
                mBluetoothGattServer.sendResponse(device, requestId, GATT_SUCCESS, 0,null);
            }else if (Constants.UUID_CHAR_LAT.equals(uuid)){
                RECV_LAT = String.valueOf(bytesToLong(value));
                mBluetoothGattServer.sendResponse(device, requestId, GATT_SUCCESS, 0, null);
            }else if (Constants.UUID_CHAR_LNG.equals(uuid)){
                RECV_LAT = String.valueOf(bytesToLong(value));
                mBluetoothGattServer.sendResponse(device, requestId, GATT_SUCCESS, 0, null);
            }else if(uuid.equals(Constants.UUID_CHAR_RSSI)){
                RECV_RSSI = String.valueOf(bytesToLong(value));
                mBluetoothGattServer.sendResponse(device, requestId, GATT_SUCCESS, 0, null);
                dbManager.insert(RECV_EID, RECV_LAT, RECV_LNG, RECV_RSSI);
                mBluetoothGattServer.cancelConnection(device);
            }
        }
    };

    public long bytesToLong(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.put(bytes);
        buffer.flip();//need flip
        return buffer.getLong();
    }

    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
    public String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    public Location getLocation() {
        try {
            locationManager = (LocationManager) getApplication().getSystemService(LOCATION_SERVICE);

            // getting GPS status
            isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

            // getting network status
            isNetworkEnabled = locationManager
                    .isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            if (!isGPSEnabled && !isNetworkEnabled) {
                // no network provider is enabled
            } else {
                this.canGetLocation = true;
                // First get location from Network Provider
                if (isNetworkEnabled) {
                    //check the network permission
                    if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions((Activity) getApplicationContext(), new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 101);
                    }
                    locationManager.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER,
                            MIN_TIME_BW_UPDATES,
                            MIN_DISTANCE_CHANGE_FOR_UPDATES, this);

                    Log.d("Network", "Network");
                    if (locationManager != null) {
                        location = locationManager
                                .getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

                        if (location != null) {
                            latitude = location.getLatitude();
                            longitude = location.getLongitude();
                        }
                    }
                }

                // if GPS Enabled get lat/long using GPS Services
                if (isGPSEnabled) {
                    if (location == null) {
                        //check the network permission
                        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions((Activity) getApplicationContext(), new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 101);
                        }
                        locationManager.requestLocationUpdates(
                                LocationManager.GPS_PROVIDER,
                                MIN_TIME_BW_UPDATES,
                                MIN_DISTANCE_CHANGE_FOR_UPDATES, this);

                        Log.d("GPS Enabled", "GPS Enabled");
                        if (locationManager != null) {
                            location = locationManager
                                    .getLastKnownLocation(LocationManager.GPS_PROVIDER);

                            if (location != null) {
                                latitude = location.getLatitude();
                                longitude = location.getLongitude();
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        Log.i("TAG", "getLocation: "+location.getLongitude());
        return location;
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {

    }
    public void stopUsingGPS(){
        if(locationManager != null){
            locationManager.removeUpdates(MyBleService.this);
        }
    }

    public double getLatitude(){
        if(location != null){
            latitude = location.getLatitude();
        }

        // return latitude
        return latitude;
    }

    /**
     * Function to get longitude
     * */

    public double getLongitude(){
        if(location != null){
            longitude = location.getLongitude();
        }

        // return longitude
        return longitude;
    }

    public boolean canGetLocation() {
        return this.canGetLocation;
    }

    public void showSettingsAlert(){
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(getApplicationContext());

        // Setting Dialog Title
        alertDialog.setTitle("GPS is settings");

        // Setting Dialog Message
        alertDialog.setMessage("GPS is not enabled. Do you want to go to settings menu?");

        // On pressing Settings button
        alertDialog.setPositiveButton("Settings", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog,int which) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                getApplicationContext().startActivity(intent);
            }
        });

        // on pressing cancel button
        alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        alertDialog.show();
    }



}
