package edu.umbc.covid19;

import android.Manifest;
import android.app.Activity;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.google.android.material.appbar.MaterialToolbar;

import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import edu.umbc.covid19.adapter.DevicesAdapter;
import edu.umbc.covid19.adapter.DiscoveredBluetoothDevice;
import edu.umbc.covid19.ble.MyBleService;
import edu.umbc.covid19.utils.Utils;
import edu.umbc.covid19.viewmodels.ScannerStateLiveData;
import edu.umbc.covid19.viewmodels.ScannerViewModel;

public class MainActivity1 extends AppCompatActivity  implements DevicesAdapter.OnItemClickListener{

    private static final int REQUEST_ACCESS_FINE_LOCATION = 1022; // random number

    private ScannerViewModel scannerViewModel;

    View scanningView;
    View emptyView;
    View noLocationPermissionView;
    Button grantPermissionButton;
    Button permissionSettingsButton;
    View noLocationView;
    View noBluetoothView;
    private Context context;
    Button grantBluetoothButthon;
    Button grantLocationButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        context = this;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_1);
        init();
        initListeners();


        final MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.app_name);
        setSupportActionBar(toolbar);

        // Create view model containing utility methods for scanning
        scannerViewModel = new ViewModelProvider(this).get(ScannerViewModel.class);
        scannerViewModel.getScannerState().observe(this, this::startScan);

        // Configure the recycler view
        final RecyclerView recyclerView = findViewById(R.id.recycler_view_ble_devices);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        final RecyclerView.ItemAnimator animator = recyclerView.getItemAnimator();
        if (animator instanceof SimpleItemAnimator) {
            ((SimpleItemAnimator) animator).setSupportsChangeAnimations(false);
        }
        final DevicesAdapter adapter = new DevicesAdapter(this, scannerViewModel.getDevices());
        adapter.setOnItemClickListener(this);
        recyclerView.setAdapter(adapter);
    }

    private void init (){

        scanningView = findViewById(R.id.state_scanning);
        emptyView = findViewById(R.id.no_devices);
        noLocationPermissionView = findViewById(R.id.no_location_permission);
        grantPermissionButton = findViewById(R.id.action_grant_location_permission);
        permissionSettingsButton = findViewById(R.id.action_permission_settings);
        noLocationView = findViewById(R.id.no_location);
        noBluetoothView = findViewById(R.id.bluetooth_off);
        grantBluetoothButthon = findViewById(R.id.action_enable_bluetooth);
        grantLocationButton = findViewById(R.id.action_enable_location);


        ComponentName serviceComponent = new ComponentName(context, MyBleService.class);
        JobInfo.Builder builder = new JobInfo.Builder(0, serviceComponent);
        builder.setPeriodic(16*60 * 1000);
        //builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED); // require unmetered network
        //builder.setRequiresDeviceIdle(true); // device should be idle
        //builder.setRequiresCharging(false); // we don't care if the device is charging or not
        JobScheduler jobScheduler = context.getSystemService(JobScheduler.class);
        int res = jobScheduler.schedule(builder.build());
        Log.i("", "***** schedulke jonb code : : "+res);
    }


    private void startScan(final ScannerStateLiveData state) {
        // First, check the Location permission. This is required on Marshmallow onwards in order
        // to scan for Bluetooth LE devices.
        if (Utils.isLocationPermissionsGranted(this)) {
            noLocationPermissionView.setVisibility(View.GONE);

            // Bluetooth must be enabled.
            if (state.isBluetoothEnabled()) {
                noBluetoothView.setVisibility(View.GONE);

                // We are now OK to start scanning.
                scannerViewModel.startScan();
                scanningView.setVisibility(View.VISIBLE);

                if (!state.hasRecords()) {
                    emptyView.setVisibility(View.VISIBLE);

                    if (!Utils.isLocationRequired(this) || Utils.isLocationEnabled(this)) {
                        noLocationView.setVisibility(View.INVISIBLE);
                    } else {
                        noLocationView.setVisibility(View.VISIBLE);
                    }
                } else {
                    emptyView.setVisibility(View.GONE);
                }
            } else {
                noBluetoothView.setVisibility(View.VISIBLE);
                scanningView.setVisibility(View.INVISIBLE);
                emptyView.setVisibility(View.GONE);

            }
        } else {
            noLocationPermissionView.setVisibility(View.VISIBLE);
            noBluetoothView.setVisibility(View.GONE);
            scanningView.setVisibility(View.INVISIBLE);
            emptyView.setVisibility(View.GONE);

            final boolean deniedForever = Utils.isLocationPermissionDeniedForever(this);
            grantPermissionButton.setVisibility(deniedForever ? View.GONE : View.VISIBLE);
            permissionSettingsButton.setVisibility(deniedForever ? View.VISIBLE : View.GONE);
        }
    }


    @Override
    protected void onRestart() {
        super.onRestart();
        clear();
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopScan();
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.filter, menu);
        menu.findItem(R.id.filter_uuid).setChecked(scannerViewModel.isUuidFilterEnabled());
        menu.findItem(R.id.filter_nearby).setChecked(scannerViewModel.isNearbyFilterEnabled());
        return true;
    }



    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.filter_uuid:
                item.setChecked(!item.isChecked());
                scannerViewModel.filterByUuid(item.isChecked());
                return true;
            case R.id.filter_nearby:
                item.setChecked(!item.isChecked());
                scannerViewModel.filterByDistance(item.isChecked());
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClick(@NonNull final DiscoveredBluetoothDevice device) {
        //final Intent controlBlinkIntent = new Intent(this, BlinkyActivity.class);
        //controlBlinkIntent.putExtra(BlinkyActivity.EXTRA_DEVICE, device);
        //startActivity(controlBlinkIntent);
        //Log.i("TAG", "****** onItemClick: "+device.getScanResult().getScanRecord().getServiceUuids().toString());
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode,
                                           @NonNull final String[] permissions,
                                           @NonNull final int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_ACCESS_FINE_LOCATION) {
            scannerViewModel.refresh();
        }
    }




    private void initListeners(){

        permissionSettingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.fromParts("package", getPackageName(), null));
                startActivity(intent);
            }
        });
        grantPermissionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Utils.markLocationPermissionRequested(context);
                ActivityCompat.requestPermissions(
                        (Activity)context,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        REQUEST_ACCESS_FINE_LOCATION);
            }
        });
        grantBluetoothButthon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivity(enableIntent);
            }
        });
        grantLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        });

}

    /**
     * stop scanning for bluetooth devices.
     */
    private void stopScan() {
        scannerViewModel.stopScan();
    }

    /**
     * Clears the list of devices, which will notify the observer.
     */
    private void clear() {
        scannerViewModel.getDevices().clear();
        scannerViewModel.getScannerState().clearRecords();
    }
}