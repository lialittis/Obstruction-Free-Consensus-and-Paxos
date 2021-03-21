package com.paxos;

public class RespondMsg {
	public String info;
	public int ballot;
	
	public RespondMsg(String info, int ballot) {
		this.info = info;
		this.ballot = ballot;
	}

}
