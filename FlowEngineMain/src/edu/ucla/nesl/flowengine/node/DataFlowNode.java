package edu.ucla.nesl.flowengine.node;

import java.util.ArrayList;

public class DataFlowNode {
	private ArrayList<DataFlowNode> mOutputList = new ArrayList<DataFlowNode>();

	public void inputData(String type, Object inputData) {
		outputData(type, inputData);
	}
	
	public final boolean addOutputNode(DataFlowNode node) {
		return mOutputList.add(node);
	}
	
	public final boolean removeOutputNode(DataFlowNode node) {
		return mOutputList.remove(node);
	}
	
	protected final void outputData(String type, Object outputData) {
		for (DataFlowNode node: mOutputList) {
			node.inputData(type, outputData);
		}
	}
}
