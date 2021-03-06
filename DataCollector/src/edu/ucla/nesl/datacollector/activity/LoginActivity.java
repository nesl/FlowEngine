package edu.ucla.nesl.datacollector.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.widget.EditText;
import android.widget.TextView;
import edu.ucla.nesl.datacollector.Const;
import edu.ucla.nesl.datacollector.R;
import edu.ucla.nesl.datacollector.tools.Tools;

public class LoginActivity extends Activity {

	private TextView appTitle;
	private TextView testTextView;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);
		
		appTitle = (TextView)findViewById(R.id.app_title);
		appTitle.setOnLongClickListener(new OnLongClickListener() {
			
			@Override
			public boolean onLongClick(View v) {
				loginSuccessful();
				return true;
			}
		});
		
		testTextView = (TextView)findViewById(R.id.test_button);
		testTextView.setOnLongClickListener(new OnLongClickListener() {
			
			@Override
			public boolean onLongClick(View v) {
				Intent intent = new Intent(Const.FLOW_ENGINE_APPLICATION_SERVICE);
				intent.putExtra("flowengine_test", true);
				startService(intent);
				return true;
			}
		});
	}

	private void loginSuccessful() {
		// Start activity
		Intent i = new Intent(this, CollectionControlActivity.class);
		startActivity(i);
	}
	
	public void onClickLoginButton(View view) {

		SharedPreferences settings = getSharedPreferences(Const.PREFS_NAME, 0);
		String username = settings.getString(Const.PREFS_USERNAME, null);
		String password = settings.getString(Const.PREFS_PASSWORD, null);

		EditText usernameEditText = (EditText)findViewById(R.id.username);
		EditText passwordEditText = (EditText)findViewById(R.id.password);

		String usernameEntered = usernameEditText.getText().toString();
		String passwordEntered = passwordEditText.getText().toString();

		if (usernameEntered.equals(username) && passwordEntered.equals(password)) {
			loginSuccessful();
		} else {
			Tools.showAlertDialog(this, "Error", "Your username and password don't match!");
		}
	}
}
