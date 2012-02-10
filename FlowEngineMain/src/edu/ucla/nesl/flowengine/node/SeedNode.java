package edu.ucla.nesl.flowengine.node;

import edu.ucla.nesl.flowengine.Device;


public class SeedNode extends DataFlowNode {
	private static final String TAG = SeedNode.class.getSimpleName();
	
	public SeedNode(int sensorType, Device attachedDevice) {
		mSensorType = sensorType;
		mAttachedDevice = attachedDevice;
	}

	public void initializeGraph() {
		super.initializeGraph(mSensorType, mAttachedDevice);
	}
	
	@Override
	public void inputData(String name, String type, Object inputData, int length, long timestamp) {
		outputData(name, type, inputData, length, timestamp);
	}
}
