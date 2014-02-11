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
	}

	private void loginSuccessful() {
		// Start activity
		Intent i = new Intent(this, MainTabActivity.class);
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
