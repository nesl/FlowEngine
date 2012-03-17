package edu.ucla.nesl.flowengine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.hardware.SensorManager;
import edu.ucla.nesl.flowengine.node.ActivityGraphControl;
import edu.ucla.nesl.flowengine.node.Buffer;
import edu.ucla.nesl.flowengine.node.DataFlowNode;
import edu.ucla.nesl.flowengine.node.Publish;
import edu.ucla.nesl.flowengine.node.SeedNode;
import edu.ucla.nesl.flowengine.node.classifier.Activity;
import edu.ucla.nesl.flowengine.node.classifier.Conversation;
import edu.ucla.nesl.flowengine.node.classifier.Motion;
import edu.ucla.nesl.flowengine.node.classifier.Outdoor;
import edu.ucla.nesl.flowengine.node.classifier.Stress;
import edu.ucla.nesl.flowengine.node.feature.BandPower;
import edu.ucla.nesl.flowengine.node.feature.BreathingDuration;
import edu.ucla.nesl.flowengine.node.feature.Exhalation;
import edu.ucla.nesl.flowengine.node.feature.FFT;
import edu.ucla.nesl.flowengine.node.feature.IERatio;
import edu.ucla.nesl.flowengine.node.feature.Inhalation;
import edu.ucla.nesl.flowengine.node.feature.LombPeriodogram;
import edu.ucla.nesl.flowengine.node.feature.Mean;
import edu.ucla.nesl.flowengine.node.feature.Median;
import edu.ucla.nesl.flowengine.node.feature.NthBest;
import edu.ucla.nesl.flowengine.node.feature.Percentile;
import edu.ucla.nesl.flowengine.node.feature.QuartileDeviation;
import edu.ucla.nesl.flowengine.node.feature.RRInterval;
import edu.ucla.nesl.flowengine.node.feature.PeakValley;
import edu.ucla.nesl.flowengine.node.feature.Respiration;
import edu.ucla.nesl.flowengine.node.feature.RootMeanSquare;
import edu.ucla.nesl.flowengine.node.feature.StandardDeviation;
import edu.ucla.nesl.flowengine.node.feature.Stretch;
import edu.ucla.nesl.flowengine.node.feature.Variance;
import edu.ucla.nesl.flowengine.node.feature.Ventilation;
import edu.ucla.nesl.flowengine.node.operation.Scale;
import edu.ucla.nesl.flowengine.node.operation.Sort;

public class GraphConfiguration {
	private static final String TAG = GraphConfiguration.class.getSimpleName();
	
	private Map<Integer, SeedNode> mSeedNodeMap;
	private Map<String, DataFlowNode> mNodeNameMap = new HashMap<String, DataFlowNode>();
	
	private boolean mIsStress = false;
	private boolean mIsConversation = false;
	private boolean mIsActivity = false;
	private boolean mIsPhoneBattery = false;
	private boolean mIsPhoneGPS = false;
	private boolean mIsZephyrBattery = false;
	private boolean mIsZephyrButtonWorn = false;

	public GraphConfiguration(Map<Integer, SeedNode> seedNodeMap) {
		mSeedNodeMap = seedNodeMap;
	}
	
	public void configureApplication(Application app) {
		DebugHelper.log(TAG, "configure..");
		ArrayList<String> nodeNames = app.getSubscribedNodeNames();
		Publish publish = new Publish(app.getApplicationInterface());
		for (String nodeName: nodeNames) {
			if (nodeName.equals("Activity")) {
				//configureActivity(publish);
			} else if (nodeName.equals("StressConversation")) {
				//configureStressConversation(publish);
			} else if (nodeName.equals("PhoneBattery")) {
				configurePhoneBattery(publish);
			} else if (nodeName.equals("PhoneGPS")) {
				configurePhoneGPS(publish);
			} else if (nodeName.equals("ZephyrBattery")) {
				configureZephyrBattery(publish);
			} else if (nodeName.equals("ZephyrButtonWorn")) {
				configureZephyrButtonWorn(publish);
			}
		}
	}
	
	private boolean configureZephyrButtonWorn(Publish publish) {
		if (mIsZephyrButtonWorn) {
			return true;
		}

		SeedNode zephyrWorn = mSeedNodeMap.get(SensorType.ZEPHYR_BUTTON_WORN);
		if (zephyrWorn != null) {
			mIsZephyrButtonWorn = true;
			zephyrWorn.addOutputNode(publish);
			zephyrWorn.startSensor();
			return true;
		}
		return false;
	}

