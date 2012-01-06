package edu.ucla.nesl.flowengine.abstractdevice.accelerometer;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Debug;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import edu.ucla.nesl.flowengine.aidl.AbstractDeviceInterface;
import edu.ucla.nesl.flowengine.aidl.FlowEngineDeviceAPI;
import edu.ucla.nesl.flowengine.aidl.WaveSegment;

public class AccelerometerService extends Service implements SensorEventListener {

	private static final String TAG = AccelerometerService.class.getSimpleName();
	private static final String FlowEngineServiceName = "edu.ucla.nesl.flowengine.FlowEngine";

	private static final String[] mFormat = { "AccelerometerX", "AccelerometerY", "AccelerometerZ" };
	
	private FlowEngineDeviceAPI 	mAPI;
	private Handler 				mHandler;
	private AccelerometerService 	mThisService = this;

	private SensorManager 			mSensorManager;
	private Sensor 					mAccelerometer;
	
	private ServiceConnection 		mServiceConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			Log.i(TAG, "Service connection established.");
			mAPI = FlowEngineDeviceAPI.Stub.asInterface(service);
			try {
				mAPI.addAbstractDevice(mAccelerometerDeviceInterface);
			} catch (RemoteException e) {
				Log.e(TAG, "Failed to add AbstractDevice..", e);
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			Log.i(TAG, "Service connection closed.");
		}
	};
	
	private AbstractDeviceInterface.Stub mAccelerometerDeviceInterface = new AbstractDeviceInterface.Stub() {
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
		Debug.startMethodTracing("AccelerometerService");
		Debug.startAllocCounting();
		
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
		Debug.stopAllocCounting();
		Debug.stopMethodTracing();
		
		Log.i(TAG, "Service destroying");
		
		mSensorManager.unregisterListener(this);
		
		try {
			mAPI.removeAbstractDevice(mAccelerometerDeviceInterface);
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
	    
	    // Wrap this event into WaveSegment.
	    //float x = event.values[0];
	    //float y = event.values[1];
	    //float z = event.values[2];
	    //long timestamp = event.timestamp; // in nano
	    //long sysTimestamp = System.nanoTime(); // in nano
	    /*WaveSegment ws = new WaveSegment();
	    ws.name = "InternalAccelerometer";
	    ws.timestamp = event.timestamp;
	    ws.interval = 0L;*/
	    //ws.format = mFormat;
	    /*ws.data = new double[3];	    
	    for (int i = 0; i < 3; i++) {
	    	ws.data[i] = event.values[i];
	    }*/

	    WaveSegment ws = new WaveSegment("Internalaccelerometer", event.timestamp, 0, 0.0, 0.0, mFormat, event.values);
	    
		// Push this WaveSegment.
		try {
			mAPI.pushWaveSegment(ws);
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
