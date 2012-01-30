package net.majakorpi.elasticity.model;

import java.util.List;

public class ClusterBuilder {
	
	private List<Metric> metrics;
	private String name;
	private int hostsUp;
	private int hostsDown;
	
	
	public ClusterBuilder() {
		super();
	}

	public Cluster build() {
		return new Cluster(metrics, name, hostsUp, hostsDown);
	}

	public List<Metric> getMetrics() {
		return metrics;
	}


	public void setMetrics(List<Metric> metrics) {
		this.metrics = metrics;
	}


	public String getName() {
		return name;
	}


	public void setName(String name) {
		this.name = name;
	}


	public int getHostsUp() {
		return hostsUp;
	}


	public void setHostsUp(int hostsUp) {
		this.hostsUp = hostsUp;
	}


	public int getHostsDown() {
		return hostsDown;
	}


	public void setHostsDown(int hostsDown) {
		this.hostsDown = hostsDown;
	}

}
