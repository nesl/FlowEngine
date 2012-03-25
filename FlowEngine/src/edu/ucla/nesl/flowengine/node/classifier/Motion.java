package edu.ucla.nesl.flowengine.node.classifier;

import edu.ucla.nesl.flowengine.DebugHelper;
import edu.ucla.nesl.flowengine.SensorType;
import edu.ucla.nesl.flowengine.node.DataFlowNode;

public class Motion extends DataFlowNode {
	private static final String TAG = Motion.class.getSimpleName();
	private double[] mFFT1_3;
	private double[] mFFT4_5;

	private boolean classifyMotion() {
		double fftSum = mFFT1_3[0] + mFFT1_3[1] + mFFT1_3[2] + mFFT4_5[0] + mFFT4_5[1];
		if (fftSum > 5654) {
			DebugHelper.log(TAG, "Motion detected.");
			return true;
		} 
		DebugHelper.log(TAG, "No motion");
		return false;
	}
	
	@Override
	protected void processInput(String name, String type, Object data, int length, long timestamp) {
		if (name.contains("FFT1.0-3.0-1.0")) {
			mFFT1_3 = (double[])data;
		} else if (name.contains("FFT4.0-5.0-1.0")) {
			mFFT4_5 = (double[])data;
		}
		
		if (mFFT1_3 != null && mFFT4_5 != null) {
			boolean isMotion = classifyMotion();
			mFFT1_3 = null;
			mFFT4_5 = null;
			output(SensorType.MOTION_CONTEXT_NAME, "boolean", isMotion, 0, timestamp);
		}
	}
}
