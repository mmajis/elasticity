package net.majakorpi.elasticity.logic;

import java.util.List;

import net.majakorpi.elasticity.model.Cluster;

/**
 * Implements the logic of making a scaling decision.
 * 
 */
public interface ScalingDecisionService {
	
	void makeScalingDecision(List<Cluster> sensorData);

}
