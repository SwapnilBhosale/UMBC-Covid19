/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package edu.umbc.covid19.viewmodel;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.Collection;
import java.util.List;

import edu.umbc.covid19.main.model.TracingStatusInterface;


public class TracingViewModel extends AndroidViewModel {
	public static final String UPDATE_INTENT_ACTION = "org.dpppt.android.sdk.UPDATE_ACTION";

	private final MutableLiveData<TracingStatus> tracingStatusLiveData = new MutableLiveData<>();
	private BroadcastReceiver tracingStatusBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			invalidateTracingStatus();
		}
	};

	private final MutableLiveData<Boolean> tracingEnabledLiveData = new MutableLiveData<>();
	private final MutableLiveData<Pair<Boolean, Boolean>> exposedLiveData = new MutableLiveData<>();
	private final MutableLiveData<Integer> numberOfHandshakesLiveData = new MutableLiveData<>(0);
	private final MutableLiveData<TracingStatusInterface> appStatusLiveData = new MutableLiveData<>();


	private final MutableLiveData<Boolean> bluetoothEnabledLiveData = new MutableLiveData<>();
	private BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) {
				invalidateBluetoothState();
				invalidateTracingStatus();
			}
		}
	};

	public TracingViewModel(@NonNull Application application) {
		super(application);

		tracingStatusLiveData.observeForever(status -> {
			tracingEnabledLiveData.setValue(status.isAdvertising() && status.isReceiving());
			numberOfHandshakesLiveData.setValue(status.getNumberOfContacts());

			/*exposedLiveData
					.setValue(new Pair<>(tracingStatusInterface.isReportedAsInfected(),
							tracingStatusInterface.wasContactReportedAsExposed()));

			appStatusLiveData.setValue(tracingStatusInterface);*/
		});

		invalidateBluetoothState();
		invalidateTracingStatus();

		application.registerReceiver(tracingStatusBroadcastReceiver, getUpdateIntentFilter());
		application.registerReceiver(bluetoothReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
	}

	public static IntentFilter getUpdateIntentFilter() {
		return new IntentFilter(UPDATE_INTENT_ACTION);
	}

	public void invalidateTracingStatus() {
		TracingStatus status =getStatus(getApplication());
		tracingStatusLiveData.setValue(status);
	}

	public static TracingStatus getStatus(Context context) {
		Database database = new Database(context);
		AppConfigManager appConfigManager = AppConfigManager.getInstance(context);
		Collection<TracingStatus.ErrorState> errorStates = ErrorHelper.checkTracingErrorStatus(context);
		List<ExposureDay> exposureDays = database.getExposureDays();
		InfectionStatus infectionStatus;
		if (appConfigManager.getIAmInfected()) {
			infectionStatus = InfectionStatus.INFECTED;
		} else if (exposureDays.size() > 0) {
			infectionStatus = InfectionStatus.EXPOSED;
		} else {
			infectionStatus = InfectionStatus.HEALTHY;
		}
		return new TracingStatus(
				database.getContacts().size(),
				appConfigManager.isAdvertisingEnabled(),
				appConfigManager.isReceivingEnabled(),
				appConfigManager.getLastSyncDate(),
				infectionStatus,
				exposureDays,
				errorStates
		);
	}

	public LiveData<Boolean> getTracingEnabledLiveData() {
		return tracingEnabledLiveData;
	}

	public LiveData<Pair<Boolean, Boolean>> getSelfOrContactExposedLiveData() {
		return exposedLiveData;
	}


	public LiveData<TracingStatusInterface> getAppStatusLiveData() {
		return appStatusLiveData;
	}

	public LiveData<TracingStatus> getTracingStatusLiveData() {
		return tracingStatusLiveData;
	}

	public LiveData<Boolean> getBluetoothEnabledLiveData() {
		return bluetoothEnabledLiveData;
	}

	public void setTracingEnabled(boolean enabled) {
		if (enabled) {
			DP3T.start(getApplication());
		} else {
			DP3T.stop(getApplication());
		}
	}


	public void sync() {
		new Thread() {
			@Override
			public void run() {
				//DP3T.sync(getApplication());
			}
		}.start();
	}

	public void invalidateService() {
		if (tracingEnabledLiveData.getValue()) {
			//DP3T.start(getApplication());
		}
	}

	private void invalidateBluetoothState() {
		//bluetoothEnabledLiveData.setValue(DeviceFeatureHelper.isBluetoothEnabled());
	}

	@Override
	protected void onCleared() {
		getApplication().unregisterReceiver(tracingStatusBroadcastReceiver);
		getApplication().unregisterReceiver(bluetoothReceiver);
	}

}
