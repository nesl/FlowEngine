package edu.ucla.nesl.datacollector.activity;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import edu.ucla.nesl.datacollector.Device;
import edu.ucla.nesl.datacollector.R;

public class TabDevicesActivity extends Activity {

	private static final String[] deviceServiceNames = new String[] {
		"edu.ucla.nesl.flowengine.device.PhoneSensorDeviceService",
		"edu.ucla.nesl.flowengine.device.ZephyrDeviceService",
		"edu.ucla.nesl.flowengine.device.MetawatchDeviceService"
	};
	private static final int MSG_UPDATE_UI = 0xAB;

	private DeviceItemsAdapter deviceItemsAdapter;
	private ListView deviceListView;
	private List<Device> devices;

	private Timer timer;
	private TimerTask timerTask;
	
	private Handler msgHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			int msgType = msg.arg1;
			if (msgType == MSG_UPDATE_UI) {
				updateUIServiceRunningStatus();
			}
		}
	};

	private class TimerTaskUpdateUI extends TimerTask {
		@Override
		public void run() {
			Message msg = new Message();
			msg.arg1 = MSG_UPDATE_UI;
			msgHandler.sendMessage(msg);
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.tab_devices_layout);

		deviceListView = (ListView) findViewById(R.id.listview_devices);
		deviceItemsAdapter = new DeviceItemsAdapter();
		deviceListView.setAdapter(deviceItemsAdapter);
		deviceListView.setOnItemClickListener(deviceItemOnClickListener);
	}

	@Override
	protected void onResume() {
		devices = new ArrayList<Device>();

		for (String serviceName : deviceServiceNames) {
			devices.add(new Device(serviceName, getSimpleServiceName(serviceName), isServiceRunning(serviceName)));
		}

		deviceItemsAdapter.notifyDataSetChanged();

		// Timer task for checking service status and updating UI.
		timer = new Timer("UpdateUI");
		timerTask = new TimerTaskUpdateUI();
	    timer.schedule(timerTask, 1000L, 1 * 1000L);
		
		super.onResume();
	}

	@Override
	protected void onPause() {
		timer.cancel();
		super.onPause();
	}

	private String getSimpleServiceName(String serviceName) {
		String split[] = serviceName.split("\\.");
		return split[split.length - 1].replace("DeviceService", "");
	}

	private static class DeviceItemViewHolder {
		public TextView deviceName;
		public CheckBox checkBox;
	}

	private class DeviceItemsAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			if (devices != null) {
				return devices.size();
			} else {
				return 0;
			}
		}

		@Override
		public Object getItem(int position) {
			if (devices != null) {
				return devices.get(position);
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
			if (devices != null) {

				DeviceItemViewHolder holder = null;

				if (convertView == null) {
					convertView = getLayoutInflater().inflate(R.layout.listitem_device, parent, false);
					holder = new DeviceItemViewHolder();
					holder.deviceName = (TextView) convertView.findViewById(R.id.device_name);
					holder.checkBox = (CheckBox) convertView.findViewById(R.id.check_box);
					convertView.setTag(holder);
				} else {
					holder = (DeviceItemViewHolder)convertView.getTag();
				}

				Device device = devices.get(position);
				holder.deviceName.setText(device.getDeviceName());
				holder.checkBox.setChecked(device.isEnabled());
				holder.checkBox.setTag(devices.get(position));
				holder.checkBox.setOnClickListener(onClickCheckboxListener);
			}
			return convertView;
		}
	}

	private OnClickListener onClickCheckboxListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			CheckBox cb = (CheckBox) v;
			Device device = (Device) cb.getTag();

			if (cb.isChecked()) {
				startService(new Intent(device.getServiceName()));
			} else {
				stopService(new Intent(device.getServiceName()));
			}
			
			if (isServiceRunning(device.getServiceName())) {
				cb.setChecked(true);
			} else {
				cb.setChecked(false);
			}
			
			device.setEnabled(cb.isChecked());
		}
	};

	private OnItemClickListener deviceItemOnClickListener = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
		}
	};

	private boolean isServiceRunning(String serviceName) {
		ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
			if (serviceName.equals(service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}
	
	private void updateUIServiceRunningStatus() {
		for (Device device : devices) {
			device.setEnabled(isServiceRunning(device.getServiceName()));
		}
		deviceItemsAdapter.notifyDataSetChanged();
	}
}
