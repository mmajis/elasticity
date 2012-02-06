package net.majakorpi.elasticity.model;

public enum Slope {
	
	ZERO, NEGATIVE, POSITIVE, BOTH;
	
	public static Slope fromString(String slopeString) {
		if ("both".equalsIgnoreCase(slopeString)) {
			return BOTH;
		} else if ("positive".equalsIgnoreCase(slopeString)) {
			return POSITIVE;
		} else if ("negative".equalsIgnoreCase(slopeString)) {
			return NEGATIVE;
		} else if ("zero".equalsIgnoreCase(slopeString)) {
			return ZERO;
		}
		return null;
	}
}