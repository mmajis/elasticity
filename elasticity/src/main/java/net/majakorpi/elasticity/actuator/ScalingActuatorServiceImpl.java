package net.majakorpi.elasticity.actuator;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;

import net.majakorpi.elasticity.model.RuleOutput;

public class ScalingActuatorServiceImpl implements ScalingActuatorService,
		JavaDelegate {

	@Override
	public void implementScalingDecision(RuleOutput decision) {
		

	}

	@Override
	public void execute(DelegateExecution execution) throws Exception {
		RuleOutput decision = (RuleOutput) execution
				.getVariable(RuleOutput.PROCESS_VARIABLE_NAME);
		implementScalingDecision(decision);
	}

}