	private boolean configureZephyrBattery(Publish publish) {
		if (mIsZephyrBattery) {
			return true;
		}
		
		SeedNode zephyrBattery = mSeedNodeMap.get(SensorType.ZEPHYR_BATTERY);
		if (zephyrBattery != null) {
			mIsZephyrBattery = true;
			zephyrBattery.addOutputNode(publish);
			zephyrBattery.startSensor();
			return true;
		}
		return false;
	}
	
	private boolean configurePhoneGPS(Publish publish) {
		if (mIsPhoneGPS) {
			return true;
		}
		
		SeedNode gps = mSeedNodeMap.get(SensorType.PHONE_GPS);
		if (gps != null) {
			mIsPhoneGPS = true;
			gps.addOutputNode(publish);
			gps.startSensor();
			return true;
		}
		return false;
	}
	
	private boolean configurePhoneBattery(Publish publish) {
		if (mIsPhoneBattery) {
			return true;
		}
		
		SeedNode batteryNode = mSeedNodeMap.get(SensorType.PHONE_BATTERY);
		if (batteryNode != null) {
			mIsPhoneBattery = true;
			batteryNode.addOutputNode(publish);
			batteryNode.startSensor();
			return true;
		}
		return false;
	}
	
	/*public void configureActivityTemp() {
		if (mIsActivity) {
			return;
		}
		
		//SeedNode accelerometer = mSeedNodeMap.get(SensorType.CHEST_ACCELEROMETER);
		SeedNode accelerometer = mSeedNodeMap.get(SensorType.PHONE_ACCELEROMETER);
		SeedNode gps = mSeedNodeMap.get(SensorType.PHONE_GPS);
		
		if (accelerometer == null || gps == null) {
			return;
		}

		mIsActivity = true;
		DebugHelper.log(TAG, "Configuring activity graph..");

		RootMeanSquare rms = new RootMeanSquare(SensorManager.GRAVITY_EARTH, 1.0);
		Buffer rmsBuffer = new Buffer(50, accelerometer.getSensor().getSampleInterval());
		Scale scaledRMS = new Scale(310);
		FFT scaledFFT1_3 = new FFT(1.0, 3.0, 1.0);
		FFT scaledFFT4_5 = new FFT(4.0, 5.0, 1.0);
		Motion motion = new Motion();
		Outdoor outdoor = new Outdoor();
		Mean scaledMean = new Mean();
		Variance scaledVariance = new Variance();
		Mean mean = new Mean();
		Variance variance = new Variance();
		FFT fft1_10 = new FFT(1.0, 10.0, 1.0);
		Activity activity = new Activity();
		ActivityGraphControl control = new ActivityGraphControl(motion, gps);
				
		accelerometer.addOutputNode(rms);
		rms.addOutputNode(rmsBuffer);
		rmsBuffer.addOutputNode(scaledRMS);
		scaledRMS.addOutputNode(scaledFFT1_3);
		scaledRMS.addOutputNode(scaledFFT4_5);
		scaledFFT1_3.addOutputNode(motion);
		scaledFFT4_5.addOutputNode(motion);
		motion.addOutputNode(outdoor);

		scaledRMS.addOutputNode(scaledMean);
		scaledRMS.addOutputNode(scaledVariance);
		scaledMean.addOutputNode(scaledVariance);
		rmsBuffer.addOutputNode(mean);
		rmsBuffer.addOutputNode(variance);
		mean.addOutputNode(variance);
		rmsBuffer.addOutputNode(fft1_10);
		
		scaledVariance.addOutputNode(activity);
		scaledFFT1_3.addOutputNode(activity);
		variance.addOutputNode(activity);
		fft1_10.addOutputNode(activity);
		gps.addOutputNode(activity);
		activity.addOutputNode(outdoor);

		outdoor.addOutputNode(control);
		
		// Recursive init from seednode
		accelerometer.initializeGraph();
		gps.initializeGraph();
		
		accelerometer.startSensor();
	}
	*/
	/*private boolean configureActivity(Publish publish) {
		if (mIsActivity) {
			return true;
		}
		
		//SeedNode accelerometer = mSeedNodeMap.get(SensorType.CHEST_ACCELEROMETER);
		SeedNode accelerometer = mSeedNodeMap.get(SensorType.PHONE_ACCELEROMETER);
		SeedNode gps = mSeedNodeMap.get(SensorType.PHONE_GPS);
		
		if (accelerometer == null || gps == null) {
			return false;
		}

		mIsActivity = true;
		DebugHelper.log(TAG, "Configuring activity graph..");

		RootMeanSquare rms = new RootMeanSquare(SensorManager.GRAVITY_EARTH, 1.0);
		Buffer rmsBuffer = new Buffer(50, accelerometer.getSensor().getSampleInterval());
		Scale scaledRMS = new Scale(310);
		FFT scaledFFT1_3 = new FFT(1.0, 3.0, 1.0);
		FFT scaledFFT4_5 = new FFT(4.0, 5.0, 1.0);
		Motion motion = new Motion();
		Outdoor outdoor = new Outdoor();
		Mean scaledMean = new Mean();
		Variance scaledVariance = new Variance();
		Mean mean = new Mean();
		Variance variance = new Variance();
		FFT fft1_10 = new FFT(1.0, 10.0, 1.0);
		Activity activity = new Activity();
		ActivityGraphControl control = new ActivityGraphControl(motion, gps);
				
		accelerometer.addOutputNode(rms);
		rms.addOutputNode(rmsBuffer);
		rmsBuffer.addOutputNode(scaledRMS);
		scaledRMS.addOutputNode(scaledFFT1_3);
		scaledRMS.addOutputNode(scaledFFT4_5);
		scaledFFT1_3.addOutputNode(motion);
		scaledFFT4_5.addOutputNode(motion);
		motion.addOutputNode(outdoor);

		scaledRMS.addOutputNode(scaledMean);
		scaledRMS.addOutputNode(scaledVariance);
		scaledMean.addOutputNode(scaledVariance);
		rmsBuffer.addOutputNode(mean);
		rmsBuffer.addOutputNode(variance);
		mean.addOutputNode(variance);
		rmsBuffer.addOutputNode(fft1_10);
		
		scaledVariance.addOutputNode(activity);
		scaledFFT1_3.addOutputNode(activity);
		variance.addOutputNode(activity);
		fft1_10.addOutputNode(activity);
		gps.addOutputNode(activity);
		activity.addOutputNode(outdoor);

		outdoor.addOutputNode(control);
		
		// Recursive init from seednode
		accelerometer.initializeGraph();
		gps.initializeGraph();
		
		activity.addOutputNode(publish);
		
		accelerometer.startSensor();
		
		return true;
	}
	*/
	/*private boolean configureStressConversation(Publish publish) {
		if (mIsStressConversation) {
			return true;
		}
		
		SeedNode ecg = mSeedNodeMap.get(SensorType.ECG);
		SeedNode rip = mSeedNodeMap.get(SensorType.RIP);

		if (ecg == null || rip == null) {
			return false;
		}
		mIsStressConversation = true;
		
		// Stress classifier
		Stress stress = new Stress();
		
		// RIP sample interval == 56 ms
		final double RIP_SAMPLE_RATE = 1000.0 / 56.0; // Hz 
		final double RIP_BUFFER_DURATION = 60; // sec
		
		Buffer ripBuffer = new Buffer(1071, rip.getSensor().getSampleInterval()); // 59976 ms
		//BufferNode ripBuffer = new BufferNode(RIP_SAMPLE_RATE * RIP_BUFFER_DURATION);
		Sort ripSort = new Sort();
		Percentile ripPercentile = new Percentile();
		PeakValley rpv = new PeakValley(RIP_SAMPLE_RATE, RIP_BUFFER_DURATION);
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
	
		// order is important here because rpv pulls percentile which also pulls sort.
		ripBuffer.addOutputNode(ripSort);
		ripBuffer.addOutputNode(rpv);
		rpv.addPullNode(ripPercentile);
		ripPercentile.addPullNode(ripSort);

		ripBuffer.addOutputNode(stretch);
		
		rpv.addOutputNode(respiration);
		rpv.addOutputNode(ieratio);
		rpv.addOutputNode(inhalation);
		rpv.addOutputNode(exhalation);
		rpv.addOutputNode(ventilation);
		rpv.addOutputNode(stretch);
		
		respiration.addOutputNode(respirationSort);
		respirationSort.addOutputNode(respirationPercentile);
		respirationPercentile.addOutputPort("Percentile75.0", 75.0);
		respirationPercentile.addOutputPort("Percentile25.0", 25.0);
		respirationPercentile.addOutputNode("Percentile75.0", respirationQD);
		respirationPercentile.addOutputNode("Percentile25.0", respirationQD);

		ieratio.addOutputNode(ieRatioSort);
		ieRatioSort.addOutputNode(ieRatioMedian);
		inhalation.addOutputNode(inhaleMean);
		
		exhalation.addOutputNode(exhaleSort);
		exhaleSort.addOutputNode(exhalePercentile);

		exhalePercentile.addOutputPort("Percentile75.0", 75.0);
		exhalePercentile.addOutputPort("Percentile25.0", 25.0);
		exhalePercentile.addOutputNode("Percentile75.0", exhaleQD);
		exhalePercentile.addOutputNode("Percentile25.0", exhaleQD);
		
		stretch.addOutputNode(stretchSort);
		stretchSort.addOutputNode(stretchMedian);
		stretchSort.addOutputNode(stretchPercentile);
		stretchPercentile.addOutputPort("Percentile75.0", 75.0);
		stretchPercentile.addOutputPort("Percentile25.0", 25.0);
		stretchPercentile.addOutputNode("Percentile75.0", stretchQD);
		stretchPercentile.addOutputNode("Percentile25.0", stretchQD);
		
		// ECG sample interval = 4ms
		//final int ECG_SAMPLE_RATE = 250; // Hz
		//final int ECG_BUFFER_DURATION = 60; // sec
		Buffer ecgBuffer = new Buffer(14994, ecg.getSensor().getSampleInterval()); // 59976 ms
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
		
		rrInterval.addOutputNode(rrSort);
		rrInterval.addOutputNode(lomb);
		rrInterval.addOutputNode(rrMean);
		rrInterval.addOutputNode(rrVariance);
		rrSort.addOutputNode(rrMedian);
		rrSort.addOutputNode(rrPercentile);
		rrPercentile.addOutputPort("Percentile75.0", 75.0);
		rrPercentile.addOutputPort("Percentile25.0", 25.0);
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
		stretchPercentile.addOutputPort("Percentile80.0", 80.0);
		stretchPercentile.addOutputNode("Percentile80.0", stress);
		stretchQD.addOutputNode(stress);
		rrMedian.addOutputNode(stress);
		rrPercentile.addOutputPort("Percentile80.0", 80.0);
		rrPercentile.addOutputNode("Percentile80.0", stress);
		rrQD.addOutputNode(stress);
		rrMean.addOutputNode(stress);
		lombBandPower.addOutputNode(stress);

		ecgBuffer.addSyncedBufferNode(ripBuffer);
		ripBuffer.addSyncedBufferNode(ecgBuffer);
		
		// Conversation classifier
		Conversation conversation = new Conversation();
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
		inhalePercentile.addOutputPort("Percentile90.0", 90.0);
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
		nbest.addOutputPort("NthBest2", 2);
		nbest.addOutputNode("NthBest2", conversation);
		
		// Recursive init from seed nodes
		ecg.initializeGraph();
		rip.initializeGraph();
		
		stress.addOutputNode(publish);
		conversation.addOutputNode(publish);
		
		// start sensors.
		ecg.startSensor();
		rip.startSensor();
		
		return true;
	}
	*/
	
