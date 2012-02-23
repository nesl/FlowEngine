package edu.ucla.nesl.flowengine.device;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

// This is for automatically starting service when USB connected.
public class UsbAccessoryActivity extends Activity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		startService(new Intent(this, MBedDeviceService.class));
		
		finish();
	}
}
