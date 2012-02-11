package net.majakorpi.elasticity.model;

import java.io.Serializable;
import java.util.List;

public class Host implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final List<Metric> metrics;
	private final String name;
	private final String vmInstanceId;
	private final String vmImageId;
	private final Cluster cluster;
	private final Long started;

	public Host(List<Metric> metrics, String name, String vmInstanceId,
			String vmImageId, Cluster cluster, Long started) {
		super();
		this.metrics = metrics;
		this.name = name;
		this.vmInstanceId = vmInstanceId;
		this.vmImageId = vmImageId;
		this.cluster = cluster;
		this.started = started;
	}

	public Cluster getCluster() {
		return cluster;
	}

	public List<Metric> getMetrics() {
		return metrics;
	}

	public String getName() {
		return name;
	}

	public String getVmInstanceId() {
		return vmInstanceId;
	}

	public String getVmImageId() {
		return vmImageId;
	}

	public Long getStarted() {
		return started;
	}

}
