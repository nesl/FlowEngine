package edu.ucla.nesl.flowengine.node;

import java.util.ArrayList;

import android.util.Log;
import edu.ucla.nesl.flowengine.DebugHelper;

//TODO: buffering based on timestamp.

public class BufferNode extends DataFlowNode {
	private static final String TAG = BufferNode.class.getSimpleName();
	
	private static final int BUFFER_SYNC_THRESHOLD = 100; // ms
	
	private Object mBuffer;
	private int mBufferSize;
	private int mIndex = 0;
	private String mDataName;
	private String mDataType;
	private long mTimestamp = -1;
	
	private ArrayList<BufferNode> syncBuffers = new ArrayList<BufferNode>();
	
	public BufferNode(int bufferSize) {
		mBufferSize = bufferSize;
	}

	public void addSyncedBufferNode(BufferNode node) {
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
			int srcIndex = (int)((timestamp - mTimestamp) / getSampleInterval());
			if (mIndex <= srcIndex) {
				//No data there yet. flush all current data
				mIndex = 0;
				mTimestamp = -1;
				DebugHelper.log(TAG, "No data in the requested timestamp. Flushing..");
			} else {
				System.arraycopy(mBuffer, srcIndex, mBuffer, 0, mIndex-srcIndex);
				mTimestamp += srcIndex * getSampleInterval();
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
			super.outputData(mDataName, mDataType, mBuffer, mBufferSize, mTimestamp);
		} else {
			super.outputData(mDataName, mDataType + "[]", mBuffer, mBufferSize, mTimestamp);
		}
		
		mIndex = 0;
		mTimestamp = -1;
	}

	@Override
	public void inputData(String name, String type, Object inputData, int length, long timestamp) {
		if (mBuffer == null || mDataType == null || mDataName == null) {
			mDataName = name;
			mDataType = type;
			mTimestamp = timestamp;
			mIndex = 0;
			if (type.contains("int")) {
				mBuffer = new int[mBufferSize];
			} else if (type.contains("long")) {
				mBuffer = new long[mBufferSize];
			} else if (type.contains("double")) {
				mBuffer = new double[mBufferSize];
			} else if (type.contains("float")) {
				mBuffer = new float[mBufferSize];
			}
		} else if (!name.equals(mDataName) || !type.equals(mDataType)) {
			throw new IllegalArgumentException(String.format("name(%s) and type(%s) doesn't match.", name, type));
		}
		
		if (mTimestamp < 0) {
			mTimestamp = timestamp;
		} else {
			// check if this is contiguous timestamp
			long expectedTime = mTimestamp + mIndex * getSampleInterval();
			if (expectedTime != timestamp) {
				DebugHelper.log(TAG, "Timestamp mismatch. Expected: " + expectedTime + ", received: " + timestamp + ", mIndex: " + mIndex + ", mTimestamp: " + timestamp);
				// flush buffer
				mIndex = 0;
				mTimestamp = -1;
				return;
			}
		}
		
		if (mIndex == 0) {
			// start of buffer. sync.
			for (BufferNode node: syncBuffers) {
				DebugHelper.log(TAG, "Requesting sync...");
				long retTimestamp = node.sync(mTimestamp);
				if (retTimestamp > 0) {
					DebugHelper.log(TAG, "Sync myself...");
					sync(retTimestamp);
				}
			}
		}
		
		if (!mDataType.contains("[]")) {
			if (mDataType.contains("int")) {
				((int[])mBuffer)[mIndex] = (Integer)inputData;	
			} else if (mDataType.contains("double")) {
				((double[])mBuffer)[mIndex] = (Double)inputData;
			} else if (mDataType.contains("float")) {
				((float[])mBuffer)[mIndex] = (Float)inputData;
			} else if (mDataType.contains("long")) {
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
				
				mTimestamp = timestamp + remainingSize * getSampleInterval();
				int nextSize = length - remainingSize;
				while (nextSize > mBufferSize) {
					System.arraycopy(inputData, remainingSize, mBuffer, 0, mBufferSize);
					outputData();
					nextSize -= mBufferSize;
					mTimestamp += mBufferSize * getSampleInterval(); 
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
