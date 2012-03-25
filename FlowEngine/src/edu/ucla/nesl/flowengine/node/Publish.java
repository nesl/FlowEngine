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
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}
}
