package edu.ucla.nesl.flowengine.ui;

import java.util.Timer;
import java.util.TimerTask;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ToggleButton;

public class FlowEngineControl extends Activity {

	private static final String TAG = FlowEngineControl.class.getSimpleName();
	
	private static final String FlowEngineMainServiceName = "edu.ucla.nesl.flowengine.FlowEngine";
	private static final String AbstractDeviceAccelerometerServiceName = "edu.ucla.nesl.flowengine.abstractdevice.accelerometer.AccelerometerService";
	private static final String FlowMBedEngineServiceName = "edu.ucla.nesl.flowengine.mbed.FlowMBedEngine";
	
	private static final int MSG_UPDATE_UI = 0xAB;
	
	private Timer timer;
	private Handler msgHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			int msgType = msg.arg1;
			if (msgType == MSG_UPDATE_UI) {
				updateUIServiceRunningStatus();
			}
		}
	};
	
	private TimerTask timerTaskUpdateUI = new TimerTask() {
		@Override
		public void run() {
			Message msg = new Message();
			msg.arg1 = MSG_UPDATE_UI;
			msgHandler.sendMessage(msg);
		}
	};
	
	private ToggleButton flowEngineButton;
	private ToggleButton accelerometerButton;
	private ToggleButton flowmbedButton;
	private ToggleButton locationButton;
	
	private boolean isFlowEngineServiceRunning() {
		ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
			if (FlowEngineMainServiceName.equals(service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}

	private boolean isAccelerometerServiceRunning() {
		ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
			if (AbstractDeviceAccelerometerServiceName.equals(service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}

	private boolean isFlowMBedEngineServiceRunning() {
		ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
			if (FlowMBedEngineServiceName.equals(service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.buttons);
		
		flowEngineButton = (ToggleButton) findViewById(R.id.toggle_flowengine);
		accelerometerButton = (ToggleButton) findViewById(R.id.toggle_accelerometer);
		flowmbedButton = (ToggleButton) findViewById(R.id.toggle_flowmbed);
		locationButton = (ToggleButton) findViewById(R.id.toggle_location);
		
		flowEngineButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				if (flowEngineButton.isChecked()) {
					startService(new Intent(FlowEngineMainServiceName));
				} else {
					stopService(new Intent(FlowEngineMainServiceName));
				}
				updateUIServiceRunningStatus();
			}
		});

		accelerometerButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				if (accelerometerButton.isChecked()) {
					startService(new Intent(AbstractDeviceAccelerometerServiceName));
				} else {
					stopService(new Intent(AbstractDeviceAccelerometerServiceName));
				}
				updateUIServiceRunningStatus();
			}
		});

		flowmbedButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				if (flowmbedButton.isChecked()) {
					startService(new Intent(FlowMBedEngineServiceName));
				} else {
					stopService(new Intent(FlowMBedEngineServiceName));
				}
				updateUIServiceRunningStatus();
			}
		});

		locationButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				if (locationButton.isChecked()) {
					
				} else {
					
				}
				updateUIServiceRunningStatus();
			}
		});

		// Start FlowEngine in case it hasn't been start at boot-up time.
		startService(new Intent(FlowEngineMainServiceName));
	
		// Timer task for checking service status and updating UI.
		timer = new Timer("UpdateUI");
	    timer.schedule(timerTaskUpdateUI, 1000L, 1 * 1000L);
		
		Log.i(TAG, "Activity created.");
	}

	private void updateUIServiceRunningStatus() {
		// Check service running status and update UI.
		if (isFlowEngineServiceRunning()) {
			flowEngineButton.setChecked(true);
		} else {
			flowEngineButton.setChecked(false);
		}
		
		if (isAccelerometerServiceRunning()) {
			accelerometerButton.setChecked(true);
		} else {
			accelerometerButton.setChecked(false);
		}

		if (isFlowMBedEngineServiceRunning()) {
			flowmbedButton.setChecked(true);
		} else {
			flowmbedButton.setChecked(false);
		}
}
	
	@Override
	protected void onResume() {
		super.onResume();
		updateUIServiceRunningStatus();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		Log.i(TAG, "FlowEngineControl destroyed.");
	}
}
