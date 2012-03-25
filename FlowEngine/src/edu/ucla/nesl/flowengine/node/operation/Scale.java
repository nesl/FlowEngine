package edu.ucla.nesl.flowengine.node.operation;

import edu.ucla.nesl.flowengine.DebugHelper;
import edu.ucla.nesl.flowengine.node.DataFlowNode;

public class Scale extends DataFlowNode {
	private static final String TAG = Scale.class.getSimpleName();
	
	private double mScale;
	
	@Override
	protected String processParentNodeName(String parentNodeName) {
		if (parentNodeName.contains("|Buffer")) {
			return parentNodeName.split("\\|Buffer")[0];
		}
		return parentNodeName;
	}

	public Scale(String parameterizedSimpleNodeName, double scale) {
		super(parameterizedSimpleNodeName);
		mScale = scale;
	}
	
	@Override
	protected void processInput(String name, String type, Object data, int length, long timestamp) {
		if (!type.equals("double[]")) {
			throw new UnsupportedOperationException("Unsupported type: " + type);
		}
		double[] oriData = (double[])data;
		double[] scaleData = new double[length];
		
		for (int i = 0; i < length; i++) {
			scaleData[i] = oriData[i] * mScale;
		}
		
		//DebugHelper.dump(TAG, scaleData);
		output(name + String.format("Scale%.1f", mScale), "double[]", scaleData, length, timestamp);
	}
}
