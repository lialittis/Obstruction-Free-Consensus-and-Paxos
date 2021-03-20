package com.template;

public class GatherMsg {
	
	public int ballot;
	public String message;
	public Integer estimate;
	public int estballot;
	
	public GatherMsg(int ballot, String message) {
		this.ballot = ballot;
		this.message = message;
	}
	
	public GatherMsg(int ballot, String message,int estballot,int estimate) {
		this.ballot = ballot;
		this.message = message;
		this.estballot = estballot;
		this.estimate = estimate;
	}

}
