package edu.ucla.nesl.flowengine.mbed;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class UsbAccessoryActivity extends Activity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		startService(new Intent(this, MBedFlowEngine.class));
		
		finish();
	}
}
