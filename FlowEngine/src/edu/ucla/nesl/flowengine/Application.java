package edu.ucla.nesl.flowengine;

import java.util.ArrayList;
import java.util.List;

import edu.ucla.nesl.flowengine.aidl.ApplicationInterface;
import edu.ucla.nesl.flowengine.node.Publish;

public class Application {
	private ApplicationInterface mAppInterface;
	private List<String> mSubscribedNodeNames = new ArrayList<String>();
	private Publish mPublishNode;
	
	public Application(ApplicationInterface appInterface) {
		mAppInterface = appInterface;
		mPublishNode = new Publish(mAppInterface);
	}
	
	public void addSubscribedNodeNames(String nodeName) {
		mSubscribedNodeNames.add(nodeName);
	}
	
	public void removeSubscribedNodeNames(String nodeName) {
		mSubscribedNodeNames.remove(nodeName);
	}
	
	public Publish getPublishNode() {
		return mPublishNode;
	}
	
	public List<String> getSubscribedNodeNames() {
		return mSubscribedNodeNames;
	}
	
	public ApplicationInterface getApplicationInterface() {
		return mAppInterface;
	}
}
