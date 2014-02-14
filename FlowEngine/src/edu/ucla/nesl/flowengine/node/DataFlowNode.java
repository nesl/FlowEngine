package edu.ucla.nesl.flowengine.node;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import android.util.Log;
import edu.ucla.nesl.flowengine.DebugHelper;

// Note: currently a node cannot be pulled by more than one node due to unregister process.
public abstract class DataFlowNode {
	private static final String TAG = DataFlowNode.class.getSimpleName();

	private static final String DEFAULT_PORT_NAME = "default";

	private boolean mIsEnabled;
	private String mNodeName = null;
	private String mSimpleNodeName = null;

	private Map<String, CopyOnWriteArrayList<DataFlowNode>> mOutPortMap = 
			new HashMap<String, CopyOnWriteArrayList<DataFlowNode>>();
	private Map<String, Object> mOutPortParameterMap = 
			new HashMap<String, Object>();
	private Map<String, CopyOnWriteArrayList<DataFlowNode>> mPullPortMap = 
			new HashMap<String, CopyOnWriteArrayList<DataFlowNode>>();
	private CopyOnWriteArrayList<DataFlowNode> mParentList = 
			new CopyOnWriteArrayList<DataFlowNode>();

	protected class ResultData {
		public String name;
		public String type;
		public Object data;
		public int length;
		public long timestamp;

		public ResultData(String name, String type, Object outputData, int length, long timestamp) {
			this.name = name;
			this.type = type;
			this.data = outputData;
			this.length = length;
			this.timestamp = timestamp;
		}
	}

	public DataFlowNode() {
		mIsEnabled = false;
		addOutputPort(DEFAULT_PORT_NAME, null);
		addPullPort(DEFAULT_PORT_NAME);
		mSimpleNodeName = this.getClass().getSimpleName();
	}

	public DataFlowNode(String simpleNodeName) {
		mIsEnabled = false;
		addOutputPort(DEFAULT_PORT_NAME, null);
		addPullPort(DEFAULT_PORT_NAME);
		mSimpleNodeName = simpleNodeName;
	}

	abstract protected void processInput(String name, String type, Object data, int length, long timestamp);

	public void input(String name, String type, Object data, int length, long timestamp) {
		if (mIsEnabled) {
			processInput(name, type, data, length, timestamp);
		}
	}

	public ResultData processPull(Object parameter) {
		return null;
	}

	public final void addPullPort(String port) {
		mPullPortMap.put(port, new CopyOnWriteArrayList<DataFlowNode>());
	}

	public final void addPullNode(DataFlowNode node) {
		addPullNode(DEFAULT_PORT_NAME, node);
	}

	private final void addPullNode(String port, DataFlowNode node) {
		List<DataFlowNode> nodeList = mPullPortMap.get(port);
		if (nodeList == null) {
			throw new IllegalArgumentException("No port name: " + port);
		}
		nodeList.add(node);
		addParent(node);
	}

	public final void addOutputPort(String port, Object parameter) {
		mOutPortMap.put(port, new CopyOnWriteArrayList<DataFlowNode>());
		mOutPortParameterMap.put(port, parameter);
	}

	public final void addOutputNode(DataFlowNode node) {
		addOutputNode(DEFAULT_PORT_NAME, node);
	}

	public final boolean addOutputNode(String port, DataFlowNode node) {
		List<DataFlowNode> nodeList = mOutPortMap.get(port);
		if (nodeList == null) {
			//throw new IllegalArgumentException("No port name: " + port);
			return false;
		}
		nodeList.add(node);
		node.addParent(this);
		return true;
	}

	public final void removeOutputNode(DataFlowNode node) {
		removeOutputNode(DEFAULT_PORT_NAME, node);
	}

	public final boolean removeOutputNode(String port, DataFlowNode node) {
		List<DataFlowNode> nodeList = mOutPortMap.get(port);
		if (nodeList == null) {
			//throw new IllegalArgumentException("No port name: " + port);
			return false;
		}
		nodeList.remove(node);

		if (nodeList.size() <= 0) {
			disable();
		}

		return true;
	}

	protected final void addParent(DataFlowNode parent) {
		for (DataFlowNode node: mParentList) {
			if (parent == node) {
				return;
			}
		}
		mParentList.add(parent);
	}

