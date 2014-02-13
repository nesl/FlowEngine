package edu.ucla.nesl.flowengine.node.feature;

import edu.ucla.nesl.flowengine.DataType;
import edu.ucla.nesl.flowengine.DebugHelper;
import edu.ucla.nesl.flowengine.InvalidDataReporter;
import edu.ucla.nesl.flowengine.node.DataFlowNode;

public class Ventilation extends DataFlowNode {
	private static final String TAG = Ventilation.class.getSimpleName();
	
	@Override
	protected String processParentNodeName(String parentNodeName) {
		if (parentNodeName.contains("|PeakValley")) {
			return parentNodeName.split("\\|PeakValley")[0];
		}
		return parentNodeName;
	}
	
	private double calculateVentilation(int[] data, int length) {
		int temp = length;
		double xx,yy;
		double ventilation = 0;
		
		for(int i=0;i<temp;i+=4)
		{
			//check the starting whether it starts from valley or not. It should be valley
			if((i==0) && (data[i+1]>data[i+3]))
				continue;						//it escaping if first member is a peak. in that case we can not find the inspiration. inspiration always starts from a valley
			
			//check last element whether it is valley or peak. it should be valley
			if((i==0)&&(data[length-1]>data[length-3]))		//at the beginning the stopping condition is changed
				temp=length-2;						//skipping the last one if it is peak
			
			if(i+3<length)
			{
				xx=(data[i+2]-data[i])/64.0;
				yy=data[i+3]-data[i+1];
				ventilation+=xx*yy/2.0;
//				int raoundedMinuteVentilation=(int)(MinuteVentilation*10000);
//				list.add(new Integer(raoundedMinuteVentilation));
			}
			
		}
		
		//MinuteVentilation=MinuteVentilation*(data.length/4 -1);
		
		/*int MV[]=new int[2];
		MV[0]=(int)(MinuteVentilation*10000);
		MV[1]=MV[0];
		return MV;*/
		
		ventilation *= 10000;
		
		DebugHelper.log(TAG, String.format("ventilation: %.3f", ventilation));
		
		return ventilation;
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

		double ventilation = calculateVentilation((int[])inputData, length);
		
		output(name.replace("PeakValley", "Ventilation"), DataType.DOUBLE, ventilation, 0, timestamp);
	}
}
