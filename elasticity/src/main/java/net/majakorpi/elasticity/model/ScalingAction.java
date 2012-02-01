package net.majakorpi.elasticity.model;

import java.io.Serializable;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;


public class ScalingAction implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Scaling action type. MOAR means to start more instance(s), LESS means to 
	 * shut instance(s) down.
	 *
	 */
	public static enum ScalingType {MOAR, LESS};
	
	/**
	 * Target cluster.
	 */
	private final Cluster cluster;
	
	/**
	 * Scaling action type
	 */
	private final ScalingType scalingType;
	
	/**
	 * How many hosts to operate on.
	 */
	private final Integer hostCount;

	public ScalingAction(Cluster cluster, ScalingType scalingType,
			Integer hostCount) {
		super();
		this.cluster = cluster;
		this.scalingType = scalingType;
		this.hostCount = hostCount;
	}

	public Cluster getCluster() {
		return cluster;
	}

	public ScalingType getScalingType() {
		return scalingType;
	}

	public Integer getHostCount() {
		return hostCount;
	}
	
	public String toString() {
		return ReflectionToStringBuilder.toString(this);
	}
	
}