	public boolean isConnected() {
		return isOutputConnected() || isPullConnected();
	}

	protected final boolean isOutputConnected() {
		for (Map.Entry<String, CopyOnWriteArrayList<DataFlowNode>> entry: mOutPortMap.entrySet()) {
			List<DataFlowNode> nodeList = entry.getValue();
			if (nodeList.size() > 0) {
				return true;
			}
		}
		return false;
	}

	protected final boolean isPullConnected() {
		for (Map.Entry<String, CopyOnWriteArrayList<DataFlowNode>> entry: mPullPortMap.entrySet()) {
			List<DataFlowNode> nodeList = entry.getValue();
			if (nodeList.size() > 0) {
				return true;
			}
		}
		return false;
	}

	protected ResultData[] pull() {
		if (!mIsEnabled) {
			return null;
		}

		return pull(null);
	}

	protected ResultData[] pull(Object parameter) {
		if (!mIsEnabled) {
			return null;
		}

		int numResults = 0;
		for (Map.Entry<String, CopyOnWriteArrayList<DataFlowNode>> entry: mPullPortMap.entrySet()) {
			List<DataFlowNode> nodeList = entry.getValue();
			numResults += nodeList.size();
		}
		ResultData[] results = new ResultData[numResults];
		int resultsIndex = 0;
		for (Map.Entry<String, CopyOnWriteArrayList<DataFlowNode>> entry: mPullPortMap.entrySet()) {
			List<DataFlowNode> nodeList = entry.getValue();
			for (DataFlowNode node: nodeList) {
				ResultData result = node.processPull(parameter);
				results[resultsIndex++] = result;
			}
		}
		return results;
	}

	protected ResultData[] pull(String port, Object parameter) {
		if (!mIsEnabled) {
			return null;
		}

		List<DataFlowNode> nodeList = mPullPortMap.get(port);
		if (nodeList == null) {
			return null;
		}
		ResultData[] results = new ResultData[nodeList.size()];
		int resultsIndex = 0;
		for (DataFlowNode node: nodeList) {
			ResultData result = node.processPull(parameter);
			results[resultsIndex++] = result;
		}
		return results;
	}

	protected ResultData getParameterizedResult(Object parameter, String name, String type, Object inputData, int length, long timestamp) {
		return null;
	}

	protected final void output(String name, String type, Object data, int length, long timestamp) {
		if (!mIsEnabled) {
			return;
		}

		for (Map.Entry<String, CopyOnWriteArrayList<DataFlowNode>> entry: mOutPortMap.entrySet()) {
			Object parameter = mOutPortParameterMap.get(entry.getKey());
			List<DataFlowNode> nodeList = entry.getValue();
			if (parameter == null) {
				for (DataFlowNode node: nodeList) {
					//DebugHelper.log(mSimpleNodeName, mNodeName + " -> " + node.mNodeName);
					node.input(name, type, data, length, timestamp);
				}
			} else {
				ResultData result = getParameterizedResult(parameter, name, type, data, length, timestamp);
				if (result != null) {
					for (DataFlowNode node: nodeList) {
						//DebugHelper.log(mSimpleNodeName, mNodeName + " -> " + node.mNodeName);
						node.input(result.name, result.type, result.data, result.length, result.timestamp);
					}
				}
			}
		}
	}

	protected final void output(String port, String name, String type, Object data, int length, long timestamp) {
		if (!mIsEnabled) {
			return;
		}

		List<DataFlowNode> nodeList = mOutPortMap.get(port);
		Object parameter = mOutPortParameterMap.get(port);
		if (parameter == null) {
			for (DataFlowNode node: nodeList) {
				node.input(name, type, data, length, timestamp);
			}
		} else {
			ResultData result = getParameterizedResult(parameter, name, type, data, length, timestamp);
			if (result != null) {
				for (DataFlowNode node: nodeList) {
					node.input(result.name, result.type, result.data, result.length, result.timestamp);
				}
			}
		}
	}

