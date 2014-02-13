package edu.ucla.nesl.flowengine.node.feature;

import edu.ucla.nesl.flowengine.DataType;
import edu.ucla.nesl.flowengine.DebugHelper;
import edu.ucla.nesl.flowengine.InvalidDataReporter;
import edu.ucla.nesl.flowengine.node.DataFlowNode;

public class FFT extends DataFlowNode {
	private static final String TAG = FFT.class.getSimpleName();

	private double mStartFrequency;
	private double mEndFrequency;
	private double mStepFrequency;
	private int mNumResult;
	private double[] mPowerSpectrum;
	
	@Override
	protected String processParentNodeName(String parentNodeName) {
		if (parentNodeName.contains("|Buffer")) {
			return parentNodeName.split("\\|Buffer")[0];
		}
		return parentNodeName;
	}

	public FFT(String parameterizedSimpleNodeName, double startFrequency, double endFrequency, double stepFrequency) {
		super(parameterizedSimpleNodeName);
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

	private void calculateSpectrum(double[] data) {
		int i = 0;
		for (double frequency = mStartFrequency; frequency <= mEndFrequency; frequency += mStepFrequency) {
			mPowerSpectrum[i] = calculatePower(frequency, data);
			i += 1;
		}
	}
	
	@Override
	protected void processInput(String name, String type, Object inputData, int length, long timestamp) {
		if (length <= 0) {
			InvalidDataReporter.report("in " + TAG + ": name: " + name + ", type: " + type + ", length: " + length);
			return;
		}
		if (!type.equals(DataType.DOUBLE_ARRAY)) {
			throw new UnsupportedOperationException("Unsupported type: " + type);
		}

		calculateSpectrum((double[])inputData);
		DebugHelper.dump(TAG, mPowerSpectrum);
		
		output(name + String.format("FFT%.1f-%.1f-%.1f", mStartFrequency, mEndFrequency, mStepFrequency), DataType.DOUBLE_ARRAY, mPowerSpectrum, mPowerSpectrum.length, timestamp);
	}
}
