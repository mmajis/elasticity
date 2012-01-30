package net.majakorpi.elasticity.model;

import java.io.Serializable;
import java.math.BigDecimal;

public class Metric implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

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
	};
	
	private final String name;
	private final BigDecimal sum;
	private final Integer num;
	private final String unit;
	private final Slope slope;
	private final Cluster cluster;
	
	public Metric(String name, BigDecimal sum, Integer num, String unit,
			Slope slope, Cluster cluster) {
		super();
		this.name = name;
		this.sum = sum;
		this.num = num;
		this.unit = unit;
		this.slope = slope;
		this.cluster = cluster;
	}

	public String getName() {
		return name;
	}

	public BigDecimal getSum() {
		return sum;
	}

	public Integer getNum() {
		return num;
	}

	public String getUnit() {
		return unit;
	}

	public Slope getSlope() {
		return slope;
	}

	public Cluster getCluster() {
		return cluster;
	}
	
	

}
