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

public class FlowEngineUI extends Activity {

	private static final String TAG = FlowEngineUI.class.getSimpleName();
	
	private static final String FLOW_ENGINE_SERVICE = "edu.ucla.nesl.flowengine.FlowEngine";
	private static final String PHONE_SENSOR_DEVICE_SERVICE = "edu.ucla.nesl.flowengine.device.PhoneSensorDeviceService";
	private static final String MBED_DEVICE_SERVICE = "edu.ucla.nesl.flowengine.device.MBedDeviceService";
	private static final String ZEPHYR_DEVICE_SERVICE = "edu.ucla.nesl.flowengine.device.ZephyrDeviceService";
	
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
	private ToggleButton mbedButton;
	private ToggleButton zephyrButton;
	
	private boolean isFlowEngineServiceRunning() {
		ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
			if (FLOW_ENGINE_SERVICE.equals(service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}

	private boolean isAccelerometerServiceRunning() {
		ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
			if (PHONE_SENSOR_DEVICE_SERVICE.equals(service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}

	private boolean isFlowMBedEngineServiceRunning() {
		ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
			if (MBED_DEVICE_SERVICE.equals(service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}

	private boolean isZephyrServiceRunning() {
		ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
			if (ZEPHYR_DEVICE_SERVICE.equals(service.service.getClassName())) {
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
		accelerometerButton = (ToggleButton) findViewById(R.id.toggle_phone);
		mbedButton = (ToggleButton) findViewById(R.id.toggle_mbed);
		zephyrButton = (ToggleButton) findViewById(R.id.toggle_zephyr);
		
		flowEngineButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				if (flowEngineButton.isChecked()) {
					startService(new Intent(FLOW_ENGINE_SERVICE));
				} else {
					stopService(new Intent(FLOW_ENGINE_SERVICE));
				}
				updateUIServiceRunningStatus();
			}
		});

		accelerometerButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				if (accelerometerButton.isChecked()) {
					startService(new Intent(PHONE_SENSOR_DEVICE_SERVICE));
				} else {
					stopService(new Intent(PHONE_SENSOR_DEVICE_SERVICE));
				}
				updateUIServiceRunningStatus();
			}
		});

		mbedButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				if (mbedButton.isChecked()) {
					startService(new Intent(MBED_DEVICE_SERVICE));
				} else {
					stopService(new Intent(MBED_DEVICE_SERVICE));
				}
				updateUIServiceRunningStatus();
			}
		});

		zephyrButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				if (zephyrButton.isChecked()) {
					startService(new Intent(ZEPHYR_DEVICE_SERVICE));
				} else {
					stopService(new Intent(ZEPHYR_DEVICE_SERVICE));
				}
				updateUIServiceRunningStatus();
			}
		});

		// Start FlowEngine in case it hasn't been start at boot-up time.
		//startService(new Intent(FlowEngineMainServiceName));
	
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
			mbedButton.setChecked(true);
		} else {
			mbedButton.setChecked(false);
		}
		
		if (isZephyrServiceRunning()) {
			zephyrButton.setChecked(true);
		} else {
			zephyrButton.setChecked(false);
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
