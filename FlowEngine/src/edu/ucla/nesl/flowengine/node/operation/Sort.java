package edu.ucla.nesl.flowengine.node.operation;

import java.util.Arrays;

import edu.ucla.nesl.flowengine.InvalidDataReporter;
import edu.ucla.nesl.flowengine.node.DataFlowNode;

public class Sort extends DataFlowNode {
	private static final String TAG = Sort.class.getSimpleName();

	String mName;
	String mType;
	int mLength;
	long mTimestamp;
	Object mSorted;
	
	private void sort() {
		if (mType.equals("int[]")) {
			Arrays.sort((int[])mSorted);
		} else if (mType.equals("double[]")) {
			Arrays.sort((double[])mSorted);
		} else {
			throw new UnsupportedOperationException("Unsupported type: " + mType);
		}
	}
	
	@Override
	public ResultData processPull(Object parameter) {
		sort();
		return new ResultData(mName + "Sorted", mType, mSorted, mLength, mTimestamp);
	}
	
	@Override
	public void input(String name, String type, Object data, int length, long timestamp) {
		if (length <= 0) {
			InvalidDataReporter.report("in " + TAG + ": name: " + name + ", type: " + type + ", length: " + length);
			return;
		}

		if (type.equals("int[]")) {
			mSorted = new int[length];
		} else if (type.equals("double[]")) {
			mSorted = new double[length];
		} else {
			throw new UnsupportedOperationException("Unsupported type: " + type);
		}
		
		System.arraycopy(data, 0, mSorted, 0, length);

		mName = name;
		mType = type;
		mLength = length;
		mTimestamp = timestamp;

		if (isOutputConnected()) {
			sort();
			output(mName + "Sorted", mType, mSorted, mLength, mTimestamp);
		}
	}
}
