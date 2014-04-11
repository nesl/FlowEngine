package edu.ucla.nesl.datacollector.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class SQLiteHelper extends SQLiteOpenHelper {

	private static final String DATABASE_NAME = "datacollector.db";
	private static final int DATABASE_VERSION = 3;

	public static final String TABLE_HISTORY = "history";
	public static final String COL_START_TIME = "start_time";
	public static final String COL_FINISH_TIME = "finish_time";
	public static final String COL_IS_REPORT = "is_report";

	private static final String CREATE_TABLE_HISTROY = "CREATE TABLE " + TABLE_HISTORY
			+ "( " + COL_START_TIME + " INTEGER, "
			+ COL_FINISH_TIME + " INTEGER, "
			+ COL_IS_REPORT + " INTEGER);";

	public SQLiteHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(CREATE_TABLE_HISTROY);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_HISTORY);
		onCreate(db);
	}
}
