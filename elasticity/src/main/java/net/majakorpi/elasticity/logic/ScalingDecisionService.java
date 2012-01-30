package net.majakorpi.elasticity.logic;

import net.majakorpi.elasticity.integration.ganglia.xml.GangliaXML;

/**
 * Implements the logic of making a scaling decision.
 * 
 * @author mika
 *
 */
interface ScalingDecisionService {
	
	GangliaXML makeScalingDecision(GangliaXML sensorData);

}
