package edu.ucla.nesl.flowengine.node.feature;

import android.util.Log;
import edu.ucla.nesl.flowengine.node.DataFlowNode;

public class Goertzel extends DataFlowNode {
	private static final String TAG = Goertzel.class.getSimpleName();

	private double mStartFrequency;
	private double mEndFrequency;
	private double mStepFrequency;
	private int mNumResult;
	private double[] mPowerSpectrum;
	
	public Goertzel(double startFrequency, double endFrequency, double stepFrequency) {
		mStartFrequency = startFrequency;
		mEndFrequency = endFrequency;
		mStepFrequency = stepFrequency;
		mNumResult = (int)Math.floor((mEndFrequency-mStartFrequency)/mStepFrequency + 1.0);
		mPowerSpectrum = new double[mNumResult];
	}
	
	private double calculatePower(double frequency, double[] data) {
		double s_prev = 0;
        double s_prev2 = 0;
        double coeff = 2 * Math.cos( (2*Math.PI*frequency) / data.length);
        double s;
        for (double sample: data)
        {
            s = sample + coeff*s_prev  - s_prev2;
            s_prev2 = s_prev;
            s_prev = s;
        }
        return s_prev2*s_prev2 + s_prev*s_prev - coeff*s_prev2*s_prev;
	}
	
	@Override
	public void inputData(String name, String type, Object inputData, int length) {
		double[] data = (double[])inputData;
		int i = 0;
		for (double frequency = mStartFrequency; frequency <= mEndFrequency; frequency += mStepFrequency) {
			mPowerSpectrum[i] = calculatePower(frequency, data);
			i += 1;
		}
		
		String debug = "Goertzel: ";
		for (double value: mPowerSpectrum) {
			debug += String.format("%.2f, ", value);
		}
		Log.d(TAG, debug);
		
		outputData("Goertzel", "double[]", mPowerSpectrum, mPowerSpectrum.length);
	}
}
