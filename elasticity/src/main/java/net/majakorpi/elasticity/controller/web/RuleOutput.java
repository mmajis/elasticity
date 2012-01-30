package net.majakorpi.elasticity.controller.web;

import java.io.Serializable;

public class RuleOutput implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private int id = 0;
	
	private String output;

	public RuleOutput(int id, String output) {
		super();
		this.id = id;
		this.output = output;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getOutput() {
		return output;
	}

	public void setOutput(String output) {
		this.output = output;
	}
	
	

}
