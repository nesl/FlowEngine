package edu.ucla.nesl.datacollector.db;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import edu.ucla.nesl.datacollector.Const;
import edu.ucla.nesl.datacollector.Range;

public class DataSource {

	protected SQLiteDatabase database;
	protected SQLiteHelper dbHelper;

	public void open() throws SQLException {
		database = dbHelper.getWritableDatabase();
	}

	public void close() {
		dbHelper.close();
	}

	public DataSource(Context context) {
		dbHelper = new SQLiteHelper(context);
	}

	public boolean isDataCollectionOngoing() {
		Cursor c = null;
		try {
			// check if there is ongoing data collection.
			c = database.query(SQLiteHelper.TABLE_HISTORY
					, null, SQLiteHelper.COL_START_TIME + " is not null and " + SQLiteHelper.COL_FINISH_TIME + " is null", null, null, null, null);

			c.moveToFirst();
			int cnt = 0;
			while (!c.isAfterLast()) {
				cnt += 1;
				c.moveToNext();
			}
			if (cnt == 1) {
				return true;
			} else if (cnt > 1) {
				throw new UnsupportedOperationException("Started data collection more than 1.");
			} else {
				return false;
			}

		} finally {
			if (c != null) 
				c.close();
		}
	}

	public void startDataCollection(Date startTime) {
		Cursor c = null;
		try {

			// check if there is ongoing data collection.
			c = database.query(SQLiteHelper.TABLE_HISTORY
					, null, SQLiteHelper.COL_START_TIME + " is not null and " + SQLiteHelper.COL_FINISH_TIME + " is null", null, null, null, null);

			c.moveToFirst();
			int cnt = 0;
			while (!c.isAfterLast()) {
				cnt += 1;
				c.moveToNext();
			}
			if (cnt == 1) {
				return;
			} else if (cnt > 1) {
				throw new UnsupportedOperationException("Started data collection more than 1.");
			}
			c.close();

			// check if there is the same day finished data collection
			Calendar cal = Calendar.getInstance();
			cal.setTime(startTime);
			Calendar dayStartCal = Calendar.getInstance();
			dayStartCal.clear();
			dayStartCal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
			Calendar dayEndCal = Calendar.getInstance();
			dayEndCal.setTime(dayStartCal.getTime());
			dayEndCal.add(Calendar.DAY_OF_MONTH, 1);
			long dayStartTime = dayStartCal.getTimeInMillis() / 1000;
			long dayEndTime = dayEndCal.getTimeInMillis() / 1000;

			Log.d(Const.TAG, "" + dayStartTime + " ~ " + dayEndTime);

			c = database.query(SQLiteHelper.TABLE_HISTORY
					, null, SQLiteHelper.COL_START_TIME + " >= ? and " + SQLiteHelper.COL_START_TIME + " < ? and " + SQLiteHelper.COL_FINISH_TIME + " is not null"
					, new String[] { Long.toString(dayStartTime), Long.toString(dayEndTime) }, null, null, null);

			c.moveToFirst();
			cnt = 0;
			long finishedStartTime = -1;
			while (!c.isAfterLast()) {
				cnt += 1;
				finishedStartTime = c.getLong(0);
				c.moveToNext();
			}

			if (cnt > 1) {
				throw new UnsupportedOperationException("Finished data collection for a day is more than 1.");
			} 

			if (cnt == 1) {
				// There is same day finished data collection. Resume it.
				ContentValues values = new ContentValues();
				values.putNull(SQLiteHelper.COL_FINISH_TIME);
				values.putNull(SQLiteHelper.COL_IS_REPORT);
				database.update(SQLiteHelper.TABLE_HISTORY, values, SQLiteHelper.COL_START_TIME + " = ?", new String[] { Long.toString(finishedStartTime) });
			} else {
				// No finished data collection. Add new data collection.
				ContentValues values = new ContentValues();
				values.put(SQLiteHelper.COL_START_TIME, startTime.getTime() / 1000);
				database.insert(SQLiteHelper.TABLE_HISTORY, null, values);
			}

		} finally {
			if (c != null) 
				c.close();
		}
	}

	public void finishDataCollection(Date finishTime) {
		Cursor c = null;
		try {
			c = database.query(SQLiteHelper.TABLE_HISTORY
					, null, SQLiteHelper.COL_START_TIME + " is not null and " + SQLiteHelper.COL_FINISH_TIME + " is null", null, null, null, null);

			c.moveToFirst();
			int cnt = 0;
			long startTime = -1;
			while (!c.isAfterLast()) {
				cnt += 1;
				startTime = c.getLong(0);
				c.moveToNext();
			}

			if (cnt > 1) {
				throw new UnsupportedOperationException("Started data collection more than 1.");
			} 

			if (cnt <= 0 || startTime == -1) {
				throw new UnsupportedOperationException("No started data collection.. cnt = " + cnt + ", startTime = " + startTime);
			}

			ContentValues values = new ContentValues();
			values.put(SQLiteHelper.COL_FINISH_TIME, finishTime.getTime() / 1000);
			database.update(SQLiteHelper.TABLE_HISTORY, values, SQLiteHelper.COL_START_TIME + " = ?", new String[] { Long.toString(startTime) });

		} finally {
			if (c != null) 
				c.close();
		}
	}

	public List<Range> getNoReportDataCollections() {
		Cursor c = null;
		List<Range> ranges = new ArrayList<Range>();
		try {
			c = database.query(SQLiteHelper.TABLE_HISTORY
					, null, SQLiteHelper.COL_START_TIME + " is not null and " 
								+ SQLiteHelper.COL_FINISH_TIME + " is not null and "
								+ SQLiteHelper.COL_IS_REPORT + " is null", null, null, null, null);

			c.moveToFirst();
			while (!c.isAfterLast()) {
				long startTime = c.getLong(0);
				long endTime = c.getLong(1);
				ranges.add(new Range(startTime, endTime));
				c.moveToNext();
			}
		} finally {
			if (c != null) 
				c.close();
		}
		return ranges;
	}

	public void markReportDone(long startTimeInSecs, long endTimeInSecs) {
		ContentValues values = new ContentValues();
		values.put(SQLiteHelper.COL_IS_REPORT, 1);
		database.update(SQLiteHelper.TABLE_HISTORY, values, SQLiteHelper.COL_START_TIME + " = ? and " + SQLiteHelper.COL_FINISH_TIME + " = ?"
				, new String[] { Long.toString(startTimeInSecs), Long.toString(endTimeInSecs) });
	}
}
