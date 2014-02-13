package edu.ucla.nesl.flowengine.node.operation;

import java.util.Arrays;

import edu.ucla.nesl.flowengine.DataType;
import edu.ucla.nesl.flowengine.InvalidDataReporter;
import edu.ucla.nesl.flowengine.node.DataFlowNode;

public class Sort extends DataFlowNode {
	private static final String TAG = Sort.class.getSimpleName();

	String mName;
	String mType;
	int mLength;
	long mTimestamp;
	Object mSorted;
	
	@Override
	protected String processParentNodeName(String parentNodeName) {
		if (parentNodeName.contains("|Buffer")) {
			return parentNodeName.split("\\|Buffer")[0];
		}
		return parentNodeName;
	}

	private void sort() {
		if (mType.equals(DataType.INTEGER_ARRAY)) {
			Arrays.sort((int[])mSorted);
		} else if (mType.equals(DataType.DOUBLE_ARRAY)) {
			Arrays.sort((double[])mSorted);
		} else {
			throw new UnsupportedOperationException("Unsupported type: " + mType);
		}
	}
	
	@Override
	public ResultData processPull(Object parameter) {
		sort();
		return new ResultData(mName + "Sort", mType, mSorted, mLength, mTimestamp);
	}
	
	@Override
	protected void processInput(String name, String type, Object data, int length, long timestamp) {
		if (length <= 0) {
			InvalidDataReporter.report("in " + TAG + ": name: " + name + ", type: " + type + ", length: " + length);
			return;
		}

		if (type.equals(DataType.INTEGER_ARRAY)) {
			mSorted = new int[length];
		} else if (type.equals(DataType.DOUBLE_ARRAY)) {
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
			output(mName + "Sort", mType, mSorted, mLength, mTimestamp);
		}
	}
}
