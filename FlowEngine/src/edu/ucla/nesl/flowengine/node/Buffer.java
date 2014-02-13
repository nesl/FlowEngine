package edu.ucla.nesl.flowengine.node;

import java.util.HashSet;
import java.util.Set;

import android.util.Log;
import edu.ucla.nesl.flowengine.DataType;
import edu.ucla.nesl.flowengine.DebugHelper;

public class Buffer extends DataFlowNode {
	private static final String TAG = Buffer.class.getSimpleName();
	
	private static final int BUFFER_SYNC_THRESHOLD = 100; // ms
	private static final int TIMESTAMP_MISMATCH_THRESHOLD = 1000; // ms
	
	private Object mBuffer;
	private int mBufferSize;
	private int mIndex = 0;
	private String mDataName;
	private String mDataType;
	private long mTimestamp = -1;
	private int mSampleInterval;
	
	private Set<Buffer> syncBuffers = new HashSet<Buffer>();
	
	@Override
	protected String processParentNodeName(String parentNodeName) {
		return parentNodeName;
	}

	public Buffer(String parameterizedSimpleNodeName, int bufferSize, int sampleInterval) {
		super(parameterizedSimpleNodeName);
		mBufferSize = bufferSize;
		mSampleInterval = sampleInterval;
	}

	public void addSyncedBufferNode(Buffer node) {
		syncBuffers.add(node);
	}
	
	public long sync(long timestamp) {
		if (mTimestamp < 0) {
			DebugHelper.log(TAG, "No data yet.");
			return -1; // no data yet
		}
		
		DebugHelper.log(TAG, "Sync request received: my time: " + mTimestamp + ", requested time: " + timestamp);
		
		if (mTimestamp - timestamp < -1 * BUFFER_SYNC_THRESHOLD) {
			// current buffer is behind. Shift current buffer
			int srcIndex = (int)((timestamp - mTimestamp) / mSampleInterval);
			if (mIndex <= srcIndex) {
				//No data there yet. flush all current data
				mIndex = 0;
				mTimestamp = -1;
				DebugHelper.log(TAG, "No data in the requested timestamp. Flushing..");
			} else {
				System.arraycopy(mBuffer, srcIndex, mBuffer, 0, mIndex-srcIndex);
				mTimestamp += srcIndex * mSampleInterval;
				mIndex -= srcIndex;
				DebugHelper.log(TAG, "After sync dropped " + srcIndex + " samples: my time: " + mTimestamp + ", requested time: " + timestamp);
			}
		} else if (mTimestamp - timestamp > BUFFER_SYNC_THRESHOLD) {
			// current buffer is in the future. Let the caller shift
			DebugHelper.log(TAG, "Current buffer is in the future.");
			return mTimestamp;
		} else {
			DebugHelper.log(TAG, "Buffer is already in sync.");
		}
		return 0; // buffer is in sync
	}
	
	private void outputData() {
		if (mDataType.contains("[]") ) {
			super.output(mDataName, mDataType, mBuffer, mBufferSize, mTimestamp);
		} else {
			super.output(mDataName, mDataType + "[]", mBuffer, mBufferSize, mTimestamp);
		}
		
		mIndex = 0;
		mTimestamp = -1;
	}

	@Override
	protected void processInput(String name, String type, Object inputData, int length, long timestamp) {
		if (mBuffer == null || mDataType == null || mDataName == null) {
			mDataName = name;
			mDataType = type;
			mTimestamp = timestamp;
			mIndex = 0;
			if (type.contains(DataType.INTEGER)) {
				mBuffer = new int[mBufferSize];
			} else if (type.contains(DataType.LONG)) {
				mBuffer = new long[mBufferSize];
			} else if (type.contains(DataType.DOUBLE)) {
				mBuffer = new double[mBufferSize];
			} else if (type.contains(DataType.FLOAT)) {
				mBuffer = new float[mBufferSize];
			}
		} else if (!name.equals(mDataName) || !type.equals(mDataType)) {
			throw new IllegalArgumentException(String.format("name(%s) and type(%s) doesn't match.", name, type));
		}
		
		if (mTimestamp < 0) {
			mTimestamp = timestamp;
		} else if (mSampleInterval > 0){
			// check if this is contiguous timestamp
			long expectedTime = mTimestamp + mIndex * mSampleInterval;
			if (Math.abs(expectedTime - timestamp) > TIMESTAMP_MISMATCH_THRESHOLD) {
				DebugHelper.log(TAG, "Timestamp mismatch. Expected: " + expectedTime + ", received: " + timestamp + ", mIndex: " + mIndex + ", mTimestamp: " + timestamp);
				// flush buffer
				mIndex = 0;
				mTimestamp = -1;
				return;
			}
		}
		
		if (mIndex == 0) {
			// start of buffer. sync.
			for (Buffer node: syncBuffers) {
				DebugHelper.log(TAG, "Requesting sync...");
				long retTimestamp = node.sync(mTimestamp);
				if (retTimestamp > 0) {
					DebugHelper.log(TAG, "Sync myself...");
					sync(retTimestamp);
				}
			}
		}
		
		if (!mDataType.contains("[]")) {
			if (mDataType.contains(DataType.INTEGER)) {
				((int[])mBuffer)[mIndex] = (Integer)inputData;	
			} else if (mDataType.contains(DataType.DOUBLE)) {
				((double[])mBuffer)[mIndex] = (Double)inputData;
			} else if (mDataType.contains(DataType.FLOAT)) {
				((float[])mBuffer)[mIndex] = (Float)inputData;
			} else if (mDataType.contains(DataType.LONG)) {
				((long[])mBuffer)[mIndex] = (Long)inputData;
			}
			mIndex += 1;
			if (mIndex >= mBufferSize) {
				outputData();
			}
		} else {
			int remainingSize = mBufferSize - mIndex;
			if (remainingSize >= length) {
				try {
					System.arraycopy(inputData, 0, mBuffer, mIndex, length);
				} catch (ArrayIndexOutOfBoundsException e) {
					Log.e(TAG, "remaining size: " + remainingSize + ", mIndex: " + mIndex);
					e.printStackTrace();
				}
				mIndex += length;
				if (mIndex >= mBufferSize) {
					outputData();
				}
			} else {
				try {
					System.arraycopy(inputData, 0, mBuffer, mIndex, remainingSize);
				} catch (ArrayIndexOutOfBoundsException e) {
					Log.e(TAG, "remaining size: " + remainingSize + ", mIndex: " + mIndex);
					e.printStackTrace();
				}
				mIndex += remainingSize;
				if (mIndex >= mBufferSize) {
					outputData();
				}
				
				mTimestamp = timestamp + remainingSize * mSampleInterval;
				int nextSize = length - remainingSize;
				while (nextSize > mBufferSize) {
					System.arraycopy(inputData, remainingSize, mBuffer, 0, mBufferSize);
					outputData();
					nextSize -= mBufferSize;
					mTimestamp += mBufferSize * mSampleInterval; 
				}

				System.arraycopy(inputData, remainingSize, mBuffer, 0, nextSize);
				mIndex += nextSize;
				if (mIndex >= mBufferSize) {
					outputData();
				}
			}
		} 
	}
}
