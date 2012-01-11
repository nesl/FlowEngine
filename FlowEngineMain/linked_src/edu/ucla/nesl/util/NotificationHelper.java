package edu.ucla.nesl.util;

import java.util.Random;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

public class NotificationHelper {
	
	private NotificationManager mNotificationManager = null;
	private Context  mContext = null;
	private String mContextTitle = null;
	private String mAction = null;
	private int mIcon;

	private Random mRandom = new Random();;
	private int mNotificationID = mRandom.nextInt();
	
	public NotificationHelper(Context context, String contextTitle, String action, int icon) {
		mContext = context;
		mContextTitle = contextTitle;
		mIcon = icon;
		mAction = action;
		mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
	}

	public void showNotificationNow(CharSequence tickerText) {
		showNotificationNow(mNotificationID++, tickerText, tickerText);
	}

	public void showNotificationNow(int notificationId, CharSequence tickerText) {
		showNotificationNow(notificationId, tickerText, tickerText);
	}

	public void showNotificationNow(int notificationId, CharSequence tickerText, CharSequence contextText) {
		long when = System.currentTimeMillis();
		showNotification(notificationId, tickerText, contextText, when);
	}

	public void showNotification(int notificationId, CharSequence tickerText, CharSequence contextText, long when) {
		CharSequence contextTitle = mContextTitle;
		
		Notification notification = new Notification(mIcon, tickerText, when);
		notification.flags |= Notification.FLAG_AUTO_CANCEL | Notification.FLAG_SHOW_LIGHTS; // | Notification.FLAG_FOREGROUND_SERVICE;
		
		Intent notificationIntent = new Intent(mAction);
		PendingIntent contentIntent = PendingIntent.getBroadcast(mContext, 0, notificationIntent, 0);
				
		notification.setLatestEventInfo(mContext, contextTitle, contextText, contentIntent);
		
		mNotificationManager.notify(notificationId, notification);
	}
	
}