	/*protected final void initializeGraph(DataFlowNode parent) {
		if (parent != null) {
			mParentList.add(parent);
		}
		if (mPullPortMap.size() > 0) {
			for (Map.Entry<String, LinkedList<DataFlowNode>> entry: mPullPortMap.entrySet()) {
				LinkedList<DataFlowNode> nodeList = entry.getValue();
				for (DataFlowNode node: nodeList) {
					mParentList.add(node);
					node.initializeGraph(null);
				}
			}
		}
		for (Map.Entry<String, LinkedList<DataFlowNode>> entry: mOutPortMap.entrySet()) {
			LinkedList<DataFlowNode> nodeList = entry.getValue();
			for (DataFlowNode node: nodeList) {
				node.initializeGraph(this);
			}
		}
	}*/

	public List<DataFlowNode> getParents() {
		return mParentList;
	}

	public boolean isEnabled() {
		return mIsEnabled;
	}

	private void enableParents() {
		if (mIsEnabled) {
			return;
		}
		mIsEnabled = true;
		DebugHelper.log(TAG, this.toString() + " enabled.");
		// check if seed node.
		if (mParentList.size() == 0) {
			startSensor();
		} else {
			for (DataFlowNode node: mParentList) {
				node.enableParents();
			}
			
			// enabling pulled nodes
			for (Map.Entry<String, CopyOnWriteArrayList<DataFlowNode>> entry: mPullPortMap.entrySet()) {
				List<DataFlowNode> nodeList = entry.getValue();
				for (DataFlowNode node: nodeList) {
					node.enableParents();
				}
			}
		}
	}

	private void enableChildren() {
		if (mIsEnabled) {
			return;
		}
		mIsEnabled = true;
		DebugHelper.log(TAG, this.toString() + " enabled.");
		for (Map.Entry<String, CopyOnWriteArrayList<DataFlowNode>> entry: mOutPortMap.entrySet()) {
			List<DataFlowNode> nodeList = entry.getValue();
			for (DataFlowNode node: nodeList) {
				node.enableChildren();
			}
		}
		
		// enabling pulled nodes
		for (Map.Entry<String, CopyOnWriteArrayList<DataFlowNode>> entry: mPullPortMap.entrySet()) {
			List<DataFlowNode> nodeList = entry.getValue();
			for (DataFlowNode node: nodeList) {
				node.enableParents();
			}
		}
	}

	public void enable() {
		if (mIsEnabled) {
			return;
		}
		mIsEnabled = true;
		DebugHelper.log(TAG, this.toString() + " initiate enabling..");
		
		// enabling children
		for (Map.Entry<String, CopyOnWriteArrayList<DataFlowNode>> entry: mOutPortMap.entrySet()) {
			List<DataFlowNode> nodeList = entry.getValue();
			for (DataFlowNode node: nodeList) {
				node.enableChildren();
			}
		}

		// enabling pulled nodes
		for (Map.Entry<String, CopyOnWriteArrayList<DataFlowNode>> entry: mPullPortMap.entrySet()) {
			List<DataFlowNode> nodeList = entry.getValue();
			for (DataFlowNode node: nodeList) {
				node.enableParents();
			}
		}

		// enabling parents
		// check if seed node.
		if (mParentList.size() == 0) {
			startSensor();
		} else {
			for (DataFlowNode node: mParentList) {
				node.enableParents();
			}
		}
	}

	private void disableChildren() {
		if (!mIsEnabled) {
			return;
		}

		// check if all parents are disabled
		for (DataFlowNode node: mParentList) {
			if (node.isEnabled()) {
				return;
			}
		}
		mIsEnabled = false;
		DebugHelper.log(TAG, this.toString() + " disabled.");

		// disable child nodes
		for (Map.Entry<String, CopyOnWriteArrayList<DataFlowNode>> entry: mOutPortMap.entrySet()) {
			List<DataFlowNode> nodeList = entry.getValue();
			for (DataFlowNode node: nodeList) {
				node.disableChildren();
			}
		}
	}

	private void disableParents() {
		if (!mIsEnabled) {
			return;
		}

		// check if all children is disabled.
		for (Map.Entry<String, CopyOnWriteArrayList<DataFlowNode>> entry: mOutPortMap.entrySet()) {
			List<DataFlowNode> nodeList = entry.getValue();
			for (DataFlowNode node: nodeList) {
				if (node.isEnabled()) {
					return;
				}
			}
		}
		mIsEnabled = false;
		DebugHelper.log(TAG, this.toString() + " disabled.");

		// if this is seed node
		if (this instanceof SeedNode) {
			stopSensor();
		}

		//disable parents
		for (DataFlowNode node: mParentList) {
			node.disableParents();
		}
	}

