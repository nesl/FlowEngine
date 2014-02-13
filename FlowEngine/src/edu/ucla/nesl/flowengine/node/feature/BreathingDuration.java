package edu.ucla.nesl.flowengine.node.feature;

import java.util.ArrayList;

import edu.ucla.nesl.flowengine.DataType;
import edu.ucla.nesl.flowengine.DebugHelper;
import edu.ucla.nesl.flowengine.InvalidDataReporter;
import edu.ucla.nesl.flowengine.node.DataFlowNode;

public class BreathingDuration extends DataFlowNode {
	private static final String TAG = BreathingDuration.class.getSimpleName();

	private int mRIP[] = null;
	private int mRPV[] = null;
	private String mName;
	private long mTimestamp;

	@Override
	protected String processParentNodeName(String parentNodeName) {
		if (parentNodeName.contains("|PeakValley")) {
			return parentNodeName.split("\\|PeakValley")[0];
		} else if (parentNodeName.contains("|Buffer")) {
			return parentNodeName.split("\\|Buffer")[0];
		}
		return parentNodeName;
	}

	@Override
	protected void processInput(String name, String type, Object inputData, int length, long timestamp) {
		if (length <= 0) {
			InvalidDataReporter.report("in " + TAG + ": name: " + name + ", type: " + type + ", length: " + length);
			return;
		}
		if (!type.equals(DataType.INTEGER_ARRAY)) {
			throw new UnsupportedOperationException("Unsupported type: " + type);
		}
		if (name.contains("RIPPeakValley")) {
			mRPV = (int[])inputData;
		} else if (name.contains("RIP")) {
			mRIP = (int[])inputData;
			mName = name;
			mTimestamp = timestamp;
		} else {
			throw new UnsupportedOperationException("Unsupported name: " + name);
		}
		
		if (mRPV != null && mRIP != null) {
			int[] bduration = getBdurationUsingRPV(mRPV);
			DebugHelper.dump(TAG, bduration);
			output(mName + "BreathingDuration", DataType.INTEGER_ARRAY, bduration, bduration.length, mTimestamp);
			mRPV = null;
			mRIP = null;
		}
	}

	private int[] getBdurationUsingRPV(int rpv[])
	{
		ArrayList<Integer> list=new ArrayList<Integer>();
		int zero[]=new int[0];
//		String rpvs="";
//		for(int k=0;k<rpv.length;k++)
//			rpvs+=rpv[k]+",";
//		Log.d("BdurationCalculation","rpv= "+rpvs);
//		Log.d("BdurationCalculation","rpv.length= "+rpv.length);
		if(rpv.length<8)
			return zero;
		for(int i=0;i<rpv.length-4;i+=4)
		{
			int valley1=rpv[i];
			int valley2=rpv[i+4];
			int bd=getBduration(valley1,valley2);
			list.add(new Integer(bd));
		}
		//converting the ArrayList to array
		int bdurations[]=new int[list.size()];
		for(int j=0;j<list.size();j++)
		{
			bdurations[j]=list.get(j).intValue();
		}
		return bdurations;
	}
	//returns stretch from data range [index1,index2]
	private int getBduration(int index1,int index2)
	{
		if(index2<=index1)
		{
			DebugHelper.log(TAG, "Wrong RPV so that index2<=index1");
			return 0;
		}
		int len=index2-index1+1;
		int data[]=new int[len];

		int i=index1;
		int k=0;
		//System.out.println("Index1= "+index1+" Index2= "+index2);
		//System.out.println("buffer length= "+mRIP.length);
//		for(int l=index1;l<index2;l++)
//			System.out.print("index= "+l+" value= "+buffer[l]+",");
		while(i<=index2)
		{
			int temp=mRIP[i];
			data[k++]=temp;
			i++;
		}
		int maxPos=getMaxPosition(data);
		int minPos=getMinPosition(data,maxPos);			//min position search is started from max pos.
		int globalMaxPos=maxPos+index1+1;
		int globalMinPos=minPos+index1+1;
		if(globalMinPos>=index2)
			return 0;
		int b_duration=B_duration(globalMinPos, data[minPos],index2);

		return b_duration;
	}
	
	private int getMaxPosition(int[] data)
	{
		int pos=0;
		int max=data[0];
		for(int i=1;i<data.length;i++)
		{
			if(data[i]>=max)
			{	
				max=data[i];
				pos=i;
			}
		}
		return pos;
	}
	
	private int getMinPosition(int[] data, int index)
	{
		int pos=0;
		int min=data[index];
		for(int i=index;i<data.length;i++)
		{
			if(data[i]<=min)
			{	
				min=data[i];
				pos=i;
			}
		}
		return pos;
	}
	
	private int B_duration(int ind,int value,int end)			//duration at the bottom
	{
		int duration=0;
		int i=ind;
		int temp=mRIP[i++];
		DebugHelper.log(TAG, "B_durationMethod: value= "+value+" start data= "+temp+",");
		while(temp<=1.025*value && i<mRIP.length && i<end)
		{
			//System.out.print(temp+",");
			temp=mRIP[i++];
			//System.out.print("B_durationMethod:= "+temp+",");
			duration++;
		}
		return duration;
	}

}
