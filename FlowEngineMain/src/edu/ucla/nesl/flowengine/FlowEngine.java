package edu.ucla.nesl.flowengine;

import java.util.HashMap;
import java.util.Map;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Parcel;
import android.os.RemoteException;
import edu.ucla.nesl.flowengine.aidl.DeviceInterface;
import edu.ucla.nesl.flowengine.aidl.FlowEngineAPI;
import edu.ucla.nesl.flowengine.node.BufferNode;
import edu.ucla.nesl.flowengine.node.SeedNode;
import edu.ucla.nesl.flowengine.node.classifier.ActivityClassifier;
import edu.ucla.nesl.flowengine.node.classifier.ConversationClassifier;
import edu.ucla.nesl.flowengine.node.classifier.StressClassifier;
import edu.ucla.nesl.flowengine.node.feature.BandPower;
import edu.ucla.nesl.flowengine.node.feature.BreathingDuration;
import edu.ucla.nesl.flowengine.node.feature.Exhalation;
import edu.ucla.nesl.flowengine.node.feature.Goertzel;
import edu.ucla.nesl.flowengine.node.feature.IERatio;
import edu.ucla.nesl.flowengine.node.feature.Inhalation;
import edu.ucla.nesl.flowengine.node.feature.LombPeriodogram;
import edu.ucla.nesl.flowengine.node.feature.Mean;
import edu.ucla.nesl.flowengine.node.feature.Median;
import edu.ucla.nesl.flowengine.node.feature.NthBest;
import edu.ucla.nesl.flowengine.node.feature.Percentile;
import edu.ucla.nesl.flowengine.node.feature.QuartileDeviation;
import edu.ucla.nesl.flowengine.node.feature.RRInterval;
import edu.ucla.nesl.flowengine.node.feature.RealPeakValley;
import edu.ucla.nesl.flowengine.node.feature.Respiration;
import edu.ucla.nesl.flowengine.node.feature.RootMeanSquare;
import edu.ucla.nesl.flowengine.node.feature.StandardDeviation;
import edu.ucla.nesl.flowengine.node.feature.Stretch;
import edu.ucla.nesl.flowengine.node.feature.Variance;
import edu.ucla.nesl.flowengine.node.feature.Ventilation;
import edu.ucla.nesl.flowengine.node.operation.Sort;
import edu.ucla.nesl.util.NotificationHelper;

public class FlowEngine extends Service {

	private static final String TAG = FlowEngine.class.getSimpleName();
	private static final String BUNDLE_TYPE = "type";
	private static final String BUNDLE_DATA = "data";
	private static final String BUNDLE_LENGTH = "length";
	private static final String BUNDLE_DEVICE_ID = "deviceID";
	private static final String BUNDLE_TIMESTAMP = "timestamp";
	
	int mNextDeviceID = 1;
	
	private NotificationHelper notify;
	private boolean mIsGraphStarted = false;
	
	private Map<Integer, Device> deviceMap = new HashMap<Integer, Device>();
	private Map<Integer, SeedNode> mSeedNodeMap = new HashMap<Integer, SeedNode>();
	
	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			//DebugHelper.log(TAG, String.format("Thread name: %s, msg.what: %d", Thread.currentThread().getName(), msg.what));
			
			if (!mIsGraphStarted)
				return;
			
			SeedNode seed = mSeedNodeMap.get(msg.what);
			if (seed == null) {
				throw new UnsupportedOperationException("No SeedNode for SensorType: " + msg.what);
			}
			
			Bundle bundle = (Bundle)msg.obj;
			int deviceID = bundle.getInt(BUNDLE_DEVICE_ID);
			
			if (seed.getAttachedDevice() != deviceMap.get(deviceID)) {
				DebugHelper.log(TAG, "Unmatched seed node and attached device(sensor: " + msg.what + ", attempted device ID: " + deviceID);
				return;
			}
			
			String type = bundle.getString(BUNDLE_TYPE);
			int length = bundle.getInt(BUNDLE_LENGTH);
			String name = SensorType.getSensorName(msg.what);
			long timestamp = bundle.getLong(BUNDLE_TIMESTAMP);
			
