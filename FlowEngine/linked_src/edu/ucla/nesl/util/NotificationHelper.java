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
	private int mNotificationID = 1000;
	
	public NotificationHelper(Context context, String contextTitle, String action, int icon) {
		mContext = context;
		mContextTitle = contextTitle;
		mIcon = icon;
		mAction = action;
		mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
	}

	public void cancel(int notificationID) {
		mNotificationManager.cancel(notificationID);
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

	public void showNotificationNow(int notificationId, CharSequence tickerText, CharSequence contextText, int flags) {
		long when = System.currentTimeMillis();
		showNotification(notificationId, tickerText, contextText, when, flags);
	}

	public void showNotificationNowOngoing(int notificationId, CharSequence tickerText) {
		int flags = 0;
		flags |= Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;
		showNotificationNow(notificationId, tickerText, tickerText, flags);
	}

	public void showNotification(int notificationId, CharSequence tickerText, CharSequence contextText, long when) {
		int flags = 0;
		flags |= Notification.FLAG_AUTO_CANCEL;
		showNotification(notificationId, tickerText, contextText, when, flags);
	}
	
	public void showNotification(int notificationId, CharSequence tickerText, CharSequence contextText, long when, int flags) {
		CharSequence contextTitle = mContextTitle;
		
		Notification notification = new Notification(mIcon, tickerText, when);
		notification.flags |= flags;
		
		Intent notificationIntent = new Intent(mAction);
		PendingIntent contentIntent = PendingIntent.getBroadcast(mContext, 0, notificationIntent, 0);
		notification.setLatestEventInfo(mContext, contextTitle, contextText, contentIntent);
		
		mNotificationManager.notify(notificationId, notification);
	}
}
