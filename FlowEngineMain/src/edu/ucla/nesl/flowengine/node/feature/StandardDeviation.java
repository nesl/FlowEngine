package edu.ucla.nesl.flowengine.node.feature;

import edu.ucla.nesl.flowengine.DebugHelper;
import edu.ucla.nesl.flowengine.InvalidDataReporter;
import edu.ucla.nesl.flowengine.node.DataFlowNode;

public class StandardDeviation extends DataFlowNode {
	private static final String TAG = StandardDeviation.class.getSimpleName();
	
	@Override
	public void inputData(String name, String type, Object inputData, int length, long timestamp) {
		if (name.contains("Variance")) {
			if (!type.equals("double")) {
				throw new UnsupportedOperationException("Unsupported type: " + type);
			}
			double stdev = Math.sqrt((Double)inputData);
			DebugHelper.log(TAG, "StandardDeviation: " + stdev);
			outputData(name + "StandardDeviation", "double", stdev, 0, timestamp);
		} else {
			throw new UnsupportedOperationException("Unsupported name: " + name);
		}
	}
}
