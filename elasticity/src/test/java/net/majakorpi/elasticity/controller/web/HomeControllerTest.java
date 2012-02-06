package net.majakorpi.elasticity.controller.web;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.xml.bind.JAXBException;

import junit.framework.Assert;

import net.majakorpi.elasticity.controller.web.HomeController;

import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import static org.mockito.Mockito.*;

public class HomeControllerTest {

	@Test
	public void testController() throws UnknownHostException, IOException, JAXBException {
//		HomeController controller = new HomeController();
//		HomeController spyController = Mockito.spy(controller);
//		Socket mockSocket = mock(Socket.class);
//		when(mockSocket.getOutputStream()).thenReturn(new ByteArrayOutputStream());
//		Resource xml = new ClassPathResource("net/majakorpi/ganglia_summary_example.xml");
//		when(mockSocket.getInputStream()).thenReturn(xml.getInputStream());
//		doReturn(mockSocket).when(spyController).getSocket();
//		
//		Model model = new ExtendedModelMap();
//		Assert.assertEquals("home",spyController.home(model, ""));
//		
//		Object message = model.asMap().get("metricSource");
//		Assert.assertEquals("gmetad",message);
//		
	}
}
