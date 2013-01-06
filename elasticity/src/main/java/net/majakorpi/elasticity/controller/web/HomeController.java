package net.majakorpi.elasticity.controller.web;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import net.majakorpi.elasticity.controller.util.GangliaConverter;
import net.majakorpi.elasticity.integration.ganglia.xml.GangliaXML;
import net.majakorpi.elasticity.model.Cluster;
import net.majakorpi.elasticity.model.SummaryMetric;
import net.majakorpi.elasticity.model.RuleOutput;
import net.majakorpi.elasticity.model.ScalingAction;

import org.activiti.engine.HistoryService;
import org.activiti.engine.ManagementService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricDetail;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.runtime.ProcessInstance;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
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

	private static final String GMETAD_ADDRESS = "ganglia.majakorpi.net";
	private static final int GMETAD_INTERACTIVE_PORT = 8652;

	@Autowired
	private RuntimeService runtimeService;

	@Autowired
	private ManagementService managementService;

	@Autowired
	private HistoryService historyService;

	@Autowired
	private TaskService taskService;

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

		InputStream isSummary = getGangliaSummaryData();
		InputStream isHosts = getGangliaHostData();
		String xmlSummary = IOUtils.toString(isSummary, "ISO-8859-1");
		String xmlHosts = IOUtils.toString(isHosts, "ISO-8859-1");
		LOGGER.debug("Summary xml:\n" + xmlSummary);
		LOGGER.debug("\n\n\n");
		LOGGER.debug("Host xml:\n" + xmlHosts);

		StringReader reader = new StringReader(xmlSummary);
		GangliaXML summaryXML = (GangliaXML) context.createUnmarshaller().unmarshal(
				reader);
		reader = new StringReader(xmlHosts);
		GangliaXML hostXML = (GangliaXML) context.createUnmarshaller().unmarshal(
				reader);
		runProcess(summaryXML, hostXML);

		String xml = xmlSummary + "\n\n\n\n" + xmlHosts;
		model.addAttribute("xml", xml.replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br/>"));
		
		return "home";
	}

	protected Socket getSocket() throws UnknownHostException, IOException {
		Socket socket = new Socket();
		SocketAddress sa = new InetSocketAddress(GMETAD_ADDRESS,
				GMETAD_INTERACTIVE_PORT);
		socket.connect(sa, 2000);
		return socket;
	}

	private void runProcess(GangliaXML summaryXML, GangliaXML hostXML) {
		List<Cluster> clusters = GangliaConverter.convert(summaryXML, hostXML);
		for (Cluster c : clusters) {
			LOGGER.debug(c.toString());
		}
		HashMap<String, Object> variableMap = new HashMap<String, Object>();
		int i = 0;
		for (Cluster c : clusters) {
			String clusterStr = "cluster" + (i++);
			variableMap.put(clusterStr, c);
			int j = 0;
			for (SummaryMetric m : c.getMetrics()) {
				variableMap.put(clusterStr + "_" + "metric" + (j++), m);
			}
		}
		RuleOutput ruleOutput = new RuleOutput();
		variableMap.put(RuleOutput.PROCESS_VARIABLE_NAME, ruleOutput);
		variableMap.put("clusters", clusters);
		System.out.println("running activiti process...");
		ProcessInstance pi = runtimeService.startProcessInstanceByKey(
				"MyProcess", variableMap);

		LOGGER.debug("ruleoutput: " + ruleOutput);

	}

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

	private InputStream getGangliaSummaryData()
			throws IOException {
		Socket gangliaXMLSocket = getSocket();
		LOGGER.debug("writing to ganglia socket");
		BufferedWriter br = new BufferedWriter(new OutputStreamWriter(
				gangliaXMLSocket.getOutputStream()));
		br.append("/?filter=summary"+"\n");
		br.flush();
		LOGGER.debug("flushed socket.");

		return gangliaXMLSocket.getInputStream();
	}

	private InputStream getGangliaHostData()
			throws IOException {
		Socket gangliaXMLSocket = getSocket();
		LOGGER.debug("writing to ganglia socket");
		BufferedWriter br = new BufferedWriter(new OutputStreamWriter(
				gangliaXMLSocket.getOutputStream()));
		br.append("/\n");
		br.flush();
		LOGGER.debug("flushed socket.");

		return gangliaXMLSocket.getInputStream();
	}
}
