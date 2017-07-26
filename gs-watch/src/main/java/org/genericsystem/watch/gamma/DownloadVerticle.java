package org.genericsystem.watch.gamma;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.http.HttpClient;
import io.vertx.core.json.JsonObject;

public class DownloadVerticle extends ActionVerticle {

	public static final String ACTION = "download";

	public DownloadVerticle(String privateAddress, String ip) {
		super(privateAddress, ip);
	}

	@Override
	public String getAction() {
		return ACTION;
	}

	@Override
	protected void handle(Future<Object> future, JsonObject task) {
		String fileName = task.getString(DistributedVerticle.FILENAME);
		File file = new File(DistributedVerticle.BASE_PATH + fileName);
		if (!file.exists())
			download((Future) future, fileName, task.getString(DistributedVerticle.IP));
		else {
			System.out.println("File : " + fileName + " is already dowloaded");
			future.complete(fileName);	
		}
	}

	@Override
	protected void handleResult(AsyncResult<Object> result, JsonObject task) {
		if (result.failed()) {
			System.out.println(result.cause());
			throw new IllegalStateException(result.cause());
		} else {
			String fileName = task.getString(DistributedVerticle.FILENAME);
			System.out.println("Download successful " + fileName);
			addTask(DistributedVerticle.BASE_PATH + fileName, getIp(), PdfConverterVerticle.ACTION);
		}
	}

	private <T> void download(Future<String> future, String fileName, String ip) {
		BlockingQueue<byte[]> blockingQueue = new ArrayBlockingQueue<>(1);
		HttpClient httpClient = vertx.createHttpClient().getNow(8084, ip, fileName, resp -> resp.bodyHandler(body -> {
			try {
				blockingQueue.put(body.getBytes());
			} catch (InterruptedException e) {
				future.fail(e);
				return;
			}
		}));
		httpClient.close();
		byte[] bytes;
		try {
			bytes = blockingQueue.take();
		} catch (InterruptedException e) {
			e.printStackTrace();
			future.fail(e);
			return;
		}
		FileOutputStream fos;

		try {
			File file = new File(DistributedVerticle.BASE_PATH + fileName);
			new File(DistributedVerticle.BASE_PATH + fileName.substring(0, fileName.lastIndexOf("/"))).mkdirs();
			fos = new FileOutputStream(file);
			fos.write(bytes);
			fos.close();
			future.complete(file.toString());
		} catch (IOException e) {
			e.printStackTrace();
			future.fail(e);
			return;
		}
	}
}
