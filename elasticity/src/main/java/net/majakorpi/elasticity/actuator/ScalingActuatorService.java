package net.majakorpi.elasticity.actuator;

import net.majakorpi.elasticity.integration.ganglia.xml.GangliaXML;

public interface ScalingActuatorService {
	
	void implementScalingDecision(GangliaXML decision);

}
