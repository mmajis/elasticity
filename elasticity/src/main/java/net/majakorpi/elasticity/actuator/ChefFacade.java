package net.majakorpi.elasticity.actuator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;

public class ChefFacade {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(ChefFacade.class);
	
	private volatile boolean isChefRunning = false;

	private final Lock chefLock = new ReentrantLock();

	public static final int RETURN_CODE_CHEF_ALREADY_RUNNING = 500;

	private ChefService chefService;
	
	public ChefFacade() {
		super();
	}
	
	@Autowired
	public void setChefService(ChefService chefService) {
		this.chefService = chefService;
	}
	
	@Async
	public Future<ChefResult> provisionNewInstance(int amount) {
		// knife ec2 server create -I ami-3d4ff254 -x ubuntu -r
		// "role[base],role[testapp]" --flavor m1.small
		if (chefLock.tryLock()) {
			try {
				isChefRunning = true;
				boolean failures = false;
				List<Future<DefaultExecuteResultHandler>> futures = 
						new ArrayList<Future<DefaultExecuteResultHandler>>();
				for (int i = 0; i < amount; ++i) {
					futures.add(chefService.newInstanceImpl());
				}
				for (Future<DefaultExecuteResultHandler> future : futures) {
					try {
						if (future.get().getExitValue() != 0) {
							failures = true;
						}
					} catch (InterruptedException e) {
						e.printStackTrace();
					} catch (ExecutionException e) {
						e.printStackTrace();
						failures = true;
					}
				}
				return new AsyncResult<ChefResult>(new ChefResult(
						failures ? 666 : 0));
			} finally {
				isChefRunning = false;
				chefLock.unlock();
			}
		} else {
			LOGGER.info("Chef already running! Skipping this provisionNewInstance request!");
			return new AsyncResult<ChefResult>(new ChefResult(
					RETURN_CODE_CHEF_ALREADY_RUNNING));
		}
	}

	@Async
	public Future<ChefResult> terminateInstance(int amount) {
		if (chefLock.tryLock()) {
			try {
				isChefRunning = true;
				List<NodeInfo> nodes = chefService.getNodes();
				if (nodes.size() > 1) {
					int amountOKToTerminate = amount;
					while (nodes.size() - amountOKToTerminate < 1) {
						--amountOKToTerminate;
					}
					LOGGER.info("Terminating " + amountOKToTerminate + " instances");
					boolean failures = false;
					List<Future<DefaultExecuteResultHandler>> futures = 
							new ArrayList<Future<DefaultExecuteResultHandler>>();
					for (int i = 0; i < amountOKToTerminate; ++i) {
						futures.add(chefService.terminateInstanceImpl(
								findAndRemoveOldestInstance(nodes)));
					}
					for (Future<DefaultExecuteResultHandler> future : futures) {
						try {
							if (future.get().getExitValue() != 0) {
								failures = true;
							}
						} catch (InterruptedException e) {
							e.printStackTrace();
						} catch (ExecutionException e) {
							e.printStackTrace();
							failures = true;
						}
					}

					return new AsyncResult<ChefResult>(
							new ChefResult(failures ? 666 : 0));
				} else {
					LOGGER.info("Not terminating an instance as there are less than two.");
					return new AsyncResult<ChefResult>(new ChefResult(0));
				}
			} finally {
				isChefRunning = false;
				chefLock.unlock();
			}
		} else {
			LOGGER.info("Chef already running! Skipping this terminate request!");
			return new AsyncResult<ChefResult>(new ChefResult(
					RETURN_CODE_CHEF_ALREADY_RUNNING));
		}
	}

	public boolean isChefRunning() {
		return isChefRunning;
	}

	/**
	 * Returns instance id of the instance with the largest uptime value.
	 * 
	 * @param nodes
	 * @return
	 */
	private String findAndRemoveOldestInstance(List<NodeInfo> nodes) {
		Collections.sort(nodes, new Comparator<NodeInfo>() {
			@Override
			public int compare(NodeInfo o1, NodeInfo o2) {
				// n.b. inverted conversion for descending order
				return o2.uptimeSeconds.compareTo(o1.uptimeSeconds);
			}
		});

		if (nodes.size() > 0) {
			return nodes.remove(0).instanceId;
		} else {
			return null;
		}
	}
}
