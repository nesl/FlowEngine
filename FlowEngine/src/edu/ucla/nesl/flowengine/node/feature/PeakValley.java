package edu.ucla.nesl.flowengine.node.feature;

import java.util.ArrayList;

import edu.ucla.nesl.flowengine.DataType;
import edu.ucla.nesl.flowengine.DebugHelper;
import edu.ucla.nesl.flowengine.InvalidDataReporter;
import edu.ucla.nesl.flowengine.node.DataFlowNode;

public class PeakValley extends DataFlowNode {
	private static final String TAG = PeakValley.class.getSimpleName();
	
	private int mIndex = 0;
	
	private final double SAMPLE_RATE; // Hz
	private final double DATA_DURATION; // seconds

	/**
	 * all peaks should be over this value
	 * threshold value is adaptive
	 */
	private double mPeakThreshold;								
	private final double PEAK_THRESHOLD_PERCENTILE = 75.0;
	private final double FALLBACK_PEAK_THRESHOLD_PERCENTILE = 65.0;
	
	/**
	 * minimum distance between two real peaks. typically duration of respiration period is more or less four seconds. 
	 */
	private final double MIN_PEAK_DURATION = 1.66; // seconds
	private final double MIN_PEAK_DURATION_SAMPLES;
	private final double MIN_NUM_PEAKS_THRESHOLD;
	private final double NUM_TOO_MANY_PEAKS;
	
	/**
	 * if number of consecutive empty real peaks are more than tolerance than we are adjusting peak value peakThreshold.
	 * if the band is disconnected then it also updates its threshold unnecessarily. in that case, data quality output can be consulted.
	 */
	private int mNumConsecutiveEmptyRealPeaks = 0;
	private final int NUM_NO_PEAKS_TOLERANCE = 2;

	@Override
	protected String processParentNodeName(String parentNodeName) {
		if (parentNodeName.contains("|Buffer")) {
			return parentNodeName.split("\\|Buffer")[0];
		}
		return parentNodeName;
	}

	public PeakValley(String parameterizedSimpleNodeName, double sampleRate, double dataDuration) {
		super(parameterizedSimpleNodeName);
		SAMPLE_RATE = sampleRate;
		DATA_DURATION = dataDuration;
		MIN_NUM_PEAKS_THRESHOLD = (DATA_DURATION / MIN_PEAK_DURATION);
		NUM_TOO_MANY_PEAKS = 4 * MIN_NUM_PEAKS_THRESHOLD;
		MIN_PEAK_DURATION_SAMPLES = MIN_PEAK_DURATION * SAMPLE_RATE;
	}

	/**
	 * calculates the real peak valleys in two steps: a) find local max min from the given samples
	 * and b) then find real peaks and valleys
	 * It depends on a threshold value. all the real peaks will be above the threshold value.
	 * adaptive threshold is calculated according to the output of real peak value.
	 * whoever is using this function should also implement adaptive threshold value code. (e.g RealPeakValleyVirtualSensor.java)
	 * @param data
	 * @param timestamp
	 * @return real peaks and valleys as an integer array
	 * @author Mahbub
	 */
	private int[] calculateRealPeakValley(int[] data) {
		ResultData[] results = pull(PEAK_THRESHOLD_PERCENTILE);
		if (results == null || results[0] == null) {
			throw new UnsupportedOperationException("No result from pulling.");
		}
		mPeakThreshold = (Double)(results[0].data);

		mIndex=0;
		int localMaxMin[] = getAllPeaknValley(data);
		int realPeakValley[] = getRealPeaknValley(localMaxMin);
		
		if (realPeakValley.length == 0)
		{
			InvalidDataReporter.report("realPeakValley too low. length: " + realPeakValley.length);
			//first check the variance. if it is very low then the band might be off
			mNumConsecutiveEmptyRealPeaks++;
			if(mNumConsecutiveEmptyRealPeaks >= NUM_NO_PEAKS_TOLERANCE)
			{
				//adjust threshold for peaks
				//should check data quality..........make sure that band is not loose
				results = pull(FALLBACK_PEAK_THRESHOLD_PERCENTILE);
				if (results == null || results[0] == null) {
					throw new UnsupportedOperationException("No result from pulling.");
				}
				mPeakThreshold = (Double)(results[0].data);
			}
		}
		else if (realPeakValley.length > NUM_TOO_MANY_PEAKS)	//too many real peaks, need to adjust the threshold to upper value; 5 is probable offset
		{
			results = pull(FALLBACK_PEAK_THRESHOLD_PERCENTILE);
			if (results == null || results[0] == null) {
				throw new UnsupportedOperationException("No result from pulling.");
			}
			mPeakThreshold = (Double)(results[0].data);
		}
		
		DebugHelper.dump(TAG, realPeakValley);

		return realPeakValley;
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

		int[] realPeakValley = calculateRealPeakValley((int[])inputData);
		
		output(name + "PeakValley", DataType.INTEGER_ARRAY, realPeakValley, realPeakValley.length, timestamp);
	}
	
