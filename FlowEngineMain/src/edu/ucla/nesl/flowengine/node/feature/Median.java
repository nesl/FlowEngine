package edu.ucla.nesl.flowengine.node.feature;

import edu.ucla.nesl.flowengine.DebugHelper;
import edu.ucla.nesl.flowengine.InvalidDataReporter;
import edu.ucla.nesl.flowengine.node.DataFlowNode;

public class Median extends DataFlowNode {
	private static final String TAG = Median.class.getSimpleName();

	private double calculateMedian(String type, Object inputData, int length) {
		double result;

		if (type.equals("int[]")) {
			int[] data = (int[])inputData;
			if (length % 2 == 1)
				result = data[(length+1)/2-1];
			else
			{
				double lower = data[length/2-1];
				double upper = data[length/2];
				result = (lower + upper) / 2.0;
			}
		} else if (type.equals("double[]")) {
			double[] data = (double[])inputData;
			if (length % 2 == 1)
				result = data[(length+1)/2-1];
			else
			{
				double lower = data[length/2-1];
				double upper = data[length/2];
				result = (lower + upper) / 2.0;
			}
		} else {
			throw new UnsupportedOperationException("Unsupported type: " + type);
		}
		
		DebugHelper.log(TAG, "Median: " + result);
		
		return result;
	}
	
	@Override
	public void input(String name, String type, Object inputData, int length, long timestamp) {
		if (length <= 0) {
			InvalidDataReporter.report("in " + TAG + ": name: " + name + ", type: " + type + ", length: " + length);
			return;
		}
		if (!name.contains("Sorted")) {
			throw new UnsupportedOperationException("Unsupported name: " + name);
		}

		double result = calculateMedian(type, inputData, length);
		
		output(name.replace("Sorted", "Median"), "double", result, 0, timestamp);
	}
}
