package net.majakorpi.elasticity.model;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

public class Cluster implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final List<Metric> metrics;
	private final String name;
	private final int hostsUp;
	private final int hostsDown;

	public Cluster(List<Metric> metrics, String name, int hostsUp, int hostsDown) {
		super();
		this.metrics = metrics;
		this.name = name;
		this.hostsDown = hostsDown;
		this.hostsUp = hostsUp;
	}

	public List<Metric> getMetrics() {
		return Collections.unmodifiableList(metrics);
	}

	public String getName() {
		return name;
	}

	public int getHostsUp() {
		return hostsUp;
	}

	public int getHostsDown() {
		return hostsDown;
	}
	
	public String toString() {
		return ReflectionToStringBuilder.toString(this);
	}
}
