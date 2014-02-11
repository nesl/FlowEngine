package edu.ucla.nesl.datacollector.activity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import android.app.Activity;
import android.app.Dialog;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import edu.ucla.nesl.datacollector.Const;
import edu.ucla.nesl.datacollector.R;
import edu.ucla.nesl.datacollector.service.DataService;
import edu.ucla.nesl.datacollector.tools.Tools;
import edu.ucla.nesl.datacollector.ui.SetupUserDialog;
import edu.ucla.nesl.datacollector.ui.SetupUserDialog.OnFinishListener;

public class MainActivity extends Activity {

	private static final String BLUETOOTH_DEVICE_SELECTED = "android.bluetooth.devicepicker.action.DEVICE_SELECTED";
	private static final String BLUETOOTH_DEVICE_PICKER_LAUNCH = "android.bluetooth.devicepicker.action.LAUNCH";
	private static final String EXTRA_NEED_AUTH = "android.bluetooth.devicepicker.extra.NEED_AUTH";
	private static final String EXTRA_FILTER_TYPE = "android.bluetooth.devicepicker.extra.FILTER_TYPE";
	private static final int FILTER_TYPE_ALL = 0;
	private static final int DIALOG_USER_SETUP = 1;

	private static final int REQUEST_CODE_NORMAL = 1;
	private static final int REQUEST_CODE_ZEPHYR_BLUETOOTH_PICK = 2;
	private static final int REQUEST_CODE_METAWATCH_BLUETOOTH_PICK = 3;

	private int curRequestCode = 0;
	private String zephyrAddr = null;
	private String metawatchAddr = null;

	private int setupMode = 0;

	private Context context = this;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		Intent intent = getIntent();
		Bundle bundle = intent.getExtras();

		if (bundle != null) {
			setupMode = bundle.getInt(Const.SETUP_MODE, 0);
		}

		if (setupMode == Const.SETUP_USER) {
			showDialog(DIALOG_USER_SETUP);			
		} else if (setupMode == Const.SETUP_BLUETOOTH) {
			startBluetoothSetup();
		} else {
			SharedPreferences settings = getSharedPreferences(Const.PREFS_NAME, 0);
			boolean isFirst = settings.getBoolean(Const.PREFS_IS_FIRST, true);

			if (isFirst) {
				Tools.showAlertDialog(this, "Welcome", "Welcome to Data Collector! You've launched Data Collector for the first time, so let's go through inital setup process.", welcomeListener);
			} else {
				// Start some services
				startService(new Intent(this, DataService.class));
				
				// Start login activity
				intent = new Intent(this, LoginActivity.class);
				startActivityForResult(intent, REQUEST_CODE_NORMAL);			
			}
		}
	}

	private OnClickListener welcomeListener = new OnClickListener() {
		@Override
		public void onClick(DialogInterface dialog, int which) {
			dialog.dismiss();
			showDialog(DIALOG_USER_SETUP);
		}
	};

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_USER_SETUP:
			return new SetupUserDialog(this, onUserSetupFinishListener);
		}
		return null;
	}

	private OnFinishListener onUserSetupFinishListener = new OnFinishListener() {
		@Override
		public void onFinish() {
			if (setupMode == Const.SETUP_USER) {
				finish();
			} else {
				startBluetoothSetup();
			}
		}
	};

	private void startBluetoothSetup() {
		registerReceiver(receiver, new IntentFilter(BLUETOOTH_DEVICE_SELECTED));

		Tools.showAlertDialog(this, "Choose chest-band", "In the following screen, please choose Zephyr chest-band bluetooth device.", new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				zephyrAddr = null;
				curRequestCode = REQUEST_CODE_ZEPHYR_BLUETOOTH_PICK;
				launchBluetoothPicker(REQUEST_CODE_ZEPHYR_BLUETOOTH_PICK);
			}
		});
	}

	private void launchBluetoothPicker(int requestCode) {
		Intent intent = new Intent(BLUETOOTH_DEVICE_PICKER_LAUNCH);
		intent.putExtra(EXTRA_NEED_AUTH, false);
		intent.putExtra(EXTRA_FILTER_TYPE, FILTER_TYPE_ALL);
		intent.setFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
		startActivityForResult(intent, requestCode);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case REQUEST_CODE_NORMAL:
			finish();
			break;
		case REQUEST_CODE_ZEPHYR_BLUETOOTH_PICK:
			Log.d(Const.TAG, "Zephyr: " + zephyrAddr);
			Tools.showAlertDialog(this, "Choose Metawatch", "In the following screen, please choose Metawatch bluetooth device.", new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
					metawatchAddr = null;
					curRequestCode = REQUEST_CODE_METAWATCH_BLUETOOTH_PICK;
					launchBluetoothPicker(REQUEST_CODE_METAWATCH_BLUETOOTH_PICK);
				}
			});
			break;
		case REQUEST_CODE_METAWATCH_BLUETOOTH_PICK:
			Log.d(Const.TAG, "Metawatch: " + metawatchAddr);
			finishBluetoothSetup();
			break;
		}
	}

	private void finishBluetoothSetup() {
		unregisterReceiver(receiver);

		storeBluetoothAddresses();

		if (setupMode == Const.SETUP_BLUETOOTH) {
			finish();
		} else {
			Tools.showAlertDialog(context, "Congratulations!", "Now you're ready to use Data Collector. Please login in the following screen."
					, new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();

					SharedPreferences settings = context.getSharedPreferences(Const.PREFS_NAME, 0);
					SharedPreferences.Editor editor = settings.edit();
					editor.putBoolean(Const.PREFS_IS_FIRST, false);
					editor.commit();

					Intent intent = new Intent(context, LoginActivity.class);
					startActivityForResult(intent, REQUEST_CODE_NORMAL);
				}
			});
		}
	}

	private void storeBluetoothAddresses() {
		try {
			String path = Environment.getExternalStorageDirectory().getPath() + "/" + Const.PROPERTY_FILE_NAME;

			Log.d(Const.TAG, path);

			File file = new File(path);
			if (!file.exists()) {
				file.createNewFile();
			}

			FileInputStream fin = new FileInputStream(file);

			Properties prop = new Properties();

			prop.load(fin);

			Log.d(Const.TAG, prop.get(Const.PROP_NAME_ZEPHYR_ADDR) + ", " + prop.get(Const.PROP_NAME_METAWATCH_ADDR));

			if (zephyrAddr != null) {
				prop.put(Const.PROP_NAME_ZEPHYR_ADDR, zephyrAddr);
			} 
			if (metawatchAddr != null) {
				prop.put(Const.PROP_NAME_METAWATCH_ADDR, metawatchAddr);
			}

			FileOutputStream fout = new FileOutputStream(file);
			prop.store(fout, "Created by DataCollector.");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	private BroadcastReceiver receiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();

			if(BLUETOOTH_DEVICE_SELECTED.equals(action)) {
				BluetoothDevice device = (BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

				if (curRequestCode == REQUEST_CODE_ZEPHYR_BLUETOOTH_PICK) {
					zephyrAddr = device.getAddress();
				} else if (curRequestCode == REQUEST_CODE_METAWATCH_BLUETOOTH_PICK) {
					metawatchAddr = device.getAddress();
				}
			}		
		}
	};
}
