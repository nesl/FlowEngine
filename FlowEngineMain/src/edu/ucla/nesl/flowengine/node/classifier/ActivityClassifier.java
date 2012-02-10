package edu.ucla.nesl.flowengine.node.classifier;

import edu.ucla.nesl.flowengine.DebugHelper;
import edu.ucla.nesl.flowengine.node.DataFlowNode;

public class ActivityClassifier extends DataFlowNode {
	private static final String TAG = ActivityClassifier.class.getSimpleName();
	
	private double[] mPower = null;
	private boolean mIsMeanNew = false, mIsVarianceNew = false;
	private double mMean, mVariance;

	@Override
	public void inputData(String name, String type, Object inputData, int length, long timestamp) {
		if (name.contains("RMSGoertzel")) {
			mPower = (double[])inputData;
		} else if (name.contains("RMSVariance")) {
			mMean = (Double)inputData;
			mIsMeanNew = true;
		} else if (name.contains("RMSMean")) {
			mVariance = (Double)inputData;
			mIsVarianceNew = true;
		}

		if (mPower != null && mIsMeanNew && mIsVarianceNew ) {
			
			String activity = classify();
			
			mPower = null; 
			mIsMeanNew = false;
			mIsVarianceNew = false;
			
			DebugHelper.log(TAG, "Activity: " + activity);
		}
	}
	
	private String classify()
	{	
		if (mVariance <= 0.0047)
		{
			if (mVariance <= 0.0016) return "still";
			else
			{
				if (mPower[4] <= 0.1532)
				{
					if (mPower[0] <= 0.5045) return "still";
					else return "walk";
				}
				else return "still";
			}
		}
		else
		{
			if (mPower[2] <= 60.3539)
			{
				if (mVariance <= 0.0085)
				{
					if (mPower[7] <= 0.0506) return "walk";
					else
					{
						if (mPower[1] <= 2.8607) return "still";
						else return "walk";
					}
				}
				else
				{
					if (mPower[1] <= 2.7725)
					{
						if (mPower[0] <= 13.0396) return "walk";
						else return "still";
					}
					else return "walk";
				}
			}
			else return "run";
		}
	}


}
