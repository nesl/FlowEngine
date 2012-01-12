package edu.ucla.nesl.flowengine.aidl;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

public class WaveSegment implements Parcelable {

	private static final String TAG = WaveSegment.class.getSimpleName();
	
	private static final int MAX_NUM_CHANNEL = 6;
	private static final int MAX_NUM_DATA = 300;
	
	public static final Creator<WaveSegment> CREATOR = new Creator<WaveSegment>() {
		@Override
		public WaveSegment[] newArray(int size) {
			return new WaveSegment[size];
		}
		@Override
		public WaveSegment createFromParcel(Parcel source) {
			return new WaveSegment(source);
		}
	};
	
	public String 		name;
	public long 		timestamp;
	public long 		interval;
	public double		latitude;
	public double		longitude;
	public int			numChannel;
	public String[] 	format = new String[MAX_NUM_CHANNEL];
	public int			numData;
	public double[] 	data = new double[MAX_NUM_DATA];

	private WaveSegment(Parcel source) {
		name = source.readString();
		timestamp = source.readLong();
		interval = source.readLong();
		latitude = source.readDouble();
		longitude = source.readDouble();
		numChannel = source.readInt();
		source.readStringArray(format);
		numData = source.readInt();
		source.readDoubleArray(data);
	}
	
	public WaveSegment(String name, long timestamp, long interval, double latitude, double longitude, String[] format, double[] data, int length) {
		this.name = name;
		this.timestamp = timestamp;
		this.interval = interval;
		this.latitude = latitude;
		this.longitude = longitude;
		this.numChannel = format.length;
		if (this.numChannel > MAX_NUM_CHANNEL) {
			this.numChannel = MAX_NUM_CHANNEL;
		}
		for (int i = 0; i < this.numChannel; i++) {
			this.format[i] = format[i];
		}
		this.numData = length;
		if (this.numData > MAX_NUM_DATA) {
			this.numData = MAX_NUM_DATA;
		}
		for (int i = 0; i < this.numData; i++) {
			this.data[i] = data[i];
		}
	}

	public WaveSegment(String name, long timestamp, long interval, double latitude, double longitude, String[] format, double[] data) {
		this(name, timestamp, interval, latitude, longitude, format, data, data.length);
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(name);
		dest.writeLong(timestamp);
		dest.writeLong(interval);
		dest.writeDouble(latitude);
		dest.writeDouble(longitude);
		dest.writeInt(numChannel);
		dest.writeStringArray(format);
		dest.writeInt(numData);
		dest.writeDoubleArray(data);
	}
}
