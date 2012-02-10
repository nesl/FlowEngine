package edu.ucla.nesl.flowengine.node.feature;

import edu.ucla.nesl.flowengine.DebugHelper;
import edu.ucla.nesl.flowengine.InvalidDataReporter;
import edu.ucla.nesl.flowengine.node.DataFlowNode;

public class QuartileDeviation extends DataFlowNode {
	private static final String TAG = QuartileDeviation.class.getSimpleName();
	
	Percentile mPercentile;
	
	public QuartileDeviation(Percentile percentile) {
		mPercentile = percentile;
	}
	
	@Override
	public void inputData(String name, String type, Object inputData, int length, long timestamp) {
		if (length <= 0) {
			InvalidDataReporter.report("in " + TAG + ": name: " + name + ", type: " + type + ", length: " + length);
			return;
		}
		double third = mPercentile.getPercentile(75.0);
		double first = mPercentile.getPercentile(25.0);
		double qd = (third - first) / 2.0; 
		DebugHelper.log(TAG, "QD: " + qd);
		outputData(name + "QuartileDeviation", "double", qd, 0, timestamp);
	}
}
