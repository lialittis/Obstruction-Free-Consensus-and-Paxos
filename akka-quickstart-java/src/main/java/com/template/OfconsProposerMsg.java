package com.template;

public class OfconsProposerMsg {
	
	public String message;
	public Integer v;
	
	public OfconsProposerMsg(String message) {
		this.message = message;
		this.v = Integer.parseInt(message);
	}

}
