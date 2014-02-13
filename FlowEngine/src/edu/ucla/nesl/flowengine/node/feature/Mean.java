package edu.ucla.nesl.flowengine.node.feature;

import edu.ucla.nesl.flowengine.DataType;
import edu.ucla.nesl.flowengine.DebugHelper;
import edu.ucla.nesl.flowengine.InvalidDataReporter;
import edu.ucla.nesl.flowengine.node.DataFlowNode;

public class Mean extends DataFlowNode {
	private static final String TAG = Mean.class.getSimpleName();

	@Override
	protected String processParentNodeName(String parentNodeName) {
		if (parentNodeName.contains("|Buffer")) {
			return parentNodeName.split("\\|Buffer")[0];
		}
		return parentNodeName;
	}
	
	private double calculateMean(String type, Object inputData, int length) {
		double mean = 0;
		if (type.equals(DataType.INTEGER_ARRAY)) {
			int[] data = (int[])inputData;
			for (int value: data) {
				mean += value;
			}
		} else if (type.equals(DataType.DOUBLE_ARRAY)) {
			double[] data = (double[])inputData;
			for (double value: data) {
				mean += value;
			}
		} else if (type.equals(DataType.FLOAT_ARRAY)) {
			float[] data = (float[])inputData;
			for (float value: data) {
				mean += value;
			}
		} else if (type.equals(DataType.LONG_ARRAY)) {
			long[] data = (long[])inputData;
			for (long value: data) {
				mean += value;
			}
		} else {
			throw new UnsupportedOperationException("Unsupported type: " + type);
		}
		mean /= length;
		
		return mean;
	}
	
	@Override
	protected void processInput(String name, String type, Object inputData, int length, long timestamp) {
		if (length <= 0) {
			InvalidDataReporter.report("in " + TAG + ": name: " + name + ", type: " + type + ", length: " + length);
			return;
		}

		double mean = calculateMean(type, inputData, length);

		DebugHelper.log(TAG, name + "Mean: " + mean);

		output(name + "Mean", DataType.DOUBLE, mean, 0, timestamp);
	}
}
