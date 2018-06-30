package com.github.saschawiegleb.ek.watcher;

import com.github.saschawiegleb.ek.api.EkService;
import com.github.saschawiegleb.ek.entity.Ad;
import com.github.saschawiegleb.ek.entity.Category;

import javaslang.collection.List;

/**
 * provides access to all relevant methods of the ek-api
 */
public class Service {
	private static final EkService service = new EkService();
	private static Category category = service.getCategory(Config.CATEGORY.getInt());

	public static List<Category> getCategories() {
		return service.getCategories();
	}

	public static List<Ad> getLatestAds(long latestAdId) {
		refreshCategoryIfNeeded();
		return service.getLatestAds(category, latestAdId);
	}

	private static void refreshCategoryIfNeeded() {
		if (category.id() != Config.CATEGORY.getInt()) {
			category = service.getCategory(Config.CATEGORY.getInt());
		}
	}
}
