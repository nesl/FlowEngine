package edu.ucla.nesl.flowengine.node.feature;

import edu.ucla.nesl.flowengine.DataType;
import edu.ucla.nesl.flowengine.DebugHelper;
import edu.ucla.nesl.flowengine.InvalidDataReporter;
import edu.ucla.nesl.flowengine.node.DataFlowNode;

public class BandPower extends DataFlowNode {
	private static final String TAG = BandPower.class.getSimpleName();
	
	private double mRangeLower;
	private double mRangeUpper;
	
	@Override
	protected String processParentNodeName(String parentNodeName) {
		return parentNodeName;
	}

	public BandPower(String parameterizedSimpleNodeName, double lower, double upper) {
		super(parameterizedSimpleNodeName);
		mRangeLower = lower;
		mRangeUpper = upper;
	}

	private double getBandPower(double[][] data) {
		double [][] fP = (double[][])data;
		double[] P = fP[0];	
		double[] f = fP[1];
		double Padd = 0.0;

		for (int k=0;k<f.length;k++){
		    if (f[k] >= mRangeLower && f[k] <= mRangeUpper) {
		        Padd+=P[k];
		    }
		}
		
		DebugHelper.log(TAG, "Padd: " + Padd);
		
		return Padd;
	}
	
	@Override
	protected void processInput(String name, String type, Object inputData, int length, long timestamp) {
		if (length <= 0) {
			InvalidDataReporter.report("in " + TAG + ": name: " + name + ", type: " + type + ", length: " + length);
			return;
		}
		if (!type.equals("double[][]")) {
			throw new UnsupportedOperationException("Unsupported type: " + type);
		}

		double result = getBandPower((double[][])inputData);
		
		output(String.format(name + "BandPower%.1f-%.1f", mRangeLower, mRangeUpper), DataType.DOUBLE, result, 0, timestamp);
	}
}
