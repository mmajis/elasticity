package net.majakorpi.elasticity.actuator;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.StreamHandler;

import net.majakorpi.elasticity.controller.web.HomeController;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteStreamHandler;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.PumpStreamHandler;
import org.codehaus.plexus.util.StringOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

public class ChefFacade {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(ChefFacade.class);

	public static String WORKING_DIRECTORY = "/Users/mika/chef/chef-repo";

	private volatile static boolean isChefRunning = false;

	private static final Lock chefLock = new ReentrantLock();

	public static final int RETURN_CODE_CHEF_ALREADY_RUNNING = 500;

	private ChefFacade() {
		super();
	}

	public static int provisionNewInstance() {
		// knife ec2 server create -I ami-3d4ff254 -x ubuntu -r
		// "role[base],role[testapp]" --flavor m1.small
		if (chefLock.tryLock()) {
			try {
				isChefRunning = true;
				CommandLine cmdLine = new CommandLine("knife")
						.addArgument("ec2").addArgument("server")
						.addArgument("create").addArgument("-I")
						.addArgument("ami-3d4ff254").addArgument("-x")
						.addArgument("ubuntu").addArgument("-r")
						.addArgument("role[base],role[testapp]")
						.addArgument("--flavor").addArgument("m1.small");

				DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();

				ExecuteWatchdog watchdog = new ExecuteWatchdog(7*60*1000);
				Executor executor = new DefaultExecutor();
				executor.setExitValue(1);
				executor.setWatchdog(watchdog);
				executor.setWorkingDirectory(new File(WORKING_DIRECTORY));
				try {
					executor.execute(cmdLine, resultHandler);
				} catch (ExecuteException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}

				try {
					resultHandler.waitFor();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				return resultHandler.getExitValue();
			} finally {
				isChefRunning = false;
				chefLock.unlock();
			}
		} else {
			return RETURN_CODE_CHEF_ALREADY_RUNNING;
		}
	}

	public static int terminateInstance() {
		if (chefLock.tryLock()) {
			try {
				isChefRunning = true;
				List<NodeInfo> nodes = getNodes();
				if (nodes.size() > 1) {
					return terminateInstanceImpl(findOldestInstance(nodes));
				} else {
					LOGGER.info("Not terminating an instance as there are less than two.");
					return 0;
				}
			} finally {
				isChefRunning = false;
				chefLock.unlock();
			}
		} else {
			return RETURN_CODE_CHEF_ALREADY_RUNNING;
		}
	}

	public static boolean isChefRunning() {
		return isChefRunning;
	}

	private static int terminateInstanceImpl(String instanceToTerminate) {
		// knife ec2 server delete i-0d4c1772 -P
		CommandLine cmdLine = new CommandLine("knife").addArgument("ec2")
				.addArgument("server").addArgument("delete")
				.addArgument(instanceToTerminate).addArgument("--purge")
				.addArgument("--yes");

		DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();

		ExecuteWatchdog watchdog = new ExecuteWatchdog(30 * 1000);
		Executor executor = new DefaultExecutor();
		executor.setExitValue(1);
		executor.setWatchdog(watchdog);
		executor.setWorkingDirectory(new File(WORKING_DIRECTORY));
		try {
			executor.execute(cmdLine, resultHandler);
		} catch (ExecuteException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			resultHandler.waitFor();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return resultHandler.getExitValue();

	}

	/**
	 * Returns instance id of the instance with the largest uptime value.
	 * 
	 * @param nodes
	 * @return
	 */
	private static String findOldestInstance(List<NodeInfo> nodes) {
		// knife search node role:testapp -F json
		if (nodes == null) {
			nodes = getNodes();
		}
		Collections.sort(nodes, new Comparator<NodeInfo>() {
			@Override
			public int compare(NodeInfo o1, NodeInfo o2) {
				// n.b. inverted conversion for descending order
				return o2.uptimeSeconds.compareTo(o1.uptimeSeconds);
			}
		});

		if (nodes.size() > 0) {
			return nodes.get(0).instanceId;
		} else {
			return null;
		}
	}

	private static List<NodeInfo> getNodes() {
		CommandLine cmdLine = new CommandLine("knife").addArgument("search")
				.addArgument("node").addArgument("role:testapp")
				.addArgument("-F").addArgument("json");

		DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();

		ExecuteWatchdog watchdog = new ExecuteWatchdog(30 * 1000);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ByteArrayOutputStream err = new ByteArrayOutputStream();
		ExecuteStreamHandler streamHandler = new PumpStreamHandler(out, err);
		Executor executor = new DefaultExecutor();
		executor.setStreamHandler(streamHandler);
		executor.setExitValue(1);
		executor.setWatchdog(watchdog);
		executor.setWorkingDirectory(new File(WORKING_DIRECTORY));

		try {
			executor.execute(cmdLine, resultHandler);
		} catch (ExecuteException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			resultHandler.waitFor();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		List<NodeInfo> nodes = new ArrayList<NodeInfo>();
		JsonElement root = null;
		try {
			root = new JsonParser().parse(out.toString("UTF-8"));
		} catch (Exception e) {
			e.printStackTrace();
			LOGGER.error("Error getting node info from Chef. "
					+ "Returning empty node info.", e);
			return nodes;
		}
		JsonArray rows = root.getAsJsonObject().getAsJsonArray("rows");
		for (JsonElement row : rows) {
			NodeInfo node = new NodeInfo();
			node.uptimeSeconds = row.getAsJsonObject()
					.getAsJsonObject("automatic")
					.getAsJsonPrimitive("uptime_seconds").getAsInt();
			node.instanceId = row.getAsJsonObject()
					.getAsJsonObject("automatic").getAsJsonObject("ec2")
					.getAsJsonPrimitive("instance_id").getAsString();
			nodes.add(node);
		}
		return nodes;
	}

	private static class NodeInfo {
		public String instanceId;
		public Integer uptimeSeconds;
	}
}
