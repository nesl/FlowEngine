package edu.ucla.nesl.flowengine.node.feature;

import edu.ucla.nesl.flowengine.DebugHelper;
import edu.ucla.nesl.flowengine.node.DataFlowNode;

public class Variance extends DataFlowNode {
	private static final String TAG = Variance.class.getSimpleName();
	
	private boolean mIsMeanNew = false;
	private double mMean;
	
	private String mName;
	private String mType;
	private Object mData = null;

	@Override
	public void inputData(String name, String type, Object inputData, int length) {
		if (name.contains("Mean")) {
			mMean = (Double)inputData;
			mIsMeanNew = true;
		} else {
			mData = inputData;
			mType = type;
			mName = name;
		} 
		
		if (mIsMeanNew && mData != null) {
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
			
			DebugHelper.log(TAG, "Variance: " + var);
			
			outputData(mName + "Variance", "double", var, 0);
			
			mIsMeanNew = false;
			mData = null;
		}
	}
}
