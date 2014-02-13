package edu.ucla.nesl.flowengine.node.feature;

import edu.ucla.nesl.flowengine.DataType;
import edu.ucla.nesl.flowengine.InvalidDataReporter;
import edu.ucla.nesl.flowengine.node.DataFlowNode;

public class RootMeanSquare extends DataFlowNode {
	private static final String TAG = RootMeanSquare.class.getSimpleName();
	
	private double mFactor = 1.0;
	private double mScale = 1.0;
	
	@Override
	protected String processParentNodeName(String parentNodeName) {
		return parentNodeName;
	}
	
	public RootMeanSquare(String parameterizedSimpleNodeName, double factor, double scale) {
		super(parameterizedSimpleNodeName);
		mFactor = factor;
		mScale = scale;
	}
	
	private double calculateRootMeanSquare(double[] data) {
		double rms = 0.0;

		for (double value: data) { 
			rms += Math.pow(value/mFactor, 2.0);
		}
		
		rms = Math.sqrt(rms) * mScale;

		return rms;
	}
	
	@Override
	protected void processInput(String name, String type, Object inputData, int length, long timestamp) {
		if (length <= 0) {
			InvalidDataReporter.report("in " + TAG + ": name: " + name + ", type: " + type + ", length: " + length);
			return;
		}
		if (!type.equals(DataType.DOUBLE_ARRAY)) {
			throw new UnsupportedOperationException("Unsupported type: " + type);
		}

		double rms = calculateRootMeanSquare((double[])inputData);
		//DebugHelper.log(TAG, "RMS: " + rms);
		output(name + "RMS", DataType.DOUBLE, rms, 0, timestamp);
	}
}
