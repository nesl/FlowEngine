package edu.ucla.nesl.flowengine.node.feature;

import java.util.ArrayList;
import java.util.Arrays;

import android.util.Log;
import edu.ucla.nesl.flowengine.node.DataFlowNode;

public class IERatio extends DataFlowNode {
	private static final String TAG = IERatio.class.getSimpleName();
	
	private final int mDurationThreshold=100;
	
	private int mPeakThreshold;
	private int mGlobalIndex;
	
	private static double percentileEvaluate(final int[] values, final double p) {
		if ((p > 100) || (p <= 0)) {
			throw new IllegalArgumentException("Invalid quantile value: " + p);
		}
		double n = (double) values.length;
		if (n == 0) {
			return Double.NaN;
		}
		if (n == 1) {
			return values[0]; // always return single value for n = 1
		}
		double pos = p * (n + 1) / 100;
		double fpos = Math.floor(pos);
		int intPos = (int) fpos;
		double dif = pos - fpos;
		int[] sorted = new int[values.length];
		System.arraycopy(values, 0, sorted, 0, values.length);
		Arrays.sort(sorted);

		if (pos < 1) {
			return sorted[0];
		}
		if (pos >= n) {
			return sorted[values.length - 1];
		}
		double lower = sorted[intPos - 1];
		double upper = sorted[intPos];
		return lower + dif * (upper - lower);
	}
	
	/**
	 * eliminates if peaks are detected wrongly
	 * @param list
	 * @return
	 */
	private ArrayList<Integer> postProcessing(ArrayList<Integer> list)
	{
		if(list.size()<8) //at least two peaks required for this processing
			return list;
		int size=list.size();
		for(int i=2;i<size-4;i+=4)
		{
			if(list.get(i+4).intValue()-list.get(i).intValue()<mDurationThreshold)
			{
				size-=4;
				if(list.get(i+5).intValue()>list.get(i+1).intValue())
				{
					list.remove(i);
					list.remove(i);
					list.remove(i);
					list.remove(i);
				}
				else
				{
					list.remove(i+2);
					list.remove(i+2);
					list.remove(i+2);
					list.remove(i+2);
				}
			}
		}
		//for negetive inhalations.....//need to thought carefully later
		if(list.size()>=8)
		{
			for(int i=0;i<list.size()-4;i+=4)
			{
				if(list.get(i).intValue()==list.get(i+4).intValue()||list.get(i).intValue()<0) //for corrupted data, index might be -1 so we need to remove this things
				{
					list.remove(i);
					list.remove(i);
					list.remove(i);
					list.remove(i);
				}
			}
		}
		return list;
	}

	/**
	 * @param prevRealPeakIndex prev real peak location in local min max array array
	 * @param data local min max array in [index+valley ,index+peak] format
	 * @param startIndex the starting point of the back tracking to search the valley anchor
	 * @return valleyAnchor Index
	 */
	private int getValleyAnchorIndexBelowThreshold(int data[],int startIndex, int prevRealPeak)
	{
		int prevRealPeakIndex=0;
		if(prevRealPeak==-1)
			return 0;
		if(startIndex>=data.length)
			return 0;
		for(int j=startIndex;j>0;j=j-2)
		{
			if(data[j]==prevRealPeak)
			{
				prevRealPeakIndex=j;
				break;
			}
		}
		for(int i=startIndex;i>0;i=i-4)
		{
			if(data[i]<mPeakThreshold && i>prevRealPeakIndex)
				return i;
		}
		return 0;
	}

	private ArrayList<Integer> addToTheList(ArrayList<Integer> list,int anchorIndex, int anchorValue)
	{
		Integer val=new Integer(anchorValue);
		Integer ind=new Integer(anchorIndex);
		list.add(ind);
		list.add(val);
		return list;
	}

