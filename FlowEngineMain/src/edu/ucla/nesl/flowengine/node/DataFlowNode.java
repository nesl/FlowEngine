package edu.ucla.nesl.flowengine.node;

import java.util.ArrayList;

public abstract class DataFlowNode {
	private static final String TAG = DataFlowNode.class.getSimpleName();
	
	private ArrayList<DataFlowNode> mOutputList = new ArrayList<DataFlowNode>();

	abstract public void inputData(String name, String type, Object inputData, int length);

	public final boolean addOutputNode(DataFlowNode node) {
		return mOutputList.add(node);
	}
	
	public final boolean removeOutputNode(DataFlowNode node) {
		return mOutputList.remove(node);
	}
	
	protected final void outputData(String name, String type, Object outputData, int length) {
		for (DataFlowNode node: mOutputList) {
			node.inputData(name, type, outputData, length);
		}
	}
}
