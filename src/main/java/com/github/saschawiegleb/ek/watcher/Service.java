package com.github.saschawiegleb.ek.watcher;

import com.github.saschawiegleb.ek.api.EkService;
import com.github.saschawiegleb.ek.entity.Ad;
import com.github.saschawiegleb.ek.entity.Category;

import javaslang.collection.List;

public class Service {
	private static final EkService service = new EkService();
	private static final Category category = service.getCategory(245);

	public static List<Ad> getLatestAds(long latestAdId) {
		return service.getLatestAds(category, latestAdId);
	}

	public static EkService getService() {
		return service;
	}
}
