package net.majakorpi.elasticity.logic;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.majakorpi.elasticity.actuator.ChefFacade;
import net.majakorpi.elasticity.model.Cluster;
import net.majakorpi.elasticity.model.Slope;
import net.majakorpi.elasticity.model.SummaryMetric;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UtilityFunctionScalingDecisionService implements
		ScalingDecisionService {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(UtilityFunctionScalingDecisionService.class);
	
	private final static String RESPONSE_TIME_METRIC_NAME = 
			"org.eclipse.jetty.server.handler.DefaultHandler.dispatches.median";
	
	private final static String VM_COUNT_METRIC_NAME = 
			"vm_count";
	
	private final static String REQUEST_RATE_METRIC_NAME = 
			"org.eclipse.jetty.server.handler.DefaultHandler.requests.1MinuteRate";

	private static final double MIN_RATE_BOUNDARY_PERCENTAGE = 0;

	private static final double MAX_RATE_BOUNDARY_PERCENTAGE = 0.9;
	
	private static final double MAX_RESPONSE_TIME = 3000;
	private static final double MIN_RESPONSE_TIME = 800;
	
	private static final long scalingQuietPeriod = 10*60*1000; 
	
	private volatile long previousScalingOperationStartTime = 0;
	
	/**
	 * Maps request rate boundaries to VM counts. Used to store history 
	 * knowledge of boundaries where scaling further out was needed.
	 */
	//private Map<Double, Integer> scaleBoundaries = new HashMap<Double, Integer>();
	/**
	 * Rates at which there was a decision to scale further out.
	 */
	private List<Double> boundaryRates = new ArrayList<Double>();

	@Override
	public void makeScalingDecision(
			List<Cluster> sensorData) {
		Map<String, SummaryMetric> metrics = digMetrics(sensorData);
		
		for (SummaryMetric metric : metrics.values()) {
			LOGGER.debug(metric.toString());
		}
		
		double vmCount = metrics.get(VM_COUNT_METRIC_NAME).getSum().doubleValue();
		LOGGER.debug("Response time preference: " 
		+ responseTimePreference(
				metrics.get(RESPONSE_TIME_METRIC_NAME).getSum().doubleValue()));
		LOGGER.debug("Cost preference: " 
		+ costPreference(vmCount));
		double requestRate = metrics.get(REQUEST_RATE_METRIC_NAME).getSum().doubleValue();
		LOGGER.debug("Request rate preference: " 
		+ requestRatePreference(requestRate, vmCount));
		double scalingUtility = getScalingUtility(
				metrics.get(RESPONSE_TIME_METRIC_NAME).getSum().doubleValue(),
				metrics.get(VM_COUNT_METRIC_NAME).getSum().doubleValue(),
				metrics.get(REQUEST_RATE_METRIC_NAME).getSum().doubleValue()
				);
		LOGGER.debug("Utility function value: " 
		+ scalingUtility);
		long timeSinceLastScaling = 
				System.currentTimeMillis() - previousScalingOperationStartTime;
		if (timeSinceLastScaling 
				<= scalingQuietPeriod) {
			LOGGER.debug("In scaling quiet period for another " 
				+ ((scalingQuietPeriod - timeSinceLastScaling)/1000f) + " seconds.");
			return;
		}
		
		if (scalingUtility > 0.5 && requestRate > 2) {
			boundaryRates.add(requestRate);
			previousScalingOperationStartTime = System.currentTimeMillis();
			LOGGER.info("Provisioning new instance...");
			int returnCode = ChefFacade.provisionNewInstance();
			LOGGER.info("Chef returned " + returnCode + " for adding a new instance.");
		} else if (scalingUtility < -0.5) {
			previousScalingOperationStartTime = System.currentTimeMillis();
			LOGGER.info("Terminating an instance...");
			int returnCode = ChefFacade.terminateInstance();
			LOGGER.info("Chef returned " + returnCode + " for terminating an instance.");
		}
	}
	
	private Map<String, SummaryMetric> digMetrics(List<Cluster> sensorData) {
		Map<String, SummaryMetric> metrics = new HashMap<String, SummaryMetric>();
		for (Cluster cluster : sensorData) {
			metrics.put(
					VM_COUNT_METRIC_NAME,
					new SummaryMetric(
							VM_COUNT_METRIC_NAME, 
							new BigDecimal(cluster.getHostsUp()), 
							1, 
							"VMs", 
							Slope.BOTH, 
							cluster)
					);
			for (SummaryMetric metric : cluster.getMetrics()) {
				if (RESPONSE_TIME_METRIC_NAME.equals(metric.getName())) {
					metrics.put(RESPONSE_TIME_METRIC_NAME, metric);
				}
				if (REQUEST_RATE_METRIC_NAME.equals(metric.getName())) {
					metrics.put(REQUEST_RATE_METRIC_NAME, metric);
				}
			}
		}
		return metrics;
	}
	
	private double getScalingUtility(
			double responseTime,
			double vmCount,
			double requestRate) {
		double respTimeWeight = 1, costWeight = 1, requestRateWeight = 1;
		
		return respTimeWeight * responseTimePreference(responseTime) + 
				costWeight * costPreference(vmCount) + 
				requestRateWeight * requestRatePreference(requestRate, vmCount);
	}
	
	/**
	 * Returns 0 when resp time is < MIN_RESPONSE_TIME seconds.
	 * 
	 * Returns (x - MIN_RESPONSE)/(MAX_RESPONSE_TIME - MIN_RESPONSE) 
	 * when 0.1s <= resp time <=  MAX_RESPONSE_TIME s.
	 * 
	 * Returns 1 when resp time > MAX_RESPONSE_TIME seconds  
	 * @return
	 */
	private double responseTimePreference(double responseTime) {
		if (responseTime < MIN_RESPONSE_TIME) {
			return 0;
		}
		if (responseTime > MAX_RESPONSE_TIME) {
			return 1;
		}
		return  (responseTime - MIN_RESPONSE_TIME)
				/(MAX_RESPONSE_TIME - MIN_RESPONSE_TIME); 
	}
	
	/**
	 * Returns -1 if cost maximum is reached. Otherwise returns 0.
	 */
	private double costPreference(double vmCount) {
		double costPerVMPerHour = 1;
		double maxCost = 15;
		//if VM count is 1, no need to scale down :)
		if (vmCount == 1) {
			return 0;
		}
		//max cost means max impulse to scale down
		if (vmCount * costPerVMPerHour >= maxCost) {
			return -1;
		}
		return 0;
	}

	/**
	 * Returns a 0..-1 value depicting the preference of scaling in due to reduced 
	 * request rate.
	 * 
	 * Allow 20% slack, then start suggesting scale in.
	 * 
	 * @param requestRate
	 * @return
	 */
	private double requestRatePreference(double requestRate, double vmCount) {
		if (requestRate < vmCount && vmCount > 1) {
			LOGGER.debug("request rate is less than 1 per vm, recommend scale in");
			return -1;
		}
		for (Double boundaryRate : boundaryRates) {
			LOGGER.debug("requestRatePreference looping boundary rate " + boundaryRate);
			if (requestRate > boundaryRate) {
				LOGGER.debug("Not a scale in situation. Request rate exceeds " +
						"previous boundary. Return 0.");
				return 0; 
			}
			double rateBoundaryPercentage = (requestRate / boundaryRate);
			if (rateBoundaryPercentage >= MAX_RATE_BOUNDARY_PERCENTAGE) {
				//is the rate smaller than a boundary but within 10%? Then don't scale.
				LOGGER.debug("rate within 10% of boundary, returning 0 for request rate preference");
				return 0;
			} else {
				double toReturn = 1-((rateBoundaryPercentage - MIN_RATE_BOUNDARY_PERCENTAGE)
						/ (MAX_RATE_BOUNDARY_PERCENTAGE - MIN_RATE_BOUNDARY_PERCENTAGE));
				LOGGER.debug("Calculated request rate preference: " + toReturn);
				return toReturn;
			}
		}
		LOGGER.debug("no info of boundary rates, we return 0 until we learn.");
		return 0;
	}
}
