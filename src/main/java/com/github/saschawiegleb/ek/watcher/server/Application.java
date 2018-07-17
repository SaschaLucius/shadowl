package com.github.saschawiegleb.ek.watcher.server;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.webapp.Configuration;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

import com.github.saschawiegleb.ek.watcher.Config;
import com.github.saschawiegleb.ek.watcher.lucene.AdSearch;
import com.github.saschawiegleb.ek.watcher.rest.AdController;
import com.github.saschawiegleb.ek.watcher.sql.AdStorage;

public class Application extends ResourceConfig {
	private static final Logger logger = LogManager.getLogger(Application.class);

	private int port;

	private Server server;

	public Application(int port) {
		register(JacksonFeature.class);
		this.port = port;
	}

	public static void main(String[] args) throws Exception {
		Configurator.setRootLevel(Level.ALL);
		Config.reconfigure("ek.conf");

		AdStorage adStorage = new AdStorage();
		AdSearch adSearch = new AdSearch();

		AdStorage.loadDriver();
		adStorage.createTables();
		ScheduledExecutorService service = Executors.newScheduledThreadPool(1);

		final Runnable refresh = new EkWatcher(service, adSearch, adStorage);
		service.schedule(refresh, 0, TimeUnit.SECONDS);

		Application application = new Application(Config.PORT.getInt());
		application.start();
		application.waitForInterrupt();
	}

	private void start() throws Exception {
		server = new Server();

		// Define ServerConnector
		ServerConnector connector = new ServerConnector(server);
		connector.setPort(port);
		server.addConnector(connector);

		// Add annotation scanning (for WebAppContexts)
		Configuration.ClassList classlist = Configuration.ClassList.setServerDefault(server);
		classlist.addBefore("org.eclipse.jetty.webapp.JettyWebXmlConfiguration",
				"org.eclipse.jetty.annotations.AnnotationConfiguration");

		// Create Servlet context
		ServletContextHandler servletContextHandler = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);// SESSIONS);
		servletContextHandler.setContextPath("/");

		// REST Servlet
		ServletHolder restHolder = new ServletHolder("default", ServletContainer.class);
		restHolder.setInitParameter("jersey.config.server.provider.classnames", AdController.class.getCanonicalName()
				+ ";" + "com.github.saschawiegleb.ek.watcher.rest.util.GsonJerseyProvider");

		servletContextHandler.addServlet(restHolder, "/*");

		server.setHandler(servletContextHandler);

		// Start Server
		server.start();

		// Show server state
		logger.debug(server.dump());
	}

	/**
	 * Cause server to keep running until it receives a Interrupt.
	 * <p>
	 * Interrupt Signal, or SIGINT (Unix Signal), is typically seen as a result of a
	 * kill -TERM {pid} or Ctrl+C
	 * 
	 * @throws InterruptedException if interrupted
	 */
	private void waitForInterrupt() throws InterruptedException {
		server.join();
	}
}
