package edu.ucla.nesl.flowengine.node.feature;

import edu.ucla.nesl.flowengine.DebugHelper;
import edu.ucla.nesl.flowengine.InvalidDataReporter;
import edu.ucla.nesl.flowengine.node.DataFlowNode;

public class RootMeanSquare extends DataFlowNode {
	private static final String TAG = RootMeanSquare.class.getSimpleName();
	
	public String mType = "double";
	
	private double calculateRootMeanSquare(double[] data) {
		double rms = 0.0;

		//Log.d(TAG, "inputData:" + Double.toString(data[0]) + ", " + Double.toString(data[1]) + ", " + Double.toString(data[2]));
		
		for (double value: data) { 
			rms += Math.pow(value, 2.0);
		}
		rms = Math.sqrt(rms);
		
		//DebugHelper.log(TAG, "RMS: " + totalForce);

		return rms;
	}
	
	@Override
	public void input(String name, String type, Object inputData, int length, long timestamp) {
		if (length <= 0) {
			InvalidDataReporter.report("in " + TAG + ": name: " + name + ", type: " + type + ", length: " + length);
			return;
		}
		if (!type.equals("double[]")) {
			throw new UnsupportedOperationException("Unsupported type: " + type);
		}

		double rms = calculateRootMeanSquare((double[])inputData);
		
		output(name + "RMS", "double", rms, 0, timestamp);
	}
}
