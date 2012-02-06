package edu.ucla.nesl.flowengine.node.feature;

import edu.ucla.nesl.flowengine.DebugHelper;
import edu.ucla.nesl.flowengine.InvalidDataReporter;
import edu.ucla.nesl.flowengine.node.DataFlowNode;

public class Percentile extends DataFlowNode {
	private static final String TAG = Percentile.class.getSimpleName();
	
	private String mType;
	private Object mSorted;
	private double mPercentile = -1;

	public Percentile() {
	}
	
	public Percentile(double percentile) {
		mPercentile = percentile;
	}
	
	public double getPercentile(double percentile) {
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
	public void inputData(String name, String type, Object inputData, int length) {
		if (length <= 0) {
			//throw new IllegalArgumentException("Invalid length value: " + length);
			InvalidDataReporter.report("length: " + length + " in " + TAG);
			return;
		}
		if (!name.contains("Sorted")) {
			throw new UnsupportedOperationException("Unsupported name: " + name);
		}
		
		mType = type;
		mSorted = inputData;
		
		if (mPercentile > 0.0) {
			double result = getPercentile(mPercentile);
			outputData(name.replace("Sorted", String.format("Percentile%.1f", mPercentile)), "double", result, 0);
		}
	}
}
