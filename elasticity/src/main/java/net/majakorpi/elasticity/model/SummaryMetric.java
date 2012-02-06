package net.majakorpi.elasticity.model;

import java.io.Serializable;
import java.math.BigDecimal;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

public class SummaryMetric implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private final String name;
	private final BigDecimal sum;
	private final Integer num;
	private final String unit;
	private final Slope slope;
	private final Cluster cluster;
	
	public SummaryMetric(String name, BigDecimal sum, Integer num, String unit,
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
	
	public String toString() {
		return ReflectionToStringBuilder.toString(this);
	}

}
