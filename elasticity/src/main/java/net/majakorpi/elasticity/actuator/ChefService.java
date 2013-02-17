package net.majakorpi.elasticity.actuator;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteStreamHandler;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.PumpStreamHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class ChefService {
	
	private static final Logger LOGGER = LoggerFactory
			.getLogger(ChefService.class);

	public static final String WORKING_DIRECTORY = "/Users/mika/chef/chef-repo";

	public ChefService() {
		super();
	}
	
	@Async
	public Future<DefaultExecuteResultHandler> newInstanceImpl() {
		CommandLine cmdLine = new CommandLine("knife")
				.addArgument("ec2").addArgument("server")
				.addArgument("create").addArgument("-I")
				.addArgument("ami-990b81f0").addArgument("-x")
				.addArgument("ubuntu").addArgument("-r")
				.addArgument("role[base],role[testapp]")
				.addArgument("--flavor").addArgument("m1.small");

		DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();

		ExecuteWatchdog watchdog = new ExecuteWatchdog(
				7 * 60 * 1000);
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
		LOGGER.info("Chef provisionNewInstance result: "
				+ resultHandler.getExitValue());
		return new AsyncResult<DefaultExecuteResultHandler>(resultHandler);
	}
	
	@Async
	public Future<DefaultExecuteResultHandler> terminateInstanceImpl(
			String instanceToTerminate) {
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
		LOGGER.info("Chef terminateInstanceImpl result: "
				+ resultHandler.getExitValue());
		return new AsyncResult<DefaultExecuteResultHandler>(resultHandler);
	}	
	
	public List<NodeInfo> getNodes() {
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
}
