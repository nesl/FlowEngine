package edu.ucla.nesl.flowengine.device;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import edu.ucla.nesl.flowengine.SensorType;
import edu.ucla.nesl.flowengine.aidl.DeviceAPI;
import edu.ucla.nesl.flowengine.aidl.FlowEngineAPI;

public class PhoneSensorDeviceService extends Service implements SensorEventListener {

	private static final String TAG = PhoneSensorDeviceService.class.getSimpleName();
	private static final String FlowEngineServiceName = "edu.ucla.nesl.flowengine.FlowEngine";
	
	private FlowEngineAPI 	mAPI;
	private Handler 				mHandler;
	private PhoneSensorDeviceService 	mThisService = this;

	private SensorManager 			mSensorManager;
	private Sensor 					mAccelerometer;
	
	private int						mDeviceID;
	
	private ServiceConnection 		mServiceConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			Log.i(TAG, "Service connection established.");
			mAPI = FlowEngineAPI.Stub.asInterface(service);
			try {
				mDeviceID = mAPI.addDevice(mAccelerometerDeviceInterface);
				mAPI.addSensor(mDeviceID, SensorType.PHONE_ACCELEROMETER, 0);
				mAPI.addSensor(mDeviceID, SensorType.PHONE_GPS, 0);
				mThisService.mSensorManager.registerListener(mThisService, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
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
			handleStartRequest();
		}
		@Override
		public void stop() throws RemoteException {
			handleStopRequest();
		}
		@Override
		public void kill() throws RemoteException {
			handleKillRequest();
		}
	};
	
	// This handling is needed because this function is called from external thread.
	private void handleStartRequest() {
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				Log.i(TAG, "start()..");
		        mThisService.mSensorManager.registerListener(mThisService, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
			}
		});
	}
	
	private void handleStopRequest() {
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				Log.i(TAG, "stop()..");
				mThisService.mSensorManager.unregisterListener(mThisService);
			}
		});
	}

	private void handleKillRequest() {
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				Log.i(TAG, "kill()..");
				mThisService.stopSelf();
			}
		});
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
		
		mHandler = new Handler();

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

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
		
		mSensorManager.unregisterListener(this);
		
		try {
			mAPI.removeDevice(mDeviceID);
			unbindService(mServiceConnection);
		} catch (Throwable t) {
			Log.w(TAG, "Failed to unbind from the service", t);
		}

		super.onDestroy();
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
	    if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER)
            return;
	    
	    double[] data = new double[3];
	    data[0] = event.values[0];
	    data[1] = event.values[1];
	    data[2] = event.values[2];
	    
		// Push this WaveSegment.
		try {
			//mAPI.pushWaveSegment(ws);
			mAPI.pushDoubleArrayData(mDeviceID, SensorType.PHONE_ACCELEROMETER, data, data.length, event.timestamp);
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
