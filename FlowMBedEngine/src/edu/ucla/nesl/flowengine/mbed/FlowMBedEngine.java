package edu.ucla.nesl.flowengine.mbed;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import mbed.adkPort.AdkPort;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.android.future.usb.UsbAccessory;
import com.android.future.usb.UsbManager;

public class FlowMBedEngine extends Service {

	private static final String TAG = FlowMBedEngine.class.getSimpleName();
	private static final String FlowEngineServiceName = "edu.ucla.nesl.flowengine.FlowEngine";
	
	private static final String USB_ACCESSORY_MANUFACTURER = "NESL";
	private static final String USB_ACCESSORY_MODEL = "FlowMBedEngine";
	private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";

	private static final int NOTIFICATION_SERVICE_STARTED = 1;
	private static final int NOTIFICATION_USB_ACCESSORY_ATTACHED = 2;
	private static final int NOTIFICATION_DEBUG = 3;
	
	private AdkPort mbed;
	
	NotificationManager mNotificationManager;
	
	UsbManager mUsbManager;
	UsbAccessory mAccessory;
	PendingIntent mPermissionIntent;
	ParcelFileDescriptor mFileDescriptor;
	FileInputStream mInputStream;
	FileOutputStream mOutputStream;
	byte[] mBuf = new byte[16348];

	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
	    public void onReceive(Context context, Intent intent) {
	    	
	    	Log.d(TAG, "intent received: " + intent);
	    	showNotificationNow(NOTIFICATION_DEBUG, "intent received: " + intent);
	    	
	        String action = intent.getAction();
	        if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) {
	            synchronized (this) {
	            	UsbAccessory accessory = UsbManager.getAccessory(intent);
		            if (accessory != null) {
		                // call your method that cleans up and closes communication with the accessory
		            	closeFlowMBedEngine();
		            }
	            }
	        } else if (ACTION_USB_PERMISSION.equals(action)) {
	            synchronized (this) {
	            	UsbAccessory accessory = UsbManager.getAccessory(intent);

	                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
	                    if(accessory != null){
	                    	//call method to set up accessory communication
	                    	mAccessory = accessory;
	                    	startFlowMBedEngine();
	                    }
	                }
	                else {
	                    Log.d(TAG, "permission denied for accessory " + accessory);
	                }
	            }
	        }
	    }
	};

	private void startFlowMBedEngine() {
		Log.d(TAG, "startFlowMBedEngine(): " + mAccessory);
		mFileDescriptor = mUsbManager.openAccessory(mAccessory);
		if (mFileDescriptor != null) {
			FileDescriptor fd = mFileDescriptor.getFileDescriptor();
			mInputStream = new FileInputStream(fd);
			mOutputStream = new FileOutputStream(fd);
			
			// The Android accessory protocol supports packet buffers up to 16384 bytes, 
			// so you can choose to always declare your buffer to be of this size for simplicity.
			Thread thread = new Thread(new Runnable() {
				@Override
				public void run() {
					int numRead;
					try {						
						numRead = mInputStream.read(mBuf);
						if (numRead < 0) {
							return;
						}
						Log.i(TAG, "FlowMBedEngine says: " + new String(mBuf));
					} catch (IOException e) {
						Log.e(TAG, "IOException while mInputStream.read()...");
						e.printStackTrace();
					}
				}
			}, "AccessoryThread");
			
			thread.start();
		} else {
			Log.d(TAG, "mFileDescriptor is null...");
		}
	}
	
	private void closeFlowMBedEngine() {
		// When you are done communicating with an accessory or if the accessory was detached, 
		// close the file descriptor that you opened by calling close().
		try {
			mInputStream.close();
			mOutputStream.close();
		} catch (IOException e) {
			Log.d(TAG, "IOException while closing FlowMBedEngine...");
			e.printStackTrace();
		}
	}
	
	private void requestPermissionForFlowMBedEngine() {
		Log.d(TAG, "requestPermissionForFlowMBedEngine()...");
		if (mUsbManager == null) {
			Log.e(TAG, "mUsbManager is null..");
			return;
		} else {
			Log.d(TAG, "mUsbManager is not null: " + mUsbManager);
		}
		UsbAccessory[] accessoryList = mUsbManager.getAccessoryList();
		if (accessoryList == null) {
			Log.d(TAG, "No attached USB accessories.");
			showNotificationNow(NOTIFICATION_DEBUG, "No attached USB accessories.");
			return;
		}
		for (UsbAccessory usbAcc : accessoryList) {
			Log.d(TAG, "usbAcc: " + usbAcc);
			if (usbAcc.getManufacturer().equals(USB_ACCESSORY_MANUFACTURER) && usbAcc.getModel().equals(USB_ACCESSORY_MODEL)) {
				mUsbManager.requestPermission(usbAcc, mPermissionIntent);
				return;
			}
		}
	}
	
	/*private ServiceConnection mServiceConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			Log.i(TAG, "Service connection established.");
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			Log.i(TAG, "Service connection closed.");
		}
	};*/
	
	private void showNotificationNow(int notificationId, CharSequence tickerText, CharSequence contextText) {
		int icon = R.drawable.ic_launcher;
		long when = System.currentTimeMillis();

		CharSequence contextTitle = "FlowMBedEngine";
		
		Notification notification = new Notification(icon, tickerText, when);
		notification.flags |= Notification.FLAG_AUTO_CANCEL; // | Notification.FLAG_FOREGROUND_SERVICE;
		
		Context context = getApplicationContext();
		Intent notificationIntent = new Intent(this, FlowMBedEngine.class);
		PendingIntent contentIntent = PendingIntent.getService(this, 0, notificationIntent, 0);
		
		notification.setLatestEventInfo(context, contextTitle, contextText, contentIntent);
		
		mNotificationManager.notify(notificationId, notification);
	}
	
	private void showNotificationNow(int notificationId, CharSequence tickerText) {
		showNotificationNow(notificationId, tickerText, tickerText);
	}
	
	private void showNotificationNow(int notificationId) {
		CharSequence tickerText;
		switch (notificationId) {
			case NOTIFICATION_SERVICE_STARTED:
				tickerText = "FlowMBedEngine Service Started."; 
				showNotificationNow(notificationId, tickerText, "Select to connect your USB accessory...");
				break;
			
			case NOTIFICATION_USB_ACCESSORY_ATTACHED:
				tickerText = "USB Accessory Attached.";
				showNotificationNow(notificationId, tickerText);
				break;
		}
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		Log.i(TAG, "Service creating...");
		mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		
		//showNotificationNow(NOTIFICATION_SERVICE_STARTED);
		
		mUsbManager = UsbManager.getInstance(this);
		Log.d(TAG, "mUsbMamager: " + mUsbManager);
		mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
		IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
		registerReceiver(mReceiver, filter);
		requestPermissionForFlowMBedEngine();
		
		/*mbed = new AdkPort(this);
		if (!mbed.isOpen) {
			Log.d(TAG, "USB Accessory is not opened. (errorCode = " + Integer.toString(mbed.errorCode) + ")");
			showNotificationNow(NOTIFICATION_DEBUG, "USB Accessory is not opened. (errorCode = " + Integer.toString(mbed.errorCode) + ")");
		} else {
			Log.d(TAG, "USB Accessory is opened.");
			showNotificationNow(NOTIFICATION_DEBUG, "USB Accessory is opened.");
			Thread thread = new Thread(mbed);
			thread.start();
			mbed.attachOnNew(new AdkPort.MessageNotifier() {
				@Override
				public void onNew() {
					byte[] in = mbed.readB();
					showNotificationNow(NOTIFICATION_DEBUG, "readB(): " + new String(in));
				}
			});
		}*/		
		
        // Start FlowEngine service if it's not running.
		//startService(new Intent(FlowEngineServiceName));
		
		// Bind to the FlowEngine service.
		//bindService(intent, mServiceConnection, 0);
	}

	@Override
	public void onDestroy() {
		Log.i(TAG, "Service destroying...");

		/*try {
			unbindService(mServiceConnection);
		} catch (Throwable t) {
			Log.w(TAG, "Failed to unbind from the service", t);
		}*/

		super.onDestroy();
		
		unregisterReceiver(mReceiver);
	}

}
