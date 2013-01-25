package net.majakorpi.elasticity.controller.util;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Gauge;
import com.yammer.metrics.core.MetricName;
import com.yammer.metrics.ganglia.GangliaReporter;

public class GangliaFacade {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(GangliaFacade.class);
	
	private static final String GMETAD_ADDRESS = "ganglia.majakorpi.net";
	private static final int GMETAD_INTERACTIVE_PORT = 8652;
	private static final int GMOND_PORT = 8649;

	static {
		GangliaReporter.enable(10, TimeUnit.SECONDS, GMETAD_ADDRESS, GMOND_PORT);
	}
	
	public static InputStream getGangliaSummaryData()
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

	public static InputStream getGangliaHostData()
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
	
	protected static Socket getSocket() throws UnknownHostException, IOException {
		Socket socket = new Socket();
		SocketAddress sa = new InetSocketAddress(GMETAD_ADDRESS,
				GMETAD_INTERACTIVE_PORT);
		socket.connect(sa, 2000);
		return socket;
	}
}
