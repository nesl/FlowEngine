package edu.ucla.nesl.datalogger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnManagerPNames;
import org.apache.http.conn.params.ConnPerRouteBean;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.SingleClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Environment;
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

	private DatabaseHandler db = new DatabaseHandler(this);

	Timer mTimer = new Timer();

	class DataUploadTimerTask extends TimerTask {
		private HttpClient mHttpClient;

		private StringBuilder inputStreamToString(InputStream is) throws IOException {
			String line = "";
			StringBuilder total = new StringBuilder();

			// Wrap a BufferedReader around the InputStream
			BufferedReader rd = new BufferedReader(new InputStreamReader(is));

			// Read response until the end
			while ((line = rd.readLine()) != null) { 
				total.append(line); 
			}

			// Return full string
			return total;
		}

		public DataUploadTimerTask() {
			SchemeRegistry schemeRegistry = new SchemeRegistry();
			schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
			schemeRegistry.register(new Scheme("https", new EasySSLSocketFactory(), 443));

			HttpParams params = new BasicHttpParams();
			params.setParameter(ConnManagerPNames.MAX_TOTAL_CONNECTIONS, 30);
			params.setParameter(ConnManagerPNames.MAX_CONNECTIONS_PER_ROUTE, new ConnPerRouteBean(30));
			params.setParameter(HttpProtocolParams.USE_EXPECT_CONTINUE, false);
			HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);

			ClientConnectionManager cm = new SingleClientConnManager(params, schemeRegistry);
			mHttpClient = new DefaultHttpClient(cm, params);
		}

		@Override
		public void run() {
			JSONObject jsonData = new JSONObject();
			try {
				// Make json data
				JSONArray dataChannel = new JSONArray();
				dataChannel.put("timestamp");
				dataChannel.put("activity");
				jsonData.put("data_channel", dataChannel);
				JSONArray data = new JSONArray();
				JSONArray data1 = new JSONArray();
				JSONArray data2 = new JSONArray();
				data1.put(0);
				data1.put("still");
				data2.put(1);
				data2.put("still");
				data.put(data1);
				data.put(data2);
				jsonData.put("data", data);
				Log.d(TAG, jsonData.toString());
			} catch (JSONException e) {
				Log.e(TAG, "JSONException: " + e.getMessage());
				return;
			}


			// Create a new HttpClient and Post Header
			HttpPost httppost = new HttpPost("https://128.97.93.29/upload/");

			// Add your data
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
			nameValuePairs.add(new BasicNameValuePair("apikey", "a4b2a76d89a337f60d669dccdebb1a0fc8c417ca"));
			nameValuePairs.add(new BasicNameValuePair("data", jsonData.toString()));
			try {
				httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
			} catch (UnsupportedEncodingException e) {
				Log.e(TAG, "UnsupportedEncodingException: " + e.getMessage());
				return;
			}

			try {
				// Execute HTTP Post Request
				HttpResponse response = mHttpClient.execute(httppost);
				int status = response.getStatusLine().getStatusCode();
				String reason = response.getStatusLine().getReasonPhrase();
				String reply = inputStreamToString(response.getEntity().getContent()).toString();
				Log.d(TAG, "STATUS " + status + " " + reason);
				Log.d(TAG, reply);
			} catch (ClientProtocolException e) {
				Log.e(TAG, "ClientProtocolException: " + e.getMessage());
			} catch (IOException e) {
				Log.e(TAG, "IOException: " + e.getMessage());
			}
		}
	}

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
						db.insertPhoneBattery(new PhoneBatteryData(timestamp, data));
					} else if (name.equals(SensorType.ACTIVITY_CONTEXT_NAME)) {
						mActivityText.setText(str);
						db.insertActivity(new ActivityData(timestamp, data));
					} else if (name.equals(SensorType.STRESS_CONTEXT_NAME)) {
						mStressText.setText(str);
						db.insertStress(new StressData(timestamp, data));
					} else if (name.equals(SensorType.CONVERSATION_CONTEXT_NAME)) {
						mConversationText.setText(str);
						db.insertConversation(new ConversationData(timestamp, data));
					}
				} else if (type.equals("double[]")) {
					double[] data = bundle.getDoubleArray(BUNDLE_DATA);
					if (name.equals(SensorType.PHONE_GPS_NAME)) {
						String str = "Lat: " + data[0] + ", Lng: " + data[1] + ", Alt: " + data[2] + ", Speed: " + data[3];
						mGPSText.setText(str + lastUpdateString);
						db.insertPhoneGPS(new PhoneGPSData(timestamp, data[0], data[1], data[2], data[3]));
					}
				} else if (type.equals("int")) {
					int data = bundle.getInt(BUNDLE_DATA);
					if (name.equals(SensorType.ZEPHYR_BATTERY_NAME)) {
						mZephyrBatteryText.setText(data + lastUpdateString);
						db.insertZephyrBattery(new ZephyrBatteryData(timestamp, data));
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

		// start upload timer task
		//mTimer.schedule(new DataUploadTimerTask(), 0, 1000);
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
