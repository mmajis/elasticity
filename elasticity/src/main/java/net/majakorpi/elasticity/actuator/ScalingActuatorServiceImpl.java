package net.majakorpi.elasticity.actuator;

import java.util.ArrayList;
import java.util.List;

import net.majakorpi.elasticity.model.Cluster;
import net.majakorpi.elasticity.model.RuleOutput;
import net.majakorpi.elasticity.model.ScalingAction;
import net.majakorpi.elasticity.model.ScalingAction.ScalingType;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;

//@Service(value="scalingActuatorService")
public class ScalingActuatorServiceImpl implements ScalingActuatorService,
		JavaDelegate {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(ScalingActuatorServiceImpl.class);

	private static final String VM_UNSPECIFIED = "unspecified";
	
	private final AmazonEC2 ec2;

	@Autowired
	public ScalingActuatorServiceImpl(AmazonEC2 ec2) {
		super();
		this.ec2 = ec2;
	}

	@Override
	public void implementScalingDecision(RuleOutput decision) {
		for (ScalingAction scalingAction : decision.getScalingActions()) {
			LOGGER.info("Scaling action for cluster "
					+ scalingAction.getCluster().getName() + " / "
					+ scalingAction.getCluster().getVirtualMachineId());

			if (scalingAction.getScalingType() == ScalingType.MORE) {
				//scale out
				RunInstancesRequest rir = new RunInstancesRequest();
				rir.setInstanceType(scalingAction.getEc2InstanceType());
				rir.setMaxCount(scalingAction.getHostCount());
				rir.setMinCount(scalingAction.getHostCount());
				rir.setKeyName(scalingAction.getEc2KeyPair());
				rir.setSubnetId(scalingAction.getEc2Subnet());
				rir.setImageId(scalingAction.getEc2ImageId());
				//security group?
				ec2.runInstances(rir);
			} else if (scalingAction.getScalingType() == ScalingType.LESS) {
				//scale in
				TerminateInstancesRequest tir = new TerminateInstancesRequest();
				List<String> instanceIds = new ArrayList<String>(); 
				instanceIds.add(scalingAction.getEc2InstanceId());
				tir.setInstanceIds(instanceIds);
				ec2.terminateInstances(tir);
			}
		}

	}

	@Override
	public void execute(DelegateExecution execution) throws Exception {
		RuleOutput decision = (RuleOutput) execution
				.getVariable(RuleOutput.PROCESS_VARIABLE_NAME);
		implementScalingDecision(decision);
	}

}
