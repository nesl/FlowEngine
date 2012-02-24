package edu.ucla.nesl.flowengine.node;

import edu.ucla.nesl.flowengine.DebugHelper;
import edu.ucla.nesl.flowengine.node.classifier.MotionClassifier;


public class ActivityGraphControl extends DataFlowNode {
	private static final String TAG = ActivityGraphControl.class.getSimpleName();
	
	private MotionClassifier mMotionNode;
	private SeedNode mGpsSeed;
	
	public ActivityGraphControl(MotionClassifier motion, SeedNode gpsSeed) {
		mMotionNode = motion;
		mGpsSeed = gpsSeed;
	}
	
	@Override
	protected void processInput(String name, String type, Object data, int length, long timestamp) {
		if (name.contains("Outdoor") && (Boolean)data) {
			// Stop motion classifier.
			DebugHelper.log(TAG, "Stopping motion classifier and starting GPS sensor..");
			mMotionNode.disable();
			mGpsSeed.startSensor();
		} else if (name.contains("Indoor") && (Boolean)data) {
			DebugHelper.log(TAG, "Starting motion classifier and stopping GPS sensor..");
			mMotionNode.enable();
			mGpsSeed.stopSensor();
		}
	}
}
