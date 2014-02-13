package edu.ucla.nesl.flowengine.node.feature;

import edu.ucla.nesl.flowengine.DataType;
import edu.ucla.nesl.flowengine.DebugHelper;
import edu.ucla.nesl.flowengine.InvalidDataReporter;
import edu.ucla.nesl.flowengine.node.DataFlowNode;

public class LombPeriodogram extends DataFlowNode {
	private static final String TAG = LombPeriodogram.class.getSimpleName();
	
	private boolean mIsMeanNew = false, mIsVarianceNew = false;
	private double mMean, mVariance;
	
	private String mName;
	private String mType;
	private long mTimestamp;
	private Object mData = null;
	
	@Override
	protected String processParentNodeName(String parentNodeName) {
		if (parentNodeName.contains("|Mean")) {
			return parentNodeName.replace("|Mean", "");
		} else if (parentNodeName.contains("|Variance")) {
			return parentNodeName.replace("|Variance", "");
		}
		return parentNodeName;
	}

	@Override
	protected void processInput(String name, String type, Object inputData, int length, long timestamp) {
		if (name.contains("Mean")) {
			if (!type.equals(DataType.DOUBLE)) {
				throw new UnsupportedOperationException("Unsupported type: " + type);
			}
			mIsMeanNew = true;
			mMean = (Double)inputData;
		} else if (name.contains("Variance")) {
			if (!type.equals(DataType.DOUBLE)) {
				throw new UnsupportedOperationException("Unsupported type: " + type);
			}
			mIsVarianceNew = true;
			mVariance = (Double)inputData;
		} else {
			if (length <= 0) {
				InvalidDataReporter.report("in " + TAG + ": name: " + name + ", type: " + type + ", length: " + length);
				return;
			}
			if (!type.equals(DataType.INTEGER_ARRAY)) {
				throw new UnsupportedOperationException("Unsupported type: " + type);
			}
			mName = name;
			mType = type;
			mData = inputData;
			mTimestamp = timestamp;
		}
		
		if (mIsMeanNew && mIsVarianceNew && mData != null) {
			double[][] psd = calculateLombPeriodogram((int[])mData);
			
			DebugHelper.dump(TAG, psd[0]);
			DebugHelper.dump(TAG, psd[1]);

			output(mName + "LombPeriodogram", "double[][]", psd, psd.length, mTimestamp);
			
			mIsMeanNew = false;
			mIsVarianceNew = false;
			mData = null;
		}
	}

	public double[][] calculateLombPeriodogram(int[] Rout) {	
		double[] t = new double[Rout.length];
		double A,Asq,B,C,Csq,D;
		double Ss2wt;
		double Sc2wt;
		double mx = mMean, vx = mVariance;
		int nt;
		int T;
		int nf;
		for (int i=0;i<Rout.length;i++)	{
			t[i] = (double) (i+1);
		}
		nt = t.length;
		T = nt-1;
		nf = (int) Math.round(0.5*4*1*nt);
		double[] f = new double[nf];
		for (int i=0;i<nf;i++) {
			f[i] = ((double)(i+ 0.0001))/(T*4);		
		}

		double[] wt=new double[t.length];
		double[] swt=new double[t.length];
		double[] cwt=new double[t.length];
		double[] cwt2=new double[t.length];
		double[] diff=new double[t.length];
		double[] sum=new double[t.length];
		double[] swttau=new double[t.length];
		double[] cwttau = new double[t.length];
		double wtau= 0;
		double swtau= 0;
		double cwtau= 0;
		double[] P= new double[f.length];
		for (int i=0;i<Rout.length;i++) {
			Rout[i]  = (int) (Rout[i]-mx);
		}

		for (int i=0;i<nf;i++) {
			for (int j=0;j<t.length;j++) {
				wt[j]  = (double) (2*Math.PI*f[i]*t[j]);
				swt[j] = (double) Math.sin(wt[j]);
				cwt[j] = (double) Math.cos(wt[j]);
				cwt2[j]= 2*cwt[j];  
				diff[j]=cwt[j]-swt[j];
				sum[j]=cwt[j]+swt[j];
			}
			Ss2wt = MatrixMTI(cwt2,swt);// Row by column to be done     	
			Sc2wt = MatrixMTI(diff,sum); 
			wtau  = (double) (0.5*Math.atan2(Ss2wt,Sc2wt));   	  
			swtau = (double) Math.sin(wtau);
			cwtau = (double) Math.cos(wtau);
			for (int j=0;j<t.length;j++) {
				swttau[j] = cwtau*swt[j] - swtau*cwt[j];
				cwttau[j] = cwtau*cwt[j] + swtau*swt[j];
			} 
			A=MatrixMT(Rout,cwttau);
			B=MatrixMTI(cwttau,cwttau);
			C=MatrixMT(Rout,swttau);
			D=MatrixMTI(swttau,swttau);	
			P[i] =(double) ((Math.pow(A, 2))/B + (Math.pow(C, 2))/D);
			P[i]= P[i]/(2*vx);
		}
		double[][] send = new double[2][f.length];
		System.arraycopy(P,0,send[0],0,f.length);
		System.arraycopy(f,0,send[1],0,f.length);
		return send;
	}

	private double MatrixMT(int[] rout,double[] array1 )
	{
		double  array2;
		int x1=rout.length;
		array2=0;
		for (int i=0; i<x1;i++) 
			array2 =array2+(rout[i]*array1[i]);     
		return array2;
	}

	private double MatrixMTI(double[] rout,double[] array1 )
	{
		double  array2;
		int x1=rout.length;
		array2=0;
		for (int i=0; i<x1;i++) 
			array2 =array2+(rout[i]*array1[i]);     
		return array2;
	}
}
