package net.majakorpi.elasticity.controller.web;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
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
import net.majakorpi.elasticity.model.Metric;

import org.activiti.engine.HistoryService;
import org.activiti.engine.ManagementService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricDetail;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.runtime.ProcessInstance;
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

	private static final String GMETAD_ADDRESS = "ctrl.majakorpi.net";
	private static final int GMETAD_INTERACTIVE_PORT = 8651;

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
			@RequestParam(defaultValue = "") String gangliaQuery)
			throws UnknownHostException, IOException, JAXBException {
		LOGGER.info("Fetching Ganglia XML...");

		JAXBContext context = JAXBContext.newInstance(GangliaXML.class);

		GangliaXML gXML = (GangliaXML) context.createUnmarshaller().unmarshal(
				getGangliaData());

		runProcess(gXML);

		model.addAttribute("metricSource", gXML.getSOURCE());
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		context.createMarshaller().marshal(gXML, baos);
		model.addAttribute("xml", baos.toString("ISO-8859-1"));

		LOGGER.info(managementService.getProperties().toString());

		return "home";
	}

	protected Socket getSocket() throws UnknownHostException, IOException {
		Socket socket = new Socket();
		SocketAddress sa = new InetSocketAddress(GMETAD_ADDRESS,
				GMETAD_INTERACTIVE_PORT);
		socket.connect(sa, 2000);
		return socket;
	}

	private void runProcess(GangliaXML gXML) {
		List<Cluster> clusters = GangliaConverter.convert(gXML);
		HashMap<String, Object> variableMap = new HashMap<String, Object>();
		int i = 0;
		for (Cluster c : clusters) {
			String clusterStr = "cluster" + (i++);
			variableMap.put(clusterStr, c);
			int j = 0;
			for (Metric m : c.getMetrics()) {
				variableMap.put(clusterStr + "_" + "metric" + (j++), m);
			}
		}
		//List<String> rulesOutput = new  ArrayList<String>();
		RuleOutput ruleOutput = new RuleOutput(0, "outputput");
		variableMap.put("rulesOutput", ruleOutput);
		//variableMap.put("rulesOutput", rulesOutput);
		variableMap.put("logger", LOGGER);
		variableMap.put("clusters", clusters);
		System.out.println("running activiti process...");
		ProcessInstance pi = runtimeService.startProcessInstanceByKey(
				"MyProcess", variableMap);


				
		LOGGER.debug("history service variable updates: " + historyService.createHistoricDetailQuery()
		  .variableUpdates()
		  .processInstanceId(pi.getId())
		  .orderByVariableRevision().desc()
		  .list());
	}

	private InputStream getGangliaData() {
		try {
			return new ClassPathResource("ganglia_summary_example.xml")
					.getInputStream();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	private InputStream getSocketGangliaData(String gangliaQuery)
			throws IOException {
		Socket gangliaXMLSocket = getSocket();

		BufferedWriter br = new BufferedWriter(new OutputStreamWriter(
				gangliaXMLSocket.getOutputStream()));
		br.append(gangliaQuery);

		return gangliaXMLSocket.getInputStream();
	}

}
