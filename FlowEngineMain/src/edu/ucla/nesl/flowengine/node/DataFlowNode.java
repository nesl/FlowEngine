package edu.ucla.nesl.flowengine.node;

import java.util.ArrayList;

import edu.ucla.nesl.flowengine.Device;

public abstract class DataFlowNode {
	private static final String TAG = DataFlowNode.class.getSimpleName();

	public int mSensorType;
	public Device mAttachedDevice;
	
	protected ArrayList<DataFlowNode> mOutputList = new ArrayList<DataFlowNode>();

	abstract public void inputData(String name, String type, Object inputData, int length, long timestamp);

	protected final int getSampleInterval() {
		return mAttachedDevice.getSensor(mSensorType).getSampleInterval();
	}
	
	protected final int getSensorType() {
		return mSensorType;
	}

	protected final void outputData(String name, String type, Object outputData, int length, long timestamp) {
		for (DataFlowNode node: mOutputList) {
			node.inputData(name, type, outputData, length, timestamp);
		}
	}
	
	protected final void initializeGraph(int sensorType, Device attachedDevice) {
		mSensorType = sensorType;
		mAttachedDevice = attachedDevice;
		for (DataFlowNode node: mOutputList) {
			node.initializeGraph(sensorType, attachedDevice);
		}
	}
	
	public final boolean addOutputNode(DataFlowNode node) {
		return mOutputList.add(node);
	}
	
	public final boolean removeOutputNode(DataFlowNode node) {
		return mOutputList.remove(node);
	}
}
