package net.majakorpi.elasticity.actuator;


public class ChefResult {

	private final long endTime;
	private final int returnCode;
	
	public ChefResult(int returnCode) {
		super();
		this.endTime = System.currentTimeMillis();
		this.returnCode = returnCode;
	}

	public long getEndMillis() {
		return endTime;
	}

	public int getReturnCode() {
		return returnCode;
	}
	
	
}
