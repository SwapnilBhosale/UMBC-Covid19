/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package edu.umbc.covid19;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import java.util.ArrayList;

import edu.umbc.covid19.ble.MyBleService;
import edu.umbc.covid19.database.DBManager;
import edu.umbc.covid19.main.HomeFragment;
import edu.umbc.covid19.main.ItemCustomAdapter;


public class MainActivity extends FragmentActivity {

	public static final String ACTION_GOTO_REPORTS = "ACTION_GOTO_REPORTS";
	public static final String ACTION_STOP_TRACING = "ACTION_STOP_TRACING";

	private static final int REQ_ONBOARDING = 123;

	private static final String STATE_CONSUMED_EXPOSED_INTENT = "STATE_CONSUMED_EXPOSED_INTENT";
	private boolean consumedExposedIntent;

	private PrefManager prefManager;





	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		prefManager = new PrefManager(this);
		showHomeFragment();
		ComponentName serviceComponent = new ComponentName(this, MyBleService.class);
		JobInfo.Builder builder = new JobInfo.Builder(0, serviceComponent);
		builder.setPeriodic(16*60 * 1000);
		JobScheduler jobScheduler = this.getSystemService(JobScheduler.class);
		int res = jobScheduler.schedule(builder.build());
		Log.i("", "***** schedulke jonb code : : "+res);
	}



	@Override
	public void onResume() {
		super.onResume();

		checkIntentForActions();
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(STATE_CONSUMED_EXPOSED_INTENT, consumedExposedIntent);
	}

	private void checkIntentForActions() {
		Intent intent = getIntent();
		String intentAction = intent.getAction();
		boolean launchedFromHistory = (intent.getFlags() & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0;
		if (ACTION_STOP_TRACING.equals(intentAction) && !launchedFromHistory) {
			intent.setAction(null);
			setIntent(intent);
		}

	}



	private void showHomeFragment() {
		getSupportFragmentManager()
				.beginTransaction()
				.add(R.id.main_fragment_container, HomeFragment.newInstance())
				.commit();
	}


}
