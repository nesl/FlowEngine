package edu.ucla.nesl.flowengine.node.feature;

import edu.ucla.nesl.flowengine.DebugHelper;
import edu.ucla.nesl.flowengine.InvalidDataReporter;
import edu.ucla.nesl.flowengine.node.DataFlowNode;

public class QuartileDeviation extends DataFlowNode {
	private static final String TAG = QuartileDeviation.class.getSimpleName();

	double mPercentile75;
	double mPercentile25;
	boolean mIsNewPercentile75 = false;
	boolean mIsNewPercentile25 = false;
	String mName;
	
	public QuartileDeviation() {
	}

	private double calculateQuartileDeviation() {
		double qd = (mPercentile75 - mPercentile25) / 2.0;
		
		DebugHelper.log(TAG, "QD: " + qd + ", mName:" + mName);
		
		return qd;
	}
	
	@Override
	public void input(String name, String type, Object inputData, int length, long timestamp) {
		if (!type.equals("double")) {
			throw new UnsupportedOperationException("Unsupported type: " + type);
		}
		if (name.contains("Percentile75.0")) {
			mIsNewPercentile75 = true;
			mPercentile75 = (Double)inputData;
			mName = name.replace("Percentile75.0", "");
		} else if (name.contains("Percentile25.0")) {
			mIsNewPercentile25 = true;
			mPercentile25 = (Double)inputData;
			mName = name.replace("Percentile25.0", "");
		}
		
		if (mIsNewPercentile25 && mIsNewPercentile75) {
			double qd = calculateQuartileDeviation();
			output(mName + "QuartileDeviation", "double", qd, 0, timestamp);
			mIsNewPercentile25 = false;
			mIsNewPercentile75 = false;
		}
	}
}
