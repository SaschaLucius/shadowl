package com.github.saschawiegleb.ek.watcher.rest;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.queryparser.classic.ParseException;

import com.github.saschawiegleb.ek.entity.Ad;
import com.github.saschawiegleb.ek.watcher.lucene.AdSearch;
import com.github.saschawiegleb.ek.watcher.sql.AdStorage;

@Path("search")
public class AdController {
	private static final Logger logger = LogManager.getLogger(AdController.class);

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("ad/{input}")
	public List<Ad> searchAds(@PathParam("input") String searchString) {
		AdStorage _adStorage = new AdStorage();
		AdSearch _adSearch = new AdSearch();

		try (Connection connection = _adStorage.getConnection()) {
			ArrayList<Long> luceneQuery = _adSearch.luceneQuery(searchString);
			List<Ad> readAds = AdStorage.readAds(connection, luceneQuery.toArray(new Long[luceneQuery.size()]));
			return readAds;
		} catch (SQLException | IOException | ParseException e) {
			logger.error(e);
		}
		return new ArrayList<Ad>();
	}
}
