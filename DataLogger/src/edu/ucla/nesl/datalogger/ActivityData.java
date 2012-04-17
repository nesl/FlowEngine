package edu.ucla.nesl.datalogger;

public class ActivityData {
	
	private long mTimestamp;
	private String mActivity;
	
	public ActivityData(long timestamp, String activity) {
		mTimestamp = timestamp;
		mActivity = activity;
	}

	public long getTimestamp() {
		return mTimestamp;
	}

	public void setTimestamp(long timestamp) {
		mTimestamp = timestamp;
	}

	public String getActivity() {
		return mActivity;
	}

	public void setActivity(String activity) {
		this.mActivity = activity;
	}
}
