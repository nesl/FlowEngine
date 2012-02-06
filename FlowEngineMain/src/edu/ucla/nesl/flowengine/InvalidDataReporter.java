package edu.ucla.nesl.flowengine;

public class InvalidDataReporter {
	private static final String TAG = InvalidDataReporter.class.getSimpleName();
	
	public static void report(String msg) {
		DebugHelper.log(TAG, msg);
	}
}
