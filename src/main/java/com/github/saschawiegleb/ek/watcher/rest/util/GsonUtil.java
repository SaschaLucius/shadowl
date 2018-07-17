package com.github.saschawiegleb.ek.watcher.rest.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.saschawiegleb.ek.entity.GsonAdaptersAd;
import com.github.saschawiegleb.ek.entity.GsonAdaptersCategory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class GsonUtil {
	private static Gson gson;

	private static Gson gsonExpose;
	private static final Logger logger = LogManager.getLogger(GsonUtil.class);

	public static <T> T fromJson(String json, Class<T> classOfT, boolean onlyExpose) {
		try {
			return getInstance(onlyExpose).fromJson(json, classOfT);
		} catch (Exception e) {
			logger.error(e);
			return null;
		}
	}

	public static Gson getExposeInstance() {
		if (gsonExpose == null) {
			gsonExpose = getGsonBuilderInstance(true).create();
		}
		return gsonExpose;
	}

	private static GsonBuilder getGsonBuilderInstance(boolean onlyExpose) {
		GsonBuilder gsonBuilder = new GsonBuilder();
		if (onlyExpose) {
			gsonBuilder.excludeFieldsWithoutExposeAnnotation();
		}
		gsonBuilder.registerTypeAdapterFactory(new GsonAdaptersAd());
		gsonBuilder.registerTypeAdapterFactory(new GsonAdaptersCategory());
		return gsonBuilder;
	}

	public static Gson getInstance() {
		if (gson == null) {
			gson = getGsonBuilderInstance(false).create();
		}
		return gson;
	}

	public static Gson getInstance(boolean onlyExpose) {
		if (!onlyExpose) {
			if (gson == null) {
				gson = getGsonBuilderInstance(false).create();
			}
			return gson;
		} else {
			if (gsonExpose == null) {
				gsonExpose = getGsonBuilderInstance(true).create();
			}
			return gsonExpose;
		}
	}
}