	public void disable() {
		if (!mIsEnabled) {
			return;
		}
		mIsEnabled = false;
		DebugHelper.log(TAG, this.toString() + " initiate disabling..");

		// if this is seed node
		if (this instanceof SeedNode) {
			stopSensor();
		}

		//disable parents
		for (DataFlowNode node: mParentList) {
			node.disableParents();
		}

		//disable children
		for (Map.Entry<String, CopyOnWriteArrayList<DataFlowNode>> entry: mOutPortMap.entrySet()) {
			List<DataFlowNode> nodeList = entry.getValue();
			for (DataFlowNode node: nodeList) {
				node.disableChildren();
			}
		}
	}

	public void remove(Map<String, DataFlowNode> nodeNameMap, Map<Integer, SeedNode> seedNodeMap) {
		// remove this from parents
		for (DataFlowNode node: mParentList) {
			node.removeChild(this, nodeNameMap, seedNodeMap);
		}

		// remove this from children
		for (Map.Entry<String, CopyOnWriteArrayList<DataFlowNode>> entry: mOutPortMap.entrySet()) {
			List<DataFlowNode> nodeList = entry.getValue();
			for (DataFlowNode node: nodeList) {
				node.removeParent(this, nodeNameMap, seedNodeMap);
			}
			nodeList.clear();
		}

		// if this is seed node
		if (this instanceof SeedNode) {
			stopSensor();
			mIsEnabled = false;
			SeedNode seed = (SeedNode)this;
			if (seed.getAttachedDevice() == null) {
				seedNodeMap.remove(seed.getSensorID());
				nodeNameMap.remove(mNodeName);
				DebugHelper.log(TAG, "SeedNode: " + mNodeName + " removed.");
			}
		} else {
			nodeNameMap.remove(mNodeName);
			DebugHelper.log(TAG, "Node: " + mNodeName + " removed.");
		}
	}

	public void removeChild(DataFlowNode child, Map<String, DataFlowNode> nodeNameMap, Map<Integer, SeedNode> seedNodeMap) {
		// remove child
		for (Map.Entry<String, CopyOnWriteArrayList<DataFlowNode>> entry: mOutPortMap.entrySet()) {
			List<DataFlowNode> nodeList = entry.getValue();
			nodeList.remove(child);
		}

		// check if all children are removed.
		for (Map.Entry<String, CopyOnWriteArrayList<DataFlowNode>> entry: mOutPortMap.entrySet()) {
			List<DataFlowNode> nodeList = entry.getValue();
			if (nodeList.size() > 0) {
				return;
			}
		}

		// remove this, too
		remove(nodeNameMap, seedNodeMap);
	}

	private void removeParent(DataFlowNode parent, Map<String, DataFlowNode> nodeNameMap, Map<Integer, SeedNode> seedNodeMap) {
		mParentList.remove(parent);

		// check if all parents are removed
		if (mParentList.size() > 0) {
			return;
		}

		// remove this, too
		remove(nodeNameMap, seedNodeMap);
	}

	// implemented by SeedNodes
	protected void stopSensor() {
	}

	protected void startSensor() {
	}

	protected String processParentNodeName(String parentNodeName) {
		return "";
	}

	protected String getParameterName(Object parameter) {
		return "";
	}

	protected void configurePushNodeName(Map<String, DataFlowNode> nodeNameMap, String parentNodeName) {
		String nodeName = processParentNodeName(parentNodeName) + "|" + mSimpleNodeName;
		if (mNodeName == null) {
			mNodeName = nodeName;
		} else {
			if (!mNodeName.equals(nodeName)) {
				throw new UnsupportedOperationException("mNodeName != nodeName: " + mNodeName + " != " + nodeName);
			}
		}
		nodeNameMap.put(mNodeName, this);
		for (Map.Entry<String, CopyOnWriteArrayList<DataFlowNode>> entry: mOutPortMap.entrySet()) {
			Object parameter = mOutPortParameterMap.get(entry.getKey());
			List<DataFlowNode> nodeList = entry.getValue();
			if (parameter == null) {
				for (DataFlowNode node: nodeList) {
					node.configurePushNodeName(nodeNameMap, mNodeName);
				}
			} else {
				String parameterName = getParameterName(parameter);
				for (DataFlowNode node: nodeList) {
					node.configurePushNodeName(nodeNameMap, mNodeName + parameterName);
				}
			}
		}
		for (Map.Entry<String, CopyOnWriteArrayList<DataFlowNode>> entry: mPullPortMap.entrySet()) {
			List<DataFlowNode> nodeList = entry.getValue();
			for (DataFlowNode node: nodeList) {
				node.configurePullNodeName(nodeNameMap);
			}
		}
	}

