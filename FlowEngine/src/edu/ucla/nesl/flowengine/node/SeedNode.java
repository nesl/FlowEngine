package edu.ucla.nesl.flowengine.node;

import edu.ucla.nesl.flowengine.DebugHelper;
import edu.ucla.nesl.flowengine.Device;


public class SeedNode extends DataFlowNode {
	private static final String TAG = SeedNode.class.getSimpleName();
	
	public SeedNode(int sensorType, Device attachedDevice) {
		mSensorType = sensorType;
		mAttachedDevice = attachedDevice;
	}

	public Device getAttachedDevice() {
		return mAttachedDevice;
	}
	
	public void initializeGraph() {
		super.initializeGraph(mSensorType, mAttachedDevice);
	}
	
	@Override
	public void input(String name, String type, Object inputData, int length, long timestamp) {
		DebugHelper.log(TAG, "name: " + name + ", type: " + type + ", length: " + length + ", timestamp: " + timestamp);
		output(name, type, inputData, length, timestamp);
	}
}
