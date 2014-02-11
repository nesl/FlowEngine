package edu.ucla.nesl.datacollector;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import edu.ucla.nesl.datacollector.service.DataService;

public class BroadcastEventReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(final Context context, final Intent intent) {
		Intent i = new Intent(context, DataService.class);
		context.startService(i);
	}
}
