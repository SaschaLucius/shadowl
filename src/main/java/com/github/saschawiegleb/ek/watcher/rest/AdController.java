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
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.apache.lucene.queryparser.classic.ParseException;

import com.github.saschawiegleb.ek.entity.Ad;
import com.github.saschawiegleb.ek.watcher.lucene.AdSearch;
import com.github.saschawiegleb.ek.watcher.sql.AdStorage;

@Path("search")
public class AdController {
	@GET
	@Path("test3/{name}") // ad/test3/world
	@Produces(MediaType.TEXT_PLAIN)
	public String pathParam(@PathParam("name") String name) {
		return "Hello, " + name;
	}

	@GET
	@Path("test2") // ad/test2?name=world
	@Produces(MediaType.TEXT_PLAIN)
	public String queryParam(@QueryParam("name") String name) {
		return "Hello, " + name;
	}

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
			e.printStackTrace();
		}
		return new ArrayList<Ad>();
	}

	@GET
	@Path("test1") // ad/test1
	@Produces(MediaType.TEXT_PLAIN)
	public String test() {
		return "Hello, world";
	}
}
