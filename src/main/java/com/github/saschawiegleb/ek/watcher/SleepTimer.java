package com.github.saschawiegleb.ek.watcher;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * timer to wait between parsing next ads
 */
public class SleepTimer {
	private static final Logger logger = LogManager.getLogger(SleepTimer.class);

	private static double SLEEP_TIMER = Config.INTERVAL.getFloat();

	public static double resizeAndGet(int count) {
		int diff = 0;
		if (count <= Config.EXPECTEDAMOUNT.getInt()) {
			diff = Math.min(Config.EXPECTEDAMOUNT.getInt() - count, Config.MAXDEVIATION.getInt());
		} else {
			diff = Math.max(Config.EXPECTEDAMOUNT.getInt() - count, -Config.MAXDEVIATION.getInt());
		}

		SLEEP_TIMER += Config.TIMESTEP.getFloat() * diff;
		if (SLEEP_TIMER < 0) {
			SLEEP_TIMER = 0;
		}
		logger.debug("New SleepTimer is: " + SLEEP_TIMER);
		return SLEEP_TIMER;
	}
}
