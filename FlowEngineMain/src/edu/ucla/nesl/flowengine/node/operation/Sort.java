package edu.ucla.nesl.flowengine.node.operation;

import java.util.Arrays;

import edu.ucla.nesl.flowengine.InvalidDataReporter;
import edu.ucla.nesl.flowengine.node.DataFlowNode;

public class Sort extends DataFlowNode {
	private static final String TAG = Sort.class.getSimpleName();

	private Object sort(String type, Object inputData, int length) {
		Object sorted;
		if (type.equals("int[]")) {
			sorted = new int[length];
			System.arraycopy(inputData, 0, sorted, 0, length);
			Arrays.sort((int[])sorted);
		} else if (type.equals("double[]")) {
			sorted = new double[length];
			System.arraycopy(inputData, 0, sorted, 0, length);
			Arrays.sort((double[])sorted);
		} else {
			throw new UnsupportedOperationException("Unsupported type: " + type);
		}
		return sorted;
	}
	
	@Override
	public void input(String name, String type, Object inputData, int length, long timestamp) {
		if (length <= 0) {
			InvalidDataReporter.report("in " + TAG + ": name: " + name + ", type: " + type + ", length: " + length);
			return;
		}
		if (!type.equals("int[]") && !type.equals("double[]")) {
			throw new UnsupportedOperationException("Unsupported type: " + type);
		}
		
		Object sorted = sort(type, inputData, length);
		
		output(name + "Sorted", type, sorted, length, timestamp);
	}
}
