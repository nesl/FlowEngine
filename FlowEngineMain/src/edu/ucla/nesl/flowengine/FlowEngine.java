package edu.ucla.nesl.flowengine;

import java.util.ArrayList;
import java.util.List;

import android.app.Service;
import android.content.Intent;
import android.os.Debug;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;
import edu.ucla.nesl.flowengine.aidl.AbstractDeviceInterface;
import edu.ucla.nesl.flowengine.aidl.FlowEngineDeviceAPI;
import edu.ucla.nesl.flowengine.aidl.WaveSegment;
import edu.ucla.nesl.util.NotificationHelper;

public class FlowEngine extends Service {

	private static final String TAG = FlowEngine.class.getSimpleName();

	private Handler 						mHandler;
	private FlowEngine 						mThisService = this;

	private NotificationHelper notify;

	private List<AbstractDeviceInterface> 	adis = new ArrayList<AbstractDeviceInterface>();
	private FlowEngineDeviceAPI.Stub 		deviceApiEndpoint = new FlowEngineDeviceAPI.Stub() {
		
		// This override is very useful for debugging.
		@Override
		public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
			try {
				return super.onTransact(code, data, reply, flags);
			} catch (RuntimeException e) {
				Log.w(TAG, "Unexpected remote exception", e);
				throw e;
			}
		}
		
		@Override
		public void pushWaveSegment(WaveSegment ws)
				throws RemoteException {
			handlePushWaveSegment(ws);
		}

		@Override
		public void addAbstractDevice(AbstractDeviceInterface adi)
				throws RemoteException {
			synchronized(adis) {
				adis.add(adi);
			}
			
			adi.start();
		}

		@Override
		public void removeAbstractDevice(AbstractDeviceInterface adi)
				throws RemoteException {
			synchronized(adis) {
				adis.remove(adi);
			}
			
			adi.stop();
		}
	};

	private String dumpWaveSegment(final WaveSegment ws) {
		String wsDump = "timestamp:" + Long.toString(ws.timestamp) 
				+ ", interval:" + Long.toString(ws.interval) 
				+ ", location:";
		wsDump += "(" + Double.toString(ws.latitude) + "," + Double.toString(ws.longitude) + ")";
		wsDump += ", format:";
		if (ws.format == null) {
			wsDump += "null";
		} else {
			wsDump += "{";
			for (int i = 0; i < ws.numChannel; i++) {
				wsDump += ws.format[i] + ",";
			}
			wsDump += "}";
		}
		wsDump += ", data:";
		if (ws.data == null) {
			wsDump += "null";
		} else {
			wsDump += "{";
			for (int i = 0; i < ws.numData; i++) {
				wsDump += Double.toString(ws.data[i]) + ",";
			}
			wsDump += "}";
		}
		return wsDump;
	}
	
	private void handlePushWaveSegment(final WaveSegment ws) {
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				notify.showNotificationNow("pushWaveSegment(): " + ws.name);
				notify.showNotificationNow(dumpWaveSegment(ws));
			}
		});
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		if (FlowEngine.class.getName().equals(intent.getAction())) {
			Log.d(TAG, "Bound by intent " + intent);
			return deviceApiEndpoint;
		} else {
			return null;
		}
	}

	@Override
	public void onCreate() {
		Debug.startMethodTracing("FlowEngine");
		
		super.onCreate();
		
		mHandler = new Handler();
		
		Log.i(TAG, "Service creating");
	}
	
	@Override
	public void onDestroy() {
		Debug.stopMethodTracing();
		
		Log.i(TAG, "Service destroying");
		
		for (AbstractDeviceInterface adi : adis) {
			try {
				adi.kill();
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
		
		super.onDestroy();
	}
}
