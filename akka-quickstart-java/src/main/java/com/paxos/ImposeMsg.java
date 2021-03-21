package com.paxos;

public class ImposeMsg {
	public String impose;
	public int ballot;
	public int proposal;
	
	public ImposeMsg(String impose,int ballot,int proposal) {
		this.impose = impose;
		this.ballot = ballot;
		this.proposal = proposal;
	}

}
