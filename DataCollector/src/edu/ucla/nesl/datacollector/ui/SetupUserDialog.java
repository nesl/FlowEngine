package edu.ucla.nesl.datacollector.ui;

import edu.ucla.nesl.datacollector.Const;
import edu.ucla.nesl.datacollector.R;
import edu.ucla.nesl.datacollector.R.id;
import edu.ucla.nesl.datacollector.R.layout;
import edu.ucla.nesl.datacollector.tools.IPAddressValidator;
import edu.ucla.nesl.datacollector.tools.Tools;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class SetupUserDialog extends Dialog {

	private Context context;
	private Dialog dialog = this;

	private OnFinishListener onFinishListener;

	private EditText serverip;
	private EditText username;
	private EditText password;
	private EditText passwordConfirm;
	
	public interface OnFinishListener {
		public void onFinish();
	}
	
	public SetupUserDialog(Context context, OnFinishListener onFinishListener) {
		super(context);
		this.context = context;
		this.onFinishListener = onFinishListener;		
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.dialog_create_login);

		setCancelable(false);
		setTitle("Create a login");

		serverip = (EditText)findViewById(R.id.server_ip);
		username = (EditText)findViewById(R.id.username);
		password = (EditText)findViewById(R.id.password);
		passwordConfirm = (EditText)findViewById(R.id.password_confirm);
		
		Button doneButton = (Button) findViewById(R.id.create_user_done);
		doneButton.setOnClickListener(onClickDoneButtonListener);
	}

	private android.view.View.OnClickListener onClickDoneButtonListener = new android.view.View.OnClickListener() {
		@Override
		public void onClick(View v) {
			
			String serveripStr = serverip.getText().toString();
			String usernameStr = username.getText().toString();
			String passwordStr = password.getText().toString();
			String passwordConfirmStr = passwordConfirm.getText().toString();
			
			if (serveripStr == null || serveripStr.length() <= 0) {
				Tools.showAlertDialog(context, "Error", "Please enter your server IP address.");
				return;
			} else {
				if (!IPAddressValidator.validate(serveripStr)) {
					Tools.showAlertDialog(context, "Error", "Please enter a valid IP address.");
					return;
				}
			}
			
			if (usernameStr == null || usernameStr.length() <= 0) {
				Tools.showAlertDialog(context, "Error", "Please enter your user name.");
				return;
			}
			
			if (passwordStr == null || passwordStr.length() <= 0) {
				Tools.showAlertDialog(context, "Error", "Please enter your password.");
				return;
			}

			if (passwordConfirmStr == null || passwordConfirmStr.length() <= 0) {
				Tools.showAlertDialog(context, "Error", "Please confirm your password.");
				return;
			}

			if (!passwordConfirmStr.equals(passwordStr)) {
				Tools.showAlertDialog(context, "Error", "Your passwords do not match.");
				return;
			}
			
			SharedPreferences settings = context.getSharedPreferences(Const.PREFS_NAME, 0);
			SharedPreferences.Editor editor = settings.edit();
			editor.putString(Const.PREFS_SERVER_IP, serveripStr);
			editor.putString(Const.PREFS_USERNAME, usernameStr);
			editor.putString(Const.PREFS_PASSWORD, passwordStr);
			editor.commit();
			
			dialog.dismiss();
			onFinishListener.onFinish();
		}
	};
}
