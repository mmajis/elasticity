package net.majakorpi.elasticity.controller.util;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.majakorpi.elasticity.integration.ganglia.xml.GangliaXML;
import net.majakorpi.elasticity.integration.ganglia.xml.Grid;
import net.majakorpi.elasticity.integration.ganglia.xml.Hosts;
import net.majakorpi.elasticity.integration.ganglia.xml.Metrics;
import net.majakorpi.elasticity.logic.UtilityFunctionScalingDecisionService;
import net.majakorpi.elasticity.model.Cluster;
import net.majakorpi.elasticity.model.Host;
import net.majakorpi.elasticity.model.Metric;
import net.majakorpi.elasticity.model.Slope;
import net.majakorpi.elasticity.model.SummaryMetric;

public class GangliaConverter {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(GangliaConverter.class);
	
	private static final String VM_IMAGE_ID_METRIC_NAME = "vmImageId";
	private static final String VM_INSTANCE_ID_METRIC_NAME = "vmInstanceId";

	public static List<Cluster> convert(GangliaXML summaryXML, GangliaXML hostXML) {
		HashMap<String, Cluster> clusters = new HashMap<String, Cluster>();
		convert(summaryXML, clusters);
		convert(hostXML, clusters);
		return new ArrayList<Cluster>(clusters.values());
	}
	
	private static HashMap<String, Cluster> convert(GangliaXML gangliaXML, HashMap<String, Cluster> clusters) {
		//List<Cluster> clusters = new ArrayList<Cluster>();
		for (Object gangliaO : gangliaXML.getGRIDOrCLUSTEROrHOST()) {
			if (gangliaO instanceof net.majakorpi.elasticity.integration.ganglia.xml.Grid) {
				Grid gangliaGrid = (Grid) gangliaO;
				for (Object gridO : gangliaGrid.getCLUSTEROrGRIDOrHOSTS()) {
					// loop stuff under grid element
					if (gridO instanceof net.majakorpi.elasticity.integration.ganglia.xml.Cluster) {
						// handle clusters
						net.majakorpi.elasticity.integration.ganglia.xml.Cluster gangliaCluster = (net.majakorpi.elasticity.integration.ganglia.xml.Cluster) gridO;
						Cluster cluster = clusters.get(gangliaCluster.getNAME());
						Integer hostsUp = null;
						Integer hostsDown = null;
						List<SummaryMetric> metrics = cluster != null ? cluster.getMetrics() : new ArrayList<SummaryMetric>();
						List<Host> hosts = cluster != null ? cluster.getHosts() : new ArrayList<Host>();
						for (Object clusterO : gangliaCluster
								.getHOSTOrHOSTSOrMETRICS()) {
							// scan for hosts element
							if (clusterO instanceof Hosts) {
								Hosts hostsElement = (Hosts) clusterO;
								hostsUp = Integer.valueOf(hostsElement.getUP());
								hostsDown = Integer.valueOf(hostsElement
										.getDOWN());
							}
						}
						if (cluster == null) {
							cluster = new Cluster(metrics, hosts,
									gangliaCluster.getNAME(),
									gangliaCluster.getOWNER(), hostsUp, hostsDown);
						}
						clusters.put(gangliaCluster.getNAME(), cluster);
						for (Object clusterO : gangliaCluster
								.getHOSTOrHOSTSOrMETRICS()) {
							if (clusterO instanceof Metrics) {
								// cluster metrics
								SummaryMetric resultMetric = convertSummaryMetric(
										cluster, clusterO);
								metrics.add(resultMetric);
							} else if (clusterO instanceof net.majakorpi.elasticity.integration.ganglia.xml.Host) {
								// host in a cluster
								Host host = convertHost(cluster, clusterO);
								hosts.add(host);
							}
						}
					}

				}
			}
		}
		return clusters;
	}

	private static SummaryMetric convertSummaryMetric(Cluster cluster,
			Object clusterO) {
		Metrics gangliaMetrics = (Metrics) clusterO;
//		LOGGER.debug("metric " + gangliaMetrics.getNAME() + ": " + gangliaMetrics.getSUM());
		BigDecimal sum = "nan".equalsIgnoreCase(gangliaMetrics.getSUM()) 
						|| "inf".equalsIgnoreCase(gangliaMetrics.getSUM()) ? 
				null : new BigDecimal(gangliaMetrics.getSUM());
		SummaryMetric resultMetric = new SummaryMetric(
				gangliaMetrics.getNAME(), 
				sum,
				Integer.valueOf(gangliaMetrics.getNUM()),
				gangliaMetrics.getUNITS(), 
				Slope.fromString(gangliaMetrics.getSLOPE()), 
				cluster);
		
		return resultMetric;
	}

	private static Host convertHost(Cluster cluster, Object clusterO) {
		net.majakorpi.elasticity.integration.ganglia.xml.Host gangliaHost = (net.majakorpi.elasticity.integration.ganglia.xml.Host) clusterO;
		List<Metric> hostMetrics = new ArrayList<Metric>();
		String vmInstanceId = null;
		String vmImageId = null;
		for (net.majakorpi.elasticity.integration.ganglia.xml.Metric gangliaMetric : gangliaHost
				.getMetric()) {

			if (VM_INSTANCE_ID_METRIC_NAME.equals(gangliaMetric.getNAME())) {
				vmInstanceId = gangliaMetric.getVAL();
			} else if (VM_IMAGE_ID_METRIC_NAME.equals(gangliaMetric.getNAME())) {
				vmImageId = gangliaMetric.getVAL();
			}
		}
		Host host = new Host(hostMetrics, gangliaHost.getNAME(), vmInstanceId,
				vmImageId, cluster, Long.valueOf(gangliaHost.getGMONDSTARTED()));
		for (net.majakorpi.elasticity.integration.ganglia.xml.Metric gangliaMetric : gangliaHost
				.getMetric()) {

			hostMetrics.add(new Metric(gangliaMetric.getNAME(), gangliaMetric
					.getVAL(), gangliaMetric.getUNITS(), Slope
					.fromString(gangliaMetric.getSLOPE()), cluster, host));
		}
		return host;
	}

}
