package edu.ucla.nesl.datacollector.activity;

import java.text.DateFormat;
import java.util.Date;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import edu.ucla.nesl.datacollector.Const;
import edu.ucla.nesl.datacollector.R;
import edu.ucla.nesl.datacollector.service.DataService;
import edu.ucla.nesl.flowengine.SensorType;

public class TabStatusActivity extends Activity {

	private TextView mGPSText;
	private TextView mAccText;
	private TextView mECGText;
	private TextView mRIPText;
	private TextView mActivityText;
	private TextView mStressText;
	private TextView mConversationText;
	private TextView mBatteryText;
	private TextView mZephyrBatteryText;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.tab_status_layout);

		mAccText = (TextView)findViewById(R.id.accelerometer_text);
		mECGText = (TextView)findViewById(R.id.ecg_text);
		mRIPText = (TextView)findViewById(R.id.respiration_text);
		mStressText = (TextView)findViewById(R.id.stress_text);
		mConversationText = (TextView)findViewById(R.id.conversation_text);
		mActivityText = (TextView)findViewById(R.id.activity_text);
		mBatteryText = (TextView)findViewById(R.id.battery_text);
		mZephyrBatteryText = (TextView)findViewById(R.id.zephyr_battery_text);
		mGPSText = (TextView)findViewById(R.id.gps_text);
	}

	@Override
	protected void onResume() {
		registerReceiver(receiver, new IntentFilter(DataService.BROADCAST_INTENT_MESSAGE));

		Intent intent = new Intent(this, DataService.class);
		intent.putExtra(DataService.REQUEST_TYPE, DataService.START_BROADCAST_SENSOR_DATA);
		startService(intent);

		super.onResume();
	}

	@Override
	protected void onPause() {
		Intent intent = new Intent(this, DataService.class);
		intent.putExtra(DataService.REQUEST_TYPE, DataService.STOP_BROADCAST_SENSOR_DATA);
		startService(intent);

		unregisterReceiver(receiver);

		super.onPause();
	}

	private String getArrayAsString(double[] data) {
		String str = "{ ";
		for (double v : data) {
			str += v + ", ";
		}
		str = str.substring(0, str.length() - 2) + " }";
		return str;
	}

	private BroadcastReceiver receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Bundle bundle = intent.getExtras();
			String name = bundle.getString(DataService.BUNDLE_NAME);
			long timestamp = bundle.getLong(DataService.BUNDLE_TIMESTAMP);
			//int length = bundle.getInt(DataService.BUNDLE_LENGTH);
			String type = bundle.getString(DataService.BUNDLE_TYPE);

			if (name != null) {
				Date date = new Date(timestamp);
				String localDate = DateFormat.getDateInstance().format(date);
				String localTime = DateFormat.getTimeInstance().format(date);
				String lastUpdateString = "\nLast Updated: " + localDate + ' ' + localTime;

				if (name.equals(SensorType.PHONE_GPS_NAME)) {
					String str = getArrayAsString(bundle.getDoubleArray(DataService.BUNDLE_DATA));
					mGPSText.setText(str + lastUpdateString);
				} else if (name.equals(SensorType.PHONE_ACCELEROMETER_NAME)) {
					String str = getArrayAsString(bundle.getDoubleArray(DataService.BUNDLE_DATA));
					mAccText.setText(str + lastUpdateString);
				} else if (name.equals(SensorType.ECG_NAME)) {
					String str = bundle.getIntArray(DataService.BUNDLE_DATA).length + " samples";
					mECGText.setText(str + lastUpdateString);
				} else if (name.equals(SensorType.RIP_NAME)) {
					String str = bundle.getIntArray(DataService.BUNDLE_DATA).length + " samples";
					mRIPText.setText(str + lastUpdateString);
				} else if (name.equals(SensorType.ACTIVITY_CONTEXT_NAME)) {
					mActivityText.setText(bundle.getString(DataService.BUNDLE_DATA) + lastUpdateString);
				} else if (name.equals(SensorType.STRESS_CONTEXT_NAME)) {
					mStressText.setText(bundle.getString(DataService.BUNDLE_DATA) + lastUpdateString);
				} else if (name.equals(SensorType.CONVERSATION_CONTEXT_NAME)) {
					mConversationText.setText(bundle.getString(DataService.BUNDLE_DATA) + lastUpdateString);
				} else if (name.equals(SensorType.PHONE_BATTERY_NAME)) {
					mBatteryText.setText(bundle.getString(DataService.BUNDLE_DATA) + lastUpdateString);				
				} else if (name.equals(SensorType.ZEPHYR_BATTERY_NAME)) {
					mZephyrBatteryText.setText(bundle.getInt(DataService.BUNDLE_DATA) + lastUpdateString);
				} else {
					Log.d(Const.TAG, "Unknown type: " + type);
				}
			}
		}
	};
}