	private static final int STATUS_DEFAULT = 0;
	private static final int STATUS_DECLARE = 1;
	private static final int STATUS_CONNECT = 2;
	private static final String classNamePrefix = "edu.ucla.nesl.flowengine.node.";
	private static final String classNameClassifierPrefix = "edu.ucla.nesl.flowengine.node.classifier.";
	private static final String classNameFeaturePrefix = "edu.ucla.nesl.flowengine.node.feature.";
	private static final String classNameOperationPrefix = "edu.ucla.nesl.flowengine.node.operation.";

	private DataFlowNode instantiateNode(String className, String instanceName) {
		Class currentClass;
		try {
			currentClass = Class.forName(classNameClassifierPrefix + className);
		} catch (ClassNotFoundException e) {
			try {
				currentClass = Class.forName(classNameFeaturePrefix + className);
			} catch (ClassNotFoundException e1) {
				try {
					currentClass = Class.forName(classNameOperationPrefix + className);
				} catch (ClassNotFoundException e2) {
					try {
						currentClass = Class.forName(classNamePrefix + className);
					} catch (ClassNotFoundException e3) {
						DebugHelper.loge(TAG, "Class not found.");
						e3.printStackTrace();
						return null;
					}
				}
			}
		}

		DataFlowNode currentNode = null;
		try {
			if (!instanceName.contains("(")) { // constructor without parameters.
				currentNode = (DataFlowNode)currentClass.newInstance();
			} else { // constructor with parameters.
				String[] split = instanceName.split("\\(");
				instanceName = split[0];
				String argstr = split[1].replace(")", "");
				String[] args = argstr.split(",");
				Class[] parTypes = new Class[args.length + 1];
				Object[] arglist = new Object[args.length + 1];
				parTypes[0] = String.class;
				arglist[0] = className + "(" + split[1];
				for (int i = 0; i < args.length; i++) {
					if (args[i].contains(".")) {
						parTypes[i+1] = Double.TYPE;
						arglist[i+1] = Double.parseDouble(args[i]);
					} else {
						parTypes[i+1] = Integer.TYPE;
						arglist[i+1] = Integer.parseInt(args[i]);
					}
				}
				Constructor ct;
				ct = currentClass.getConstructor(parTypes);
				currentNode = (DataFlowNode)ct.newInstance(arglist);
			}
		} catch (IllegalArgumentException e) {
			DebugHelper.loge(TAG, "IllegalArgumentException");
			e.printStackTrace();
			return null;
		} catch (InstantiationException e) {
			DebugHelper.loge(TAG, "InstantiationException");
			e.printStackTrace();
			return null;
		} catch (IllegalAccessException e) {
			DebugHelper.loge(TAG, "IllegalAccessException");
			e.printStackTrace();
			return null;
		} catch (InvocationTargetException e) {
			DebugHelper.loge(TAG, "InvocationTargetException");
			e.printStackTrace();
			return null;
		} catch (SecurityException e) {
			DebugHelper.loge(TAG, "SecurityException");
			e.printStackTrace();
			return null;
		} catch (NoSuchMethodException e) {
			DebugHelper.loge(TAG, "NoSuchMethodException");
			e.printStackTrace();
			return null;
		}
		return currentNode;
	}
	
