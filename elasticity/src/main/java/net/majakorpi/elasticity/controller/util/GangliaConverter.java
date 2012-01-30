package net.majakorpi.elasticity.controller.util;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import net.majakorpi.elasticity.integration.ganglia.xml.GangliaXML;
import net.majakorpi.elasticity.integration.ganglia.xml.Grid;
import net.majakorpi.elasticity.integration.ganglia.xml.Hosts;
import net.majakorpi.elasticity.integration.ganglia.xml.Metrics;
import net.majakorpi.elasticity.model.Cluster;
import net.majakorpi.elasticity.model.Metric;
import net.majakorpi.elasticity.model.Metric.Slope;

public class GangliaConverter {

	public static List<Cluster> convert(GangliaXML gangliaXML) {
		List<Cluster> clusters = new ArrayList<Cluster>();
		for (Object gangliaO : gangliaXML.getGRIDOrCLUSTEROrHOST()) {
			if (gangliaO instanceof net.majakorpi.elasticity.integration.ganglia.xml.Grid) {
				Grid gangliaGrid = (Grid) gangliaO;
				for (Object gridO : gangliaGrid.getCLUSTEROrGRIDOrHOSTS()) {
					if (gridO instanceof net.majakorpi.elasticity.integration.ganglia.xml.Cluster) {
						net.majakorpi.elasticity.integration.ganglia.xml.Cluster gangliaCluster = (net.majakorpi.elasticity.integration.ganglia.xml.Cluster) gridO;
						Integer hostsUp = null;
						Integer hostsDown = null;
						List<Metric> metrics = new ArrayList<Metric>();
						for (Object clusterO : gangliaCluster
								.getHOSTOrHOSTSOrMETRICS()) {
							if (clusterO instanceof Hosts) {
								Hosts hosts = (Hosts) clusterO;
								hostsUp = Integer.valueOf(hosts.getUP());
								hostsDown = Integer.valueOf(hosts.getDOWN());
							}
						}
						Cluster cluster = new Cluster(metrics,
								gangliaCluster.getNAME(), hostsUp, hostsDown);
						clusters.add(cluster);
						for (Object clusterO : gangliaCluster
								.getHOSTOrHOSTSOrMETRICS()) {
							if (clusterO instanceof Metrics) {
								Metrics gangliaMetrics = (Metrics) clusterO;
								Metric resultMetric = new Metric(
										gangliaMetrics.getNAME(),
										new BigDecimal(gangliaMetrics.getSUM()),
										Integer.valueOf(gangliaMetrics.getNUM()),
										gangliaMetrics.getUNITS(), Slope
												.fromString(gangliaMetrics
														.getSLOPE()), cluster);
								metrics.add(resultMetric);
							}
						}
					}

				}
			}
		}
		return clusters;
	}

}
