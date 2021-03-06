package net.majakorpi.elasticity.controller.web;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import net.majakorpi.elasticity.controller.util.GangliaConverter;
import net.majakorpi.elasticity.controller.util.GangliaFacade;
import net.majakorpi.elasticity.integration.ganglia.xml.GangliaXML;
import net.majakorpi.elasticity.logic.ScalingDecisionService;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Sample controller for going to the home page with a message
 */
@Controller
public class HomeController {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(HomeController.class);

//	@Autowired
//	private RuntimeService runtimeService;
//
//	@Autowired
//	private ManagementService managementService;
//
//	@Autowired
//	private HistoryService historyService;
//
//	@Autowired
//	private TaskService taskService;
	
	@Autowired
	private ScalingDecisionService scalingDecisionService;

	/**
	 * Selects the home page and populates the model with a message
	 * 
	 * @throws IOException
	 * @throws UnknownHostException
	 * @throws JAXBException
	 */
	@RequestMapping(value = "/", method = RequestMethod.GET)
	public String home(Model model,
			@RequestParam(defaultValue = "/?filter=summary") String gangliaQuery)
			throws UnknownHostException, IOException, JAXBException {
		LOGGER.info("Fetching Ganglia XML with query " + gangliaQuery);

		JAXBContext context = JAXBContext.newInstance(GangliaXML.class);

		InputStream isSummary = GangliaFacade.getGangliaSummaryData();
		InputStream isHosts = GangliaFacade.getGangliaHostData();
		String xmlSummary = IOUtils.toString(isSummary, "ISO-8859-1");
		String xmlHosts = IOUtils.toString(isHosts, "ISO-8859-1");

		StringReader reader = new StringReader(xmlSummary);
		GangliaXML summaryXML = (GangliaXML) context.createUnmarshaller().unmarshal(
				reader);
		reader = new StringReader(xmlHosts);
		GangliaXML hostXML = (GangliaXML) context.createUnmarshaller().unmarshal(
				reader);
//		runProcess(summaryXML, hostXML);
		
		//don't call this, it will screw up slope calculation if called outside schedule.
//		scalingDecisionService.makeScalingDecision(
//				GangliaConverter.convert(summaryXML, hostXML));

		String xml = "summary:\n" + xmlSummary + "\n\n\n\nhosts:\n" + xmlHosts;
		model.addAttribute(
				"xml", 
				xml.replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br/>"));
		
		return "home";
	}

//	private void runProcess(GangliaXML summaryXML, GangliaXML hostXML) {
//		List<Cluster> clusters = GangliaConverter.convert(summaryXML, hostXML);
//		for (Cluster c : clusters) {
//			LOGGER.debug(c.toString());
//		}
//		HashMap<String, Object> variableMap = new HashMap<String, Object>();
//		int i = 0;
//		for (Cluster c : clusters) {
//			String clusterStr = "cluster" + (i++);
//			variableMap.put(clusterStr, c);
//			int j = 0;
//			for (SummaryMetric m : c.getMetrics()) {
//				variableMap.put(clusterStr + "_" + "metric" + (j++), m);
//			}
//		}
//		RuleOutput ruleOutput = new RuleOutput();
//		variableMap.put(RuleOutput.PROCESS_VARIABLE_NAME, ruleOutput);
//		variableMap.put("clusters", clusters);
//		System.out.println("running activiti process...");
//		ProcessInstance pi = runtimeService.startProcessInstanceByKey(
//				"MyProcess", variableMap);
//
//		LOGGER.debug("ruleoutput: " + ruleOutput);
//
//	}

	/**
	 * Temporary method to feed xml data from file instead of socket.
	 * @return
	 */
	private InputStream getGangliaData() {
		try {
			return new ClassPathResource("ganglia_summary_example.xml")
					.getInputStream();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
}
