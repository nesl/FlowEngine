package edu.ucla.nesl.datacollector.activity;

import java.util.Date;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.widget.TextView;
import android.widget.ToggleButton;
import edu.ucla.nesl.datacollector.Const;
import edu.ucla.nesl.datacollector.R;
import edu.ucla.nesl.datacollector.db.DataSource;
import edu.ucla.nesl.datacollector.service.DataService;

public class CollectionControlActivity extends Activity {

	private Context context = this;
	private DataSource dataSource = null;
	
	private TextView hiddenText;
	private ToggleButton collectionControl;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_collection_control);

		dataSource = new DataSource(this);

		collectionControl = (ToggleButton)findViewById(R.id.toggle_collection_control);
		
		hiddenText = (TextView)findViewById(R.id.hidden_text);
		hiddenText.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				Intent i = new Intent(context, MainTabActivity.class);
				startActivity(i);
				return true;
			}
		});
	}

	@Override
	protected void onResume() {
		if (dataSource != null) {
			dataSource.open();
		}
		if (dataSource.isDataCollectionOngoing()) {
			collectionControl.setChecked(true);
		} else {
			collectionControl.setChecked(false);
		}
		super.onResume();
	}
	
	@Override
	protected void onPause() {
		if (dataSource != null) {
			dataSource.close();
		}
		super.onPause();
	}

	public void onToggleCollectionControl(View view) {
		boolean status = collectionControl.isChecked();
		
		if (status) {
			dataSource.startDataCollection(new Date());
			signalDataService(true);
		} else {
			dataSource.finishDataCollection(new Date());
			signalDataService(false);
		}
	}

	private void signalDataService(boolean isEnabled) {
		SharedPreferences settings = context.getSharedPreferences(Const.PREFS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();

		for (String sensor : TabSensorsActivity.sensorNames) {
			Intent intent = new Intent(getApplicationContext(), DataService.class);
			intent.putExtra(DataService.REQUEST_TYPE, DataService.CHANGE_SUBSCRIPTION);
			intent.putExtra(DataService.EXTRA_SENSOR_NAME, sensor);
			intent.putExtra(DataService.EXTRA_IS_ENABLED,isEnabled);
			startService(intent);
			
			editor.putBoolean(sensor, isEnabled);
		}

		editor.commit();
	}
}
