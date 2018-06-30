package com.github.saschawiegleb.ek.watcher.server;

import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.github.saschawiegleb.ek.entity.Ad;
import com.github.saschawiegleb.ek.watcher.Service;
import com.github.saschawiegleb.ek.watcher.SleepTimer;

import javaslang.collection.List;

/**
 * core logic to watch incoming ads
 */
public class EkWatcherCallable implements Callable<List<Ad>> {
	private static final Logger logger = Logger.getLogger(EkWatcherCallable.class.getName());

	private final long _latestAdId;
	private final ScheduledExecutorService _service;

	public EkWatcherCallable(ScheduledExecutorService service, long latestAdId) {
		_service = service;
		_latestAdId = latestAdId;
	}

	@Override
	public List<Ad> call() throws Exception {
		final long timeStart = System.currentTimeMillis();
		List<Ad> adsLightweight = Service.getLatestAds(_latestAdId);
		logger.info("Get Ads: " + (System.currentTimeMillis() - timeStart) / 1000 + " Sekunden");

		// schedule again
		_service.schedule(this, (long) SleepTimer.resizeAndGet(adsLightweight.length()), TimeUnit.SECONDS);

		return adsLightweight;
	}
}
