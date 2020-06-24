package edu.umbc.covid19.ble;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import edu.umbc.covid19.utils.ScheduleUtil;

public class BleServiceReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        ScheduleUtil.scheduleJob(context);
    }
}
