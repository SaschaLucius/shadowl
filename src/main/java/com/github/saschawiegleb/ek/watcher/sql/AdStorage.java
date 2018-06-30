package com.github.saschawiegleb.ek.watcher.sql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.saschawiegleb.ek.entity.Ad;
import com.github.saschawiegleb.ek.entity.Category;
import com.github.saschawiegleb.ek.entity.ImmutableAd;
import com.github.saschawiegleb.ek.watcher.Config;
import com.github.saschawiegleb.ek.watcher.Service;

import javaslang.collection.List;
import javaslang.control.Either;

/**
 * for storing the information about the ads in an database
 */
public class AdStorage {
	private static final String AD = "CREATE TABLE IF NOT EXISTS ad(" + "id INT," + "headline VARCHAR(70) NOT NULL,"
			+ "description TEXT(4000) NOT NULL," + "price VARCHAR(15)," + "vendor_id VARCHAR(50)," + "category_id INT,"
			+ "location VARCHAR(70) NOT NULL," + "time DATETIME NOT NULL," + "FOREIGN KEY (vendor_id)"
			+ "REFERENCES vendor(id)" + "ON DELETE CASCADE," + "FOREIGN KEY (category_id)" + "REFERENCES category(id)"
			+ "ON DELETE CASCADE,	" + "PRIMARY KEY (id));";

	private static final String AD_INDEX = "CREATE UNIQUE INDEX id_desc_index ON ad (id DESC)";

	private static final String CATEGORY = "CREATE TABLE IF NOT EXISTS category(" + "id INT,"
			+ "name VARCHAR(50) NOT NULL,"
			+ "PRIMARY KEY (id));";

	private static final String DETAILS = "CREATE TABLE IF NOT EXISTS additionalDetail(" + "ad_id INT NOT NULL,"
			+ "FOREIGN KEY (ad_id)" + "REFERENCES ad(id)" + "ON DELETE CASCADE," + "name VARCHAR(255) NOT NULL,"
			+ "value VARCHAR(255)," + "PRIMARY KEY (ad_id, name));";

	private static final String IMAGE = "CREATE TABLE IF NOT EXISTS image(" + "ad_id INT NOT NULL,"
			+ "FOREIGN KEY (ad_id)"
			+ "REFERENCES ad(id)" + "ON DELETE CASCADE," + "id INT NOT NULL," + "url VARCHAR(255) NOT NULL,"
			+ "PRIMARY KEY (ad_id, id));";

	private static final Logger logger = LogManager.getLogger(AdStorage.class);

	private static final String VENDOR = "CREATE TABLE IF NOT EXISTS vendor(" + "id VARCHAR(50),"
			+ "name VARCHAR(50) NOT NULL," + "PRIMARY KEY (id));";

	private static void injectCategories(Connection connection) throws SQLException {
		String query = String.format("SELECT * FROM category where id='%s'", 0);
		ResultSet resultSet = connection.prepareStatement(query).executeQuery();
		if (resultSet.next()) {
			return;
		}
		for (Category category : Service.getCategories()) {
			Statement statement = connection.createStatement();
			String insert = String.format("INSERT INTO category VALUES ('%s','%s')", category.id(), category.name());
			statement.executeUpdate(insert);
		}
		connection.commit();
	}

	public static void loadDriver() {
		try {
			Class.forName("com.mysql.cj.jdbc.Driver").getDeclaredConstructor().newInstance();
			Class.forName("org.hsqldb.jdbc.JDBCDriver").getDeclaredConstructor().newInstance();
		} catch (Exception e) {
			logger.error(e);
		}
	}

