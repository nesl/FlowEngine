package edu.ucla.nesl.flowengine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import android.util.Log;
import edu.ucla.nesl.flowengine.node.Buffer;
import edu.ucla.nesl.flowengine.node.DataFlowNode;
import edu.ucla.nesl.flowengine.node.Publish;
import edu.ucla.nesl.flowengine.node.SeedNode;

public class GraphConfiguration {
	private static final String TAG = GraphConfiguration.class.getSimpleName();
	
	private Map<Integer, SeedNode> mSeedNodeMap;
	private Map<String, DataFlowNode> mNodeNameMap;
	private Set<String> mConfiguredGraphs;
	
	private static final int STATUS_DEFAULT = 0;
	private static final int STATUS_DECLARE = 1;
	private static final int STATUS_CONNECT = 2;
	
	private static final String classNamePrefix = "edu.ucla.nesl.flowengine.node.";
	private static final String classNameClassifierPrefix = "edu.ucla.nesl.flowengine.node.classifier.";
	private static final String classNameFeaturePrefix = "edu.ucla.nesl.flowengine.node.feature.";
	private static final String classNameOperationPrefix = "edu.ucla.nesl.flowengine.node.operation.";

	public GraphConfiguration(Map<Integer, SeedNode> seedNodeMap, Map<String, DataFlowNode> nodeNameMap) {
		mSeedNodeMap = seedNodeMap;
		mNodeNameMap = nodeNameMap;
		mConfiguredGraphs = new HashSet<String>();
	}
	
	public boolean subscribe(Application app, String nodeName) {
		Publish publish = app.getPublishNode();
		DataFlowNode node = mNodeNameMap.get("|" + nodeName);
		if (node == null) {
			if (configureSeedNode(nodeName)) {
				node = mNodeNameMap.get("|" + nodeName);
				if (node == null) {
					Log.e(TAG, nodeName + "is not configured.");
					return false;
				}
			} else {
				DebugHelper.loge(TAG, "Could not configure " + nodeName);
				return false;
			}
		}
		node.addOutputNode(publish);
		node.enable();
		return true;
	}

	public boolean unsubscribe(Application app, String nodeName) {
		DataFlowNode node = mNodeNameMap.get("|" + nodeName);
		if (node == null) {
			return false;
		}
		Publish publish = app.getPublishNode();
		node.removeOutputNode(publish);
		return true;
	}
	
	public void removeApplication(Application removedApp) {
		// remove Publish node for this app.
		Publish node = removedApp.getPublishNode();
		node.remove(mNodeNameMap, mSeedNodeMap);

		// change flags
		/*Set<String> nodeNames = removedApp.getSubscribedNodeNames();
		for (String nodeName: nodeNames) {
			if (nodeName.equals(SensorType.ACTIVITY_CONTEXT_NAME)) {
				mIsActivityGraphConfigured = false;
			} else if (nodeName.equals(SensorType.STRESS_CONTEXT_NAME)) {
				mIsStressGraphConfigured = false;
			} else if (nodeName.equals(SensorType.CONVERSATION_CONTEXT_NAME)) {
				mIsConversationGraphConfigured = false;
			}
		}*/
		
		// print seed map
		DebugHelper.log(TAG, "Printing mSeedNodeMap..");
		for (Map.Entry<Integer, SeedNode> entry: mSeedNodeMap.entrySet()) {
			int sensor = entry.getKey();
			SeedNode node1 = entry.getValue();
			DebugHelper.log(TAG, node1.getClass().getName() + ": " + sensor + " device: " + node1.getAttachedDevice());
		}
		DebugHelper.log(TAG, "Done.");

		// print node name map
		DebugHelper.log(TAG, "Printing mNodeNameMap..");
		for (Map.Entry<String, DataFlowNode> entry: mNodeNameMap.entrySet()) {
			String nodeName = entry.getKey();
			DataFlowNode node2 = entry.getValue();
			DebugHelper.log(TAG, nodeName + ": " + node2.getClass().getName());
		}
		DebugHelper.log(TAG, "Done.");
	}
	
