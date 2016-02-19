package org.genericsystem.distributed;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javafx.event.ActionEvent;

import org.genericsystem.distributed.cacheonserver.ui.js.HtmlNode;
import org.genericsystem.distributed.cacheonserver.ui.js.todomvc.HtmlAdmin;
import org.genericsystem.distributed.cacheonserver.ui.js.todomvc.TodoList;
import org.genericsystem.kernel.AbstractServer;

public class WebSocketServer<T extends AbstractServer> {
	private List<HttpServer> httpServers = new ArrayList<>();
	private final int port;
	private final String host;
	private AbstractGSServer<T> server;

	public WebSocketServer(AbstractGSServer<T> server, GSDeploymentOptions options) {
		this.server = server;
		this.port = options.getPort();
		this.host = options.getHost();
	}

	// @SuppressWarnings("unchecked")
	public void start(Map<String, AbstractServer> roots) {

		System.out.println("start");
		Vertx vertx = GSVertx.vertx().getVertx();
		HttpServer httpServer = vertx.createHttpServer(new HttpServerOptions().setPort(port).setHost(host));
		httpServer.websocketHandler(webSocket -> {
			String path = webSocket.path();
			AbstractServer root = roots.get(path);
			if (root == null)
				throw new IllegalStateException("Unable to find database :" + path);
			webSocket.exceptionHandler(e -> {

				e.printStackTrace();
				throw new IllegalStateException(e);
			});

			TodoList todolist = new TodoList();
			HtmlNode parent = new HtmlNode(webSocket);
			HtmlAdmin jsAdmin = new HtmlAdmin(todolist, parent, webSocket);

			webSocket.handler(buffer -> {
				GSBuffer gsBuffer = new GSBuffer(buffer);
				String message = gsBuffer.getString(0, gsBuffer.length());
				JsonObject obj = new JsonObject(message);
				HtmlNode node = jsAdmin.getRootViewContext().getNodeById().get(obj.getString("nodeId"));
				if (node != null) {
					if (obj.getString("msg_type").equals("A"))
						node.getActionProperty().get().handle(new ActionEvent());

					if (obj.getString("msg_type").equals("U"))
						node.getText().setValue(obj.getString("textContent"));
				}
				// webSocket.writeBinaryMessage(server.getReplyBuffer(methodId, op, (T) root, gsBuffer));
				});
		});
		AbstractGSServer.<HttpServer> synchronizeTask(handler -> httpServer.listen(handler));
		httpServers.add(httpServer);
		System.out.println("Generic System server ready!");
	}

	public void stop(Map<String, AbstractServer> roots) {
		httpServers.forEach(httpServer -> AbstractGSServer.<Void> synchronizeTask(handler -> httpServer.close(handler)));
		roots.values().forEach(root -> root.close());
		roots = null;
		System.out.println("Generic System server stopped!");
	}

}
