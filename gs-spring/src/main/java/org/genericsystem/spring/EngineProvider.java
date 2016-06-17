package org.genericsystem.spring;

import java.util.Arrays;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

//@ApplicationScoped
@Scope("singleton")
@Configuration
@Component
@Lazy
public class EngineProvider {

	protected static Logger log = LoggerFactory.getLogger(EngineProvider.class);

	private transient Engine engine;

	@Lazy
	@Autowired
	private UserClassesProvider userClassesProvider;

	@Lazy
	@Autowired
	private PersistentDirectoryProvider persistentDirectoryProvider;

	@Lazy
	@Autowired
	private CacheRequestProvider cacheRequestProvider;

	@PostConstruct
	public void init() {
		String logo = "\n";
		logo += ("____________________________________________________________________________________________________________\n");
		logo += ("|___________________________________________________________________________________________________________|\n");
		logo += ("|___________________________________________________________________________________________________________|\n");
		logo += ("|____________|         ____                      _      ____             __                  /______________|\n");
		logo += ("|____________|        / ___)___  _  _____  ___  /_)__  / ___)_  __ ___  / /  ___  ____      /_______________|\n");
		logo += ("|____________|       / /___/ __)/ \\/ / __)/ _ )/ |/ _)/___ \\/ \\/  ) __)/___)/ __)/    )    /________________|\n");
		logo += ("|____________|      / /_  / __)/    / __)/   \\/  / /_ ___/ /\\    (__  / /_ / __)/ / / /   /_________________|\n");
		logo += ("|____________|      \\____(____(_/\\_(____(_/\\_(__(____(____/  \\  (____(____(____(_/_/_/   /__________________|\n");
		logo += ("|____________|                                               /_/                        /___________________|\n");
		logo += ("|____________|_________________________________________________________________________/____________________|\n");
		logo += ("|___________________________________________________________________________________________________________|\n");
		logo += ("|___________________________________________________________________________________________________________|  \n");

		log.info(logo);
		log.info("-----------------------------------------------------------------------------------------------");
		log.info("-  directory path : " + persistentDirectoryProvider.getDirectoryPath());
		log.info("-  userClasses : " + Arrays.toString(userClassesProvider.getUserClassesArray()));
		log.info("-----------------------------------------------------------------------------------------------");

		engine = new Engine(() -> cacheRequestProvider.getCurrentCache(), persistentDirectoryProvider.getDirectoryPath(),
				userClassesProvider.getUserClassesArray());
	}

	@Bean
	@Lazy
	public Engine getEngine() {
		return engine;
	}

	@PreDestroy
	public void destroy() {
		log.info("Generic System is currently stopping...");
		engine.close();
		engine = null;
		log.info("Generic System is stopped");
	}
}