	private boolean configureSeedNode(String nodeName) {
		/*if (nodeName.equals(SensorType.ACTIVITY_CONTEXT_NAME)) {
			configureActivityGraph();
		} else if (nodeName.equals(SensorType.STRESS_CONTEXT_NAME)) {
			configureStressGraph();
		} else if (nodeName.equals(SensorType.CONVERSATION_CONTEXT_NAME)) {
			configureConversationGraph();
		} else {*/
			int sensorID = SensorType.getSensorId(nodeName);
			if (sensorID == -1) {
				DebugHelper.loge(TAG, "nodeName: " + nodeName + " cannot be configured as SeedNode.");
				return false;
			}
			SeedNode seed = new SeedNode(nodeName, sensorID, null);
			mSeedNodeMap.put(sensorID, seed);
			seed.configureNodeName(mNodeNameMap);
			return true;
		//}
	}
	
	/*public void configureActivityGraph() {
		if (!mIsActivityGraphConfigured) {
			configureGraph(R.raw.activity);
			mIsActivityGraphConfigured = true;
		}
	}
	
	public void configureStressGraph() {
		if (!mIsStressGraphConfigured) {
			configureGraph(R.raw.stress);
			mIsStressGraphConfigured = true;
		}
	}

	public void configureConversationGraph() {
		if (!mIsConversationGraphConfigured) {
			configureGraph(R.raw.conversation);
			mIsConversationGraphConfigured = true;
		}
	}*/

