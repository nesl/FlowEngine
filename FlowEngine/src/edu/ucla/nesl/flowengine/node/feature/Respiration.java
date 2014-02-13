package edu.ucla.nesl.flowengine.node.feature;

import java.util.ArrayList;

import edu.ucla.nesl.flowengine.DataType;
import edu.ucla.nesl.flowengine.DebugHelper;
import edu.ucla.nesl.flowengine.InvalidDataReporter;
import edu.ucla.nesl.flowengine.node.DataFlowNode;

public class Respiration extends DataFlowNode {
	private static final String TAG = Respiration.class.getSimpleName();

	@Override
	protected String processParentNodeName(String parentNodeName) {
		if (parentNodeName.contains("|PeakValley")) {
			return parentNodeName.split("\\|PeakValley")[0];
		}
		return parentNodeName;
	}

	private int[] calculateRespiration(int[] data, int length) {
		int inhalation = 0, exhalation = 0;
		
		ArrayList<Integer> list = new ArrayList<Integer>();
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
				float Respiration=(float)inhalation+(float)exhalation;
				int raoundedRespiration=(int)(Respiration*10000);
				list.add(new Integer(raoundedRespiration));
			}
		}
		
		//converting the ArrayList to array
		int respiration[]=new int[list.size()];
		for(int j=0;j<list.size();j++)
		{
			respiration[j]=list.get(j).intValue();
		}

		DebugHelper.dump(TAG, respiration);
		
		return respiration;
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

		int[] respiration = calculateRespiration((int[])inputData, length);
		
		output(name.replace("PeakValley", "Respiration"), DataType.INTEGER_ARRAY, respiration, respiration.length, timestamp);
	}
}