	/**
	 * calculates peaks and valleys (false + real) from the data buffer
	 * @param buffer
	 * @return list of tuple containing (valleyIndex, valley, peakIndex, peak). so if any method wants to use this method, it should read all the four values together.
	 * @author Mahbub
	 */
	private int[] getAllPeaknValley(int[] data)					//consider the timestamp issues. because it is important
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
					mIndex++;
					curr_value=data[i++];
					mIndex++;
					//skipping up to the first increasing sequence
					while((prev_value1>=curr_value)&& (i < length))
					{
						prev_value1=curr_value;
						line=data[i++];
						mIndex++;
						curr_value=line;
					}
					list=addToTheList(list, mIndex-1, prev_value1);		//prev_value1 is the current valley
					continue;
				}
				if(curr_value>prev_value1 )			//this means the sequence is increasing
				{
					while((prev_value1<=curr_value)&& (i < length))
					{
						prev_value1=curr_value;
						line=data[i++];
						mIndex++;
						curr_value=line;
					}
					list=addToTheList(list,mIndex-1, prev_value1);		//prev_value1 is the current valley
				}else //if(Integer.parseInt(curr_value)<Integer.parseInt(prev_value1))
				{
					while((prev_value1>=curr_value)&& (i < length))
					{
						prev_value1=curr_value;
						line=data[i++];
						mIndex++;
						curr_value=line;
					}
					if(i!=length)
						list=addToTheList(list,mIndex-1, prev_value1);		//prev_value1 is the current valley
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
	private ArrayList<Integer> addToTheList(ArrayList<Integer> list,int anchorIndex, int anchorValue)
	{
		Integer val=new Integer(anchorValue);
		Integer ind=new Integer(anchorIndex);
		list.add(ind);
		list.add(val);
		return list;
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
//				if(prev1_peak>=peakThreshold)
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
						if(peakAnchorIndex!=-1||(current_peakIndex - realPeakIndex)>=MIN_PEAK_DURATION_SAMPLES || realPeak==-1)
						{
							if((peakAnchorIndex!=-1)&&((current_peakIndex- peakAnchorIndex)>=MIN_PEAK_DURATION_SAMPLES)&& (realPeakIndex!=peakAnchorIndex)&& peakAnchorIndex>valleyAnchorIndex)
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
					if((current_peakIndex - realPeakIndex)>=MIN_PEAK_DURATION_SAMPLES)
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
					if((current_peak>=mPeakThreshold) && ((current_peakIndex - realPeakIndex)>=MIN_PEAK_DURATION_SAMPLES) && realPeak!=-1)				//then the previous valleyAnchor is real valley, check the peak to previous real peak against duration threshold
					{
						if(realPeakIndex<peakAnchorIndex && realPeakIndex!=-1 && ((current_peakIndex - peakAnchorIndex)>=MIN_PEAK_DURATION_SAMPLES))
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
			if(list.get(i+4).intValue()-list.get(i).intValue()<MIN_PEAK_DURATION_SAMPLES)
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

}
