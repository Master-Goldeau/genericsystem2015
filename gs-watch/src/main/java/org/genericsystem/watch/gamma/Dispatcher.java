package org.genericsystem.watch.gamma;

import org.genericsystem.common.Generic;
import org.genericsystem.kernel.Cache;
import org.genericsystem.kernel.Engine;
import org.genericsystem.watch.beta.Model.Task;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;

public class Dispatcher extends AbstractVerticle {

	protected final Engine engine = new Engine(System.getenv("HOME") + "/genericsystem/tasks/", Task.class);
	protected Cache cache = engine.newCache();
	protected final Generic taskType = engine.find(Task.class);

	public static final String OK = "OK";
	public static final String KO = "KO";

	public static final String ADDRESS = "org.genericsystem.repartitor";
	protected static final String TASK = "task";
	protected static final String NEW_STATE = "newState";
	protected static final String STATE = "state";
	protected static final String TODO = "todo";
	protected static final String RUNNING = "running";
	protected static final String FINISHED = "finished";
	protected static final String ABORTED = "aborted";
	private static final long MESSAGE_SEND_PERIODICITY = 5000;
	public static final long TIMEOUT = 2000;

	public static void main(String[] args) {
		ClusterManager mgr = new HazelcastClusterManager();

		VertxOptions vertxOptions = new VertxOptions().setClustered(true).setClusterManager(mgr);
		vertxOptions.setClusterHost(LocalNet.getIpAddress());
		vertxOptions.setMaxWorkerExecuteTime(Long.MAX_VALUE);
		Vertx.clusteredVertx(vertxOptions, res -> {
			if (res.failed())
				throw new IllegalStateException(res.cause());
			Vertx vertx = res.result();
			vertx.deployVerticle(new Dispatcher(), res_ -> {
				if (res_.failed())
					throw new IllegalStateException(res_.cause());
			});
		});
	}

	@Override
	public void start(Future<Void> startFuture) throws Exception {
		cache.safeConsum(nothing -> {
			for (Generic task : taskType.getInstances()) {
				JsonObject json = new JsonObject((String) task.getValue());
				if (RUNNING.equals(json.getString(STATE)))
					updateTaskState(json, TODO);
			}
		});
		watchMail();
		vertx.deployVerticle(new HttpServerVerticle(), ar -> {
			if (ar.failed())
				throw new IllegalStateException("Unable to create HTTP server.", ar.cause());
			else
				System.out.println("HTTP server started.");
		});
		vertx.eventBus().consumer(ADDRESS + ":watchMail", message -> {
			System.out.println("Restarting mail watcher thread…");
			watchMail();
		});
		vertx.eventBus().consumer(ADDRESS + ":updateState", message -> {
			JsonObject json = new JsonObject((String) message.body());
			updateTaskState(json.getJsonObject(TASK), json.getString(NEW_STATE));
		});
		vertx.eventBus().consumer(ADDRESS + ":add", message -> {
			cache.safeConsum(unused -> {
				taskType.addInstance((String) message.body());
				cache.flush();
			});
		});
		vertx.setPeriodic(MESSAGE_SEND_PERIODICITY, h -> {
			cache.safeConsum(unused -> {
				for (Generic task : taskType.getInstances()) {
					JsonObject json = new JsonObject((String) task.getValue());
					if (TODO.equals(json.getString(STATE))) {
						vertx.eventBus().send(json.getString(DistributedVerticle.TYPE), new JsonObject(json.encode()).put(STATE, RUNNING).encodePrettily(), new DeliveryOptions().setSendTimeout(TIMEOUT), reply -> {
							if (reply.failed()) {
								System.out.println("Failed: " + reply.cause());
							} else if (OK.equals(reply.result().body()))
								updateTaskState(json, RUNNING);
						});
					}
				}
				cache.flush();
			});
		});
		startFuture.complete();
	}

	private void updateTaskState(JsonObject oldValue, String newState) {
		System.out.println("Updating: " + oldValue.encodePrettily() + ", newState:" + newState);
		cache.safeConsum(unused -> {
			Generic task = taskType.getInstances().filter(g -> oldValue.equals(new JsonObject((String) g.getValue()))).first();
			JsonObject newValue = new JsonObject(oldValue.encode()).put(STATE, newState);
			task.update(newValue.encodePrettily());
			cache.flush();
		});
	}

	private void watchMail() {
		vertx.fileSystem().readFile("src/main/conf/MailWatcherVerticle.json", ar -> {
			if (ar.failed())
				throw new IllegalStateException("Impossible to load configuration for MailWatcherVerticle.", ar.cause());
			else {
				JsonObject config = new JsonObject(ar.result());
				vertx.deployVerticle(new MailWatcherVerticle(), new DeploymentOptions().setConfig(config), res -> {
					if (res.failed())
						throw new IllegalStateException("Unable to deploy MailWatcherVerticle", res.cause());
				});				
			}
		});
	}
}
