package edu.ucla.nesl.flowengine.node.feature;

import edu.ucla.nesl.flowengine.DataType;
import edu.ucla.nesl.flowengine.DebugHelper;
import edu.ucla.nesl.flowengine.InvalidDataReporter;
import edu.ucla.nesl.flowengine.node.DataFlowNode;

public class Median extends DataFlowNode {
	private static final String TAG = Median.class.getSimpleName();

	@Override
	protected String processParentNodeName(String parentNodeName) {
		if (parentNodeName.contains("|Sort")) {
			return parentNodeName.replace("|Sort", "");
		} else if (parentNodeName.contains("|Buffer")) {
			return parentNodeName.split("\\|Buffer")[0];
		}
		return parentNodeName;
	}

	private double calculateMedian(String type, Object inputData, int length) {
		double result;

		if (type.equals(DataType.INTEGER_ARRAY)) {
			int[] data = (int[])inputData;
			if (length % 2 == 1)
				result = data[(length+1)/2-1];
			else
			{
				double lower = data[length/2-1];
				double upper = data[length/2];
				result = (lower + upper) / 2.0;
			}
		} else if (type.equals(DataType.DOUBLE_ARRAY)) {
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
	protected void processInput(String name, String type, Object inputData, int length, long timestamp) {
		if (length <= 0) {
			InvalidDataReporter.report("in " + TAG + ": name: " + name + ", type: " + type + ", length: " + length);
			return;
		}
		if (!name.contains("Sort")) {
			throw new UnsupportedOperationException("Unsupported name: " + name);
		}

		double result = calculateMedian(type, inputData, length);
		
		output(name.replace("Sort", "Median"), DataType.DOUBLE, result, 0, timestamp);
	}
}
