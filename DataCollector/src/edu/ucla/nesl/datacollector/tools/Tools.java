package edu.ucla.nesl.datacollector.tools;

import javax.sql.DataSource;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.Toast;

public class Tools {

	public static void showAlertDialog(Context context, String title, String message) {
		showAlertDialog(context, title, message, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
	}

	public static void showAlertDialog(Context context, String title, String message, DialogInterface.OnClickListener listener) {
		AlertDialog dialog = new AlertDialog.Builder(context).create();
		if (title != null) 
			dialog.setTitle(title);
		if (message != null)
			dialog.setMessage(message);
		dialog.setButton("OK", listener);
		dialog.setCancelable(false);
		dialog.show();
	}

	public static void showAlertDialog(Context context, String title, String message, DialogInterface.OnClickListener okListener, DialogInterface.OnClickListener cancelListener) {
		AlertDialog dialog = new AlertDialog.Builder(context).create();
		if (title != null) 
			dialog.setTitle(title);
		if (message != null)
			dialog.setMessage(message);
		dialog.setButton("OK", okListener);
		dialog.setButton2("CANCEL", cancelListener);
		dialog.setCancelable(false);
		dialog.show();
	}

	public static void showMessage(Context context, String message) {
		Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
	}
}
