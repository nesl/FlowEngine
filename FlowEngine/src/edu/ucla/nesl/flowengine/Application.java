package edu.ucla.nesl.flowengine;

import java.util.ArrayList;

import edu.ucla.nesl.flowengine.aidl.ApplicationInterface;
import edu.ucla.nesl.flowengine.node.Publish;

public class Application {
	private ApplicationInterface mAppInterface;
	private ArrayList<String> mSubscribedNodeNames = new ArrayList<String>();
	private ArrayList<Publish> mPublishNodes = new ArrayList<Publish>();
	
	public Application(ApplicationInterface appInterface) {
		mAppInterface = appInterface;
	}
	
	public void addSubscribedNodeNames(String nodeName) {
		mSubscribedNodeNames.add(nodeName);
	}
	
	public void addPublishNodes(Publish publishNode) {
		mPublishNodes.add(publishNode);
	}
	
	public ArrayList<Publish> getPublishNodes() {
		return mPublishNodes;
	}
	
	public ArrayList<String> getSubscribedNodeNames() {
		return mSubscribedNodeNames;
	}
	
	public ApplicationInterface getApplicationInterface() {
		return mAppInterface;
	}
}
