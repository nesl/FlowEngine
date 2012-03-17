package edu.ucla.nesl.flowengine.device;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Log;

import com.android.future.usb.UsbAccessory;
import com.android.future.usb.UsbManager;

import edu.ucla.nesl.flowengine.SensorType;
import edu.ucla.nesl.flowengine.aidl.DeviceAPI;
import edu.ucla.nesl.flowengine.aidl.FlowEngineAPI;
import edu.ucla.nesl.flowengine.device.mbed.R;
import edu.ucla.nesl.util.NotificationHelper;

public class MBedDeviceService extends Service implements Runnable {

	private static final String TAG = MBedDeviceService.class.getSimpleName();
	private static final String FlowEngineServiceName = "edu.ucla.nesl.flowengine.FlowEngine";
	private NotificationHelper notify;
	
	private static final String[] mFormat = { "X", "Y", "Z" };
	
	private static final String ACTION_USB_PERMISSION = "edu.ucla.nesl.flowengine.mbed.action.USB_PERMISSION";
	private static final int MSG_TYPE_MBED_DATA = 1;
	private static final int MSG_TYPE_TIMER_TASK = 2;
	private static final long mDelay = 1000L;
	private static final long mPeriod = 1000L;

	private UsbManager mUsbManager;
	private UsbAccessory mAccessory;
	private PendingIntent mPermissionIntent;
	private ParcelFileDescriptor mFileDescriptor;
	private FileInputStream mInputStream;
	private FileOutputStream mOutputStream;
	private boolean mPermissionRequestPending;

	private FlowEngineAPI 	mAPI;
	private MBedDeviceService 			mThisService = this;
	private int mDeviceID;
	
	private Timer mTimer;
	private TimerTask mTimerTask;
			
	private class TestTimerTask extends TimerTask {
		@Override
		public void run() {
			Message m = Message.obtain(mHandler, MSG_TYPE_TIMER_TASK);
			mHandler.sendMessage(m);
		}
	};

