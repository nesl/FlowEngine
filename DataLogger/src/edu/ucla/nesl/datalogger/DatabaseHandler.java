package edu.ucla.nesl.datalogger;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHandler extends SQLiteOpenHelper {

	private static final int DATABASE_VERSION = 1;
	private static final String DATABASE_NAME = "/sdcard/datalogger_db";
	private static final String KEY_ID = "id";
	private static final String KEY_UPLOAD = "upload";
	private static final String KEY_TIMESTAMP = "timestamp";
	
	private static final String TABLE_ACTIVITY = "activity";
	private static final String KEY_ACTIVITY = "activity";

	private static final String TABLE_STRESS = "stress";
	private static final String KEY_STRESS = "stress";
	
	private static final String TABLE_CONVERSATION = "conversation";
	private static final String KEY_CONVERSATION = "conversation";
	
	private static final String TABLE_PHONE_BATTERY = "phone_battery";
	private static final String KEY_PHONE_BATTERY = "phone_battery";
	
	private static final String TABLE_ZEPHYR_BATTERY = "zephyr_battery";
	private static final String KEY_ZEPHYR_BATTERY = "zephyr_battery";
	
	private static final String TABLE_PHONE_GPS = "phone_gps";
	private static final String KEY_LATITUDE = "latitude";
	private static final String KEY_LONGITUDE = "longitude";
	private static final String KEY_ALTITUDE = "altitude";
	private static final String KEY_SPEED = "speed";
	
	public DatabaseHandler(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		String CREATE_ACTIVITY_TABLE = "CREATE TABLE " + TABLE_ACTIVITY + "("
				+ KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
				+ KEY_TIMESTAMP + " INTEGER, "
				+ KEY_ACTIVITY + " TEXT, " 
				+ KEY_UPLOAD + " TEXT)";
		String CREATE_STRESS_TABLE = "CREATE TABLE " + TABLE_STRESS + "("
				+ KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
				+ KEY_TIMESTAMP + " INTEGER, "
				+ KEY_STRESS + " TEXT, "
				+ KEY_UPLOAD + " TEXT)";
		String CREATE_CONVERSATION_TABLE = "CREATE TABLE " + TABLE_CONVERSATION + "("
				+ KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
				+ KEY_TIMESTAMP + " INTEGER, "
				+ KEY_CONVERSATION + " TEXT, "
				+ KEY_UPLOAD + " TEXT)";
		String CREATE_PHONE_BATTERY_TABLE = "CREATE TABLE " + TABLE_PHONE_BATTERY + "("
				+ KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
				+ KEY_TIMESTAMP + " INTEGER, "
				+ KEY_PHONE_BATTERY + " TEXT, "
				+ KEY_UPLOAD + " TEXT)";
		String CREATE_ZEPHYR_BATTERY_TABLE = "CREATE TABLE " + TABLE_ZEPHYR_BATTERY + "("
				+ KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
				+ KEY_TIMESTAMP + " INTEGER, "
				+ KEY_ZEPHYR_BATTERY + " INTEGER, "
				+ KEY_UPLOAD + " TEXT)";
		String CREATE_PHONE_GPS_TABLE = "CREATE TABLE " + TABLE_PHONE_GPS + "("
				+ KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
				+ KEY_TIMESTAMP + " INTEGER, "
				+ KEY_LATITUDE + " REAL, "
				+ KEY_LONGITUDE + " REAL, "
				+ KEY_ALTITUDE + " REAL, "
				+ KEY_SPEED + " REAL, "
				+ KEY_UPLOAD + " TEXT)";
		db.execSQL(CREATE_ACTIVITY_TABLE);
		db.execSQL(CREATE_STRESS_TABLE);
		db.execSQL(CREATE_CONVERSATION_TABLE);
		db.execSQL(CREATE_PHONE_BATTERY_TABLE);
		db.execSQL(CREATE_ZEPHYR_BATTERY_TABLE);
		db.execSQL(CREATE_PHONE_GPS_TABLE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_ACTIVITY);
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_CONVERSATION);
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_STRESS);
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_PHONE_BATTERY);
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_ZEPHYR_BATTERY);
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_PHONE_GPS);
		onCreate(db);
	}

	public void insertActivity(ActivityData activityData) {
		SQLiteDatabase db = this.getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(KEY_TIMESTAMP, activityData.getTimestamp());
		values.put(KEY_ACTIVITY, activityData.getActivity());
		db.insert(TABLE_ACTIVITY, null, values);
		db.close();
	}
	
	public void insertStress(StressData stressData) {
		SQLiteDatabase db = this.getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(KEY_TIMESTAMP, stressData.getTimestamp());
		values.put(KEY_STRESS, stressData.getStress());
		db.insert(TABLE_STRESS, null, values);
		db.close();
	}

	public void insertConversation(ConversationData conversationData) {
		SQLiteDatabase db = this.getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(KEY_TIMESTAMP, conversationData.getTimestamp());
		values.put(KEY_CONVERSATION, conversationData.getConversation());
		db.insert(TABLE_CONVERSATION, null, values);
		db.close();
	}

	public void insertPhoneBattery(PhoneBatteryData phoneBatteryData) {
		SQLiteDatabase db = this.getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(KEY_TIMESTAMP, phoneBatteryData.getTimestamp());
		values.put(KEY_PHONE_BATTERY, phoneBatteryData.getBatteryInfo());
		db.insert(TABLE_PHONE_BATTERY, null, values);
		db.close();
	}

	public void insertZephyrBattery(ZephyrBatteryData zephyrBatteryData) {
		SQLiteDatabase db = this.getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(KEY_TIMESTAMP, zephyrBatteryData.getTimestamp());
		values.put(KEY_ZEPHYR_BATTERY, zephyrBatteryData.getZephyrBattery());
		db.insert(TABLE_ZEPHYR_BATTERY, null, values);
		db.close();
	}

	public void insertPhoneGPS(PhoneGPSData phoneGPSData) {
		SQLiteDatabase db = this.getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(KEY_TIMESTAMP, phoneGPSData.getTimestamp());
		values.put(KEY_LATITUDE, phoneGPSData.getLatitude());
		values.put(KEY_LONGITUDE, phoneGPSData.getLongitude());
		values.put(KEY_ALTITUDE, phoneGPSData.getAltitude());
		values.put(KEY_SPEED, phoneGPSData.getSpeed());
		db.insert(TABLE_PHONE_GPS, null, values);
		db.close();
	}
}
