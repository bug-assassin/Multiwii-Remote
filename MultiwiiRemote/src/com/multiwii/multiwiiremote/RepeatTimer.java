package com.multiwii.multiwiiremote;


public class RepeatTimer {
	long lastMillis = 0;
	int millisBetween = 0;
	
	public RepeatTimer(int millisBetween) {
		setMillisBetween(millisBetween);
	}
	public void setMillisBetween(int millisBetween) {
		this.millisBetween = millisBetween;
	}
	public boolean isTime() {
		return System.currentTimeMillis() > lastMillis + millisBetween;
	}
	public void reset() {
		lastMillis = getCurrentTime();
	}
	private long getCurrentTime() {
		return System.currentTimeMillis();
	}
}
