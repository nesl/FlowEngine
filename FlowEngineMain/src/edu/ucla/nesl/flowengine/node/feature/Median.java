package edu.ucla.nesl.flowengine.node.feature;

import edu.ucla.nesl.flowengine.DebugHelper;
import edu.ucla.nesl.flowengine.InvalidDataReporter;
import edu.ucla.nesl.flowengine.node.DataFlowNode;

public class Median extends DataFlowNode {
	private static final String TAG = Median.class.getSimpleName();

	@Override
	public void inputData(String name, String type, Object inputData, int length) {
		if (length <= 0) {
			//throw new IllegalArgumentException("length: " + length);
			InvalidDataReporter.report("length: " + length + " in " + TAG);
			return;
		}
		
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
		
		outputData(name.replace("Sorted", "Median"), "double", result, 0);
	}
}
