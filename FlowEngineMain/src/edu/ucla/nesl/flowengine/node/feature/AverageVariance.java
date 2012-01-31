package edu.ucla.nesl.flowengine.node.feature;

import android.util.Log;
import edu.ucla.nesl.flowengine.node.DataFlowNode;

public class AverageVariance extends DataFlowNode {
	private static final String TAG = AverageVariance.class.getSimpleName();
	
	public void inputData(String type, Object inputData) {
		double sum = 0.0, avg, var;
		double[] data = (double[])inputData;
		
		for (double value: data)
		{
			sum += value;
		}
		avg = sum / data.length;
		
		sum = 0.0;
		for (double value: data)
		{
			sum += Math.pow(value - avg,2.0);
		}
		var = sum / data.length;

		Log.d(TAG, String.format("avg=%.2f, var=%.2f", avg, var));
		
		double[] avgVar = new double[2];
		avgVar[0] = avg;
		avgVar[1] = var;
		
		outputData("AverageVariance", avgVar);
	}
}
