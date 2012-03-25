package edu.ucla.nesl.flowengine.node.feature;

import edu.ucla.nesl.flowengine.DebugHelper;
import edu.ucla.nesl.flowengine.InvalidDataReporter;
import edu.ucla.nesl.flowengine.node.DataFlowNode;

public class Variance extends DataFlowNode {
	private static final String TAG = Variance.class.getSimpleName();
	
	private boolean mIsMeanNew = false;
	private double mMean;
	
	private String mName;
	private String mType;
	private long mTimestamp;
	private Object mData = null;

	@Override
	protected String processParentNodeName(String parentNodeName) {
		if (parentNodeName.contains("|Mean")) {
			return parentNodeName.replace("|Mean", "");
		} else if (parentNodeName.contains("|Buffer")) {
			return parentNodeName.split("\\|Buffer")[0];
		}
		return parentNodeName;
	}
	
	private double calculateVariance() {
		double sum = 0.0;
		double var;
		if (mType.equals("int[]")) {
			for (int value: (int[])mData) {
				sum += Math.pow(value - mMean, 2.0);
			}
			var = sum / ((int[])mData).length;
		} else if (mType.equals("double[]")) {
			for (double value: (double[])mData) {
				sum += Math.pow(value - mMean, 2.0);
			}
			var = sum / ((double[])mData).length;
		} else {
			throw new UnsupportedOperationException("Unsuported type: " + mType);
		}
		
		DebugHelper.log(TAG, mName + "Variance: " + var);

		return var;
	}
	
	@Override
	protected void processInput(String name, String type, Object inputData, int length, long timestamp) {
		if (name.contains("Mean")) {
			if (!type.equals("double")) {
				throw new UnsupportedOperationException("Unsupported type: " + type);
			}
			mMean = (Double)inputData;
			mIsMeanNew = true;
		} else {
			if (length <= 0) {
				InvalidDataReporter.report("in " + TAG + ": name: " + name + ", type: " + type + ", length: " + length);
				return;
			}
			if (!type.equals("double[]") && !type.equals("int[]")) {
				throw new UnsupportedOperationException("Unsupported type: " + type);
			}
			mData = inputData;
			mType = type;
			mName = name;
			mTimestamp = timestamp;
		} 
		
		if (mIsMeanNew && mData != null) {
			double var = calculateVariance();
			output(mName + "Variance", "double", var, 0, mTimestamp);
			mIsMeanNew = false;
			mData = null;
		}
	}
}
