package edu.ucla.nesl.flowengine.node.classifier;

import edu.ucla.nesl.flowengine.DataType;
import edu.ucla.nesl.flowengine.DebugHelper;
import edu.ucla.nesl.flowengine.InvalidDataReporter;
import edu.ucla.nesl.flowengine.SensorType;
import edu.ucla.nesl.flowengine.node.DataFlowNode;

public class Conversation extends DataFlowNode {
	private static final String TAG = Conversation.class.getSimpleName();

	private static final int mRoundingMultiplier = 10000;
	
	private long mCurTime = 0;
	private long mLastTime = 0;
	
	private static final int NUM_FEATURES = 8;
	private double[] mFeatures = new double[NUM_FEATURES];
	private int mFeatureBitVector = 0;
	
	private static final int INDEX_PERCENTILE90_INHALATION = 0;
	private static final int INDEX_STDEV_INHALATION = 1;
	private static final int INDEX_MEAN_EXHALATION = 2;
	private static final int INDEX_MEAN_IERATIO = 3;
	private static final int INDEX_MEDIAN_IERATIO = 4;
	private static final int INDEX_MEAN_BREATHINGDURATION = 5;
	private static final int INDEX_SECONDBEST_BREATHINGDURATION = 6;
	private static final int INDEX_STDEV_STRETCH = 7;
	
	private static final int BIT_PERCENTILE90_INHALATION = 1 << INDEX_PERCENTILE90_INHALATION;
	private static final int BIT_STDEV_INHALATION = 1 << INDEX_STDEV_INHALATION;
	private static final int BIT_MEAN_EXHALATION = 1 << INDEX_MEAN_EXHALATION;
	private static final int BIT_MEAN_IERATIO = 1 << INDEX_MEAN_IERATIO;
	private static final int BIT_MEDIAN_IERATIO = 1 << INDEX_MEDIAN_IERATIO;
	private static final int BIT_MEAN_BREATHINGDURATION = 1 << INDEX_MEAN_BREATHINGDURATION;
	private static final int BIT_SECONDBEST_BREATHINGDURATION = 1 << INDEX_SECONDBEST_BREATHINGDURATION;
	private static final int BIT_STDEV_STRETCH = 1 << INDEX_STDEV_STRETCH;
	
	private void clearFeatureBitVector() {
		mFeatureBitVector = 0;
	}
	
	private boolean isAllFeature() {
		return mFeatureBitVector == 0xFF; 
	}
	
	@Override
	protected void processInput(String name, String type, Object inputData, int length, long timestamp) {
		if (!type.equals(DataType.DOUBLE)) {
			throw new UnsupportedOperationException("Unsupported type: " + type);
		}
		
		mCurTime = System.currentTimeMillis();
		double diff = mCurTime - mLastTime;
		if (mFeatureBitVector != 0 && diff >= 10000) {
			InvalidDataReporter.report("Too large time difference among features: " + diff);
		}
		mLastTime = mCurTime;

		if (name.contains("RIPInhalationPercentile90.0")) {
			mFeatures[INDEX_PERCENTILE90_INHALATION] = (Double)inputData;
			mFeatureBitVector |= BIT_PERCENTILE90_INHALATION;
		} else if (name.contains("RIPInhalationStandardDeviation")) {
			mFeatures[INDEX_STDEV_INHALATION] = (Double)inputData;
			mFeatureBitVector |= BIT_STDEV_INHALATION;
		} else if (name.contains("RIPExhalationMean")) {
			mFeatures[INDEX_MEAN_EXHALATION] = (Double)inputData;
			mFeatureBitVector |= BIT_MEAN_EXHALATION;
		} else if (name.contains("RIPIERatioMean")) {
			mFeatures[INDEX_MEAN_IERATIO] = (Double)inputData;
			mFeatureBitVector |= BIT_MEAN_IERATIO;
		} else if (name.contains("RIPIERatioMedian")) {
			mFeatures[INDEX_MEDIAN_IERATIO] = (Double)inputData;
			mFeatureBitVector |= BIT_MEDIAN_IERATIO;
		} else if (name.contains("RIPBreathingDurationMean")) {
			mFeatures[INDEX_MEAN_BREATHINGDURATION] = (Double)inputData;
			mFeatureBitVector |= BIT_MEAN_BREATHINGDURATION;
		} else if (name.contains("RIPBreathingDurationNthBest2")) {
			mFeatures[INDEX_SECONDBEST_BREATHINGDURATION] = (Double)inputData;
			mFeatureBitVector |= BIT_SECONDBEST_BREATHINGDURATION;
		} else if (name.contains("RIPStretchStandardDeviation")) {
			mFeatures[INDEX_STDEV_STRETCH] = (Double)inputData;
			mFeatureBitVector |= BIT_STDEV_STRETCH;
		}
		
		DebugHelper.log(TAG, name);
		DebugHelper.log(TAG, "bit vector: " + Integer.toHexString(mFeatureBitVector));
		DebugHelper.dump(TAG, mFeatures);
		
		if (isAllFeature()) {
			mFeatures[3] = mFeatures[3] / mRoundingMultiplier;
			mFeatures[4] = mFeatures[4] / mRoundingMultiplier;
			int conversation = getConversationClassification(mFeatures[0], mFeatures[1], mFeatures[2], mFeatures[3], mFeatures[4], mFeatures[5], mFeatures[6], mFeatures[7]);
			DebugHelper.log(TAG, "conversation: " + conversation);
			output(SensorType.CONVERSATION_CONTEXT_NAME, DataType.INTEGER, conversation, 0, timestamp);
			clearFeatureBitVector();
			
			synchronized (DebugHelper.lock){
				if (DebugHelper.isMethodTrace || DebugHelper.isAllocCounting) {
					DebugHelper.conversationCount++;
					DebugHelper.forcelog(TAG, "conversation:" + conversation + " count:" + DebugHelper.conversationCount);
					if (DebugHelper.conversationCount >= DebugHelper.numCount
							&& DebugHelper.stressCount >= DebugHelper.numCount) {
						DebugHelper.stopTrace();
					}
				}
			}
		}
	}

	public int getConversationClassification(double percentile_inhal, double std_inhal, double mean_exhal, double mean_ie, double meadian_ie, double mean_bd,double secondBest_bd, double std_strch) {
		if(mean_exhal <= 126.125)
		   if(std_inhal <= 44.47134)
		      if(mean_bd <= 0.222222) return SensorType.SPEAKING;
		      else return SensorType.QUIET;
		   else return SensorType.SMOKING;
		else
		   if(std_strch <= 315.59674)
		      if(mean_ie <= 0.665843)
		         if(percentile_inhal <= 50)
		            if(mean_exhal <= 132.4) return SensorType.SMOKING;
		            else 
		               if(meadian_ie <= 0.2226) return SensorType.SMOKING;
		               else return SensorType.SPEAKING;
		         else
		            if(meadian_ie <= 0.4888) return SensorType.SMOKING;
		            else return SensorType.SPEAKING;
		      else return SensorType.QUIET;
		   else 
		      if(secondBest_bd <= 20)
		         if(mean_bd <= 1.666667) return SensorType.SPEAKING;
		         else return SensorType.SMOKING;
		      else return SensorType.SPEAKING;
	}
}