	private double[] parseMBedMessage(byte[] msg) {
		double[] values = { 0.5, 0.5, 0.5 };
		return values;
	}
	
	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {

			case MSG_TYPE_MBED_DATA:
				Bundle bundle = msg.getData();
				byte[] data = bundle.getByteArray("data");
				int length = bundle.getInt("length");
				//notify.showNotificationNow("read(" + Integer.toString(length) + "): " + new String(data));
				
				try {
					if (mAPI != null) {
						mAPI.pushInt(mDeviceID, SensorType.EXTERNAL_ACCELEROMETER, 0, 0, 0);
					} else {
						notify.showNotificationNow("mAPI is null..");
					}
				} catch (RemoteException e1) {
					notify.showNotificationNow("RemoteException while pushWaveSegment()..");
					e1.printStackTrace();
				}
				break;

			case MSG_TYPE_TIMER_TASK:
				try {
					if (mOutputStream != null) {
						mOutputStream.write("Hello mbed!\n".getBytes());
					} else {
						notify.showNotificationNow("mOutputStream is null..");
						mTimer.cancel();
					}
				} catch (IOException e) {
					notify.showNotificationNow("IOException while mOutputStream.write()..");
					e.printStackTrace();
					mTimer.cancel();
				}
				break;
			}
		}
	};

	private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			notify.showNotificationNow("intent received: " + intent);

			String action = intent.getAction();
			if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) {
				synchronized (this) {
					UsbAccessory accessory = UsbManager.getAccessory(intent);
					if (accessory != null && accessory.equals(mAccessory)) {
						closeAccessory();
					}

				}
			} else if (ACTION_USB_PERMISSION.equals(action)) {
				synchronized (this) {
					UsbAccessory accessory = UsbManager.getAccessory(intent);
					if (intent.getBooleanExtra(
							UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
						openAccessory(accessory);
					} else {
						Log.d(TAG, "permission denied for accessory "
								+ accessory);
					}
					mPermissionRequestPending = false;
				}
			}
		}
	};

	private ServiceConnection mServiceConnection = new ServiceConnection() {
		@Override public void onServiceConnected(ComponentName name, IBinder service) { 
			notify.showNotificationNow("Connected to FlowEngine Service."); 
			mAPI = FlowEngineAPI.Stub.asInterface(service);
			try {
				mDeviceID = mAPI.addDevice(mMBedDeviceInterface);
				mAPI.addSensor(mDeviceID, SensorType.EXTERNAL_ACCELEROMETER, 0);
			} catch (RemoteException e) {
				Log.e(TAG, "Failed to add AbstractDevice..", e);
			}
		}

		@Override public void onServiceDisconnected(ComponentName name) {
			notify.showNotificationNow("Disconnected from FlowEngine Service."); 
		} 
	};
	
	private DeviceAPI.Stub mMBedDeviceInterface = new DeviceAPI.Stub() {
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
		@Override
		public void startSensor(int sensor) throws RemoteException {
		}
		@Override
		public void stopSensor(int sensor) throws RemoteException {
		}
	};

	// This handling is needed because this function is called from external thread.
	private void handleStartRequest() {
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				Log.i(TAG, "start()..");
				//TODO
			}
		});
	}
	
	private void handleStopRequest() {
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				Log.i(TAG, "stop()..");
				//TODO
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
	
	private void openAccessory(UsbAccessory accessory) {
		mFileDescriptor = mUsbManager.openAccessory(accessory);
		if (mFileDescriptor != null) {
			mAccessory = accessory;
			FileDescriptor fd = mFileDescriptor.getFileDescriptor();
			mInputStream = new FileInputStream(fd);
			mOutputStream = new FileOutputStream(fd);
			Thread thread = new Thread(this, "MBedFlowEngine");
			thread.start();

			mTimer = new Timer("TestTask");
			mTimerTask = new TestTimerTask();
			mTimer.schedule(mTimerTask, mDelay, mPeriod);
		} else {
			notify.showNotificationNow("mFileDescriptor is null..");
		}
	}

	private void closeAccessory() {
		try {
			if (mFileDescriptor != null) {
				mFileDescriptor.close();
			}
		} catch (IOException e) {
		} finally {
			mFileDescriptor = null;
			mAccessory = null;
			mInputStream = null;
			mOutputStream = null;
			
			mTimer.cancel();
		}
	}

	@Override
	public void run() {
		int length = 0;
		byte[] buffer = new byte[16384];

		while (length >= 0) {
			try {
				length = mInputStream.read(buffer);
			} catch (IOException e) {
				notify.showNotificationNow("IOException while mInputStream.read()..");
				e.printStackTrace();
				break;
			}
			
			Message m = Message.obtain(mHandler, MSG_TYPE_MBED_DATA);
			Bundle bundle = new Bundle();
			bundle.putByteArray("data", buffer);
			bundle.putInt("length", length);
			m.setData(bundle);
			mHandler.sendMessage(m);
		}

		notify.showNotificationNow("mInputStream.read() returned "
				+ Integer.toString(length) + ". Receiving thread terminated.");
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();

		notify = new NotificationHelper(this, this.getClass().getSimpleName(),
				this.getClass().getName(), R.drawable.ic_launcher);
		notify.showNotificationNow("onCreate()..");

		mUsbManager = UsbManager.getInstance(this);
		mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(
				ACTION_USB_PERMISSION), 0);
		IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
		filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
		registerReceiver(mUsbReceiver, filter);

		// Start FlowEngine service if it's not running.
		Intent intent = new Intent(FlowEngineServiceName);
		startService(intent);

		// Bind to the FlowEngine service.
		bindService(intent, mServiceConnection, 0);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		notify.showNotificationNow("onStartCommand()..");

		if (mInputStream != null && mOutputStream != null) {
			return super.onStartCommand(intent, flags, startId);
		}

		UsbAccessory[] accessories = mUsbManager.getAccessoryList();
		UsbAccessory accessory = (accessories == null ? null : accessories[0]);
		if (accessory != null) {
			if (mUsbManager.hasPermission(accessory)) {
				openAccessory(accessory);
			} else {
				synchronized (mUsbReceiver) {
					if (!mPermissionRequestPending) {
						mUsbManager.requestPermission(accessory,
								mPermissionIntent);
						mPermissionRequestPending = true;
					}
				}
			}
		} else {
			notify.showNotificationNow("accessory is null..");
		}

		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public void onDestroy() {
		Log.i(TAG, "Service destroying...");

		notify.showNotificationNow("onDestroy()..");
		unregisterReceiver(mUsbReceiver);

		try { 
			unbindService(mServiceConnection); 
		} catch (Throwable t) { 
			Log.w(TAG, "Failed to unbind from the service", t); 
		}

		super.onDestroy();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		notify.showNotificationNow("onConfigurationChanged()..");
		super.onConfigurationChanged(newConfig);
	}

	@Override
	public void onLowMemory() {
		notify.showNotificationNow("onLowMemory()..");
		closeAccessory();
		super.onLowMemory();
	}

	@Override
	public void onRebind(Intent intent) {
		notify.showNotificationNow("onRebind()..");
		super.onRebind(intent);
	}

	@Override
	public boolean onUnbind(Intent intent) {
		notify.showNotificationNow("onUnbind()..");
		return super.onUnbind(intent);
	}
}