	/**
	 * calculates peaks and valleys (false + real) from the data buffer
	 * @param buffer
	 * @return list of tuple containing (valleyIndex, valley, peakIndex, peak). so if any method wants to use this method, it should read all the four values together.
	 * @author Mahbub
	 */
	private int[] getAllPeaknValley(int[] data)
	{

		int prev_value1=0;
		int curr_value=0;
		boolean isStarting=true;
		int length=data.length;
		ArrayList<Integer> list=new ArrayList<Integer>();

		try {
			for(int i=0;i<length;){
				int line;
				if(isStarting && (i < length-1))
				{
					isStarting=false;
					prev_value1=data[i++];
					mGlobalIndex++;
					curr_value=data[i++];
					mGlobalIndex++;
					//skipping up to the first increasing sequence
					while((prev_value1>=curr_value)&& (i < length))
					{
						prev_value1=curr_value;
						line=data[i++];
						mGlobalIndex++;
						curr_value=line;
					}
					list=addToTheList(list, mGlobalIndex-1, prev_value1);		//prev_value1 is the current valley
					continue;
				}
				if(curr_value>prev_value1 )			//this means the sequence is increasing
				{
					while((prev_value1<=curr_value)&& (i < length))
					{
						prev_value1=curr_value;
						line=data[i++];
						mGlobalIndex++;
						curr_value=line;
					}
					list=addToTheList(list,mGlobalIndex-1, prev_value1);		//prev_value1 is the current valley
				}else //if(Integer.parseInt(curr_value)<Integer.parseInt(prev_value1))
				{
					while((prev_value1>=curr_value)&& (i < length))
					{
						prev_value1=curr_value;
						line=data[i++];
						mGlobalIndex++;
						curr_value=line;
					}
					if(i!=length)
						list=addToTheList(list,mGlobalIndex-1, prev_value1);		//prev_value1 is the current valley
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		//converting the ArrayList to array
		int peakValleys[]=new int[list.size()];
		for(int i=0;i<list.size();i++)
		{
			peakValleys[i]=list.get(i).intValue();
		}
		return peakValleys;
	}

	/**
	 * calculates real peaks and valleys from the data buffer
	 * @param data
	 * @return list of tuple containing (valleyIndex, valley, peakIndex, peak). so if any method wants to use this method, it should read all the four values together.
	 * @author Mahbub
	 */
	private int[] getRealPeaknValley(int[] data)			//check whether it is multiple of four....if not then discard the last part which does not fit to
	{

		boolean isStarting=true;
		ArrayList<Integer> list=new ArrayList<Integer>();

		int prev1_valleyIndex=-1;
		int prev1_valley=-1;		
		int prev1_peakIndex=-1;
		int prev1_peak=-1;
		int current_valleyIndex=-1;
		int current_valley=-1;		
		int current_peakIndex=-1;
		int current_peak=-1;
		int valleyAnchor=-1;
		int valleyAnchorIndex=-1;
		int realPeak=-1;
		int realPeakIndex=-1;
		int realValley=-1;
		int realValleyIndex=-1;
		
		//int valleyAnchorIndex=-1;
		//int valleyAnchor=-1;
		int valleyAnchorIndex1=-1;
		int valleyAnchor1=-1;
		int peakAnchor=-1;
		int peakAnchorIndex=-1;


		//I have to consider four values together to calculate the real peaks and valleys
		int i=0;
		int size=data.length;
		outer:
		for(;i<size;)
		{
			if(isStarting)		//check ...it should be equal or greater
			{
				//find the first real valley
				isStarting=false;
				if((size-i)<4)
				{
					i+=4;
					continue outer;
				}
				prev1_valleyIndex=data[i];
				prev1_valley=data[i+1];
				prev1_peakIndex=data[i+2];
				prev1_peak=data[i+3];
				valleyAnchor=prev1_valley;
				valleyAnchorIndex=prev1_valleyIndex;
//				if(prev1_peak>=mPeakThreshold)
//				{
//					realPeak=prev1_peak;
//					realPeakIndex=prev1_peakIndex;
//					realValley=valleyAnchor;
//					realValleyIndex=valleyAnchorIndex;
//					list=addToTheList(list, realValleyIndex, realValley);
//					list=addToTheList(list, realPeakIndex, realPeak);
//				}
				i+=4;
				if((size-i)<4)
				{
					i+=4;
					continue outer;
				}
				current_valleyIndex=data[i];
				current_valley=data[i+1];		
				current_peakIndex=data[i+2];
				current_peak=data[i+3];
				i+=4;
			}
			if(current_peak>prev1_peak)			//this means the sequence is increasing
			{
				while(prev1_peak<=current_peak)		//this is increasing trend
				{
					if((current_peak>=mPeakThreshold) /*&& realPeak!=0*/)				//then the previous valleyAnchor is real valley, check the peak to previous real peak against duration threshold
					{
						//then real valley update, inhalation period, exhalation period, IE ratio.
						if(peakAnchorIndex!=-1||(current_peakIndex - realPeakIndex)>=mDurationThreshold || realPeak==-1)
						{
							if((peakAnchorIndex!=-1)&&((current_peakIndex- peakAnchorIndex)>=mDurationThreshold)&& (realPeakIndex!=peakAnchorIndex)&& peakAnchorIndex>valleyAnchorIndex)
							{
								realPeak=peakAnchor;
								realPeakIndex=peakAnchorIndex;
								if(valleyAnchor<mPeakThreshold || valleyAnchorIndex<valleyAnchorIndex1)
								{
									realValley=valleyAnchor;
									realValleyIndex=valleyAnchorIndex;
								}
								else
								{
									realValley=valleyAnchor1;					//this is a previous valley candidate
									realValleyIndex=valleyAnchorIndex1;
								}
								peakAnchor=current_peak;
								peakAnchorIndex=current_peakIndex;
								if(realPeak!=-1)
								{
									list=addToTheList(list, realValleyIndex, realValley);
									list=addToTheList(list, realPeakIndex, realPeak);
								}
							}
							peakAnchor=current_peak;
							peakAnchorIndex=current_peakIndex;
							if(current_valley<mPeakThreshold || realValleyIndex==prev1_peakIndex)
							{
								valleyAnchor=current_valley;
								valleyAnchorIndex=current_valleyIndex;
							}
							else
							{
//								valleyAnchor=prev1_valley;
//								valleyAnchorIndex=prev1_valleyIndex;
								if(prev1_valley<mPeakThreshold)
								{
									valleyAnchor=prev1_valley;
									valleyAnchorIndex=prev1_valleyIndex;
								}else
								{
									int m=getValleyAnchorIndexBelowThreshold(data, i+1, realPeak);
									if(m==0)
									{
										valleyAnchor=prev1_valley;
										valleyAnchorIndex=prev1_valleyIndex;
									}
									else if(data[i]<peakAnchorIndex)
									{
										valleyAnchor=data[m];
										valleyAnchorIndex=data[m-1];
									}
								}
							}
						}
					}
					prev1_valleyIndex=current_valleyIndex;
					prev1_valley=current_valley;
					prev1_peakIndex=current_peakIndex;
					prev1_peak=current_peak;
					if((size-i)<4)
					{
						i+=4;
						continue outer;
					}
					current_valleyIndex=data[i];				//line=dis.readLine();
					current_valley=data[i+1];		
					current_peakIndex=data[i+2];
					current_peak=data[i+3];
					i+=4;										//curr_value=line.split(" ");
				}
				if(realPeakIndex<peakAnchorIndex && realPeakIndex!=-1)
				{
					
					realPeak=peakAnchor;
					realPeakIndex=peakAnchorIndex;
					
					if(valleyAnchor<mPeakThreshold ||valleyAnchorIndex1<realPeakIndex || valleyAnchorIndex<valleyAnchorIndex1 || (valleyAnchorIndex1<realValleyIndex && valleyAnchorIndex>realValleyIndex))
					{
						realValley=valleyAnchor;
						realValleyIndex=valleyAnchorIndex;
					}
					else
					{
						realValley=valleyAnchor1;					//this is a previous valley candidate
						realValleyIndex=valleyAnchorIndex1;
					}
					list=addToTheList(list, realValleyIndex, realValley);
					list=addToTheList(list, realPeakIndex, realPeak);
				}
				else if(current_peak>=mPeakThreshold)
				{
					if(realPeakIndex==-1 && realPeakIndex<peakAnchorIndex)
					{
						//check this thing
						realPeak=peakAnchor;
						realPeakIndex=peakAnchorIndex;
						realValley=valleyAnchor;
						realValleyIndex=valleyAnchorIndex;
						list=addToTheList(list, realValleyIndex, realValley);
						list=addToTheList(list, realPeakIndex, realPeak);
					}
					if((current_peakIndex - realPeakIndex)>=mDurationThreshold)
					{
						peakAnchor=current_peak;
						peakAnchorIndex=current_peakIndex;
						if(current_valley<mPeakThreshold || realValleyIndex==prev1_peakIndex)
						{
							valleyAnchor=current_valley;
							valleyAnchorIndex=current_valleyIndex;
						}
						else
						{
//													valleyAnchor=prev1_valley;
//													valleyAnchorIndex=prev1_valleyIndex;
							if(prev1_valley<mPeakThreshold)
							{
								valleyAnchor=prev1_valley;
								valleyAnchorIndex=prev1_valleyIndex;
							}else
							{
								int m=getValleyAnchorIndexBelowThreshold(data, i+1, realPeak);
								if(m==0)
								{
									valleyAnchor=prev1_valley;
									valleyAnchorIndex=prev1_valleyIndex;
								}
								else if(data[i]<peakAnchorIndex)
								{
									valleyAnchor=data[m];
									valleyAnchorIndex=data[m-1];
								}
							}
						}
					}
				}
			}else
			{
				while(prev1_peak>=current_peak)		//this is decreasing trend
				{
					if((current_peak>=mPeakThreshold) && ((current_peakIndex - realPeakIndex)>=mDurationThreshold) && realPeak!=-1)				//then the previous valleyAnchor is real valley, check the peak to previous real peak against duration threshold
					{
						if(realPeakIndex<peakAnchorIndex && realPeakIndex!=-1 && ((current_peakIndex - peakAnchorIndex)>=mDurationThreshold))
						{
							realPeak=peakAnchor;
							realPeakIndex=peakAnchorIndex;
							if(valleyAnchor<mPeakThreshold)
							{
								realValley=valleyAnchor;
								realValleyIndex=valleyAnchorIndex;
							}
							else
							{
								realValley=valleyAnchor1;					//this is a previous valley candidate
								realValleyIndex=valleyAnchorIndex1;
							}					
							list=addToTheList(list, realValleyIndex, realValley);
							list=addToTheList(list, realPeakIndex, realPeak);
						}
						peakAnchor=current_peak;
						peakAnchorIndex=current_peakIndex;
						if(current_valley<mPeakThreshold || realValleyIndex==prev1_valleyIndex)
						{
							valleyAnchor=current_valley;
							valleyAnchorIndex=current_valleyIndex;
						}
						else
						{
//							valleyAnchor=prev1_valley;
//							valleyAnchorIndex=prev1_valleyIndex;
							if(prev1_valley<mPeakThreshold)
							{
								valleyAnchor=prev1_valley;
								valleyAnchorIndex=prev1_valleyIndex;
							}else
							{
								int m=getValleyAnchorIndexBelowThreshold(data, i+1, realPeak);
								if(m==0)
								{
									valleyAnchor=prev1_valley;
									valleyAnchorIndex=prev1_valleyIndex;
								}
								else if(data[i]<peakAnchorIndex)
								{
									valleyAnchor=data[m];
									valleyAnchorIndex=data[m-1];
								}
							}
						}
					}
					prev1_valleyIndex=current_valleyIndex;			//prev_value1=curr_value;
					prev1_valley=current_valley;
					prev1_peakIndex=current_peakIndex;
					prev1_peak=current_peak;
					
					if((size-i)<4)
					{
						i+=4;
						continue outer;
					}
					
					current_valleyIndex=data[i];				//line=dis.readLine();
					current_valley=data[i+1];		
					current_peakIndex=data[i+2];
					current_peak=data[i+3];
					i+=4;										//curr_value=line.split(" ");
				}
				valleyAnchor1=current_valley;
				valleyAnchorIndex1=current_valleyIndex;
			}
		}
		ArrayList<Integer> list1=postProcessing(list);
		//converting the ArrayList to array
		int realPeakValleys[]=new int[list1.size()];
		for(int j=0;j<list1.size();j++)
		{
			realPeakValleys[j]=list1.get(j).intValue();
		}
		return realPeakValleys;
	}
	
	private int[] realPeakValley(int[] data) {
		mGlobalIndex=0;
		int len = data.length / 3;
		mPeakThreshold = (int) percentileEvaluate(data, 70.0F);;
		int localMaxMin[]=getAllPeaknValley(data);
		int realPeakValley[]=getRealPeaknValley(localMaxMin);
		return realPeakValley;
	}

	private int[] getIEratio(int[] data)			//window size will be 4*(number of real peaks) is typically 4*5 because we are gonna consider 5 real peaks at a time
	{
		int length=data.length;
		int inhalation=0,exhalation=0;
		
		ArrayList<Integer> list=new ArrayList<Integer>();
		int temp=length;
		for(int i=0;i<temp;i+=4)
		{
			//check the starting whether it starts from valley or not. It should be valley
			if((i==0) && (data[i+1]>data[i+3]))
				continue;						//it escaping if first member is a peak. in that case we can not find the inspiration. inspiration always starts from a valley
			
			//check last element whether it is valley or peak. it should be valley
			if((i==0)&&(data[length-1]>data[length-3]))		//at the beginning the stopping condition is changed
				temp=length-2;						//skipping the last one if it is peak
			
			if(i+4<length)
			{
				inhalation=data[i+2]-data[i];
				exhalation=data[i+4]-data[i+2];
				float ieRatio=(float)inhalation/exhalation;
				int raoundedIeRatio=(int)(ieRatio*10000);
				list.add(new Integer(raoundedIeRatio));
			}
		}
		//converting the ArrayList to array
		int ieRatio[]=new int[list.size()];
		for(int j=0;j<list.size();j++)
		{
			ieRatio[j]=list.get(j).intValue();
		}
		return ieRatio;
	}

	@Override
	public void inputData(String name, String type, Object inputData, int length) {
		
		String str = "inputData: ";
		for (int value: (int[])inputData) {
			str += Integer.toString(value) + ", ";
		}
		Log.d(TAG, str);

		int realPeakValley[] = realPeakValley((int[])inputData);
		int ie[] = getIEratio(realPeakValley);
		
		str = "IERatio: ";
		for (int value: ie) {
			str += Integer.toString(value) + ", ";
		}
		Log.d(TAG, str);
		
		outputData("IERatio", "int[]", ie, ie.length);
	}
}
