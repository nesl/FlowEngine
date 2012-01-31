package edu.ucla.nesl.flowengine.node.classifier;

import android.util.Log;
import edu.ucla.nesl.flowengine.aidl.WaveSegment;
import edu.ucla.nesl.flowengine.node.DataFlowNode;

public class ActivityClassifier extends DataFlowNode {
	private static final String TAG = ActivityClassifier.class.getSimpleName();
	
	private double[] mPower = null;
	private double[] mAvgVar = null;

	@Override
	public void inputData(String type, Object inputData) {
		if (type.equals("Goertzel")) {
			mPower = (double[])inputData;
		} else if (type.equals("AverageVariance")) {
			mAvgVar = (double[])inputData;
		}

		if (mPower != null && mAvgVar != null) {
			String activity = classify();
			mPower = null; 
			mAvgVar = null;
			Log.d(TAG, "Activity: " + activity);
		}
	}
	
	private String classify()
	{	
		if (mAvgVar[1] <= 0.0047)
		{
			if (mAvgVar[1] <= 0.0016) return "still";
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
				if (mAvgVar[1] <= 0.0085)
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