	private DataFlowNode instantiateNode(String className, String instanceName, Map<String, DataFlowNode> instanceNameMap) {
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
				arglist[0] = className + "(" + split[1];  // pass parameterized simple node name for graph merging
				Pattern integerPattern = Pattern.compile("[-+]?([0-9]*)");
				Pattern doublePattern = Pattern.compile("([0-9]*)\\.([0-9]*)");
				for (int i = 0; i < args.length; i++) {
					if (integerPattern.matcher(args[i]).matches()) {
						parTypes[i+1] = Integer.TYPE;
						arglist[i+1] = Integer.parseInt(args[i]);
					} else if (doublePattern.matcher(args[i]).matches()) {
						parTypes[i+1] = Double.TYPE;
						arglist[i+1] = Double.parseDouble(args[i]);
					} else {
						// Handle constructor with instance names
						String param = args[i];
						DataFlowNode node = instanceNameMap.get(param);
						
						if (node == null) {
							DebugHelper.loge(TAG, "Invalid parameter: " + args[i]);
							return null;
						}
						
						if (node instanceof SeedNode) {
							int sensorId = ((SeedNode) node).getSensorID();
							SeedNode seedNode = mSeedNodeMap.get(sensorId);
							if (seedNode == null) {
								seedNode = (SeedNode)instanceNameMap.get(param);
								if (seedNode == null) {
									DebugHelper.loge(TAG, "Invalid parameter: " + param);
									return null;
								}
							}
							parTypes[i+1] = seedNode.getClass();
							arglist[i+1] = seedNode;
							Log.d(TAG, "name: " + seedNode.getName() + ", class: " + seedNode.getClass());
						} else {
							parTypes[i+1] = node.getClass();
							arglist[i+1] = node;
							Log.d(TAG, "name: " + node.getName() + ", class: " + node.getClass());
						}
						
						/*for (Map.Entry<String, DataFlowNode> entry : instanceNameMap.entrySet()) {
							String name = entry.getKey();
							Class classObj = entry.getValue().getClass();
							Log.d(TAG, "name: " + name + ", class: " + classObj.getName());
						}*/
					}
				}
				Constructor ct;
				ct = currentClass.getConstructor(parTypes);
				currentNode = (DataFlowNode)ct.newInstance(arglist);
			}
		} catch (IllegalArgumentException e) {
			DebugHelper.loge(TAG, "IllegalArgumentException: " + e.getLocalizedMessage());
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
		} else if (operation.contains("->") && operation.contains("(") && operation.contains(")")) { // process parameterized push port
			String portName = operation.split("\\(")[1].split("\\)")[0];
			Object parameter;
			if (portName.contains(".")) { 
				parameter = Double.parseDouble(portName);
			} else {
				parameter = Integer.parseInt(portName);
			}
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
	
	private boolean configureGraph(String graph) {
		BufferedReader configFile = new BufferedReader(new StringReader(graph));
		
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
							return false;
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
								return false;
							}
							
							String className = split[0];
							String instanceName = split[1];
							
							DataFlowNode node;
							int sensorId = SensorType.getSensorId(className);
							if (sensorId > 0) {
								node = new SeedNode(className, sensorId, null);
								seedNodeMap.put(sensorId, (SeedNode)node);
							} else {
								node = instantiateNode(className, instanceName, instanceNameMap);
								if (node == null) {
									DebugHelper.loge(TAG, "Failed to instantiate a node: " + line);
									return false;
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
							return false;
						}
						if (right == null) {
							DebugHelper.loge(TAG, "Cannot find instance name: " + split[2]);
							return false;
						}

						if (!isMergePass) {
							connect(left, operation, right);
						} else { // if it is merge pass
							DataFlowNode globalLeft = mNodeNameMap.get(left.getName());
							DebugHelper.log(TAG, "left.getName(): " + left.getName() + ", globalLeft: " + globalLeft);
							if (globalLeft == null) {
								left.resetConnection();
								mNodeNameMap.put(left.getName(), left);
								globalLeft = mNodeNameMap.get(left.getName());
							}
							
							DataFlowNode globalRight = mNodeNameMap.get(right.getName());
							DebugHelper.log(TAG, "right.getName(): " + right.getName() + ", globalRight: " + globalRight);
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
			return false;
		}
		
		// initialize and add new seed to mSeedNodeMap
		for (Map.Entry<Integer, SeedNode> entry: seedNodeMap.entrySet()) {
			SeedNode newSeed = entry.getValue();
			Integer sensor = entry.getKey();
			SeedNode existingSeedNode = mSeedNodeMap.get(sensor);
			if (existingSeedNode == null) {
				//newSeed.initializeGraph();
				mSeedNodeMap.put(sensor, newSeed);
			} else {
				//existingSeedNode.initializeGraph();
			}
		}

		// handle ActivityGraphControl node
		/*ActivityGraphControl agc = (ActivityGraphControl)nodeNameMap.get("|ActivityGraphControl");
		if (agc != null) {
			agc = (ActivityGraphControl)mNodeNameMap.get("|ActivityGraphControl");
			agc.setGpsNode(mSeedNodeMap.get(SensorType.PHONE_GPS));
			agc.setMotionNode((Motion)mNodeNameMap.get("|Motion"));
			DebugHelper.log(TAG, "AGC Set!");
		}*/

		// sync buffers
		ArrayList<DataFlowNode> bufferNodes = new ArrayList<DataFlowNode>();
		for (Map.Entry<String, DataFlowNode> entry: nodeNameMap.entrySet()) {
			if (entry.getKey().contains("Buffer")) {
				bufferNodes.add(mNodeNameMap.get(entry.getKey()));
			}
		}
		if (bufferNodes.size() >= 2) {
			for (DataFlowNode node1: bufferNodes) {
				Buffer buffer1 = (Buffer)node1;
				for (DataFlowNode node2: bufferNodes) {
					Buffer buffer2 = (Buffer)node2;
					buffer1.addSyncedBufferNode(buffer2);
				}
			}
		}
		
		// print instance name map
		/*for (Map.Entry<String, DataFlowNode> entry: instanceNameMap.entrySet()) {
			String instanceName = entry.getKey();
			DataFlowNode node = entry.getValue();
			DebugHelper.log(TAG, instanceName + ": " + node.getClass().getName());
		}*/

		// print seed map
		/*for (Map.Entry<Integer, SeedNode> entry: mSeedNodeMap.entrySet()) {
			int sensor = entry.getKey();
			SeedNode node = entry.getValue();
			DebugHelper.log(TAG, node.getClass().getName() + ": " + sensor + " device: " + node.getAttachedDevice());
		}*/

		// print node name map
		/*for (Map.Entry<String, DataFlowNode> entry: mNodeNameMap.entrySet()) {
			String nodeName = entry.getKey();
			DataFlowNode node = entry.getValue();
			DebugHelper.log(TAG, nodeName + ": " + node.getClass().getName());
		}*/
		
		return true;
	}

	public boolean submitGraph(String contextName, String graph) {
		if (mNodeNameMap.get("|" + contextName) == null) {
			if (configureGraph(graph)) {
				Log.d(TAG, "configureGraph() true");
				return true;
			} else {
				Log.d(TAG, "configureGraph() false");
				return false;
			}
		}
		Log.d(TAG, "already there!");
		return true;
	}
}
