package edu.ucla.nesl.flowengine.node.classifier;

import java.util.LinkedList;

import edu.ucla.nesl.flowengine.DebugHelper;
import edu.ucla.nesl.flowengine.SensorType;
import edu.ucla.nesl.flowengine.node.DataFlowNode;

public class Outdoor extends DataFlowNode {
	private static final String TAG = Outdoor.class.getSimpleName();

	// If Activity classifier outputs "still" for this duration, then it's INDOOR.
	private static final int INDOOR_STILL_THRESHOLD = 5 * 60 * 1000; // 5 minutes
	//private static final int INDOOR_STILL_THRESHOLD = 5 * 1000;

	// If Motion classifier outputs THRESHOLD times within WINDOW, then OUTDOOR. 
	private static final int WINDOW = 90;
	//private static final int WINDOW = 5; 
	private static final int THRESHOLD = WINDOW / 2;
	
	private static final int NO_RESULT = 0;
	private static final int OUTDOOR = 1;
	private static final int INDOOR = 2;
	
	private LinkedList<Boolean> mMotionHistory = new LinkedList<Boolean>();
	private int mMotionScore = 0;
	private long mStillStartTime = -1;
	private boolean mIsLastIndoor = false;

	private int determineOutdoor(boolean isMotion) {
		if (mMotionHistory.size() >= WINDOW) {
			boolean oldestMotion = mMotionHistory.removeLast();
			if (oldestMotion) {
				mMotionScore--;
			}
		}
		
		mMotionHistory.addFirst(isMotion);
		if (isMotion) {
			mMotionScore++;
		}
		
		if (mMotionScore > THRESHOLD) {
			DebugHelper.log(TAG, "Outdoor");
			mMotionHistory.clear();
			mMotionScore = 0;
			return OUTDOOR;
		}
		return NO_RESULT;
	}

	private int determineIndoor(int activity) {
		if (activity == SensorType.ACTIVITY_STILL) {
			if (mStillStartTime == -1) {
				mStillStartTime = System.currentTimeMillis();
			} else if (System.currentTimeMillis() - mStillStartTime > INDOOR_STILL_THRESHOLD) {
				if (!mIsLastIndoor) {
					mIsLastIndoor = true;
					return INDOOR;
				}
			}
		} else {
			mStillStartTime = -1;
			mIsLastIndoor = false;
		}
		return NO_RESULT;
	}
	
	@Override
	protected void processInput(String name, String type, Object data, int length, long timestamp) {
		if (name.contains(SensorType.MOTION_CONTEXT_NAME)) {
			int result = determineOutdoor((Boolean)data);
			if (result == OUTDOOR) {
				output(SensorType.OUTDOOR_CONTEXT_NAME, "boolean", true, 0, timestamp);
			}
		} else if (name.contains(SensorType.ACTIVITY_CONTEXT_NAME)) {
			int result = determineIndoor((Integer)data);
			if (result == INDOOR) {
				output(SensorType.OUTDOOR_CONTEXT_NAME, "boolean", false, 0, timestamp);
			}
		}
	}
}
