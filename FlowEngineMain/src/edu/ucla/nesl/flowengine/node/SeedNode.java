package edu.ucla.nesl.flowengine.node;

import edu.ucla.nesl.flowengine.DebugHelper;

public class SeedNode extends DataFlowNode {
	private static final String TAG = SeedNode.class.getSimpleName();
	
	public int mSeedName;
	
	public SeedNode(int seedName) {
		mSeedName = seedName;
	}

	@Override
	public void inputData(String name, String type, Object inputData, int length) {
		//DebugHelper.log(TAG, String.format("mSeedName = %d, name = %s, type = %s", mSeedName, name, type));
		outputData(name, type, inputData, length);
	}
}
