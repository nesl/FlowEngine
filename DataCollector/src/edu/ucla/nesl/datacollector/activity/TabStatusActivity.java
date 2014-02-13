package edu.ucla.nesl.datacollector.activity;

import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;
import edu.ucla.nesl.datacollector.Const;
import edu.ucla.nesl.datacollector.R;
import edu.ucla.nesl.datacollector.service.DataService;
import edu.ucla.nesl.flowengine.DataType;

public class TabStatusActivity extends Activity {

	private Map<String, TextView> textMap;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.tab_status_layout);

		textMap = new HashMap<String, TextView>();
		
		LinearLayout statusLayout = (LinearLayout)findViewById(R.id.status_layout);
		
		for (String sensor : TabSensorsActivity.sensorNames) {
			TextView sensorText = new TextView(this);
			sensorText.setText("\n" + sensor + ":");
			TextView textView = new TextView(this);
			textView.setText("No data yet.");
			textMap.put(sensor, textView);
			statusLayout.addView(sensorText);
			statusLayout.addView(textView);
		}
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

	private String getArrayAsString(int[] data) {
		String str = "{ ";
		for (int v : data) {
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

				TextView textView = textMap.get(name);
				if (textView != null) {
					if (type.equals(DataType.STRING)) {
						textView.setText(bundle.getString(DataService.BUNDLE_DATA) + lastUpdateString);
					} else if (type.equals(DataType.INTEGER)) {
						textView.setText(bundle.getInt(DataService.BUNDLE_DATA) + lastUpdateString);
					} else if (type.equals(DataType.DOUBLE)) {
						textView.setText(bundle.getDouble(DataService.BUNDLE_DATA) + lastUpdateString);
					} else if (type.equals(DataType.INTEGER_ARRAY)) {
						String str = getArrayAsString(bundle.getIntArray(DataService.BUNDLE_DATA));
						textView.setText(str + lastUpdateString);
					} else if (type.equals(DataType.DOUBLE_ARRAY)) {
						String str = getArrayAsString(bundle.getDoubleArray(DataService.BUNDLE_DATA));
						textView.setText(str + lastUpdateString);
					} else {
						Log.d(Const.TAG, "Unknown type: " + type);
					}
				} else {
					Log.d(Const.TAG, "Unknown name: " + name);
				}
			}
		}
	};
}