			if (type.equals("double[]")) {
				seed.input(name, type, bundle.getDoubleArray(BUNDLE_DATA), length, timestamp);	
			} else if (type.equals("int[]")) {
				seed.input(name, type, bundle.getIntArray(BUNDLE_DATA), length, timestamp);
			} else if (type.equals("int")) {
				seed.input(name, type, bundle.getInt(BUNDLE_DATA), length, timestamp);
			} else {
				throw new IllegalArgumentException("Unknown data_type: " + type);
			}
		}
	};
	
	private FlowEngineAPI.Stub deviceApiEndpoint = new FlowEngineAPI.Stub() {
		// This override is very useful for debugging.
		@Override
		public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
			try {
				return super.onTransact(code, data, reply, flags);
			} catch (RuntimeException e) {
				throw e;
			}
		}
		
		@Override
		public int addDevice(DeviceInterface deviceInterface) throws RemoteException {
			synchronized(deviceMap) {
				Device device = new Device(deviceInterface);
				int deviceID = mNextDeviceID;
				mNextDeviceID += 1;
				deviceMap.put(deviceID, device);
				DebugHelper.log(TAG, "Added device ID: " + Integer.toString(deviceID));
				return deviceID;
			}
		}

		@Override
		public void addSensor(int deviceID, int sensorType, int sampleInterval) throws RemoteException {
			Device device = deviceMap.get(deviceID);
			device.addSensor(sensorType, sampleInterval);
			mSeedNodeMap.put(sensorType, new SeedNode(sensorType, device));
			
			if (!mIsGraphStarted 
					/*&& mSeedNodeMap.get(SensorType.ACCELEROMETER) != null*/
					&& mSeedNodeMap.get(SensorType.ECG) != null
					&& mSeedNodeMap.get(SensorType.RIP) != null) {
				configureGraph();
				startGraph();
			}
		}

		@Override
		public void removeDevice(int deviceID) throws RemoteException {
			synchronized(deviceMap) {
				deviceMap.remove(deviceID);
			}
		}

		@Override
		public void pushDoubleArrayData(int deviceID, int sensor, double[] data, int length, long timestamp) throws RemoteException {
			//DebugHelper.log(TAG, String.format("Thread name: %s", Thread.currentThread().getName()));
			Bundle bundle = new Bundle();
			bundle.putInt(BUNDLE_DEVICE_ID, deviceID);
			bundle.putString(BUNDLE_TYPE, "double[]");
			bundle.putDoubleArray(BUNDLE_DATA, data);
			bundle.putInt(BUNDLE_LENGTH, length);
			bundle.putLong(BUNDLE_TIMESTAMP, timestamp);
			mHandler.sendMessage(mHandler.obtainMessage(sensor, bundle));
		}

		@Override
		public void pushIntData(int deviceID, int sensor, int data, int length, long timestamp) throws RemoteException {
			//DebugHelper.log(TAG, String.format("Thread name: %s", Thread.currentThread().getName()));
			Bundle bundle = new Bundle();
			bundle.putInt(BUNDLE_DEVICE_ID, deviceID);
			bundle.putString(BUNDLE_TYPE, "int");
			bundle.putInt(BUNDLE_DATA, data);
			bundle.putInt(BUNDLE_LENGTH, length);
			bundle.putLong(BUNDLE_TIMESTAMP, timestamp);
			mHandler.sendMessage(mHandler.obtainMessage(sensor, bundle));
		}

		@Override
		public void pushIntArrayData(int deviceID, int sensor, int[] data, int length, long timestamp) throws RemoteException {
			//DebugHelper.log(TAG, String.format("Thread name: %s", Thread.currentThread().getName()));
			Bundle bundle = new Bundle();
			bundle.putInt(BUNDLE_DEVICE_ID, deviceID);
			bundle.putString(BUNDLE_TYPE, "int[]");
			bundle.putIntArray(BUNDLE_DATA, data);
			bundle.putInt(BUNDLE_LENGTH, length);
			bundle.putLong(BUNDLE_TIMESTAMP, timestamp);
			mHandler.sendMessage(mHandler.obtainMessage(sensor, bundle));
		}
	};

	@Override
	public IBinder onBind(Intent intent) {
		if (FlowEngine.class.getName().equals(intent.getAction())) {
			DebugHelper.log(TAG, "Bound by intent " + intent);
			return deviceApiEndpoint;
		} else {
			return null;
		}
	}

	private void startGraph() {
		mIsGraphStarted = true;
	}
	
	private void stopGraph() {
		mIsGraphStarted = false;
	}
	
	private void configureGraph() {
		//SeedNode accelerometer = mSeedNodeMap.get(SensorType.ACCELEROMETER);
		SeedNode ecg = mSeedNodeMap.get(SensorType.ECG);
		SeedNode rip = mSeedNodeMap.get(SensorType.RIP);

		// Activity classifier
		/*RootMeanSquare rms = new RootMeanSquare();
		BufferNode rmsBuffer = new BufferNode(50);
		Mean rmsMean = new Mean();
		Variance rmsVariance = new Variance();
		Goertzel goertzel = new Goertzel(1.0, 10.0, 1.0);
		ActivityClassifier activity= new ActivityClassifier();
		
		accelerometer.addOutputNode(rms);
		rms.addOutputNode(rmsBuffer);
		rmsBuffer.addOutputNode(goertzel);
		rmsBuffer.addOutputNode(rmsVariance);
		rmsBuffer.addOutputNode(rmsMean);
		goertzel.addOutputNode(activity);
		rmsMean.addOutputNode(activity);
		rmsMean.addOutputNode(rmsVariance);
		rmsVariance.addOutputNode(activity);*/
		
		// Stress classifier
		StressClassifier stress = new StressClassifier();
		
		// RIP sample interval == 56 ms
		final int RIP_SAMPLE_RATE = 18; // Hz 
		final int RIP_BUFFER_DURATION = 60; // sec
		
		BufferNode ripBuffer = new BufferNode(1071); // 59976 ms
		//BufferNode ripBuffer = new BufferNode(RIP_SAMPLE_RATE * RIP_BUFFER_DURATION);
		Sort ripSort = new Sort();
		Percentile ripPercentile = new Percentile();
		RealPeakValley rpv = new RealPeakValley(ripPercentile, RIP_SAMPLE_RATE, RIP_BUFFER_DURATION);
		IERatio ieratio = new IERatio();
		Respiration respiration = new Respiration();
		Stretch stretch = new Stretch();
		Inhalation inhalation = new Inhalation();
		Exhalation exhalation = new Exhalation();
		Ventilation ventilation = new Ventilation();
		
		Sort respirationSort = new Sort();
		Percentile respirationPercentile = new Percentile();
		QuartileDeviation respirationQD = new QuartileDeviation();
		Sort ieRatioSort = new Sort();
		Median ieRatioMedian = new Median();
		Mean inhaleMean = new Mean();
		Sort exhaleSort = new Sort();
		Percentile exhalePercentile = new Percentile();
		QuartileDeviation exhaleQD = new QuartileDeviation();
		Sort stretchSort = new Sort();
		Median stretchMedian = new Median();
		Percentile stretchPercentile = new Percentile();
		QuartileDeviation stretchQD = new QuartileDeviation();
		
		rip.addOutputNode(ripBuffer);
		
		//order is important b/c rpv pulls percentile.
		ripBuffer.addOutputNode(ripSort);
		ripBuffer.addOutputNode(rpv);

		ripBuffer.addOutputNode(stretch);
		ripSort.addOutputNode(ripPercentile);
		
		rpv.addOutputNode(respiration);
		rpv.addOutputNode(ieratio);
		rpv.addOutputNode(inhalation);
		rpv.addOutputNode(exhalation);
		rpv.addOutputNode(ventilation);
		rpv.addOutputNode(stretch);
		
		respiration.addOutputNode(respirationSort);
		respirationSort.addOutputNode(respirationPercentile);
		respirationPercentile.addOutPort("Percentile75.0", 75.0);
		respirationPercentile.addOutPort("Percentile25.0", 25.0);
		respirationPercentile.addOutputNode("Percentile75.0", respirationQD);
		respirationPercentile.addOutputNode("Percentile25.0", respirationQD);

		ieratio.addOutputNode(ieRatioSort);
		ieRatioSort.addOutputNode(ieRatioMedian);
		inhalation.addOutputNode(inhaleMean);
		
		exhalation.addOutputNode(exhaleSort);
		exhaleSort.addOutputNode(exhalePercentile);

		exhalePercentile.addOutPort("Percentile75.0", 75.0);
		exhalePercentile.addOutPort("Percentile25.0", 25.0);
		exhalePercentile.addOutputNode("Percentile75.0", exhaleQD);
		exhalePercentile.addOutputNode("Percentile25.0", exhaleQD);
		
		stretch.addOutputNode(stretchSort);
		stretchSort.addOutputNode(stretchMedian);
		stretchSort.addOutputNode(stretchPercentile);
		stretchPercentile.addOutPort("Percentile75.0", 75.0);
		stretchPercentile.addOutPort("Percentile25.0", 25.0);
		stretchPercentile.addOutputNode("Percentile75.0", stretchQD);
		stretchPercentile.addOutputNode("Percentile25.0", stretchQD);
		
		// ECG sample interval = 4ms
		final int ECG_SAMPLE_RATE = 250; // Hz
		final int ECG_BUFFER_DURATION = 60; // sec
		BufferNode ecgBuffer = new BufferNode(14994); // 59976 ms
		//BufferNode ecgBuffer = new BufferNode(ECG_SAMPLE_RATE * ECG_BUFFER_DURATION);
		RRInterval rrInterval = new RRInterval();
		Sort rrSort = new Sort();
		Median rrMedian = new Median();
		Percentile rrPercentile = new Percentile();
		QuartileDeviation rrQD = new QuartileDeviation();
		Mean rrMean = new Mean();
		Variance rrVariance = new Variance();
		LombPeriodogram lomb = new LombPeriodogram();
		BandPower lombBandPower = new BandPower(0.1, 0.2);
		
		ecg.addOutputNode(ecgBuffer);
		ecgBuffer.addOutputNode(rrInterval);
		
		// order is important here.
		rrInterval.addOutputNode(rrSort);
		
		rrInterval.addOutputNode(lomb);
		rrInterval.addOutputNode(rrMean);
		rrInterval.addOutputNode(rrVariance);
		rrSort.addOutputNode(rrMedian);
		rrSort.addOutputNode(rrPercentile);
		rrPercentile.addOutPort("Percentile75.0", 75.0);
		rrPercentile.addOutPort("Percentile25.0", 25.0);
		rrPercentile.addOutputNode("Percentile75.0", rrQD);
		rrPercentile.addOutputNode("Percentile25.0", rrQD);
		rrMean.addOutputNode(rrVariance);
		rrMean.addOutputNode(lomb);
		rrVariance.addOutputNode(lomb);
		lomb.addOutputNode(lombBandPower);
		
		respirationQD.addOutputNode(stress);
		ieRatioMedian.addOutputNode(stress);
		ventilation.addOutputNode(stress);
		inhaleMean.addOutputNode(stress);
		exhaleQD.addOutputNode(stress);
		stretchMedian.addOutputNode(stress);
		stretchPercentile.addOutPort("Percentile80.0", 80.0);
		stretchPercentile.addOutputNode("Percentile80.0", stress);
		stretchQD.addOutputNode(stress);
		rrMedian.addOutputNode(stress);
		rrPercentile.addOutPort("Percentile80.0", 80.0);
		rrPercentile.addOutputNode("Percentile80.0", stress);
		rrQD.addOutputNode(stress);
		rrMean.addOutputNode(stress);
		lombBandPower.addOutputNode(stress);

		ecgBuffer.addSyncedBufferNode(ripBuffer);
		ripBuffer.addSyncedBufferNode(ecgBuffer);
		
		// Conversation classifier
		ConversationClassifier conversation = new ConversationClassifier();
		Mean ieRatioMean = new Mean();
		Variance inhaleVariance = new Variance();
		StandardDeviation inhaleStdev = new StandardDeviation();
		Sort inhaleSort = new Sort();
		Percentile inhalePercentile = new Percentile();
		Mean exhaleMean = new Mean();
		Mean stretchMean = new Mean();
		Variance stretchVariance = new Variance();
		StandardDeviation stretchStdev = new StandardDeviation();
		BreathingDuration bdur = new BreathingDuration();
		Mean bdMean = new Mean();
		Sort bdurSort = new Sort();
		NthBest nbest = new NthBest();
				
		ieRatioMedian.addOutputNode(conversation);
		ieratio.addOutputNode(ieRatioMean);
		ieRatioMean.addOutputNode(conversation);
		inhalation.addOutputNode(inhaleVariance);
		inhaleMean.addOutputNode(inhaleVariance);
		inhaleVariance.addOutputNode(inhaleStdev);
		inhaleStdev.addOutputNode(conversation);
		inhalation.addOutputNode(inhaleSort);
		inhaleSort.addOutputNode(inhalePercentile);
		inhalePercentile.addOutPort("Percentile90.0", 90.0);
		inhalePercentile.addOutputNode("Percentile90.0", conversation);
		exhalation.addOutputNode(exhaleMean);
		exhaleMean.addOutputNode(conversation);
		stretch.addOutputNode(stretchMean);
		stretch.addOutputNode(stretchVariance);
		stretchMean.addOutputNode(stretchVariance);
		stretchVariance.addOutputNode(stretchStdev);
		stretchStdev.addOutputNode(conversation);
		rpv.addOutputNode(bdur);
		ripBuffer.addOutputNode(bdur);
		bdur.addOutputNode(bdMean);
		bdur.addOutputNode(bdurSort);
		bdurSort.addOutputNode(nbest);
		bdMean.addOutputNode(conversation);
		nbest.addOutPort("NthBest2", 2);
		nbest.addOutputNode("NthBest2", conversation);
		
		// Init from seed nodes
		//accelerometer.initializeGraph();
		ecg.initializeGraph();
		rip.initializeGraph();
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		DebugHelper.logi(TAG, "Service creating..");
		
		notify = new NotificationHelper(this, this.getClass().getSimpleName(), this.getClass().getName(), R.drawable.ic_launcher);
		
		DebugHelper.startTrace();
		
		DebugHelper.logi(TAG, "Service created.");
	}
	
	@Override
	public void onDestroy() {
		DebugHelper.logi(TAG, "Service destroying");

		DebugHelper.stopTrace();
		
		for (Map.Entry<Integer, Device> entry : deviceMap.entrySet()) {
			try {
				entry.getValue().getInterface().kill();
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
		
		super.onDestroy();
	}
}
