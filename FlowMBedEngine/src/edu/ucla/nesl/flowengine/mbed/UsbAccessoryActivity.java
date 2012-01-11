package edu.ucla.nesl.flowengine.mbed;

import edu.ucla.nesl.util.NotificationHelper;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class UsbAccessoryActivity extends Activity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		startService(new Intent(this, MBedFlowEngine.class));
		
		finish();
	}
}
