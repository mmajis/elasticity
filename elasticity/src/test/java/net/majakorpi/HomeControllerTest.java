package net.majakorpi;

import java.io.IOException;
import java.net.UnknownHostException;

import javax.xml.bind.JAXBException;

import junit.framework.Assert;

import org.junit.Test;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

public class HomeControllerTest {

	@Test
	public void testController() throws UnknownHostException, IOException, JAXBException {
		HomeController controller = new HomeController();
		Model model = new ExtendedModelMap();
		Assert.assertEquals("home",controller.home(model));
		
		Object message = model.asMap().get("metricSource");
		Assert.assertEquals("gmetad",message);
		
	}
}
