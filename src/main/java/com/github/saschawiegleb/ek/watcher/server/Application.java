package com.github.saschawiegleb.ek.watcher.server;

import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.tomcat.util.scan.StandardJarScanner;
import org.eclipse.jetty.apache.jsp.JettyJasperInitializer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
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

	// Resource path pointing to where the WEBROOT is
	private static final String WEBROOT_INDEX = "/webroot/";

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

//	/**
//	 * Setup JSP Support for ServletContextHandlers.
//	 * <p>
//	 * NOTE: This is not required or appropriate if using a WebAppContext.
//	 * </p>
//	 *
//	 * @param servletContextHandler
//	 *            the ServletContextHandler to configure
//	 * @throws IOException
//	 *             if unable to configure
//	 */
//	private void enableEmbeddedJspSupport(ServletContextHandler servletContextHandler) throws IOException {
//		// Establish Scratch directory for the servlet context (used by JSP
//		// compilation)
//		File tempDir = new File(System.getProperty("java.io.tmpdir"));
//		File scratchDir = new File(tempDir.toString(), "ek-watcher-jsp");
//
//		if (!scratchDir.exists()) {
//			if (!scratchDir.mkdirs()) {
//				throw new IOException("Unable to create scratch directory: " + scratchDir);
//			}
//		}
//		servletContextHandler.setAttribute("javax.servlet.context.tempdir", scratchDir);
//
//		// Set Classloader of Context to be sane (needed for JSTL)
//		// JSP requires a non-System classloader, this simply wraps the
//		// embedded System classloader in a way that makes it suitable
//		// for JSP to use
//		ClassLoader jspClassLoader = new URLClassLoader(new URL[0], this.getClass().getClassLoader());
//		servletContextHandler.setClassLoader(jspClassLoader);
//
//		// Manually call JettyJasperInitializer on context startup
//		servletContextHandler.addBean(new JspStarter(servletContextHandler));
//
//		// Create / Register JSP Servlet (must be named "jsp" per spec)
//		ServletHolder holderJsp = new ServletHolder("jsp", JettyJspServlet.class);
//		holderJsp.setInitOrder(0);
//		holderJsp.setInitParameter("logVerbosityLevel", "DEBUG");
//		holderJsp.setInitParameter("fork", "false");
//		holderJsp.setInitParameter("xpoweredBy", "false");
//		holderJsp.setInitParameter("compilerTargetVM", "1.8");
//		holderJsp.setInitParameter("compilerSourceVM", "1.8");
//		holderJsp.setInitParameter("keepgenerated", "true");
//		servletContextHandler.addServlet(holderJsp, "*.jsp");
//	}

	private URI getWebRootResourceUri() throws FileNotFoundException, URISyntaxException {
		URL indexUri = this.getClass().getResource(WEBROOT_INDEX);
		if (indexUri == null) {
			throw new FileNotFoundException("Unable to find resource " + WEBROOT_INDEX);
		}
		// Points to wherever /webroot/ (the resource) is
		return indexUri.toURI();
	}

	public void start() throws Exception {
		server = new Server();

		// Define ServerConnector
		ServerConnector connector = new ServerConnector(server);
		connector.setPort(port);
		server.addConnector(connector);

		// Add annotation scanning (for WebAppContexts)
		Configuration.ClassList classlist = Configuration.ClassList.setServerDefault(server);
		classlist.addBefore("org.eclipse.jetty.webapp.JettyWebXmlConfiguration",
				"org.eclipse.jetty.annotations.AnnotationConfiguration");

		// Base URI for servlet context
		URI baseUri = getWebRootResourceUri();
		logger.info("Base URI: " + baseUri);

		// Create Servlet context
		ServletContextHandler servletContextHandler = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);// SESSIONS);
		servletContextHandler.setContextPath("/");
		// servletContextHandler.setResourceBase(baseUri.toASCIIString());

		// Since this is a ServletContextHandler we must manually configure JSP
		// support.
//		enableEmbeddedJspSupport(servletContextHandler);

		// Default Servlet (always last, always named "default")
		// ServletHolder holderDefault = new ServletHolder("default",
		// DefaultServlet.class);
		// holderDefault.setInitParameter("resourceBase",
		// baseUri.toASCIIString());
		// holderDefault.setInitParameter("dirAllowed", "true");
		// servletContextHandler.addServlet(holderDefault, "/");

		// REST Servlet
		ServletHolder restHolder = new ServletHolder("default", ServletContainer.class);
//		restHolder.setInitParameter("resourceBase", baseUri.toASCIIString());
//		restHolder.setInitParameter("dirAllowed", "true");
//		restHolder.setInitOrder(0);
		restHolder.setInitParameter("jersey.config.server.provider.classnames", AdController.class.getCanonicalName()
				+ ";" + "com.github.saschawiegleb.ek.watcher.rest.util.GsonJerseyProvider");

		servletContextHandler.addServlet(restHolder, "/*");

		server.setHandler(servletContextHandler);

		// Start Server
		server.start();

		// Show server state
//		if (logger.isLoggable(Level.FINE)) {
//			LOG.fine(server.dump());
//		}
	}

	/**
	 * Cause server to keep running until it receives a Interrupt.
	 * <p>
	 * Interrupt Signal, or SIGINT (Unix Signal), is typically seen as a result of a
	 * kill -TERM {pid} or Ctrl+C
	 * 
	 * @throws InterruptedException if interrupted
	 */
	public void waitForInterrupt() throws InterruptedException {
		server.join();
	}

	/**
	 * JspStarter for embedded ServletContextHandlers
	 * 
	 * This is added as a bean that is a jetty LifeCycle on the
	 * ServletContextHandler. This bean's doStart method will be called as the
	 * ServletContextHandler starts, and will call the ServletContainerInitializer
	 * for the jsp engine.
	 *
	 */
	public static class JspStarter extends AbstractLifeCycle
			implements ServletContextHandler.ServletContainerInitializerCaller {
		ServletContextHandler context;
		JettyJasperInitializer sci;

		public JspStarter(ServletContextHandler context) {
			this.sci = new JettyJasperInitializer();
			this.context = context;
			this.context.setAttribute("org.apache.tomcat.JarScanner", new StandardJarScanner());
		}

		@Override
		protected void doStart() throws Exception {
			ClassLoader old = Thread.currentThread().getContextClassLoader();
			Thread.currentThread().setContextClassLoader(context.getClassLoader());
			try {
				sci.onStartup(null, context.getServletContext());
				super.doStart();
			} finally {
				Thread.currentThread().setContextClassLoader(old);
			}
		}
	}
}
