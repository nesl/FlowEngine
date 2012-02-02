package edu.ucla.nesl.flowengine.node.feature;

import android.util.Log;
import edu.ucla.nesl.flowengine.node.DataFlowNode;

public class RootMeanSquare extends DataFlowNode {
	private static final String TAG = RootMeanSquare.class.getSimpleName();
	
	public String mName = "RootMeanSquare";
	public String mType = "double";
	
	@Override
	public void inputData(String name, String type, Object inputData, int length) {
		double[] data = (double[])inputData;
		double totalForce = 0.0;

		//Log.d(TAG, "inputData:" + Double.toString(data[0]) + ", " + Double.toString(data[1]) + ", " + Double.toString(data[2]));
		
		for (double value: data) { 
			totalForce += Math.pow(value, 2.0);
		}
		totalForce = Math.sqrt(totalForce);
		outputData("RootMeanSquare", "double", totalForce, 0);
	}
}
