package edu.ucla.nesl.flowengine.node;

import android.util.Log;

//TODO: bufferring based on timestamp.

public class BufferNode<T> extends DataFlowNode {
	private static final String TAG = BufferNode.class.getSimpleName();

	private Object mBuffer;
	private int mBufferSize;
	private int mIndex = 0;
	private String mDataName;
	private String mDataType;

	public BufferNode(int bufferSize/*, String name, String type*/) {
		mBufferSize = bufferSize;
		//mDataName = name;
		//mDataType = type;
		/*if (type.contains("int")) {
			mBuffer = new int[bufferSize];
		} else if (type.contains("long")) {
			mBuffer = new long[bufferSize];
		} else if (type.contains("double")) {
			mBuffer = new double[bufferSize];
		} else if (type.contains("float")) {
			mBuffer = new float[bufferSize];
		}*/
	}

	private void outputData() {
		if (mDataType.contains("[]") ) {
			outputData(mDataName, mDataType, mBuffer, mBufferSize);
		} else {
			outputData(mDataName, mDataType + "[]", mBuffer, mBufferSize);
		}
		mIndex = 0;

		String str = "buffer: ";
		for (int value: (int[])mBuffer) {
			str += value + ", ";
		}
		Log.d(TAG, str);
	}

	@Override
	public void inputData(String name, String type, Object inputData, int length) {
		if (mBuffer == null || mDataType == null || mDataName == null) {
			mDataName = name;
			mDataType = type;
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
			Log.d(TAG, "Type or name mismatch.");
			return;
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
				System.arraycopy(inputData, 0, mBuffer, mIndex, length);
				mIndex += length;
				if (mIndex >= mBufferSize) {
					outputData();
				}
			} else {
				System.arraycopy(inputData, 0, mBuffer, mIndex, remainingSize);
				mIndex += remainingSize;
				if (mIndex >= mBufferSize) {
					outputData();
				}

				int nextSize = length - remainingSize;
				while (nextSize > mBufferSize) {
					System.arraycopy(inputData, remainingSize, mBuffer, 0, mBufferSize);	
					outputData();
					nextSize -= mBufferSize;
				}

				System.arraycopy(inputData, remainingSize, mBuffer, 0, nextSize);
				mIndex += nextSize;
				if (mIndex >= mBufferSize) {
					outputData();
				}
			}
			/*} else if (mDataType.contains("double")) {
				double[] data = (double[])inputData;
				if (remainingSize >= data.length) {
					System.arraycopy(data, 0, mBuffer, mIndex, data.length);
					mIndex += data.length;
					if (mIndex >= mBufferSize) {
						outputData();
					}
				} else {
					System.arraycopy(data, 0, mBuffer, mIndex, remainingSize);
					mIndex += remainingSize;
					if (mIndex >= mBufferSize) {
						outputData();
					}

					int nextSize = data.length - remainingSize;
					while (nextSize > mBufferSize) {
						System.arraycopy(data, remainingSize, mBuffer, 0, mBufferSize);	
						outputData();
						nextSize -= mBufferSize;
					}

					System.arraycopy(data, remainingSize, mBuffer, 0, nextSize);
					mIndex += nextSize;
					if (mIndex >= mBufferSize) {
						outputData();
					}
				}
			} else if (mDataType.contains("float")) {
				float[] data = (float[])inputData;
				if (remainingSize >= data.length) {
					System.arraycopy(data, 0, mBuffer, mIndex, data.length);
					mIndex += data.length;
					if (mIndex >= mBufferSize) {
						outputData();
					}
				} else {
					System.arraycopy(data, 0, mBuffer, mIndex, remainingSize);
					mIndex += remainingSize;
					if (mIndex >= mBufferSize) {
						outputData();
					}

					int nextSize = data.length - remainingSize;
					while (nextSize > mBufferSize) {
						System.arraycopy(data, remainingSize, mBuffer, 0, mBufferSize);	
						outputData();
						nextSize -= mBufferSize;
					}

					System.arraycopy(data, remainingSize, mBuffer, 0, nextSize);
					mIndex += nextSize;
					if (mIndex >= mBufferSize) {
						outputData();
					}
				}
			} else if (mDataType.contains("long")) {
				long[] data = (long[])inputData;
				if (remainingSize >= data.length) {
					System.arraycopy(data, 0, mBuffer, mIndex, data.length);
					mIndex += data.length;
					if (mIndex >= mBufferSize) {
						outputData();
					}
				} else {
					System.arraycopy(data, 0, mBuffer, mIndex, remainingSize);
					mIndex += remainingSize;
					if (mIndex >= mBufferSize) {
						outputData();
					}

					int nextSize = data.length - remainingSize;
					while (nextSize > mBufferSize) {
						System.arraycopy(data, remainingSize, mBuffer, 0, mBufferSize);	
						outputData();
						nextSize -= mBufferSize;
					}

					System.arraycopy(data, remainingSize, mBuffer, 0, nextSize);
					mIndex += nextSize;
					if (mIndex >= mBufferSize) {
						outputData();
					}
				}
			} */
		} 
	}
}
