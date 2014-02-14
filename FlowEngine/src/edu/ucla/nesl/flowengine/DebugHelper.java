package edu.ucla.nesl.flowengine;

import java.util.Random;

import android.os.Debug;
import android.os.Debug.MemoryInfo;
import android.util.Log;

public class DebugHelper {
	private static final String TAG = DebugHelper.class.getSimpleName();

	private static boolean mIsLogging = false;

	public static boolean isMethodTrace = false;
	public static boolean isAllocCounting = false;
	public static int traceSize = 150000000;
	
	public static Object lock = new Object();
	public static int stressCount = 0;
	public static int conversationCount = 0;
	public static int numCount = 5;
	
	private static Random rand = new Random(System.currentTimeMillis());
	
	public static void stopTrace() {
		if (isMethodTrace) {
			forcelog(TAG, "Stopping methid tracing..");
			isMethodTrace = false;
			Debug.stopMethodTracing();
		}
		if (isAllocCounting) {
			forcelog(TAG, "Stopping alloc counting..");
			isAllocCounting = false;
			Debug.stopAllocCounting();
			printMemoryInfo();
		}
	}
	
	public static void startTrace() {
		if (isMethodTrace) {
			String str = "FlowEngine" + rand.nextInt();
			forcelog(TAG, "Starting method tracing (" + str + ")..");
			Debug.startMethodTracing(str, traceSize);
		}
		if (isAllocCounting) {
			forcelog(TAG, "Starting alloc counting..");
			Debug.startAllocCounting();
		}
	}
	
	private static void printMemoryInfo() {
		MemoryInfo meminfo = new MemoryInfo();
		Debug.getMemoryInfo(meminfo);
		forcelog(TAG, "dalvikPrivateDirty:" + meminfo.dalvikPrivateDirty);
		forcelog(TAG, "dalvikPss:" + meminfo.dalvikPss);
		forcelog(TAG, "dalvikSharedDirty:" + meminfo.dalvikSharedDirty);
		forcelog(TAG, "nativePrivateDirty:" + meminfo.nativePrivateDirty);
		forcelog(TAG, "nativePss:" + meminfo.nativePss);
		forcelog(TAG, "nativeSharedDirty:" + meminfo.nativeSharedDirty);
		forcelog(TAG, "otherPrivateDirty:" + meminfo.otherPrivateDirty);
		forcelog(TAG, "otherePss:" + meminfo.otherPss);
		forcelog(TAG, "otherSharedDirty:" + meminfo.otherSharedDirty);
		
		int globalAllocSize = Debug.getGlobalAllocSize();
		
		long nativeHeapAllocSize = Debug.getNativeHeapAllocatedSize();
		forcelog(TAG, "globalAllocSize:" + globalAllocSize);
		forcelog(TAG, "nativeHeapAllocSize:" + nativeHeapAllocSize);
	}
	
	public static void logOff() {
		mIsLogging = false;
	}
	
	public static void logOn() {
		mIsLogging = true;
	}

	public static void forcelog(String tag, String msg) {
		Log.d(tag, msg);
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
