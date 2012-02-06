package edu.ucla.nesl.flowengine.node.feature;

import java.util.ArrayList;

import edu.ucla.nesl.flowengine.DebugHelper;
import edu.ucla.nesl.flowengine.node.DataFlowNode;

public class Stretch extends DataFlowNode {
	private static final String TAG = Stretch.class.getSimpleName();
	
	private final int EMPTY_INT_ARRAY[] = new int[0];
	private int[] mRPV = null;
	private int[] mRIP = null;
	private String mName;
	
	@Override
	public void inputData(String name, String type, Object inputData, int length) {
		if (name.contains("RIPRealPeakValley")) {
			mRPV = (int[])inputData;
		} else if (name.contains("RIP")) {
			mRIP = (int[])inputData;
			mName = name;
		} else {
			throw new UnsupportedOperationException("Unsupported name: " + name);
		}
		
		if (mRPV != null && mRIP != null) {
			ArrayList<Integer> list=new ArrayList<Integer>();

			if(mRPV.length < 8)
				outputData("Stretch", "int[]", EMPTY_INT_ARRAY, 0);

			for(int i=0;i<mRPV.length-4;i+=4)
			{
				int valley1=mRPV[i];
				int valley2=mRPV[i+4];
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

			outputData(mName + "Stretch", "int[]", stretches, stretches.length);
			
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
		if(index2<index1)
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
