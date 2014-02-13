package edu.ucla.nesl.datacollector.activity;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import edu.ucla.nesl.datacollector.Const;
import edu.ucla.nesl.datacollector.Device;
import edu.ucla.nesl.datacollector.R;
import edu.ucla.nesl.datacollector.service.DataService;
import edu.ucla.nesl.flowengine.SensorType;

public class TabSensorsActivity extends Activity {

	public static final String[] sensorNames = {
		SensorType.PHONE_GPS_NAME,
		SensorType.PHONE_ACCELEROMETER_NAME,
		SensorType.ECG_NAME,
		SensorType.RIP_NAME,
		SensorType.ACTIVITY_CONTEXT_NAME,
		SensorType.STRESS_CONTEXT_NAME,
		SensorType.CONVERSATION_CONTEXT_NAME,
		SensorType.PHONE_BATTERY_NAME,
		SensorType.ZEPHYR_BATTERY_NAME,
		SensorType.ZEPHYR_BUTTON_WORN_NAME
	};
	
	private Context context = this;
	
	private SensorItemsAdapter sensorItemsAdapter;
	private ListView sensorListView;
	private List<Device> sensors;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.tab_sensors_layout);
		
		sensorListView = (ListView) findViewById(R.id.listview_sensors);
		sensorItemsAdapter = new SensorItemsAdapter();
		sensorListView.setAdapter(sensorItemsAdapter);
		sensorListView.setOnItemClickListener(locationLabelItemOnClickListener);
	}
	
	@Override
	protected void onResume() {
		registerReceiver(receiver, new IntentFilter(DataService.BROADCAST_INTENT_MESSAGE));
		
		if (sensors == null) {
			sensors = new ArrayList<Device>();
		} else {
			sensors.clear();
		}
		
		for (String sensorName: sensorNames) {
			sensors.add(new Device(sensorName, false));
		}
		
		sensorItemsAdapter.notifyDataSetChanged();
		
		Intent intent = new Intent(this, DataService.class);
		intent.putExtra(DataService.REQUEST_TYPE, DataService.GET_SUBSCRIBED_SENSORS);
		startService(intent);
		
		super.onResume();
	}

	@Override
	protected void onPause() {
		unregisterReceiver(receiver);
		super.onPause();
	}

	private BroadcastReceiver receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Bundle bundle = intent.getExtras();
			String[] subscribedSensors = bundle.getStringArray(DataService.BUNDLE_SUBSCRIBED_SENSORS);
			if (subscribedSensors != null) {
				for (Device device : sensors) {
					device.setEnabled(false);
				}
				for (String sensor : subscribedSensors) {
					for (Device device : sensors) {
						String deviceName = device.getDeviceName();
						if (sensor.equals(deviceName)) {
							device.setEnabled(true);	
						}
					}
				}
				sensorItemsAdapter.notifyDataSetChanged();
			}
		}
	};

	private static class SensorItemViewHolder {
		public TextView sensorName;
		public CheckBox checkBox;
	}

	private class SensorItemsAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			if (sensors != null) {
				return sensors.size();
			} else {
				return 0;
			}
		}

		@Override
		public Object getItem(int position) {
			if (sensors != null) {
				return sensors.get(position);
			} else {
				return null;				
			}
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (sensors != null) {

				SensorItemViewHolder holder = null;

				if (convertView == null) {
					convertView = getLayoutInflater().inflate(R.layout.listitem_device, parent, false);
					holder = new SensorItemViewHolder();
					holder.sensorName = (TextView) convertView.findViewById(R.id.device_name);
					holder.checkBox = (CheckBox) convertView.findViewById(R.id.check_box);
					convertView.setTag(holder);
				} else {
					holder = (SensorItemViewHolder)convertView.getTag();
				}

				Device device = sensors.get(position);
				holder.sensorName.setText(device.getDeviceName());
				holder.checkBox.setChecked(device.isEnabled());
				holder.checkBox.setTag(sensors.get(position));
				holder.checkBox.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						CheckBox cb = (CheckBox) v;
						Device device = (Device) cb.getTag();
						device.setEnabled(cb.isChecked());
						
						Intent intent = new Intent(getApplicationContext(), DataService.class);
						intent.putExtra(DataService.REQUEST_TYPE, DataService.CHANGE_SUBSCRIPTION);
						intent.putExtra(DataService.EXTRA_SENSOR_NAME, device.getDeviceName());
						intent.putExtra(DataService.EXTRA_IS_ENABLED, device.isEnabled());
						startService(intent);
						
						SharedPreferences settings = context.getSharedPreferences(Const.PREFS_NAME, 0);
						SharedPreferences.Editor editor = settings.edit();
						editor.putBoolean(device.getDeviceName(), device.isEnabled());
						editor.commit();
					}
				});
				
			}
			return convertView;
		}
	}

	private OnItemClickListener locationLabelItemOnClickListener = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
		}
	};


}
