package net.majakorpi.elasticity.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

public class RuleOutput implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static final String PROCESS_VARIABLE_NAME = "ruleOutput";
	
	private List<ScalingAction> scalingActions = new ArrayList<ScalingAction>();

	public RuleOutput() {
		super();
	}

	public void add(ScalingAction action) {
		if (action != null) {
			scalingActions.add(action);
		}
	}
	
	public List<ScalingAction> getScalingActions() {
		return scalingActions;
	}
	
	public String toString() {
		return ReflectionToStringBuilder.toString(this);
	}
}
