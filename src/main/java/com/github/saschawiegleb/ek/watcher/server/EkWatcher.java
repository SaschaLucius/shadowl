package com.github.saschawiegleb.ek.watcher.server;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.saschawiegleb.ek.entity.Ad;
import com.github.saschawiegleb.ek.watcher.Service;
import com.github.saschawiegleb.ek.watcher.SleepTimer;
import com.github.saschawiegleb.ek.watcher.lucene.AdSearch;
import com.github.saschawiegleb.ek.watcher.sql.AdStorage;

import javaslang.collection.List;

/**
 * core logic to watch incoming ads
 */
public class EkWatcher implements Runnable {
	private static final Logger logger = LogManager.getLogger(EkWatcher.class);

	private final AdSearch _search;
	private final ScheduledExecutorService _service;
	private final AdStorage _storage;

	public EkWatcher(ScheduledExecutorService service, AdSearch search, AdStorage storage) {
		_service = service;
		_search = search;
		_storage = storage;
	}

	private long latestAdId() {
		try (Connection conn = _storage.getConnection()) {
			return _storage.getLatestAdId(conn);
		} catch (SQLException e) {
			logger.error(e);
		}
		return 0;
	}

	@Override
	public void run() {
		final long timeStart = System.currentTimeMillis();
		long latestAdId = latestAdId();
		logger.info("Latest ad: " + latestAdId);
		List<Ad> adsLightweight = Service.getLatestAds(latestAdId);
		logger.info(String.format("Get %s Ads in %s seconds.", adsLightweight.length(),
				(System.currentTimeMillis() - timeStart) / 1000));

		storeAds(adsLightweight);
		_search.insertAdsLucene(adsLightweight);

		_service.schedule(this, (long) SleepTimer.resizeAndGet(adsLightweight.length()), TimeUnit.SECONDS);
	}

	private void storeAds(List<Ad> adsLightweight) {
		try (Connection conn = _storage.getConnection()) {
			conn.setAutoCommit(false);
			for (Ad ad : adsLightweight) {
				_storage.insertFullAd(ad, conn);
			}
		} catch (SQLException e) {
			logger.error(e);
		}
	}
}