	static ArrayList<Ad> mapAds(ResultSet resultSet, Connection connection) {
		ArrayList<Ad> adList = new ArrayList<Ad>();
		try {
			while (resultSet.next()) {
				ImmutableAd.Builder builder = ImmutableAd.builder();
				builder.id(resultSet.getLong("id"));
				builder.headline(resultSet.getString("headline"));
				builder.description(resultSet.getString("description"));
				builder.images(readImages(connection, resultSet.getLong("id")));
				builder.price(resultSet.getString("price"));
				builder.location(resultSet.getString("location"));
				builder.time(Either.right(ZonedDateTime.of(
						LocalDateTime.ofInstant(resultSet.getTimestamp("time").toInstant(), ZoneId.of("Europe/Berlin")),
						ZoneId.of("Europe/Berlin"))));
				adList.add(builder.build());
			}
		} catch (SQLException e) {
			logger.error(e);
		}
		return adList;
	}

	public static ArrayList<Ad> readAds(Connection connection, Long[] ids) throws SQLException {
		if (ids.length == 0) {
			return new ArrayList<Ad>();
		}
		StringBuilder builder = new StringBuilder();
		for (Long id : ids) {
			builder.append(id).append(",");
		}
		String query = String.format("SELECT * FROM ad WHERE id IN (%s) ORDER BY id DESC",
				builder.substring(0, builder.length() - 1));
		PreparedStatement statement = connection.prepareStatement(query);
		ResultSet resultSet = statement.executeQuery();
		return mapAds(resultSet, connection);
	}

	public static List<String> readImages(Connection connection, Long adId) throws SQLException {
		ArrayList<String> imageList = new ArrayList<String>();
		String query = String.format("select * from image where ad_id = %s ORDER BY id", adId);
		PreparedStatement statement = connection.prepareStatement(query);
		ResultSet resultSet = statement.executeQuery();
		while (resultSet.next()) {
			imageList.add(resultSet.getString("url"));
		}
		return List.ofAll(imageList);
	}

	public void createTables() { // TODO add connection as parameter
		try (Connection connection = getConnection()) {
			Statement statement = null;
			statement = connection.createStatement();
			statement.executeUpdate(CATEGORY);
			statement.executeUpdate(VENDOR);
			statement.executeUpdate(AD);
			statement.executeUpdate(AD_INDEX);
			statement.executeUpdate(IMAGE);
			statement.executeUpdate(DETAILS);
			injectCategories(connection);
		} catch (Exception e) {
			logger.error(e);
		}
	}

	public Connection getConnection() throws SQLException { // TODO add helper methods for all external calls
		return DriverManager.getConnection(Config.CONNECTION.get());
	}

	public long getLatestAdId(Connection connection) throws SQLException {
		ResultSet resultSet = connection.prepareStatement("SELECT id FROM ad ORDER BY id DESC LIMIT 1").executeQuery();
		if (!resultSet.next()) {
			return 0;
		}
		return resultSet.getLong(1);
	}

	private void insertAd(Ad ad, Connection connection) throws SQLException {
		Statement statement = connection.createStatement();
		statement.executeUpdate("INSERT INTO ad VALUES ('" + ad.id() + "','" + ad.headline().replaceAll("'", "-") + "','"
				+ ad.description().replaceAll("'", "-") + "','" + ad.price() + "','" + ad.vendorId() + "','"
				+ ad.category().id() + "','" + ad.location() + "','"
				+ (ad.time().isRight() ? DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(ad.time().get())
						: DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now()))
				+ "')");
	}

	public void insertFullAd(Ad ad, Connection connection) throws SQLException {
		insertVendor(ad, connection);
		insertAd(ad, connection);
		insertImages(ad, connection);
		connection.commit();
	}

	private void insertImages(Ad ad, Connection connection) throws SQLException {
		Statement st = connection.createStatement();
		for (int i = 0; i < ad.images().length(); i++) {
			String statement = String.format("INSERT INTO image VALUES ('%s','%s','%s')", ad.id(), i,
					ad.images().get(i));
			st.executeUpdate(statement);
		}
	}

	private void insertVendor(Ad ad, Connection connection) throws SQLException {
		Statement statement = connection.createStatement();
		String insert = String.format(
				"INSERT INTO vendor (id,name) VALUES ('%s','%s') ON DUPLICATE KEY UPDATE name=VALUES(name)",
				ad.vendorId(), ad.vendorName().replaceAll("'", "-"));
		statement.executeUpdate(insert);
	}
}