	private void connect(DataFlowNode left, String operation, DataFlowNode right) {
		if (operation.equals("->")) { // process push port
			if (!left.isPushConnected(right)) {
				left.addOutputNode(right);
			}
		} else if (operation.equals("=>")) { // process pull port
			if (!left.isPullConnected(right)) {
				left.addPullNode(right);
			}
		} else if (operation.contains("(") && operation.contains(")")) { // process parameterized push port
			String portName = operation.split("\\(")[1].split("\\)")[0];
			double parameter = Double.parseDouble(portName);
			if (!left.isPushParameterizedConnected(right, portName)) {
				if (!left.addOutputNode(portName, right)) {
					left.addOutputPort(portName, parameter);
					left.addOutputNode(portName, right);
				}
			}
		} else {
			throw new UnsupportedOperationException("Wrong operator: " + operation);
		}
	}
	
	public void configureStressTemp() {
		if (mIsStress) {
			return;
		}
		
		int configResourceId = R.raw.stress;
		
		InputStream is = FlowEngine.getInstance().getResources().openRawResource(configResourceId);
		BufferedReader configFile = new BufferedReader(new InputStreamReader(is));
		
		Map<String, DataFlowNode> instanceNameMap = new HashMap<String, DataFlowNode>();
		Map<String, DataFlowNode> nodeNameMap = new HashMap<String, DataFlowNode>();
		Map<Integer, SeedNode> seedNodeMap = new HashMap<Integer, SeedNode>();
		
		boolean isMergePass = false;
		
		try {
			int status = STATUS_DEFAULT;
			while (true) {
				String line = configFile.readLine();
				
				if (line == null)
					break;
				if (line.length() <= 0) {
					continue;
				} else if (line.startsWith("//")) {
					continue;
				}
				
				switch (status) {
				
					case STATUS_DEFAULT: {
						if (line.contains("BEGIN DECLARE")) {
							status = STATUS_DECLARE;
						} else if (line.contains("BEGIN CONNECT")) {
							status = STATUS_CONNECT;
							configFile.mark(Integer.MAX_VALUE);
						} else {
							DebugHelper.loge(TAG, "Unrecognized line: " + line);
							return;
						}
						break;
					}
					
					case STATUS_DECLARE: {
						if (!isMergePass) {
							if (line.contains("END")) {
								status = STATUS_DEFAULT;
								continue;
							}
							
							String[] split = line.split(" ");
							if (split.length != 2) {
								DebugHelper.loge(TAG, "Wrong formatted declare statement: " + line);
							}
							
							String className = split[0];
							String instanceName = split[1];
							
							DataFlowNode node;
							int sensorId = SensorType.getSensorId(className);
							if (sensorId > 0) {
								node = new SeedNode(className, sensorId, null);
								seedNodeMap.put(sensorId, (SeedNode)node);
							} else {
								node = instantiateNode(className, instanceName);
								if (node == null) {
									DebugHelper.loge(TAG, "Failed to instantiate a node: " + line);
									return;
								} 
							}
							instanceNameMap.put(instanceName.split("\\(")[0], node);
						}
						break;
					}
					
					case STATUS_CONNECT: {
						if (line.contains("END")) {
							if (!isMergePass) {
								configFile.reset();
								isMergePass = true;
								for (Map.Entry<Integer, SeedNode> entry: seedNodeMap.entrySet()) {
									entry.getValue().configureNodeName(nodeNameMap);
								}
								DebugHelper.log(TAG, "Starting merge pass..");
							} else {
								status = STATUS_DEFAULT;
							}
							continue;
						}
						
						// check validity
						String[] split = line.split(" ");
						if (split.length != 3) {
							DebugHelper.loge(TAG, "Wrong formatted connect statement: " + line);
						}
						String operation = split[1];
						DataFlowNode left = instanceNameMap.get(split[0]);
						DataFlowNode right = instanceNameMap.get(split[2]);
						if (left == null) {
							DebugHelper.loge(TAG, "Cannot find instance name: " + split[0]);
							return;
						}
						if (right == null) {
							DebugHelper.loge(TAG, "Cannot find instance name: " + split[2]);
							return;
						}

						if (!isMergePass) {
							connect(left, operation, right);
						} else { // if it is merge pass
							// find left node in mNodeNameMap
							DataFlowNode globalLeft = mNodeNameMap.get(left.getName());
							if (globalLeft == null) {
								left.resetConnection();
								mNodeNameMap.put(left.getName(), left);
								globalLeft = mNodeNameMap.get(left.getName());
							}
							
							DataFlowNode globalRight = mNodeNameMap.get(right.getName());
							if (globalRight == null) {
								right.resetConnection();
								mNodeNameMap.put(right.getName(), right);
								globalRight = mNodeNameMap.get(right.getName());
							}
							
							connect(globalLeft, operation, globalRight);
						}
						break;
					}
				}
			}
		} catch (IOException e) {
			DebugHelper.loge(TAG, "IOException");
			e.printStackTrace();
		}
		
		/*for (Map.Entry<String, DataFlowNode> entry: instanceNameMap.entrySet()) {
			String instanceName = entry.getKey();
			DataFlowNode node = entry.getValue();
			DebugHelper.log(TAG, instanceName + ": " + node.getClass().getName());
		}*/

		// merge with mSeedNodeMap
		for (Map.Entry<Integer, SeedNode> entry: seedNodeMap.entrySet()) {
			entry.getValue().initializeGraph();
			SeedNode existingSeedNode = mSeedNodeMap.get(entry.getKey());
			if (existingSeedNode != null) {
				existingSeedNode.reconnect(entry.getValue());
			} else {
				mSeedNodeMap.put(entry.getKey(), entry.getValue());
			}
		}
		
		for (Map.Entry<Integer, SeedNode> entry: mSeedNodeMap.entrySet()) {
			int sensor = entry.getKey();
			SeedNode node = entry.getValue();
			DebugHelper.log(TAG, node.getClass().getName() + ": " + sensor + " device: " + node.getAttachedDevice());
		}

		for (Map.Entry<String, DataFlowNode> entry: mNodeNameMap.entrySet()) {
			String nodeName = entry.getKey();
			DataFlowNode node = entry.getValue();
			DebugHelper.log(TAG, nodeName + ": " + node.getClass().getName());
		}
		
		mIsStress = true;
	}
}
