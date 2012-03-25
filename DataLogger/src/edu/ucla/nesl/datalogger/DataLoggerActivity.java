package edu.ucla.nesl.datalogger;

import java.text.DateFormat;
import java.util.Date;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.widget.TextView;
import edu.ucla.nesl.flowengine.SensorType;
import edu.ucla.nesl.flowengine.aidl.ApplicationInterface;
import edu.ucla.nesl.flowengine.aidl.FlowEngineAppAPI;

public class DataLoggerActivity extends Activity {
	private static final String TAG = DataLoggerActivity.class.getSimpleName();
	
	private static final String FLOW_ENGINE_SERVICE = "edu.ucla.nesl.flowengine.FlowEngine.application";
	
	private static final String BUNDLE_NAME = "name";
	private static final String BUNDLE_TYPE = "type";
	private static final String BUNDLE_DATA = "data";
	private static final String BUNDLE_LENGTH = "length";
	private static final String BUNDLE_TIMESTAMP = "timestamp";

	private static final int MSG_PUBLISH = 1;

	private TextView mStressText;
	private TextView mConversationText;
	private TextView mActivityText;
	private TextView mBatteryText;
	private TextView mZephyrBatteryText;
	private TextView mGPSText;
	
	private FlowEngineAppAPI mAPI;
	private int mAppID;

	private ApplicationInterface.Stub mAppInterface = new ApplicationInterface.Stub() {
		@Override
		public void publishString(String name, String data, long timestamp) throws RemoteException {
			Bundle bundle = new Bundle();
			bundle.putString(BUNDLE_NAME, name);
			bundle.putString(BUNDLE_TYPE, "String");
			bundle.putString(BUNDLE_DATA, data);
			bundle.putInt(BUNDLE_LENGTH, 1);
			bundle.putLong(BUNDLE_TIMESTAMP, timestamp);
			mHandler.sendMessage(mHandler.obtainMessage(MSG_PUBLISH, bundle));
		}

		@Override
		public void publishDouble(String name, double data, long timestamp)
				throws RemoteException {
			Bundle bundle = new Bundle();
			bundle.putString(BUNDLE_NAME, name);
			bundle.putString(BUNDLE_TYPE, "double");
			bundle.putDouble(BUNDLE_DATA, data);
			bundle.putInt(BUNDLE_LENGTH, 1);
			bundle.putLong(BUNDLE_TIMESTAMP, timestamp);
			mHandler.sendMessage(mHandler.obtainMessage(MSG_PUBLISH, bundle));
		}

		@Override
		public void publishDoubleArray(String name, double[] data, int length,
				long timestamp) throws RemoteException {
			Bundle bundle = new Bundle();
			bundle.putString(BUNDLE_NAME, name);
			bundle.putString(BUNDLE_TYPE, "double[]");
			bundle.putDoubleArray(BUNDLE_DATA, data);
			bundle.putInt(BUNDLE_LENGTH, length);
			bundle.putLong(BUNDLE_TIMESTAMP, timestamp);
			mHandler.sendMessage(mHandler.obtainMessage(MSG_PUBLISH, bundle));
		}

		@Override
		public void publishInt(String name, int data, long timestamp)
				throws RemoteException {
			Bundle bundle = new Bundle();
			bundle.putString(BUNDLE_NAME, name);
			bundle.putString(BUNDLE_TYPE, "int");
			bundle.putInt(BUNDLE_DATA, data);
			bundle.putInt(BUNDLE_LENGTH, 1);
			bundle.putLong(BUNDLE_TIMESTAMP, timestamp);
			mHandler.sendMessage(mHandler.obtainMessage(MSG_PUBLISH, bundle));
		}

		@Override
		public void publishIntArray(String name, int[] data, int length,
				long timestamp) throws RemoteException {
			Bundle bundle = new Bundle();
			bundle.putString(BUNDLE_NAME, name);
			bundle.putString(BUNDLE_TYPE, "int[]");
			bundle.putIntArray(BUNDLE_DATA, data);
			bundle.putInt(BUNDLE_LENGTH, length);
			bundle.putLong(BUNDLE_TIMESTAMP, timestamp);
			mHandler.sendMessage(mHandler.obtainMessage(MSG_PUBLISH, bundle));
		}
	};
	
	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_PUBLISH:
				Bundle bundle = (Bundle)msg.obj;
				String name = bundle.getString(BUNDLE_NAME);
				long timestamp = bundle.getLong(BUNDLE_TIMESTAMP);
				int length = bundle.getInt(BUNDLE_LENGTH);
				String type = bundle.getString(BUNDLE_TYPE);
				
