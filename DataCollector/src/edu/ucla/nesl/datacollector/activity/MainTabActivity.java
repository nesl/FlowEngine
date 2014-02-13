package edu.ucla.nesl.datacollector.activity;

import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;
import edu.ucla.nesl.datacollector.Const;
import edu.ucla.nesl.datacollector.R;

public class MainTabActivity extends TabActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tab_main);
         
        TabHost tabHost = getTabHost();
         
        // Tab for Photos
        TabSpec devicesSpec = tabHost.newTabSpec("Devices");
        // setting Title and Icon for the Tab
        devicesSpec.setIndicator("Devices", getResources().getDrawable(R.drawable.icon_devices_tab));
        Intent photosIntent = new Intent(this, TabDevicesActivity.class);
        devicesSpec.setContent(photosIntent);
         
        // Tab for Songs
        TabSpec sensorSpec = tabHost.newTabSpec("Sensors");        
        sensorSpec.setIndicator("Sensors", getResources().getDrawable(R.drawable.icon_sensors_tab));
        Intent songsIntent = new Intent(this, TabSensorsActivity.class);
        sensorSpec.setContent(songsIntent);
         
        // Tab for Videos
        TabSpec statusSpec = tabHost.newTabSpec("Status");
        statusSpec.setIndicator("Status", getResources().getDrawable(R.drawable.icon_status_tab));
        Intent videosIntent = new Intent(this, TabStatusActivity.class);
        statusSpec.setContent(videosIntent);
         
        // Adding all TabSpec to TabHost
        tabHost.addTab(devicesSpec);
        tabHost.addTab(sensorSpec);
        tabHost.addTab(statusSpec);
    }
    
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		Intent intent;
		switch (item.getItemId()) {
		case R.id.action_user_setup:
			intent = new Intent(this, MainActivity.class);
			intent.putExtra(Const.SETUP_MODE, Const.SETUP_USER);
			startActivity(intent);
			return true;
		case R.id.action_bluetooth_setup:
			intent = new Intent(this, MainActivity.class);
			intent.putExtra(Const.SETUP_MODE, Const.SETUP_BLUETOOTH);
			startActivity(intent);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
}
