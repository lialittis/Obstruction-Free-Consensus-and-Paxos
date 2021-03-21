package com.paxos;

public class HoldMsg {
	public String info;
	public int state;
	
	public HoldMsg() {
		this.info = "HOLD";
		this.state = 3;
	}

}
