package net.majakorpi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import net.majakorpi.elasticity.integration.ganglia.xml.GangliaXML;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Sample controller for going to the home page with a message
 */
@Controller
public class HomeController {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(HomeController.class);
	
	private static final String GMETAD_ADDRESS = "ctrl.majakorpi.net";
	private static final int GMETAD_INTERACTIVE_PORT = 8651;

	/**
	 * Selects the home page and populates the model with a message
	 * @throws IOException 
	 * @throws UnknownHostException 
	 * @throws JAXBException 
	 */
	@RequestMapping(value = "/", method = RequestMethod.GET)
	public String home(Model model) throws UnknownHostException, IOException, JAXBException {
		LOGGER.info("Fetching Ganglia XML...");
			
        Socket gangliaXMLSocket = new Socket(GMETAD_ADDRESS, GMETAD_INTERACTIVE_PORT);

        JAXBContext context = JAXBContext.newInstance(GangliaXML.class);
        
        GangliaXML gXML = (GangliaXML) context.createUnmarshaller().unmarshal(gangliaXMLSocket.getInputStream());
        
		model.addAttribute("metricSource",
				gXML.getSOURCE());
		return "home";
	}

}
