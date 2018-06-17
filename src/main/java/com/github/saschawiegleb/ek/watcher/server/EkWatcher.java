package com.github.saschawiegleb.ek.watcher.server;

import java.sql.Connection;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.github.saschawiegleb.ek.entity.Ad;
import com.github.saschawiegleb.ek.watcher.Service;
import com.github.saschawiegleb.ek.watcher.SleepTimer;
import com.github.saschawiegleb.ek.watcher.lucene.AdSearch;
import com.github.saschawiegleb.ek.watcher.sql.AdStorage;

import javaslang.collection.List;

public class EkWatcher implements Runnable {
	private static final Logger logger = Logger.getLogger(EkWatcher.class.getName());

	ScheduledExecutorService _service = null;

	public EkWatcher(ScheduledExecutorService service) {
		_service = service;
	}

	// public static void main(String[] args) {
	// AdStorage.loadDriver();
	// AdStorage.createTables();
	// ScheduledExecutorService service = Executors.newScheduledThreadPool(1);
	// final Runnable refresh = new EkWatcher(service);
	// service.schedule(refresh, 0, TimeUnit.SECONDS);
	// }

	@Override
	public void run() {
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
		logger.info("Start: " + sdf.format(cal.getTime()));

		Ad currentAd = null; // for error handling

		long latestAdId = 0;
		try (Connection conn = AdStorage.getConnection(AdStorage.defaultConnectionType)) {
			latestAdId = AdStorage.getLatestAdId(conn);
		} catch (SQLException e) {
			e.printStackTrace();
		}

		final long timeStart = System.currentTimeMillis();
		List<Ad> adsLightweight = Service.getLatestAds(latestAdId);
		System.out.println(
				"Get Ads: " + (System.currentTimeMillis() - timeStart) / 1000 + " Sekunden");

		try (Connection conn = AdStorage.getConnection(AdStorage.defaultConnectionType)) {
			conn.setAutoCommit(false);
			for (Ad ad : adsLightweight) {
				currentAd = ad;
				AdStorage.insertFullAd(ad, conn);
			}
		} catch (SQLException e) {
			System.err.println(currentAd);
			e.printStackTrace();
		}

		AdSearch.insertAdsLucene(adsLightweight);
		logger.info("End: " + sdf.format(cal.getTime()));
		SleepTimer.resizeAndSleep(adsLightweight.length());

		// schedule again
		_service.schedule(this, (long) SleepTimer.getTimer(), TimeUnit.SECONDS);
	}
}
