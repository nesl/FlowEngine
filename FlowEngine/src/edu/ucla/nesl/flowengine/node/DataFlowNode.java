package edu.ucla.nesl.flowengine.node;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import edu.ucla.nesl.flowengine.Device;

public abstract class DataFlowNode {
	private static final String TAG = DataFlowNode.class.getSimpleName();

	protected int mSensorType;
	protected Device mAttachedDevice;
	
	protected Map<String, ArrayList<DataFlowNode>> mOutPortMap = 
			new HashMap<String, ArrayList<DataFlowNode>>();
	protected Map<String, Object> mOutPortParameterMap = 
			new HashMap<String, Object>();
	protected Map<String, ArrayList<DataFlowNode>> mPullPortMap = 
			new HashMap<String, ArrayList<DataFlowNode>>();

	protected class ResultData {
		public String name;
		public String type;
		public Object data;
		public int length;
		public long timestamp;
		
		public ResultData(String name, String type, Object outputData, int length, long timestamp) {
			this.name = name;
			this.type = type;
			this.data = outputData;
			this.length = length;
			this.timestamp = timestamp;
		}
	}

	public DataFlowNode() {
		addOutputPort("default", null);
		addPullPort("default");
	}
	
	public void input(String name, String type, Object data, int length, long timestamp) {
	}

	public ResultData processPull(Object parameter) {
		return null;
	}
	
	public final void addPullPort(String port) {
		mPullPortMap.put(port, new ArrayList<DataFlowNode>());
	}
	
	public final void addPullNode(DataFlowNode node) {
		addPullNode("default", node);
	}
	
	public final void addPullNode(String port, DataFlowNode node) {
		ArrayList<DataFlowNode> nodeList = mPullPortMap.get(port);
		if (nodeList == null) {
			throw new IllegalArgumentException("No port name: " + port);
		}
		nodeList.add(node);
	}
	
	public final void addOutputPort(String port, Object parameter) {
		mOutPortMap.put(port, new ArrayList<DataFlowNode>());
		mOutPortParameterMap.put(port, parameter);
	}
	
	public final void addOutputNode(DataFlowNode node) {
		addOutputNode("default", node);
	}
	
	public final void addOutputNode(String port, DataFlowNode node) {
		ArrayList<DataFlowNode> nodeList = mOutPortMap.get(port);
		if (nodeList == null) {
			throw new IllegalArgumentException("No port name: " + port);
		}
		nodeList.add(node);
	}
	
	protected final int getSampleInterval() {
		return mAttachedDevice.getSensor(mSensorType).getSampleInterval();
	}
	
	protected final int getSensorType() {
		return mSensorType;
	}
	
	protected final boolean isOutputConnected() {
		for (Map.Entry<String, ArrayList<DataFlowNode>> entry: mOutPortMap.entrySet()) {
			ArrayList<DataFlowNode> nodeList = entry.getValue();
			if (nodeList.size() > 0) {
				return true;
			}
		}
		return false;
	}
	
	protected final boolean isPullConnected() {
		for (Map.Entry<String, ArrayList<DataFlowNode>> entry: mPullPortMap.entrySet()) {
			ArrayList<DataFlowNode> nodeList = entry.getValue();
			if (nodeList.size() > 0) {
				return true;
			}
		}
		return false;
	}

	protected ResultData[] pull() {
		return pull(null);
	}
	
	protected ResultData[] pull(Object parameter) {
		int numResults = 0;
		for (Map.Entry<String, ArrayList<DataFlowNode>> entry: mPullPortMap.entrySet()) {
			ArrayList<DataFlowNode> nodeList = entry.getValue();
			numResults += nodeList.size();
		}
		ResultData[] results = new ResultData[numResults];
		int resultsIndex = 0;
		for (Map.Entry<String, ArrayList<DataFlowNode>> entry: mPullPortMap.entrySet()) {
			ArrayList<DataFlowNode> nodeList = entry.getValue();
			for (DataFlowNode node: nodeList) {
				ResultData result = node.processPull(parameter);
				results[resultsIndex++] = result;
			}
		}
		return results;
	}
	
	protected ResultData[] pull(String port, Object parameter) {
		ArrayList<DataFlowNode> nodeList = mPullPortMap.get(port);
		if (nodeList == null) {
			return null;
		}
		ResultData[] results = new ResultData[nodeList.size()];
		int resultsIndex = 0;
		for (DataFlowNode node: nodeList) {
			ResultData result = node.processPull(parameter);
			results[resultsIndex++] = result;
		}
		return results;
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
						node.input(result.name, result.type, result.data, result.length, result.timestamp);
					}
				}
			}
		}
	}
		
	protected final void output(String port, String name, String type, Object outputData, int length, long timestamp) {
		ArrayList<DataFlowNode> nodeList = mOutPortMap.get(port);
		Object parameter = mOutPortParameterMap.get(port);
		if (parameter == null) {
			for (DataFlowNode node: nodeList) {
				node.input(name, type, outputData, length, timestamp);
			}
		} else {
			ResultData result = getParameterizedResult(parameter, name, type, outputData, length, timestamp);
			if (result != null) {
				for (DataFlowNode node: nodeList) {
					node.input(result.name, result.type, result.data, result.length, result.timestamp);
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
