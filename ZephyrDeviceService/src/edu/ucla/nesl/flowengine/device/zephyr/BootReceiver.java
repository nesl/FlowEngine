package edu.ucla.nesl.flowengine.device.zephyr;

import edu.ucla.nesl.flowengine.device.ZephyrDeviceService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		context.startService(new Intent(ZephyrDeviceService.class.getName()));
	}
}
