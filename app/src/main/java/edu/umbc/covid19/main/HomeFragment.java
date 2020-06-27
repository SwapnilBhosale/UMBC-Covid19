/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package edu.umbc.covid19.main;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.Fragment;


import java.util.ArrayList;

import edu.umbc.covid19.PrefManager;
import edu.umbc.covid19.R;
import edu.umbc.covid19.database.DBManager;

import static edu.umbc.covid19.Constants.BUTTON_CLICKED_INTENT;
import static edu.umbc.covid19.Constants.BUTTON_CLICKED_INTENT_STATUS;

public class HomeFragment extends Fragment {


	private ScrollView scrollView;
	private Switch vSwitch;


	private PrefManager prefManager;

	private DBManager dbManager;
	private ItemCustomAdapter itemCustomAdapter;
	ListView list_view;	public HomeFragment() {
		super(R.layout.fragment_home);
	}

	public static HomeFragment newInstance() {
		Bundle args = new Bundle();
		HomeFragment fragment = new HomeFragment();
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		prefManager = new PrefManager(getContext());

	}

	public void initView(){
		list_view = (ListView) getView().findViewById(R.id.list1);
		itemCustomAdapter = new ItemCustomAdapter(new ArrayList<>(), getActivity());
		list_view.setAdapter(itemCustomAdapter);
	}



	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

		scrollView = view.findViewById(R.id.home_scroll_view);
		vSwitch = view.findViewById(R.id.switch1);
		initView();
		vSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
				Log.i("TAG", "onCheckedChanged: "+b);
				if (b && !prefManager.getIsTracingOn()){

					//Toast.makeText(getActivity(), "Status of switch "+String.valueOf(b), Toast.LENGTH_LONG);
					prefManager.setIsTracingOn(true);
					Intent filter = new Intent(BUTTON_CLICKED_INTENT);
					filter.putExtra(BUTTON_CLICKED_INTENT_STATUS, b);
					getActivity().sendBroadcast(filter);
				}
			}
		});
	}

	@Override
	public void onStart() {
		super.onStart();
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
	}




	private void openChannelSettings(String channelId) {

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
			intent.putExtra(Settings.EXTRA_APP_PACKAGE, requireActivity().getPackageName());
			startActivity(intent);
		} else {
			Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
			intent.setData(Uri.parse("package:" + requireActivity().getPackageName()));
			startActivity(intent);
		}
	}

	private boolean isNotificationChannelEnabled(Context context, @Nullable String channelId) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			if (!TextUtils.isEmpty(channelId)) {
				NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
				NotificationChannel channel = manager.getNotificationChannel(channelId);
				if (channel == null) {
					return true;
				}
				return channel.getImportance() != NotificationManager.IMPORTANCE_NONE &&
						!(!manager.areNotificationsEnabled() &&
								channel.getImportance() == NotificationManager.IMPORTANCE_DEFAULT) &&
						manager.areNotificationsEnabled();
			}
			return true;
		} else {
			return NotificationManagerCompat.from(context).areNotificationsEnabled();
		}
	}

}
