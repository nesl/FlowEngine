package edu.ucla.nesl.flowengine.node.feature;

import edu.ucla.nesl.flowengine.DataType;
import edu.ucla.nesl.flowengine.DebugHelper;
import edu.ucla.nesl.flowengine.InvalidDataReporter;
import edu.ucla.nesl.flowengine.node.DataFlowNode;

public class Percentile extends DataFlowNode {
	private static final String TAG = Percentile.class.getSimpleName();
	
	private String mType;
	private Object mSorted;

	@Override
	protected String processParentNodeName(String parentNodeName) {
		if (parentNodeName.contains("|Sort")) {
			return parentNodeName.replace("|Sort", "");
		}
		return parentNodeName;
	}

	@Override 
	protected String getParameterName(Object parameter) {
		return String.format("%.1f", (Double)parameter);
	}
	
	@Override
	public ResultData processPull(Object parameter) {
		ResultData[] results = pull();
		
		if (results == null || results[0] == null) {
			throw new UnsupportedOperationException("No result from pulling.");
		}
		
		mType = results[0].type;
		mSorted = results[0].data;
		String name = results[0].name;
		long timestamp = results[0].timestamp;
		double percentile = (Double)parameter;
		double percentileResult = getPercentile(percentile);
		
		return new ResultData(
				name.replace("Sort", String.format("Percentile%.1f", percentile)), 
				DataType.DOUBLE, 
				percentileResult, 
				0, 
				timestamp);
	}
	
	private double getPercentile(double percentile) {
		if (mType == null || mSorted == null) {
			InvalidDataReporter.report("Invalid mType(" + mType + ") or mSorted(" + mSorted + ")");
			return 0.0;
		}
		
		double lower;
		double upper;
		double dif;
		
		if (mType.equals(DataType.INTEGER_ARRAY)) {
			int[] sorted = (int[])mSorted;
			if (sorted.length == 1) {
				return sorted[0];
			}
			double n = (double)sorted.length;
			double pos = percentile * (n + 1) / 100;
			double fpos = Math.floor(pos);
			int intPos = (int) fpos;
			dif = pos - fpos;
			if (pos < 1) {
				return sorted[0];
			}
			if (pos >= n) {
				return sorted[sorted.length - 1];
			}
			lower = sorted[intPos - 1];
			upper = sorted[intPos];
		} else if (mType.equals(DataType.DOUBLE_ARRAY)) {
			double[] sorted = (double[])mSorted;
			if (sorted.length == 1) {
				return sorted[0];
			}
			double n = (double)sorted.length;
			double pos = percentile * (n + 1) / 100;
			double fpos = Math.floor(pos);
			int intPos = (int) fpos;
			dif = pos - fpos;
			if (pos < 1) {
				return sorted[0];
			}
			if (pos >= n) {
				return sorted[sorted.length - 1];
			}
			lower = sorted[intPos - 1];
			upper = sorted[intPos];
		} else {
			throw new UnsupportedOperationException("Unsupported data type: " + mType);
		}

		double result = lower + dif * (upper - lower);
		
		DebugHelper.log(TAG, String.format("Percentile(%.1f): %.2f", percentile, result));

		return result;
	}
	
	@Override
	protected ResultData getParameterizedResult(Object parameter, String name, String type, Object outputData, int length, long timestamp) {
		double percentile = (Double)parameter;
		
		if (percentile < 0.0) {
			throw new IllegalArgumentException("Wrong percentile value: " + percentile);
		}
		double result = getPercentile(percentile);
		
		ResultData resultData = new ResultData(name.replace("Sort", String.format("Percentile%.1f", percentile)), DataType.DOUBLE, result, 0, timestamp);
		return resultData;
	}
	
	@Override
	protected void processInput(String name, String type, Object inputData, int length, long timestamp) {
		if (length <= 0) {
			InvalidDataReporter.report("in " + TAG + ": name: " + name + ", type: " + type + ", length: " + length);
			return;
		}
		if (!name.contains("Sort") || !(type.equals(DataType.INTEGER_ARRAY) || type.equals(DataType.DOUBLE_ARRAY))) {
			throw new UnsupportedOperationException("Unsupported name: " + name + " or type: " + type);
		}
		
		mType = type;
		mSorted = inputData;
		
		output(name, null, null, -1, timestamp); // nulls and -1 will be replaced by getParametizedResult()
	}
}
