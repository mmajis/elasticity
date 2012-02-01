package net.majakorpi.elasticity.actuator;

import net.majakorpi.elasticity.integration.ganglia.xml.GangliaXML;
import net.majakorpi.elasticity.model.RuleOutput;

public interface ScalingActuatorService {
	
	void implementScalingDecision(RuleOutput decision);

}
