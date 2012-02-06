package edu.ucla.nesl.flowengine;

import android.util.Log;

public class DebugHelper {
	private static boolean mIsLogging = true;
	
	public static void logOff() {
		mIsLogging = false;
	}
	
	public static void logOn() {
		mIsLogging = true;
	}

	public static void log(String tag, String msg) {
		if (mIsLogging) 
			Log.d(tag, msg);
	}
	
	public static void loge(String tag, String msg) {
		if (mIsLogging) 
			Log.e(tag, msg);
	}
	
	public static void logi(String tag, String msg) {
		if (mIsLogging) 
			Log.i(tag, msg);
	}
	
	public static void dump(String tag, double[] buffer) {
		if (!mIsLogging) 
			return;
		
		String str = "len(" + buffer.length + "): ";
		for (double value: buffer) {
			str += String.format("%.2f, ", value);
		}
		Log.d(tag, str);
	}
	
	public static void dump(String tag, int[] buffer) {
		if (!mIsLogging) 
			return;

		String str = "len(" + buffer.length + "): ";
		for (int value: buffer) {
			str += value + ", ";
		}
		Log.d(tag, str);
	}
}
