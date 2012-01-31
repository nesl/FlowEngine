package edu.ucla.nesl.flowengine.node;

import android.util.Log;

public class BufferNode extends DataFlowNode {
	private static final String TAG = BufferNode.class.getSimpleName();
	
	private double[] buffer;
	private int mBufferSize;
	private int mIndex = 0;
	
	public BufferNode(int bufferSize) {
		mBufferSize = bufferSize;
		buffer = new double[mBufferSize];
	}
	
	@Override
	public void inputData(String type, Object inputData) {
		buffer[mIndex] = (Double)inputData;
		mIndex += 1;
		
		//Log.d(TAG, "inputData: " + Double.toString((Double)inputData));
		
		if (mIndex >= mBufferSize) {
			outputData("BufferedDoubles", buffer);
			mIndex = 0;
		}
	}
}
