package net.majakorpi.elasticity.model;

import java.io.Serializable;
import java.math.BigDecimal;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

public class Metric implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final String name;
	private final BigDecimal bigDecimalValue;
	private final String stringValue;
	private final String unit;
	private final Slope slope;
	private final Cluster cluster;
	private final Host host;

	public Metric(String name, String value, String unit, Slope slope,
			Cluster cluster, Host host) {
		super();
		this.name = name;
		this.unit = unit;
		this.slope = slope;
		this.cluster = cluster;
		this.host = host;
		this.stringValue = value;
		BigDecimal convertedValue = null;
		try {
			convertedValue = new BigDecimal(value);
		} catch (NumberFormatException nfe) {
			// Do nothing and yield to the fact that the value is not a number.
		}
		this.bigDecimalValue = convertedValue;
	}

	public Host getHost() {
		return host;
	}

	public String getName() {
		return name;
	}

	/**
	 * Returns the numeric value of this metric or <code>null</code> if the
	 * value is not numeric.
	 * @return 	 The numeric value of this metric or <code>null</code> if the
	 * value is not numeric.

	 */
	public BigDecimal getNumericValue() {
		return bigDecimalValue;
	}

	public String getValue() {
		return stringValue;
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
