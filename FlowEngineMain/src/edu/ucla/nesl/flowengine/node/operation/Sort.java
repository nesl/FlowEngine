package edu.ucla.nesl.flowengine.node.operation;

import java.util.Arrays;

import edu.ucla.nesl.flowengine.node.DataFlowNode;

public class Sort extends DataFlowNode {

	private Object mSorted;
	
	@Override
	public void inputData(String name, String type, Object inputData, int length) {
		if (type.equals("int[]")) {
			mSorted = new int[length];
			System.arraycopy(inputData, 0, mSorted, 0, length);
			Arrays.sort((int[])mSorted);
		} else if (type.equals("double[]")) {
			mSorted = new double[length];
			System.arraycopy(inputData, 0, mSorted, 0, length);
			Arrays.sort((double[])mSorted);
		} else {
			throw new UnsupportedOperationException("Unsupported type: " + type);
		}
		outputData(name + "Sorted", type, mSorted, length);
	}
}
