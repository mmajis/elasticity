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
	private final List<SummaryMetric> metrics;
	private final List<Host> hosts;
	private final String name;
	private final String virtualMachineId;
	private final int hostsUp;
	private final int hostsDown;
	
	
	public Cluster(List<SummaryMetric> metrics, List<Host> hosts, String name,
			String virtualMachineId, int hostsUp, int hostsDown) {
		super();
		this.metrics = metrics;
		this.hosts = hosts;
		this.name = name;
		this.virtualMachineId = virtualMachineId;
		this.hostsUp = hostsUp;
		this.hostsDown = hostsDown;
	}

	public List<SummaryMetric> getMetrics() {
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
	
	public String getVirtualMachineId() {
		return virtualMachineId;
	}

	public List<Host> getHosts() {
		return hosts;
	}

	public String toString() {
		return ReflectionToStringBuilder.toString(this);
	}
}
