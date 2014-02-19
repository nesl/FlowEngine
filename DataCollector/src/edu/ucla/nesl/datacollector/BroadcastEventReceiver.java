package edu.ucla.nesl.datacollector;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import edu.ucla.nesl.datacollector.service.DataService;
import edu.ucla.nesl.datacollector.service.SyncService;

public class BroadcastEventReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(final Context context, final Intent intent) {
		
		String action = intent.getAction();
		
		if (action.equals("android.intent.action.BOOT_COMPLETED")) {
			Intent i = new Intent(context, DataService.class);
			context.startService(i);
		} else if (action.equals("android.net.wifi.WIFI_STATE_CHANGED") 
				|| action.equals("android.net.conn.CONNECTIVITY_CHANGE")
				|| action.equals("android.intent.action.ACTION_POWER_CONNECTED")) {
			Intent i = new Intent(context, SyncService.class);
			context.startService(i);
		}
	}
}
