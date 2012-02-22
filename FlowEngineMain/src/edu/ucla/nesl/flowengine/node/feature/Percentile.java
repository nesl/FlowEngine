package edu.ucla.nesl.flowengine.node.feature;

import edu.ucla.nesl.flowengine.DebugHelper;
import edu.ucla.nesl.flowengine.InvalidDataReporter;
import edu.ucla.nesl.flowengine.node.DataFlowNode;

public class Percentile extends DataFlowNode {
	private static final String TAG = Percentile.class.getSimpleName();
	
	private String mType;
	private Object mSorted;

	@Override
	public Object pull(Object percentile) {
		return getPercentile((Double)percentile);
	}
	
	private double getPercentile(double percentile) {
		if (mType == null || mSorted == null) {
			InvalidDataReporter.report("Invalid mType(" + mType + ") or mSorted(" + mSorted + ")");
			return 0.0;
		}
		
		double lower;
		double upper;
		double dif;
		
		if (mType.equals("int[]")) {
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
		} else if (mType.equals("double[]")) {
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
		
		ResultData resultData = new ResultData(name.replace("Sorted", String.format("Percentile%.1f", percentile)), "double", result, 0, timestamp);
		return resultData;
	}
	
	@Override
	public void input(String name, String type, Object inputData, int length, long timestamp) {
		if (length <= 0) {
			InvalidDataReporter.report("in " + TAG + ": name: " + name + ", type: " + type + ", length: " + length);
			return;
		}
		if (!name.contains("Sorted") || !(type.equals("int[]") || type.equals("double[]"))) {
			throw new UnsupportedOperationException("Unsupported name: " + name + " or type: " + type);
		}
		
		mType = type;
		mSorted = inputData;
		
		output(name, null, null, -1, timestamp); // nulls and -1 will be replaced by getParametizedResult()
	}
}
