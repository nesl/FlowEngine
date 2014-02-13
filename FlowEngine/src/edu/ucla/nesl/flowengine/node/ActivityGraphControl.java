package edu.ucla.nesl.flowengine.node;

import java.util.List;

import edu.ucla.nesl.flowengine.DebugHelper;
import edu.ucla.nesl.flowengine.SensorType;
import edu.ucla.nesl.flowengine.node.classifier.Motion;


public class ActivityGraphControl extends DataFlowNode {
	private static final String TAG = ActivityGraphControl.class.getSimpleName();
	
	public Motion mMotionNode;
	public SeedNode mGpsSeed;

	public ActivityGraphControl(String parameterizedSimpleNodeName, SeedNode gpsSeedNode, Motion motionNode) {
		super(parameterizedSimpleNodeName);
		mMotionNode = motionNode;
		mGpsSeed = gpsSeedNode;
	}
	
	/*public void setMotionNode(Motion motion) {
		mMotionNode = motion;
	}
	
	public void setGpsNode(SeedNode gps) {
		mGpsSeed = gps;
	}*/
	
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
			if (isGpsOnlyParentActivity()) {
				mGpsSeed.stopSensor();
			}
		}
	}
	
	private boolean isGpsOnlyParentActivity() {
		List<DataFlowNode> gpsChildren = mGpsSeed.getDefaultChildrenList();
		if (gpsChildren.size() == 1 && gpsChildren.get(0).getName().contains("Activity")) {
			return true;
		}
		return false;
	}
}