				Date date = new Date(timestamp);
				String localDate = DateFormat.getDateInstance().format(date);
				String localTime = DateFormat.getTimeInstance().format(date);
				String lastUpdateString = "\nLast Updated: " + localDate + ' ' + localTime;

				if (type.equals("String")) {
					String data = bundle.getString(BUNDLE_DATA);
					String str = data + lastUpdateString;
					if (name.equals(SensorType.PHONE_BATTERY_NAME)) {
						mBatteryText.setText(str);
					} else if (name.equals(SensorType.ACTIVITY_CONTEXT_NAME)) {
						mActivityText.setText(str);
					} else if (name.equals(SensorType.STRESS_CONTEXT_NAME)) {
						mStressText.setText(str);
					} else if (name.equals(SensorType.CONVERSATION_CONTEXT_NAME)) {
						mConversationText.setText(str);
					}
				} else if (type.equals("double[]")) {
					double[] data = bundle.getDoubleArray(BUNDLE_DATA);
					if (name.equals(SensorType.PHONE_GPS_NAME)) {
						String str = "Lat: " + data[0] + ", Lng: " + data[1] + ", Alt: " + data[2] + ", Speed: " + data[3];
						mGPSText.setText(str + lastUpdateString);						
					}
				} else if (type.equals("int")) {
					int data = bundle.getInt(BUNDLE_DATA);
					if (name.equals(SensorType.ZEPHYR_BATTERY_NAME)) {
						mZephyrBatteryText.setText(data + lastUpdateString);
					}
				}
				break;
			}
			super.handleMessage(msg);
		}
	};
	
	private ServiceConnection mServiceConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			Log.i(TAG, "Service connection established.");
			mAPI = FlowEngineAppAPI.Stub.asInterface(service);
			try {
				mAppID = mAPI.register(mAppInterface);
				mAPI.subscribe(mAppID, SensorType.ACTIVITY_CONTEXT_NAME);
				mAPI.subscribe(mAppID, SensorType.STRESS_CONTEXT_NAME);
				mAPI.subscribe(mAppID, SensorType.CONVERSATION_CONTEXT_NAME);
				mAPI.subscribe(mAppID, SensorType.PHONE_BATTERY_NAME);
				mAPI.subscribe(mAppID, SensorType.PHONE_GPS_NAME);
				mAPI.subscribe(mAppID, SensorType.ZEPHYR_BATTERY_NAME);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			Log.i(TAG, "Service connection closed.");
		}
	};

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        mStressText = (TextView)findViewById(R.id.stress_text);
        mConversationText = (TextView)findViewById(R.id.conversation_text);
        mActivityText = (TextView)findViewById(R.id.activity_text);
        mBatteryText = (TextView)findViewById(R.id.battery_text);
        mZephyrBatteryText = (TextView)findViewById(R.id.zephyr_battery_text);
        mGPSText = (TextView)findViewById(R.id.gps_text);
        
        // Start FlowEngine service if it's not running.
		Intent intent = new Intent(FLOW_ENGINE_SERVICE);
		startService(intent);
		
		// Bind to the FlowEngine service.
		bindService(intent, mServiceConnection, 0);
    }

	@Override
	protected void onDestroy() {
		if (mAPI != null) {
			try {
				mAPI.unregister(mAppID);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
			unbindService(mServiceConnection);
		}
		
		super.onDestroy();
	}
}