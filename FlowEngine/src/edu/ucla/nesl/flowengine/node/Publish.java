package edu.ucla.nesl.flowengine.node;

import android.os.DeadObjectException;
import android.os.RemoteException;
import edu.ucla.nesl.flowengine.Application;
import edu.ucla.nesl.flowengine.FlowEngine;
import edu.ucla.nesl.flowengine.aidl.ApplicationInterface;

public class Publish extends DataFlowNode {
	private static final String TAG = Publish.class.getSimpleName();
	
	private int mAppId;
	private ApplicationInterface mAppInterface;
	
	public Publish(int appId, ApplicationInterface appInterface) {
		mAppId = appId;
		mAppInterface = appInterface;
	}
	
	@Override
	protected void processInput(String name, String type, Object data, int length, long timestamp) {
		try {
			if (type.equals("String")) {
				mAppInterface.publishString(name, (String)data, timestamp);
			} else if (type.equals("double[]")) {
				mAppInterface.publishDoubleArray(name, (double [])data, length, timestamp);
			} else if (type.equals("int[]")) {
				mAppInterface.publishIntArray(name, (int [])data, length, timestamp);
			} else if (type.equals("int")) {
				mAppInterface.publishInt(name, (Integer)data, timestamp);
			} else if (type.equals("double")) {
				mAppInterface.publishDouble(name, (Double)data, timestamp);
			}
		} catch (DeadObjectException e) {
			FlowEngine.getInstance().unregisterApplication(mAppId);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}
}
