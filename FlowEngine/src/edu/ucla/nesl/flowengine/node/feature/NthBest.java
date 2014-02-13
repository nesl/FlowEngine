package edu.ucla.nesl.flowengine.node.feature;

import edu.ucla.nesl.flowengine.DataType;
import edu.ucla.nesl.flowengine.InvalidDataReporter;
import edu.ucla.nesl.flowengine.node.DataFlowNode;

public class NthBest extends DataFlowNode {
	private static final String TAG = NthBest.class.getSimpleName(); 

	private String mType;
	private Object mSorted;

	@Override
	protected String processParentNodeName(String parentNodeName) {
		if (parentNodeName.contains("|Sort")) {
			return parentNodeName.replace("|Sort", "");
		}
		return parentNodeName;
	}

	public double getNthBest(int nth) {
		if (mType == null || mSorted == null) {
			InvalidDataReporter.report("Invalid mType(" + mType + ") or mSorted(" + mSorted + ")");
			return 0;
		}
		if (!mType.equals(DataType.INTEGER_ARRAY)) {
			throw new UnsupportedOperationException("Unsupported mType: " + mType);
		}

		int[] sorted = (int[])mSorted;

		if (sorted.length <= nth) {
			InvalidDataReporter.report("in " + TAG + ", sorted.length: " + sorted.length + ", nth: " + nth);
			return Double.NaN;
		}

		return sorted[sorted.length - nth];
	}
	
	@Override
	protected ResultData getParameterizedResult(
			Object parameter, 
			String name, 
			String type, 
			Object outputData, 
			int length, 
			long timestamp) 
	{
		int nth = (Integer)parameter;
		
		if (nth <= 0) {
			throw new IllegalArgumentException("Wrong Nth value: " + nth);
		}
		
		double result = getNthBest(nth);
		
		ResultData resultData = new ResultData(
				name.replace("Sort", String.format("NthBest%d", nth)), 
				DataType.DOUBLE, 
				result, 
				0, 
				timestamp);
		return resultData;
	}

	@Override
	protected void processInput(String name, String type, Object inputData, int length, long timestamp) {
		if (length <= 0) {
			InvalidDataReporter.report("in " + TAG + ": name: " + name + ", type: " + type + ", length: " + length);
			return;
		}
		if (!name.contains("Sort") || !(type.equals(DataType.INTEGER_ARRAY))) {
			throw new UnsupportedOperationException("Unsupported name: " + name + " or type: " + type);
		}
		
		mType = type;
		mSorted = inputData;
		
		output(name, null, null, -1, timestamp); // nulls and -1 will be replaced by getParametizedResult()
	}
}
