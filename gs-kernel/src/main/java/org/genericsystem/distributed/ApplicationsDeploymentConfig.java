package org.genericsystem.distributed;

import io.vertx.core.json.JsonObject;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.genericsystem.distributed.EnginesDeploymentConfig.EngineDeploymentConfig;
import org.genericsystem.distributed.ui.components.HtmlApp;
import org.genericsystem.kernel.Statics;

/**
 * @author Nicolas Feybesse
 *
 */
public class ApplicationsDeploymentConfig extends JsonObject {

	protected ApplicationsDeploymentConfig() {
		this(Statics.DEFAULT_HOST, Statics.DEFAULT_PORT);
	}

	public ApplicationsDeploymentConfig(String host, int port) {
		put("apps", new JsonObject());
		put("host", host);
		put("port", port);
	}

	public String getHost() {
		return getString("host");
	}

	public int getPort() {
		return getInteger("port");
	}

	public Set<String> getApplicationsPaths() {
		return getJsonObject("apps").getMap().keySet();
	}

	public Class<? extends HtmlApp> getApplicationClass(String applicationPath) {
		return getApplicationDeploymentConfig(applicationPath).getApplicationClass();
	}

	private ApplicationDeploymentConfig getApplicationDeploymentConfig(String applicationPath) {
		return getApplicationDeploymentConfig(getJsonObject("apps").getJsonObject(applicationPath));
	}

	private ApplicationDeploymentConfig getApplicationDeploymentConfig(JsonObject json) {
		return new ApplicationDeploymentConfig(json.getMap());
	}

	public void addApplication(String path, Class<? extends HtmlApp> clazz, String persistentDirectoryPath, Class<?>... classes) {
		getJsonObject("apps").put(path, new ApplicationDeploymentConfig(clazz, persistentDirectoryPath, classes));
	}

	public void removeApplication(String path) {
		getJsonObject("apps").remove(path);
	}

	public Set<Class<?>> getClasses(String persistentDirectoryPath) {
		return getJsonObject("apps").getMap().values().stream().map(json -> getApplicationDeploymentConfig((JsonObject) json)).filter(conf -> persistentDirectoryPath.equals(conf.getPersistentDirectoryPath())).flatMap(conf -> conf.getClasses().stream())
				.collect(Collectors.toSet());
	}

	public Set<String> getPersistentDirectoryPaths() {
		return getJsonObject("apps").getMap().keySet().stream().map(this::getPersistentDirectoryPath).collect(Collectors.toSet());
	}

	public String getPersistentDirectoryPath(String applicationPath) {
		return getApplicationDeploymentConfig(applicationPath).getPersistentDirectoryPath();
	}

	public static class ApplicationDeploymentConfig extends EngineDeploymentConfig {
		public ApplicationDeploymentConfig(Map<String, Object> map) {
			super(map);
		}

		public ApplicationDeploymentConfig(Class<? extends HtmlApp> applicationClass, String repositoryPath, Class<?>... classes) {
			super(repositoryPath, classes);
			put("applicationClass", applicationClass.getName());
		}

		@SuppressWarnings("unchecked")
		public Class<? extends HtmlApp> getApplicationClass() {
			try {
				return (Class<? extends HtmlApp>) Class.forName(getString("applicationClass)"));
			} catch (ClassNotFoundException e) {
				throw new IllegalStateException(e);
			}
		}
	}

	public static class DefaultPathSingleWebAppDeployment extends ApplicationsDeploymentConfig {
		public DefaultPathSingleWebAppDeployment(Class<? extends HtmlApp> htmlApp, Class<?>... classes) {
			addApplication("/", htmlApp, null, classes);
		}

		public DefaultPathSingleWebAppDeployment(Class<? extends HtmlApp> htmlApp, String persistentDirectoryPath, Class<?>... classes) {
			addApplication("/", htmlApp, persistentDirectoryPath, classes);
		}
	}

}