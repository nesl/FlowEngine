package edu.ucla.nesl.flowengine.node.feature;

import java.util.ArrayList;

import edu.ucla.nesl.flowengine.DataType;
import edu.ucla.nesl.flowengine.DebugHelper;
import edu.ucla.nesl.flowengine.InvalidDataReporter;
import edu.ucla.nesl.flowengine.node.DataFlowNode;

public class Stretch extends DataFlowNode {
	private static final String TAG = Stretch.class.getSimpleName();
	
	private final int EMPTY_INT_ARRAY[] = new int[0];
	private int[] mRPV = null;
	private int[] mRIP = null;
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

	private int[] calculateStretch() {
		ArrayList<Integer> list = new ArrayList<Integer>();

		for(int i=0;i<mRPV.length-4;i+=4)
		{
			int valley1=mRPV[i];
			int valley2=mRPV[i+4];
			if (valley1 < 0 || valley2 < 0) {
				continue;
			}
			int strch=getStretch(valley1,valley2);
			list.add(new Integer(strch));
		}
		//converting the ArrayList to array
		int stretches[]=new int[list.size()];
		for(int j=0;j<list.size();j++)
		{
			stretches[j]=list.get(j).intValue();
		}

		DebugHelper.dump(TAG, stretches);
		
		return stretches;
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
			if(mRPV.length < 8) {
				output(mName + "Stretch", DataType.INTEGER_ARRAY, EMPTY_INT_ARRAY, 0, mTimestamp);
			} else {
				int[] stretches = calculateStretch();
				output(mName + "Stretch", DataType.INTEGER_ARRAY, stretches, stretches.length, mTimestamp);
			}
			
			mRPV = null;
			mRIP = null;
		}
	}

	private int getStretch(int index1,int index2)
	{
		if(index1==index2)		//this is a wrong rpv calculation case...should not apear
			return 0;
		int stretch=0;
		//to avoid negetive array allocation exception......need to check why it happens sometimes
		if(index2 < index1)
		{
			int temp=index1;
			index1=index2;
			index2=temp;
		}
		int len=index2-index1;
		int data[]=new int[len];

		int i=index1;
		int k=0;

		while(i<index2)
		{
			data[k++]=mRIP[i];
			i++;
		}
		int maxPos=getMaxPosition(data);
		int minPos=getMinPosition(data,maxPos);			//min position search is started from max pos.
		stretch=data[maxPos]- data[minPos];
		
		return stretch;
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
}
