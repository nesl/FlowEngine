package edu.ucla.nesl.flowengine.node;

import edu.ucla.nesl.flowengine.DebugHelper;
import edu.ucla.nesl.flowengine.SensorType;
import edu.ucla.nesl.flowengine.node.classifier.Motion;


public class ActivityGraphControl extends DataFlowNode {
	private static final String TAG = ActivityGraphControl.class.getSimpleName();
	
	private Motion mMotionNode;
	private SeedNode mGpsSeed;

	public void setMotionNode(Motion motion) {
		mMotionNode = motion;
	}
	
	public void setGpsNode(SeedNode gps) {
		mGpsSeed = gps;
	}
	
	@Override
	protected void processInput(String name, String type, Object data, int length, long timestamp) {
		if (name.contains(SensorType.OUTDOOR_CONTEXT_NAME) && (Boolean)data) {
			// Stop motion classifier.
			DebugHelper.log(TAG, "Stopping motion classifier and starting GPS sensor..");
			mMotionNode.disable();
			mGpsSeed.startSensor();
		} else if (name.contains(SensorType.OUTDOOR_CONTEXT_NAME) && !(Boolean)data) {
			DebugHelper.log(TAG, "Starting motion classifier and stopping GPS sensor..");
			mMotionNode.enable();
			mGpsSeed.stopSensor();
		}
	}
}
