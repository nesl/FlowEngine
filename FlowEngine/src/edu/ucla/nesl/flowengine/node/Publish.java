package edu.ucla.nesl.flowengine.node;

import android.os.RemoteException;
import edu.ucla.nesl.flowengine.aidl.ApplicationInterface;

public class Publish extends DataFlowNode {

	ApplicationInterface mAppInterface;
	
	public Publish(ApplicationInterface appInterface) {
		mAppInterface = appInterface;
	}
	
	@Override
	protected void processInput(String name, String type, Object data, int length, long timestamp) {
		if (!type.equals("String")) {
			return;
		}
		try {
			mAppInterface.publish(name, (String)data, timestamp);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}
}