	protected String configurePullNodeName(Map<String, DataFlowNode> nodeNameMap) {
		if (mNodeName != null) {
			DebugHelper.log(TAG, mNodeName);
			return mNodeName;
		}
		ArrayList<String> nodeNameCandidates = new ArrayList<String>();
		for (Map.Entry<String, CopyOnWriteArrayList<DataFlowNode>> entry: mPullPortMap.entrySet()) {
			List<DataFlowNode> nodeList = entry.getValue();
			for (DataFlowNode node: nodeList) {
				String parentNodeName = node.configurePullNodeName(nodeNameMap);
				DebugHelper.log(TAG, parentNodeName);
				nodeNameCandidates.add(processParentNodeName(parentNodeName) + "|" + mSimpleNodeName);
			}
		}
		String prevName = null;
		for (String nodeName: nodeNameCandidates) {
			DebugHelper.log(TAG, nodeName);
			if (prevName == null) {
				prevName = nodeName;
				continue;
			}
			if (!prevName.equals(nodeName)) {
				throw new UnsupportedOperationException("prevName != nodeName: " + prevName + " != " + nodeName);
			}
			prevName = nodeName;
		}
		mNodeName = prevName;
		if (mNodeName == null) {
			throw new UnsupportedOperationException("mNodeName still null");
		}
		nodeNameMap.put(mNodeName, this);
		return mNodeName;
	}

	public String getName() {
		return mNodeName;
	}

	public void resetConnection() {
		mOutPortMap.clear();
		mPullPortMap.clear();
		mOutPortParameterMap.clear();
		mParentList.clear();
		addOutputPort(DEFAULT_PORT_NAME, null);
		addPullPort(DEFAULT_PORT_NAME);
	}

	public boolean isPushConnected(DataFlowNode node) {
		for (Map.Entry<String, CopyOnWriteArrayList<DataFlowNode>> entry: mOutPortMap.entrySet()) {
			List<DataFlowNode> nodeList = entry.getValue();
			for (DataFlowNode curNode: nodeList) {
				if (curNode == node) {
					return true;
				}
			}
		}
		return false;
	}

	public boolean isPullConnected(DataFlowNode node) {
		for (Map.Entry<String, CopyOnWriteArrayList<DataFlowNode>> entry: mPullPortMap.entrySet()) {
			List<DataFlowNode> nodeList = entry.getValue();
			for (DataFlowNode curNode: nodeList) {
				if (curNode == node) {
					return true;
				}
			}
		}
		return false;
	}

	public boolean isPushParameterizedConnected(DataFlowNode node, String portName) {
		for (Map.Entry<String, CopyOnWriteArrayList<DataFlowNode>> entry: mOutPortMap.entrySet()) {
			String curPortName = entry.getKey();
			List<DataFlowNode> nodeList = entry.getValue();
			for (DataFlowNode curNode: nodeList) {
				if (curNode == node && curPortName == portName) {
					return true;
				}
			}
		}
		return false;
	}

	/*public void reconnect(DataFlowNode node) {
		mOutPortMap = node.getOutPortMap();
		mOutPortParameterMap = node.getOutPortParameterMap();
		mPullPortMap = node.getPullPortMap();
		mParentList = node.getParentList();
	}*/

	public Map<String, CopyOnWriteArrayList<DataFlowNode>> getOutPortMap() {
		return mOutPortMap;
	}

	public Map<String, Object> getOutPortParameterMap() {
		return mOutPortParameterMap;
	}

	public Map<String, CopyOnWriteArrayList<DataFlowNode>> getPullPortMap() {
		return mPullPortMap;
	}

	public List<DataFlowNode> getParentList() {
		return mParentList;
	}
	
	public List<DataFlowNode> getDefaultChildrenList() {
		return mOutPortMap.get(DEFAULT_PORT_NAME);
	}
}
