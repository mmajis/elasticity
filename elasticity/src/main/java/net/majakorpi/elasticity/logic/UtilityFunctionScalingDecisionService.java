package net.majakorpi.elasticity.logic;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import net.majakorpi.elasticity.actuator.ChefFacade;
import net.majakorpi.elasticity.actuator.ChefResult;
import net.majakorpi.elasticity.controller.util.GangliaConverter;
import net.majakorpi.elasticity.controller.util.GangliaFacade;
import net.majakorpi.elasticity.integration.ganglia.xml.GangliaXML;
import net.majakorpi.elasticity.model.Cluster;
import net.majakorpi.elasticity.model.Slope;
import net.majakorpi.elasticity.model.SummaryMetric;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;

import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Gauge;
import com.yammer.metrics.core.MetricName;

public class UtilityFunctionScalingDecisionService implements
		ScalingDecisionService {

	private static final int RESP_TIME_SPIKE_SLOPE_THRESHOLD = 200;

	private static final Logger LOGGER = LoggerFactory
			.getLogger(UtilityFunctionScalingDecisionService.class);

	private final static String RESPONSE_TIME_METRIC_NAME = "org.eclipse.jetty.server.handler.DefaultHandler.dispatches.median";

	private final static String VM_COUNT_METRIC_NAME = "vm_count";

	private final static String REQUEST_RATE_METRIC_NAME = "org.eclipse.jetty.server.handler.DefaultHandler.requests.1MinuteRate";
	private final static String QUEUE_SIZE_METRIC_NAME = "org.eclipse.jetty.util.thread.QueuedThreadPool.queue-size-now";

//	private static final double MIN_RATE_BOUNDARY_PERCENTAGE = 0;
//
//	private static final double MAX_RATE_BOUNDARY_PERCENTAGE = 0.9;

	private static final double MAX_RESPONSE_TIME = 1500;
	private static final double MIN_RESPONSE_TIME = 800;

	private static final long scalingQuietPeriod = 3 * 60 * 1000;
	
	private static final double MAX_COST = 20;
	private static final double COST_PER_VM = 1;
	
	private static final double VM_THREAD_POOL_SIZE = 20;
	
	/**
	 * Assumption or "prior knowledge" based guideline of throughput capacity 
	 * for scaling in purposes.
	 */
	private volatile double okThroughputPerVM = 1; 

	private volatile long previousScalingOperationStartTime = 0;
	private volatile long previousScalingOperationEndTime = 0;
	
	private volatile double utilityGaugeValue, requestRatePreferenceGaugeValue,
			costPreferenceGaugeValue, responseTimePreferenceGaugeValue, 
			costPerRequestPerSecondGaugeValue, 	responseTimeSlopeGaugeValue,
			vmCountGaugeValue, referenceThroughputGaugeValue, 
			maxCostPerReqGaugeValue, minCostPerReqGaugeValue;
	
	private Future<ChefResult> chefResultFuture;
	
	/**
	 * 3 values per minute if scheduling is to check performance every 20 seconds.
	 * Now we get slope with new value compared to 5 minutes ago...
	 */
	private volatile double[] responseTimesForSlope = new double[] {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1};
	private volatile int respTimeSlopeIndex = 0;

	/**
	 * Maps request rate boundaries to VM counts. Used to store history
	 * knowledge of boundaries where scaling further out was needed.
	 */
	// private Map<Double, Integer> scaleBoundaries = new HashMap<Double,
	// Integer>();
	/**
	 * Rates at which there was a decision to scale further out.
	 */
	private Deque<Double> boundaryRates = new LinkedList<Double>();

	private final ChefFacade chefFacade;
	
	@Autowired
	public UtilityFunctionScalingDecisionService(ChefFacade chefFacade) {
		this.chefFacade = chefFacade;
		Metrics.defaultRegistry().newGauge(
				new MetricName("elasticity", "utility", 
						"utility"),
				new Gauge<Double>() {
					@Override
					public Double getValue() {
						return utilityGaugeValue;
					}
				});
		Metrics.defaultRegistry().newGauge(
				new MetricName("elasticity", "utility", 
						"costPreference"),
				new Gauge<Double>() {
					@Override
					public Double getValue() {
						return costPreferenceGaugeValue;
					}
				});
		Metrics.defaultRegistry().newGauge(
				new MetricName("elasticity", "utility",
						"costPerRequestPerSecond"), new Gauge<Double>() {
					@Override
					public Double getValue() {
						return costPerRequestPerSecondGaugeValue;
					}
				});
		Metrics.defaultRegistry()
				.newGauge(
						new MetricName("elasticity", "utility",
								"requestRatePreference"), new Gauge<Double>() {
							@Override
							public Double getValue() {
								return requestRatePreferenceGaugeValue;
							}
						});
		Metrics.defaultRegistry().newGauge(
				new MetricName("elasticity", "utility",
						"responseTimePreference"), new Gauge<Double>() {
					@Override
					public Double getValue() {
						return responseTimePreferenceGaugeValue;
					}
				});
		Metrics.defaultRegistry().newGauge(
				new MetricName("elasticity", "utility",
						"referenceThroughput"), new Gauge<Double>() {
					@Override
					public Double getValue() {
						return okThroughputPerVM;
					}
				});
		Metrics.defaultRegistry().newGauge(
				new MetricName("elasticity", "utility",
						"responseTimeSlope"), new Gauge<Double>() {
					@Override
					public Double getValue() {
						return responseTimeSlopeGaugeValue;
					}
				});		
		Metrics.defaultRegistry().newGauge(
				new MetricName("elasticity", "utility",
						"vmCount"), new Gauge<Double>() {

					@Override
					public Double getValue() {
						return vmCountGaugeValue;
					}
				});
		Metrics.defaultRegistry().newGauge(
				new MetricName("elasticity", "utility",
						"referenceThroughput"), new Gauge<Double>() {

					@Override
					public Double getValue() {
						return referenceThroughputGaugeValue;
					}
				});
		Metrics.defaultRegistry().newGauge(
				new MetricName("elasticity", "utility",
						"maxCostPerRequest"), new Gauge<Double>() {

					@Override
					public Double getValue() {
						return maxCostPerReqGaugeValue;
					}
				});
		Metrics.defaultRegistry().newGauge(
				new MetricName("elasticity", "utility",
						"minCostPerRequest"), new Gauge<Double>() {

					@Override
					public Double getValue() {
						return minCostPerReqGaugeValue;
					}
				});
	}

	@Scheduled(fixedRate = 20000)
	public void scheduledScalingDecision() throws JAXBException, IOException {
		LOGGER.info("Starting scaling analysis");

		JAXBContext context = JAXBContext.newInstance(GangliaXML.class);

		InputStream isSummary = GangliaFacade.getGangliaSummaryData();
		InputStream isHosts = GangliaFacade.getGangliaHostData();
		String xmlSummary = IOUtils.toString(isSummary, "ISO-8859-1");
		String xmlHosts = IOUtils.toString(isHosts, "ISO-8859-1");

		StringReader reader = new StringReader(xmlSummary);
		GangliaXML summaryXML = (GangliaXML) context.createUnmarshaller()
				.unmarshal(reader);
		reader = new StringReader(xmlHosts);
		GangliaXML hostXML = (GangliaXML) context.createUnmarshaller()
				.unmarshal(reader);

		makeScalingDecision(GangliaConverter.convert(summaryXML, hostXML));
	}

	@Override
	public void makeScalingDecision(
			List<Cluster> sensorData) {
		Map<String, SummaryMetric> metrics = digMetrics(sensorData);
		
		for (SummaryMetric metric : metrics.values()) {
			LOGGER.debug(metric.toString());
		}
		
		double vmCount = metrics.get(VM_COUNT_METRIC_NAME).getSum().doubleValue();
		vmCountGaugeValue = vmCount;
		double requestRate = metrics.get(REQUEST_RATE_METRIC_NAME).getSum().doubleValue();
		double responseTime = metrics.get(RESPONSE_TIME_METRIC_NAME).getSum().doubleValue()
				/metrics.get(RESPONSE_TIME_METRIC_NAME).getNum().doubleValue(); 
		double queueSize = metrics.get(QUEUE_SIZE_METRIC_NAME).getSum().doubleValue()
				/ metrics.get(QUEUE_SIZE_METRIC_NAME).getNum();
		double responseTimeSlope = calculateRespTimeSlope(responseTime);
		if (responseTimeSlope > RESP_TIME_SPIKE_SLOPE_THRESHOLD) {
			LOGGER.debug("\n\n\n!!!SPIKE!!!");
		}
		double scalingUtility = getScalingUtility(		
				responseTime, vmCount, requestRate, queueSize, responseTimeSlope);
		
		synchronized(this) {
			ChefResult chefResult = null;
			if (chefResultFuture != null && chefResultFuture.isDone()) {
				try {
					chefResult = chefResultFuture.get();
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (ExecutionException e) {
					e.printStackTrace();
				}
				chefResultFuture = null;
				LOGGER.info("Previous chef run completed with code: " 
						+ chefResult.getReturnCode() + " and took " 
						+ (chefResult.getEndMillis() 
								- previousScalingOperationStartTime)
						+ " ms");
				previousScalingOperationEndTime = chefResult.getEndMillis();
			} else if (chefResultFuture != null) {
				LOGGER.info("Chef is running so no scaling operations will be " +
						"undertaken now!");
				return;
			}
			
			if (chefResultFuture == null) {
				long timeSinceLastChefEnd = System.currentTimeMillis() 
						- previousScalingOperationEndTime;
				if (timeSinceLastChefEnd <= scalingQuietPeriod) {
					LOGGER.debug("In post scaling quiet period for another " 
							+ ((scalingQuietPeriod - timeSinceLastChefEnd)/1000f) 
							+ " seconds.");
						return;
					
				}
			}

			if (vmCount * COST_PER_VM > MAX_COST) {
				for (int i = (int)vmCount; i * COST_PER_VM > MAX_COST; --i) {
					LOGGER.info("Terminating an instance to get below max cost!");
					chefResultFuture = terminateInstances(vmCount, requestRate,
							responseTimeSlope, queueSize);
				}
			}
			
			if (scalingUtility > 0.9 
					&& requestRate > 2 
					&& ((vmCount + 1) * COST_PER_VM) <= MAX_COST) {
				boundaryRates.add(requestRate);
				LOGGER.info("Provisioning new instance...");
				chefResultFuture = provisionNewInstances(vmCount, queueSize, responseTimeSlope);
			} else if (scalingUtility < -0.5) {
				LOGGER.info("Terminating an instance...");
				previousScalingOperationStartTime = System.currentTimeMillis();
				chefResultFuture = terminateInstances(vmCount, requestRate, 
						responseTimeSlope, queueSize);
			} else {
				LOGGER.info("No scaling operations decided.");
				if (((vmCount + 1) * COST_PER_VM) > MAX_COST) {
					LOGGER.info("Scaling up would exceed cost.");
				}
			}
		}
	}

	private synchronized double calculateRespTimeSlope(double responseTime) {
		if (responseTimesForSlope[respTimeSlopeIndex] == -1) {
			//first time
			responseTimesForSlope[respTimeSlopeIndex] = responseTime;
			++respTimeSlopeIndex;
			respTimeSlopeIndex = respTimeSlopeIndex % responseTimesForSlope.length;
			responseTimeSlopeGaugeValue = 0;
			return 0;
		} else {
			double slope = (responseTime - responseTimesForSlope[respTimeSlopeIndex]) / 5;
			LOGGER.debug("Calculated slope " + slope + " from current " + responseTime 
					+ " and past " + responseTimesForSlope[respTimeSlopeIndex]);
			responseTimesForSlope[respTimeSlopeIndex] = responseTime;
			++respTimeSlopeIndex;
			respTimeSlopeIndex = respTimeSlopeIndex % responseTimesForSlope.length;
			responseTimeSlopeGaugeValue = slope;
			return slope;
		}
	}

	private Future<ChefResult> provisionNewInstances(
			double vmCount, double queueSize, double responseTimeSlope) {
		previousScalingOperationStartTime = System.currentTimeMillis();
		int amountToProvision = getScaleOutAmount(vmCount, queueSize, responseTimeSlope);
//		return fakeChefRun();
		return chefFacade.provisionNewInstance(amountToProvision);
	}

	private Future<ChefResult> terminateInstances(double vmCount, double requestRate, 
			double responseTimeSlope, double queueSize) {
		previousScalingOperationStartTime = System.currentTimeMillis();
		int amountToTerminate = getScaleInAmount(vmCount, requestRate, 
				responseTimeSlope, queueSize);
//		return fakeChefRun();
		return chefFacade.terminateInstance(amountToTerminate);
	}

	private Map<String, SummaryMetric> digMetrics(List<Cluster> sensorData) {
		Map<String, SummaryMetric> metrics = new HashMap<String, SummaryMetric>();
		boolean vmCountFound = false;
		for (Cluster cluster : sensorData) {
			for (SummaryMetric metric : cluster.getMetrics()) {
				if (RESPONSE_TIME_METRIC_NAME.equals(metric.getName())) {
					metrics.put(RESPONSE_TIME_METRIC_NAME, metric);
				}
				if (REQUEST_RATE_METRIC_NAME.equals(metric.getName())) {
					metrics.put(REQUEST_RATE_METRIC_NAME, metric);
				}
				if (!vmCountFound && metric.getName() != null 
						&& metric.getName().contains("jetty")) {
					metrics.put(VM_COUNT_METRIC_NAME, new SummaryMetric(
							VM_COUNT_METRIC_NAME, new BigDecimal(metric.getNum()),
							1, "VMs", Slope.BOTH, cluster));
					vmCountFound = true;
				}
				if (QUEUE_SIZE_METRIC_NAME.equals(metric.getName())) {
					metrics.put(QUEUE_SIZE_METRIC_NAME, metric);
				}
			}
		}
		return metrics;
	}

	@SuppressWarnings("unused")
	private double getScalingUtility(double responseTime, double vmCount,
			double requestRate, double queueSize, double responseTimeSlope) {
		LOGGER.debug("Calculating utility for respTime " 
			+ responseTime + ", vmCount " + vmCount + ", requestRate " 
				+ requestRate + ", referenceThroughput " + okThroughputPerVM
				+ ", queue size " + queueSize + ", resp time slope " + responseTimeSlope);
		double respTimeWeight = 1, costWeight = 1, requestRateWeight = 1;

		//calculated even though not used in utility so we update okThroughputperVM if necessary.
		double throughputPreference = throughputPreference(
				requestRate, vmCount, responseTime,
				queueSize);
		
		double value = 
				respTimeWeight 
				* responseTimePreference(requestRate, vmCount, responseTime, responseTimeSlope)
				+ costWeight 
				* costPreference(requestRate, vmCount, responseTimeSlope, queueSize);
//				+ requestRateWeight
//				* throughputPreference(requestRate, vmCount, responseTime);

		utilityGaugeValue = value;
		LOGGER.debug("Utility: " + value);
		return value;
	}

	/**
	 * Returns 0 when resp time is < MIN_RESPONSE_TIME seconds.
	 * 
	 * Returns (x - MIN_RESPONSE)/(MAX_RESPONSE_TIME - MIN_RESPONSE) when 0.1s
	 * <= resp time <= MAX_RESPONSE_TIME s.
	 * 
	 * Returns 1 when resp time > MAX_RESPONSE_TIME seconds
	 * 
	 * @return
	 */
	private double responseTimePreference(double requestRate, double vmCount, 
			  double responseTime, double ResponseTimeSlope) {
//		if (vmCount == 1 && requestRate < okThroughputPerVM) {
//			return 0;
//		}
		double value;
		if (responseTime < MIN_RESPONSE_TIME) {
			value = 0;
		} else if (responseTime > MAX_RESPONSE_TIME || ResponseTimeSlope > RESP_TIME_SPIKE_SLOPE_THRESHOLD) {
			value = 1;
		} else {
			value = (responseTime - MIN_RESPONSE_TIME)
					/ (MAX_RESPONSE_TIME - MIN_RESPONSE_TIME);
		}
		responseTimePreferenceGaugeValue = value;
		LOGGER.debug("Response time preference: " + value);
		return value;
	}

	/**
	 * 
	 */
	private double costPreference(double requestRate, double vmCount, 
			double responseTimeSlope, double queueSize) {
		double cost = (vmCount * COST_PER_VM) / requestRate;
		double minCostPerRequest = ((1.0 * COST_PER_VM) / okThroughputPerVM);
		double maxCostPerRequest = 1;
		if (minCostPerRequest > maxCostPerRequest) {
			minCostPerRequest = maxCostPerRequest;
		}
		double costRangeMean = (maxCostPerRequest + minCostPerRequest) / 2.0;
		
		LOGGER.debug("Max cost per request: " + maxCostPerRequest);
		LOGGER.debug("Min cost per request: " + minCostPerRequest);
		LOGGER.debug("Cost per request: " + cost);
		LOGGER.debug("Mean of min + max: " + costRangeMean);
		double costPerRequestPreference = 0;
		if ((vmCount * COST_PER_VM) > MAX_COST) {
			LOGGER.debug("Raw cost " + vmCount * COST_PER_VM 
					+ " exceeds global MAX_COST of " + MAX_COST);
			costPerRequestPreference = -1;
		} else {
			if (cost > maxCostPerRequest) {
				if (vmCount == 1) {
					costPerRequestPreference = 0;
				} else {
					costPerRequestPreference = -1;
				}
			} else if (cost < minCostPerRequest) {
				costPerRequestPreference = 0;
			}
//			else if (cost > costRangeMean) {
			else if (cost > minCostPerRequest) {
				if (vmCount == 1) {
					costPerRequestPreference = 0;
				} else {
					costPerRequestPreference = 
							-((cost - minCostPerRequest)
							/ (maxCostPerRequest - minCostPerRequest));
				}
			}
			else if (cost <= minCostPerRequest) {
//			else if (cost <= costRangeMean) {
//				costPerRequestPreference = 1 - (
//						((cost - minCostPerRequest)
//						/ (costRangeMean - minCostPerRequest)));
				costPerRequestPreference = 0;
			}
		}
		if (costPerRequestPreference > 0 && ((vmCount + 1) * COST_PER_VM) > MAX_COST) {
			LOGGER.debug("Setting cost preference to zero in order not to exceed MAX_COST");
			costPerRequestPreference = 0;
		}
		if (costPerRequestPreference < 0 && vmCount == 1) {
			LOGGER.debug("Setting cost preference ("+costPerRequestPreference
					+") to zero. Not suggesting to remove single VM instance");
			costPerRequestPreference = 0;
		}
		if (responseTimeSlope > 50 && costPerRequestPreference < 0) {
			LOGGER.debug("Setting cost preference ("+costPerRequestPreference
					+") to zero. Response time slope is high.");
			costPerRequestPreference = 0;
		}
		if (queueSize > 0 && costPerRequestPreference < 0) {
			//TODO should cost pref be positive here since there is queue?
			LOGGER.debug("Setting cost preference ("+costPerRequestPreference
					+") to zero. There are queueing requests.");
			costPerRequestPreference = 0;
		}
		costPerRequestPerSecondGaugeValue = cost;
		costPreferenceGaugeValue = costPerRequestPreference;
		LOGGER.debug("Cost preference: " + costPerRequestPreference);
		return costPerRequestPreference;
	}

//	/**
//	 * Returns a 0..-1 value depicting the preference of scaling in due to
//	 * reduced request rate.
//	 * 
//	 * Allow 20% slack, then start suggesting scale in.
//	 * 
//	 * @param requestRate
//	 * @return
//	 */
//	private double requestRatePreference(double requestRate, double vmCount) {
//		double value = 0;
//		if (requestRateTooLowForVMCount(vmCount, requestRate)) {
//			LOGGER.debug("request rate is less than 1 per vm, recommend scale in -1");
//			value = -1;
//			requestRatePreferenceGaugeValue = value;
//			logRequestRatePreference(value);
//			return value;
//		}
//		Iterator<Double> boundaryIterator = boundaryRates.descendingIterator();
//		for (Double boundaryRate = boundaryIterator.next(); boundaryIterator.hasNext();) {
//			LOGGER.debug("requestRatePreference looping boundary rate "
//					+ boundaryRate);
//			if (requestRate > boundaryRate) {
//				LOGGER.debug("Not a scale in situation. Request rate exceeds "
//						+ "previous boundary. Return 0.");
//				value = 0;
//				break;
//			}
//			double rateBoundaryPercentage = (requestRate / boundaryRate);
//			if (rateBoundaryPercentage >= MAX_RATE_BOUNDARY_PERCENTAGE) {
//				// is the rate smaller than a boundary but within 10%? Then
//				// don't scale.
//				LOGGER.debug("rate within 10% of boundary, returning 0 for request rate preference");
//				value = 0;
//				break;
//			} else {
//				value = 1 - ((rateBoundaryPercentage - MIN_RATE_BOUNDARY_PERCENTAGE) / (MAX_RATE_BOUNDARY_PERCENTAGE - MIN_RATE_BOUNDARY_PERCENTAGE));
//				LOGGER.debug("Calculated request rate preference: " + value);
//				break;
//			}
//		}
//		requestRatePreferenceGaugeValue = value;
//		logRequestRatePreference(value);
//		return value;
//	}
//	
	/**
	 * Knowledge based throughput calculation of request rate preference.
	 * @param requestRate
	 * @param vmCount
	 * @return
	 */
	private double throughputPreference(
			double requestRate, double vmCount, double responseTime, double queueSize)  {
		if (responseTime < MIN_RESPONSE_TIME 
				&& ((requestRate / vmCount) > okThroughputPerVM) && queueSize == 0) {
			//queue size is checked because if a spike has just started, response time will
			//not have reacted yet (5 minute median)
			okThroughputPerVM = requestRate / vmCount;
			LOGGER.debug("Storing new reference throughtput measure: " + okThroughputPerVM);
		}
		double knowledgeBasedVMCountTarget = requestRate / okThroughputPerVM;
		if (knowledgeBasedVMCountTarget < 1) {
			knowledgeBasedVMCountTarget = 1;
		}
		LOGGER.debug("throughputPreference target VMs: " + knowledgeBasedVMCountTarget);
		double value;
		//ok throughput should be set a bit lower than actual maximum so that
		//positive preference is possible (and so that some cpu capacity at VMs 
		//is reserved for maintenance etc. 
		double difference = (knowledgeBasedVMCountTarget - vmCount);
		
		if (difference > 1) {
			value = 1;
		} else if (difference < -1) {
			value = -1;
		} else {
			value = difference;
		}
		requestRatePreferenceGaugeValue = value;
		LOGGER.debug("throughputPreference: " + value);
		return value;
	}
	
	
	/**
	 * Returns the amount of VMs to terminate when scaling in.
	 * @return
	 */
	private int getScaleInAmount(final double vmCount, final double requestRate,
			double responseTimeSlope, double queueSize) {
		int amountToTerminate = 0;
		double costPref = -1;
		int i = (int)vmCount;
		while (true) {
			costPref = costPreference(requestRate, i, responseTimeSlope, queueSize);
			LOGGER.debug("getScaleInAmount trying " 
					+ i + " VM. CostPreference: " + costPref);
			if (costPref >= 0) {
				amountToTerminate = (int)vmCount - i;
				break;
			} else {
				--i;
			}
		}
		LOGGER.debug("Cost preference suggests to terminate " 
				+ amountToTerminate + " VMs." );
		return amountToTerminate;
	}

	private int getScaleOutAmount(double vmCount, double queueSize, double responseTimeSlope) {
		double suggestedScaleOutAmount = 0;
		if (queueSize < 1) {
			//slope based calculation
			/*
			 * lets say the 1 minute slope is more than half a second (> 500). 
			 * Then we go for spike. Otherwise we go for slow growth.
			 */
			if (responseTimeSlope > RESP_TIME_SPIKE_SLOPE_THRESHOLD) {
				suggestedScaleOutAmount = vmCount * 3;
			} else {
				suggestedScaleOutAmount = 0.5 * vmCount;
			}
		} else {
			//divide pool size by 2 because the accumulated queue keeps load high so request
			//rate needs to really drop well to be able to clear the queue
			
			//TODO add VMs until cost goes to max? i.e. always max out? wtf? 
			//or make below calculation with nginx active conns amount instead of jetty queue?
			
			//TODO Limit jetty queue so that a queued up instance won't give bad performance after others join!
			
			suggestedScaleOutAmount = queueSize / (VM_THREAD_POOL_SIZE / 2.0);
		}
		return ceilSuggestedVMAmountToCostLimit(vmCount,
				suggestedScaleOutAmount);
	}

	private int ceilSuggestedVMAmountToCostLimit(double vmCount,
			double suggestedAmount) {
		int allowedAmount = (int)Math.ceil(suggestedAmount);
		while ((vmCount + allowedAmount) * COST_PER_VM > MAX_COST) {
			--allowedAmount;
		}
		LOGGER.debug("" +(int)Math.ceil(suggestedAmount) 
				+ " instances suggested for scale out. After cost limit checking: " + allowedAmount);
		return allowedAmount;
	}

	private Future<ChefResult> fakeChefRun() {
		return new Future<ChefResult>() {
	
			@Override
			public boolean cancel(boolean mayInterruptIfRunning) {
				return true;
			}
	
			@Override
			public boolean isCancelled() {
				return false;
			}
	
			@Override
			public boolean isDone() {
				return true;
			}
	
			@Override
			public ChefResult get() throws InterruptedException,
					ExecutionException {
				return new ChefResult(0);
			}
	
			@Override
			public ChefResult get(long timeout, TimeUnit unit)
					throws InterruptedException, ExecutionException,
					TimeoutException {
				return new ChefResult(0);
			}
			
		};
	}

//	private boolean requestRateTooLowForVMCount(double vmCount,
//			double requestRate) {
//		return requestRate < vmCount && vmCount > 1;
//	}
//	
//	private void logRequestRatePreference(double value){
//		LOGGER.debug("Request rate preference: " + value);
//	}
}

