package edu.ucla.nesl.flowengine.node;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import edu.ucla.nesl.flowengine.Device;

public abstract class DataFlowNode {
	private static final String TAG = DataFlowNode.class.getSimpleName();

	protected int mSensorType;
	protected Device mAttachedDevice;
	
	protected Map<String, ArrayList<DataFlowNode>> mOutPortMap = new HashMap<String, ArrayList<DataFlowNode>>();
	protected Map<String, Object> mOutPortParameterMap = new HashMap<String, Object>();

	protected class ResultData {
		public String name;
		public String type;
		public Object outputData;
		public int length;
		public long timestamp;
		
		public ResultData(String name, String type, Object outputData, int length, long timestamp) {
			this.name = name;
			this.type = type;
			this.outputData = outputData;
			this.length = length;
			this.timestamp = timestamp;
		}
	}

	abstract public void input(String name, String type, Object inputData, int length, long timestamp);

	public Object pull(Object parameter) {
		return null;
	}
	
	public final void addOutputNode(DataFlowNode node) {
		addOutputNode("default", node);
	}
	
	public final void addOutputNode(String portName, DataFlowNode node) {
		ArrayList<DataFlowNode> port = mOutPortMap.get(portName);
		if (port == null) {
			addOutPort(portName, null);
			port = mOutPortMap.get(portName);
		}
		port.add(node);
	}
	
	public final void addOutPort(String portName, Object parameter) {
		mOutPortMap.put(portName, new ArrayList<DataFlowNode>());
		mOutPortParameterMap.put(portName, parameter);
	}
	
	protected final int getSampleInterval() {
		return mAttachedDevice.getSensor(mSensorType).getSampleInterval();
	}
	
	protected final int getSensorType() {
		return mSensorType;
	}

	protected ResultData getParameterizedResult(Object parameter, String name, String type, Object inputData, int length, long timestamp) {
		return null;
	}
	
	protected final void output(String name, String type, Object outputData, int length, long timestamp) {
		for (Map.Entry<String, ArrayList<DataFlowNode>> entry: mOutPortMap.entrySet()) {
			Object parameter = mOutPortParameterMap.get(entry.getKey());
			ArrayList<DataFlowNode> nodeList = entry.getValue();
			if (parameter == null) {
				for (DataFlowNode node: nodeList) {
					node.input(name, type, outputData, length, timestamp);
				}
			} else {
				ResultData result = getParameterizedResult(parameter, name, type, outputData, length, timestamp);
				if (result != null) {
					for (DataFlowNode node: nodeList) {
						node.input(result.name, result.type, result.outputData, result.length, result.timestamp);
					}
				}
			}
		}
	}
		
	protected final void initializeGraph(int sensorType, Device attachedDevice) {
		mSensorType = sensorType;
		mAttachedDevice = attachedDevice;
		for (Map.Entry<String, ArrayList<DataFlowNode>> entry: mOutPortMap.entrySet()) {
			ArrayList<DataFlowNode> nodeList = entry.getValue();
			for (DataFlowNode node: nodeList) {
				node.initializeGraph(sensorType, attachedDevice);
			}
		}
	}

}
