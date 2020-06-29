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

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.Fragment;


import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import edu.umbc.covid19.Constants;
import edu.umbc.covid19.PrefManager;
import edu.umbc.covid19.R;
import edu.umbc.covid19.ble.AlarmReceiver;
import edu.umbc.covid19.database.DBManager;

import static edu.umbc.covid19.Constants.BUTTON_CLICKED_INTENT;
import static edu.umbc.covid19.Constants.BUTTON_CLICKED_INTENT_STATUS;
import static edu.umbc.covid19.Constants.INFECTED_ACTION;

public class HomeFragment extends Fragment {


	private ScrollView scrollView;
	private Switch vSwitch;
	private Button infectedButton;


	private PrefManager prefManager;

	private DBManager dbManager;
	private ItemCustomAdapter itemCustomAdapter;
	ListView list_view;	public HomeFragment() {
		super(R.layout.fragment_home);
	}
	List<InfectStatus> infectList = new ArrayList<>();

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

	BroadcastReceiver listReveiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if(INFECTED_ACTION.equals(intent.getAction())){
				itemCustomAdapter.setList(intent.getExtras().getParcelableArrayList("infectData"));
				itemCustomAdapter.notifyDataSetChanged();

			}
		}
	};

	private void sendNotification(InfectStatus status){
		NotificationCompat.Builder mBuilder =
				new NotificationCompat.Builder(getActivity());

		//Create the intent that’ll fire when the user taps the notification//

		//Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.androidauthority.com/"));
		//PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

		//mBuilder.setContentIntent(pendingIntent);

		mBuilder.setSmallIcon(R.drawable.corona_icon);
		mBuilder.setContentTitle("You have been in proximity of the infected person.");
		mBuilder.setContentText("One person reported the infection and found at location: "+ "" +". You seems to passed this person. Please take care of yourself and report if any symptoms of the Corona Virus");

		NotificationManager mNotificationManager =

				(NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);

		mNotificationManager.notify(001, mBuilder.build());
	}

	public void initView(){
		scheduleAlarm();
		infectedButton = getView().findViewById(R.id.infectedButton);
		if (prefManager.getIsReported()){
			infectedButton.setEnabled(false);
		}
		list_view = (ListView) getView().findViewById(R.id.list1);
		list_view.setEmptyView(getView().findViewById(R.id.emptyView));
		itemCustomAdapter = new ItemCustomAdapter(new ArrayList<>(), getActivity());
		list_view.setAdapter(itemCustomAdapter);
		RequestQueue queue = Volley.newRequestQueue(getActivity());
		infectedButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {

				Log.i("TAG", "******** onClick: i am infecyted clicked");
				try{
					JSONObject object = new JSONObject();
					Log.i("TAG", "onClick: &&&& keys is :  "+prefManager.getDailySecretKey());
					object.put("public_key", prefManager.getDailySecretKey());
					JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, Constants.DP3T_SERVER_URL+"addInfected", object, new Response.Listener<JSONObject>() {
						@Override
						public void onResponse(JSONObject response) {

							prefManager.setIsReported(true);
							infectedButton.setEnabled(false);
						}
					}, new Response.ErrorListener() {
						@Override
						public void onErrorResponse(VolleyError error) {
							Log.i("TAG", "******** onErrorResponse: post call error: "+error.getMessage());
						}
					});
					queue.add(request);
					queue.start();

				}catch (Exception e){
					Log.i("TAG", "onClick: im infecrted "+e.getMessage());
				}

			}
		});
	}



	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {


		vSwitch = view.findViewById(R.id.switch1);
		getContext().registerReceiver(listReveiver, new IntentFilter(INFECTED_ACTION));
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
		getContext().unregisterReceiver(listReveiver);
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

	public void scheduleAlarm() {
		// time at which alarm will be scheduled here alarm is scheduled at 1 day from current time,
		// we fetch  the current time in milliseconds and added 1 day time
		// i.e. 24*60*60*1000= 86,400,000   milliseconds in a day
		Long time = new GregorianCalendar().getTimeInMillis()+60*1000;
		Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("EST"));
		calendar.set(Calendar.HOUR_OF_DAY, 23);
		calendar.set(Calendar.SECOND,0);
		calendar.set(Calendar.MINUTE,58);
		// create an Intent and set the class which will execute when Alarm triggers, here we have
		// given AlarmReciever in the Intent, the onRecieve() method of this class will execute when
		// alarm triggers and
		//we call the method inside onRecieve() method pf Alarmreciever class
		Intent intentAlarm = new Intent(getActivity(), AlarmReceiver.class);
		// create the object
		AlarmManager alarmManager = (AlarmManager) getActivity().getSystemService(Context.ALARM_SERVICE);
		//set the alarm for particular time
		alarmManager.setRepeating(AlarmManager.RTC_WAKEUP,System.currentTimeMillis()+500, AlarmManager.INTERVAL_DAY,  PendingIntent.getBroadcast(getActivity(),1,  intentAlarm, PendingIntent.FLAG_UPDATE_CURRENT));
		//Toast.makeText(this, "Alarm Scheduled ", Toast.LENGTH_LONG).show();
		Log.i("TAG", "###### scheduleAlarm: scheduled");

	}

}
