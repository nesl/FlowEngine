package edu.ucla.nesl.flowengine.node.feature;

import edu.ucla.nesl.flowengine.DebugHelper;
import edu.ucla.nesl.flowengine.InvalidDataReporter;
import edu.ucla.nesl.flowengine.node.DataFlowNode;

public class RootMeanSquare extends DataFlowNode {
	private static final String TAG = RootMeanSquare.class.getSimpleName();
	
	public String mType = "double";
	
	@Override
	public void inputData(String name, String type, Object inputData, int length, long timestamp) {
		if (length <= 0) {
			InvalidDataReporter.report("in " + TAG + ": name: " + name + ", type: " + type + ", length: " + length);
			return;
		}
		if (!type.equals("double[]")) {
			throw new UnsupportedOperationException("Unsupported type: " + type);
		}

		double[] data = (double[])inputData;
		double totalForce = 0.0;

		//Log.d(TAG, "inputData:" + Double.toString(data[0]) + ", " + Double.toString(data[1]) + ", " + Double.toString(data[2]));
		
		for (double value: data) { 
			totalForce += Math.pow(value, 2.0);
		}
		totalForce = Math.sqrt(totalForce);
		
		//DebugHelper.log(TAG, "RMS: " + totalForce);
		
		outputData(name + "RMS", "double", totalForce, 0, timestamp);
	}
}
