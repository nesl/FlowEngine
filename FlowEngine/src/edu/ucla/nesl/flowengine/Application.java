package edu.ucla.nesl.flowengine;

import java.util.HashSet;
import java.util.Set;

import edu.ucla.nesl.flowengine.aidl.ApplicationInterface;
import edu.ucla.nesl.flowengine.node.Publish;

public class Application {
	private ApplicationInterface mAppInterface;
	private Set<String> mSubscribedNodeNames = new HashSet<String>();
	private Publish mPublishNode;
	
	public Application(int appId, ApplicationInterface appInterface) {
		mAppInterface = appInterface;
		mPublishNode = new Publish(appId, mAppInterface);
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
	
	public Set<String> getSubscribedNodeNames() {
		return mSubscribedNodeNames;
	}
	
	public ApplicationInterface getApplicationInterface() {
		return mAppInterface;
	}
}
