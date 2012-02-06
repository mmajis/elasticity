package net.majakorpi.elasticity.model;

import java.io.Serializable;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

public class ScalingAction implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Scaling action type. MORE means to start more instance(s), LESS means to
	 * shut instance(s) down.
	 * 
	 */
	public static enum ScalingType {
		MORE, LESS
	};

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

	private final String ec2KeyPair;
	private final String ec2Subnet;
	private final String ec2InstanceType;
	private final String ec2SecurityGroup;
	private final String ec2ImageId;
	private final String ec2InstanceId;

	public ScalingAction(Cluster cluster, ScalingType scalingType,
			Integer hostCount, String ec2KeyPair, String ec2Subnet,
			String ec2InstanceType, String ec2SecurityGroup, String ec2ImageId,
			String ec2InstanceId) {
		super();
		this.cluster = cluster;
		this.scalingType = scalingType;
		this.hostCount = hostCount;
		this.ec2KeyPair = ec2KeyPair;
		this.ec2Subnet = ec2Subnet;
		this.ec2InstanceType = ec2InstanceType;
		this.ec2SecurityGroup = ec2SecurityGroup;
		this.ec2ImageId = ec2ImageId;
		this.ec2InstanceId = ec2InstanceId;
	}

	public Cluster getCluster() {
		return cluster;
	}

	public String getEc2ImageId() {
		return ec2ImageId;
	}

	public ScalingType getScalingType() {
		return scalingType;
	}

	public Integer getHostCount() {
		return hostCount;
	}

	public String getEc2KeyPair() {
		return ec2KeyPair;
	}

	public String getEc2Subnet() {
		return ec2Subnet;
	}

	public String getEc2InstanceType() {
		return ec2InstanceType;
	}

	public String getEc2SecurityGroup() {
		return ec2SecurityGroup;
	}

	public String getEc2InstanceId() {
		return ec2InstanceId;
	}

	public String toString() {
		return ReflectionToStringBuilder.toString(this);
	}

}
