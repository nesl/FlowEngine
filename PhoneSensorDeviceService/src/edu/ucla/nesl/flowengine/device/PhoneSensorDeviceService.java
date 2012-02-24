package edu.ucla.nesl.flowengine.device;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import edu.ucla.nesl.flowengine.SensorType;
import edu.ucla.nesl.flowengine.aidl.DeviceAPI;
import edu.ucla.nesl.flowengine.aidl.FlowEngineAPI;

public class PhoneSensorDeviceService extends Service implements SensorEventListener, LocationListener {
	private static final String TAG = PhoneSensorDeviceService.class.getSimpleName();
	
	private static final String FlowEngineServiceName = "edu.ucla.nesl.flowengine.FlowEngine";
	
	private static final int GPS_INTERVAL = 1000; // ms
	private static final int GPS_LOCATION_INTERVAL = 0; // meters
	
	private static final int MSG_KILL = 0;
	private static final int MSG_START = 1;
	private static final int MSG_STOP = 2;
	private static final int MSG_START_SENSOR = 3;
	private static final int MSG_STOP_SENSOR = 4;
	
	private FlowEngineAPI 	mAPI;
	private PhoneSensorDeviceService mThisService = this;

	private SensorManager mSensorManager;
	private Sensor mAccelerometer;
	private LocationManager mLocationManager;

	private int	mDeviceID;
	
	private ServiceConnection mServiceConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			Log.i(TAG, "Service connection established.");
			mAPI = FlowEngineAPI.Stub.asInterface(service);
			try {
				mDeviceID = mAPI.addDevice(mAccelerometerDeviceInterface);
				mAPI.addSensor(mDeviceID, SensorType.PHONE_ACCELEROMETER, -1);
				mAPI.addSensor(mDeviceID, SensorType.PHONE_GPS, -1);
			} catch (RemoteException e) {
				Log.e(TAG, "Failed to add device..", e);
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			Log.i(TAG, "Service connection closed.");
		}
	};
	
	private DeviceAPI.Stub mAccelerometerDeviceInterface = new DeviceAPI.Stub() {
		@Override
		public void start() throws RemoteException {
			mHandler.sendMessage(mHandler.obtainMessage(MSG_START));
		}
		@Override
		public void stop() throws RemoteException {
			mHandler.sendMessage(mHandler.obtainMessage(MSG_STOP));
		}
		@Override
		public void startSensor(int sensor) throws RemoteException {
			mHandler.sendMessage(mHandler.obtainMessage(MSG_START_SENSOR, (Object)sensor));
		}
		@Override
		public void stopSensor(int sensor) throws RemoteException {
			mHandler.sendMessage(mHandler.obtainMessage(MSG_STOP_SENSOR, (Object)sensor));
		}
		@Override
		public void kill() throws RemoteException {
			mHandler.sendMessage(mHandler.obtainMessage(MSG_KILL));
		}
	};
	
	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			int sensor;
			switch (msg.what) {
				case MSG_KILL:
					mThisService.stopSelf();
					break;
				case MSG_START:
					startAccelerometer();
					startGPS();
					break;
				case MSG_STOP:
					stopAccelerometer();
					stopGPS();
					break;
				case MSG_START_SENSOR:
					sensor = (Integer)msg.obj;
					if (sensor == SensorType.PHONE_ACCELEROMETER) {
						startAccelerometer();
					} else if (sensor == SensorType.PHONE_GPS) {
						startGPS();
					}
					break;
				case MSG_STOP_SENSOR:
					sensor = (Integer)msg.obj;
					if (sensor == SensorType.PHONE_ACCELEROMETER) {
						stopAccelerometer();
					} else if (sensor == SensorType.PHONE_GPS) {
						stopGPS();
					}
					break;
				default:
					super.handleMessage(msg);
			}
		}
	};

	private void startAccelerometer() {
		mSensorManager.registerListener(mThisService, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
	}
	
	private void startGPS() {
		mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, GPS_INTERVAL, GPS_LOCATION_INTERVAL, mThisService);
	}
	
	private void stopAccelerometer() {
		mSensorManager.unregisterListener(mThisService, mAccelerometer);
	}
	
	private void stopGPS() {
		mLocationManager.removeUpdates(mThisService);
	}
	
	@Override
	public IBinder onBind(Intent intent) {		
		return null;
	}

	@Override
	public void onCreate() {
		//Debug.startMethodTracing("AccelerometerService");
		//Debug.startAllocCounting();
		
		super.onCreate();
		Log.i(TAG, "Service creating");
		
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // Start FlowEngine service if it's not running.
		Intent intent = new Intent(FlowEngineServiceName);
		startService(intent);
		
		// Bind to the FlowEngine service.
		bindService(intent, mServiceConnection, 0);
	}
	
	@Override
	public void onDestroy() {
		//Debug.stopAllocCounting();
		//Debug.stopMethodTracing();
		
		Log.i(TAG, "Service destroying");
		
		stopAccelerometer();
		stopGPS();

		try {
			mAPI.removeDevice(mDeviceID);
			unbindService(mServiceConnection);
		} catch (Throwable t) {
			Log.w(TAG, "Failed to unbind from the service", t);
		}

		super.onDestroy();
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
	    if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER)
            return;
	    
	    double[] data = new double[3];
	    data[0] = event.values[0];
	    data[1] = event.values[1];
	    data[2] = event.values[2];
	    
		try {
			mAPI.pushDoubleArrayData(mDeviceID, SensorType.PHONE_ACCELEROMETER, data, data.length, event.timestamp/1000);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onLocationChanged(Location location) {
		long timestamp = System.currentTimeMillis(); 
		double[] data = new double[4];
		data[0] = location.getLatitude();
		data[1] = location.getLongitude();
		data[2] = location.getAltitude();
		data[3] = location.getSpeed();
		try {
			mAPI.pushDoubleArrayData(mDeviceID, SensorType.PHONE_GPS, data, data.length, timestamp);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

	@Override
	public void onProviderDisabled(String provider) {
	}

	@Override
	public void onProviderEnabled(String provider) {
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
	}
}
