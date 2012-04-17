package edu.ucla.nesl.datalogger;

public class ConversationData {
	private long mTimestamp;
	private String mConversation;

	public ConversationData(long timestamp, String conversation) {
		this.mTimestamp = timestamp;
		this.mConversation = conversation;
	}
	
	public long getTimestamp() {
		return mTimestamp;
	}
	
	public void setTimestamp(long timestamp) {
		this.mTimestamp = timestamp;
	}
	
	public String getConversation() {
		return mConversation;
	}
	
	public void setConversation(String conversation) {
		this.mConversation = conversation;
	}
}
