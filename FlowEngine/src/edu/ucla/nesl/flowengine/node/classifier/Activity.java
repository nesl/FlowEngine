package edu.ucla.nesl.flowengine.node.classifier;

import edu.ucla.nesl.flowengine.DebugHelper;
import edu.ucla.nesl.flowengine.SensorType;
import edu.ucla.nesl.flowengine.node.DataFlowNode;

public class Activity extends DataFlowNode {
	private static final String TAG = Activity.class.getSimpleName();
	
	private double[] mFFT1_10 = null;
	private double[] mScaledFFT1_3 = null;
	private boolean mIsVarianceNew = false, mIsScaledVarianceNew = false, mIsSpeedNew = false;
	private double mVariance, mScaledVariance, mSpeed;

	@Override
	protected void processInput(String name, String type, Object data, int length, long timestamp) {
		if (name.contains("AccelerometerRMSFFT1.0-10.0-1.0")) {
			mFFT1_10 = (double[])data;
		} else if (name.contains("AccelerometerRMSVariance")) {
			mVariance = (Double)data;
			mIsVarianceNew = true;
		} else if (name.contains("AccelerometerRMSScale310.0FFT1.0-3.0-1.0")) {
			mScaledFFT1_3 = (double[])data;
		} else if (name.contains("AccelerometerRMSScale310.0Variance")) {
			mScaledVariance = (Double)data;
			mIsScaledVarianceNew = true;
		} else if (name.contains("GPS")) {
			mSpeed = ((double[])data)[3];
			mIsSpeedNew = true;
		} else {
			throw new UnsupportedOperationException("Unsupported name: " + name);
		}

		String activity = null;
			
		if (mIsSpeedNew && mSpeed > 0.29 && mIsScaledVarianceNew && mScaledFFT1_3 != null) {
			activity = classifyActivityWithGPS(mSpeed, mScaledVariance, mScaledFFT1_3);
			mIsSpeedNew = false;
			mIsScaledVarianceNew = false;
			mScaledFFT1_3 = null;
		} else if (mFFT1_10 != null && mIsVarianceNew) {
			activity = classifyActivityWithoutGPS(mVariance, mFFT1_10);
			mIsVarianceNew = false;
			mFFT1_10 = null;
		}
		
		if (activity != null) {
			DebugHelper.log(TAG, activity);
			output(SensorType.ACTIVITY_CONTEXT_NAME, "String", activity, 0, timestamp);
		}
	}
	
	private String classifyActivityWithGPS(double gpsSpeed, double scaledVariance, double[] scaledFFT1_3)
	{
		String output = "still";

		if(scaledFFT1_3[2] <= 2663606.69633)
			if(gpsSpeed <= 6.37)
				if(scaledFFT1_3[1] <= 463400.011249)
					if(scaledVariance <= 205.972492)
						if(scaledVariance <= 13.084102)
							if(gpsSpeed <= 0.8)
								output = "still";
							else
								output = "drive";
						else
							if(gpsSpeed <= 1.33)
								output = "still";
							else
								output = "drive";
					else
						if(gpsSpeed <= 1.84)
							if(scaledFFT1_3[0] <= 125502.942136)
								output = "walk";
							else
								output = "walk";
						else
							output = "bike";

				else
					if(scaledVariance <= 41153.783729)
						if(gpsSpeed <= 2.12)
							output = "walk";
						else
							output = "bike";
					else
						output = "run";
			else
				output = "drive";

		else
			if(scaledFFT1_3[2] <= 5132319.94693)
				if(gpsSpeed <= 1.86)
					output = "walk";
				else
					output = "run";
			else
				output = "run";

		return output;
	}

	private String classifyActivityWithoutGPS(double variance, double[] fft1_10)
	{	
		if (variance <= 0.0047)
		{
			if (variance <= 0.0016) 
				return "still";
			else
			{
				if (fft1_10[4] <= 0.1532)
				{
					if (fft1_10[0] <= 0.5045) 
						return "still";
					else 
						return "walk";
				}
				else 
					return "still";
			}
		}
		else
		{
			if (fft1_10[2] <= 60.3539)
			{
				if (variance <= 0.0085)
				{
					if (fft1_10[7] <= 0.0506) 
						return "walk";
					else
					{
						if (fft1_10[1] <= 2.8607) 
							return "still";
						else 
							return "walk";
					}
				}
				else
				{
					if (fft1_10[1] <= 2.7725)
					{
						if (fft1_10[0] <= 13.0396) 
							return "walk";
						else 
							return "still";
					}
					else 
						return "walk";
				}
			}
			else 
				return "run";
		}
	}
}
