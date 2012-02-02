package edu.ucla.nesl.flowengine.node;

import android.util.Log;

public class SeedNode extends DataFlowNode {
	private static final String TAG = SeedNode.class.getSimpleName();
	
	public int mSeedName;
	
	public SeedNode(int seedName) {
		mSeedName = seedName;
	}

	@Override
	public void inputData(String name, String type, Object inputData, int length) {
		Log.d(TAG, String.format("mSeedName = %d, name = %s, type = %s", mSeedName, name, type));
		outputData(name, type, inputData, length);
	}
}
