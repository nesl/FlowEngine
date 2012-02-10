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

	private double calculateQuartileDeviation() {
		double third = (Double)mPercentile.pull(75.0);
		double first = (Double)mPercentile.pull(25.0);
		double qd = (third - first) / 2.0;
		
		DebugHelper.log(TAG, "QD: " + qd);
		
		return qd;
	}
	
	@Override
	public void input(String name, String type, Object inputData, int length, long timestamp) {
		if (length <= 0) {
			InvalidDataReporter.report("in " + TAG + ": name: " + name + ", type: " + type + ", length: " + length);
			return;
		}
		
		double qd = calculateQuartileDeviation();
		
		output(name + "QuartileDeviation", "double", qd, 0, timestamp);
	}
}
