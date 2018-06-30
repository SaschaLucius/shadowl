package com.github.saschawiegleb.ek.watcher;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public enum Config {

	CATEGORY("0"),

	/**
	 * jdbc:hsqldb:file:./data/adDb;sql.syntax_mys=true
	 * jdbc:hsqldb:mem:mymemdb;sql.syntax_mys=true jdbc:mysql://localhost:3306/ebay
	 */
	CONNECTION("jdbc:hsqldb:mem:mymemdb;sql.syntax_mys=true;user=SA;password="),

	EXPECTEDAMOUNT("5"),

	INDEXFOLDER("./data/"),

	/**
	 * initial interval to check the ads
	 */
	INTERVAL("90"),

	/**
	 * just -5 EXPECTEDAMOUNT +5 will effect the INTERVAL
	 */
	MAXDEVIATION("5"),

	PORT("8080"),

	/**
	 * amount of ads for search
	 */
	RESULTS("50"),

	/**
	 * step to decrease or increase the INTERVAL
	 */
	TIMESTEP("0.2");

	private static final Logger logger = LogManager.getLogger(Config.class);

	static Properties props = new Properties();

	private final String _defaultValue;

	private Config(String defaultValue) {
		_defaultValue = defaultValue;
	}

	public static void reconfigure(String filemane) {
		InputStream in = Config.class.getClassLoader().getResourceAsStream(filemane);
		props = new Properties();
		try {
			props.load(in);
		} catch (IOException e) {
			logger.error(e);
		}
	}

	public String get() {
		return getOrDefault();
	}

	public boolean getBoolean() {
		return Boolean.parseBoolean(getOrDefault());
	}

	public float getFloat() {
		return Float.parseFloat(getOrDefault());
	}

	public int getInt() {
		return Integer.parseInt(getOrDefault());
	}

	private String getOrDefault() {
		return props.getProperty(this.name().toLowerCase(), _defaultValue);
	}
}
