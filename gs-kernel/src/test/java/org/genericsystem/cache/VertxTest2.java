package org.genericsystem.cache;

import java.io.File;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.genericsystem.api.core.exceptions.ConcurrencyControlException;
import org.genericsystem.api.core.exceptions.OptimisticLockConstraintViolationException;
import org.genericsystem.kernel.Generic;
import org.genericsystem.kernel.Statics;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test
public class VertxTest2 extends AbstractClassicTest {

	String ServerVerticleId;
	private final String directoryPath = System.getenv("HOME") + "/test/Vertx_tests/snapshot_save";

	private void cleanDirectory(String directoryPath) {
		File file = new File(directoryPath);
		if (file.exists())
			for (File f : file.listFiles())
				f.delete();
	}

	@Override
	@BeforeClass
	public void beforeClass() {
		System.out.println("before class");
		cleanDirectory(directoryPath);
		BlockingQueue<String> queue = new ArrayBlockingQueue<>(1);
		GSVertx.vertx().getVertx().deployVerticle(HttpGSServer.class.getName(), new GSDeploymentOptions().addEngine(Statics.ENGINE_VALUE, directoryPath), result -> {
			try {
				queue.put(result.result());
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		});
		try {
			ServerVerticleId = queue.take();
		} catch (InterruptedException e) {
			e.printStackTrace();
			return;
		}
		System.out.println("beforeClass ok");
	}

	public void test_001() {
		ClientEngine engine = new ClientEngine();
	}

	public void test_002() {
		ClientEngine engine = new ClientEngine();
		Generic myVehicle = engine.addInstance("Vehicle");
		engine.getCurrentCache().flush();
		ClientEngine secondEngine = new ClientEngine();
		secondEngine.newCache().start();
		assert myVehicle.isAlive();
		assert secondEngine.getInstance("Vehicle") != null;
	}

	public void testPersistanceOK() {
		ClientEngine engine = new ClientEngine(Statics.ENGINE_VALUE);
		Generic myVehicle = engine.addInstance("Vehicle2");
		engine.getCurrentCache().flush();
		engine.close();
		afterClass();
		beforeClass();
		ClientEngine secondEngine = new ClientEngine(Statics.ENGINE_VALUE);
		assert secondEngine.getInstance("Vehicle2") != null;
	}

	public void testConcurrencyControlException() {
		ClientEngine engine = new ClientEngine();
		ClientCache cache = engine.getCurrentCache().start();
		final Generic car = engine.addInstance("Car");
		cache.flush();
		ClientEngine engine2 = new ClientEngine();
		ClientCache cache2 = engine2.newCache().start();
		Generic car2 = engine2.getInstance("Car");
		engine.getCurrentCache().start();
		car.remove();
		assert !engine.getCurrentCache().isAlive(car);
		assert !engine.getInstances().contains(car);
		engine2.getCurrentCache().start();
		assert engine2.getCurrentCache().isAlive(car2);
		assert engine2.getInstances().contains(car2);
		engine.getCurrentCache().start();
		try {
			engine.getCurrentCache().tryFlush();
			throw new IllegalStateException();
		} catch (ConcurrencyControlException ex) {
			// ignore
		}
	}

	public void testConcurentRemoveKO() {
		ClientEngine engine = new ClientEngine();
		ClientCache cache = engine.getCurrentCache().start();
		final Generic car = engine.addInstance("Car");
		cache.flush();
		ClientEngine engine2 = new ClientEngine();
		ClientCache cache2 = engine2.newCache().start();
		Generic car2 = engine2.getInstance("Car");
		engine.getCurrentCache().start();
		car.remove();
		engine.getCurrentCache().flush();
		engine2.getCurrentCache().start();
		catchAndCheckCause(() -> {
			car2.remove();
			engine2.getCurrentCache().flush();
		}, OptimisticLockConstraintViolationException.class);
	}

	@Override
	@AfterClass
	public void afterClass() {
		BlockingQueue<Integer> queue = new ArrayBlockingQueue<>(1);
		GSVertx.vertx().getVertx().undeploy(ServerVerticleId, result -> {
			try {
				queue.put(0);
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		});
		try {
			queue.take();
		} catch (InterruptedException e) {
			e.printStackTrace();
			return;
		}
		System.out.println("afterClass ok");
	}

}
