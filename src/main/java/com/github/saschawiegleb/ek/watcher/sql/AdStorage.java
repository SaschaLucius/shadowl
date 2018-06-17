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

import com.github.saschawiegleb.ek.entity.Ad;
import com.github.saschawiegleb.ek.entity.Category;
import com.github.saschawiegleb.ek.entity.ImmutableAd;
import com.github.saschawiegleb.ek.watcher.Service;

import javaslang.collection.List;
import javaslang.control.Either;

/**
 * for storing the information about the ads in an database
 */
public class AdStorage {
	private static String AD = "CREATE TABLE IF NOT EXISTS ad(" + "id INT," + "headline VARCHAR(70) NOT NULL,"
			+ "description TEXT(4000) NOT NULL," + "price VARCHAR(15)," + "vendor_id VARCHAR(50)," + "category_id INT,"
			+ "location VARCHAR(70) NOT NULL," + "time DATETIME NOT NULL," + "FOREIGN KEY (vendor_id)"
			+ "REFERENCES vendor(id)" + "ON DELETE CASCADE," + "FOREIGN KEY (category_id)" + "REFERENCES category(id)"
			+ "ON DELETE CASCADE,	" + "PRIMARY KEY (id));";

	private static String AD_INDEX = "CREATE UNIQUE INDEX id_desc_index ON ad (id DESC)";

	private static String CATEGORY = "CREATE TABLE IF NOT EXISTS category(" + "id INT," + "name VARCHAR(50) NOT NULL,"
			+ "PRIMARY KEY (id));";

	public static ConnectionType defaultConnectionType = ConnectionType.HSQL_MEM;

	private static String DETAILS = "CREATE TABLE IF NOT EXISTS additionalDetail(" + "ad_id INT NOT NULL,"
			+ "FOREIGN KEY (ad_id)" + "REFERENCES ad(id)" + "ON DELETE CASCADE," + "name VARCHAR(255) NOT NULL,"
			+ "value VARCHAR(255)," + "PRIMARY KEY (ad_id, name));";

	private static String IMAGE = "CREATE TABLE IF NOT EXISTS image(" + "ad_id INT NOT NULL," + "FOREIGN KEY (ad_id)"
			+ "REFERENCES ad(id)" + "ON DELETE CASCADE," + "id INT NOT NULL," + "url VARCHAR(255) NOT NULL,"
			+ "PRIMARY KEY (ad_id, id));";

	private static String VENDOR = "CREATE TABLE IF NOT EXISTS vendor(" + "id VARCHAR(50),"
			+ "name VARCHAR(50) NOT NULL," + "PRIMARY KEY (id));";

	public static void createTables() {
		try (Connection con = getConnection(defaultConnectionType)) {
			Statement stmt = null;
			stmt = con.createStatement();
			stmt.executeUpdate(CATEGORY);
			stmt.executeUpdate(VENDOR);
			stmt.executeUpdate(AD);
			stmt.executeUpdate(AD_INDEX);
			stmt.executeUpdate(IMAGE);
			stmt.executeUpdate(DETAILS);
			injectCategories(con);
		} catch (Exception e) {
			e.printStackTrace(System.out);
		}
	}

	public static Connection getConnection(ConnectionType type) throws SQLException {
		// TODO move to ConnectionType
		switch (type) {
		case HSQL_FILE:
			return DriverManager.getConnection("jdbc:hsqldb:file:testdb;sql.syntax_mys=true", "SA", "");
		case HSQL_MEM:
			return DriverManager.getConnection("jdbc:hsqldb:mem:mymemdb;sql.syntax_mys=true", "SA", "");
		case MY_SQL:
			return DriverManager.getConnection("jdbc:mysql://localhost:3306/ebay", "root", "");
		default:
			throw new UnsupportedOperationException();
		}
	}

	public static long getLatestAdId(Connection conn) throws SQLException {
		ResultSet rs = conn.prepareStatement("SELECT id FROM ad ORDER BY id DESC LIMIT 1").executeQuery();
		boolean next = rs.next();
		if (!next) {
			return 0;
		}
		return rs.getLong(1);
	}

	private static void injectCategories(Connection conn) throws SQLException {
		ResultSet rs = conn.prepareStatement("SELECT * FROM category where id='" + 0 + "'").executeQuery();
		if (rs.next()) {
			return;
		}
		for (Category cat : Service.getService().getCategorys()) {
			Statement st = conn.createStatement();
			st.executeUpdate("INSERT INTO category VALUES ('" + cat.id() + "','" + cat.name() + "')");
		}
		conn.commit();
	}

	private static void insertAd(Ad ad, Connection conn) throws SQLException {
		Statement st = conn.createStatement();
		st.executeUpdate("INSERT INTO ad VALUES ('" + ad.id() + "','" + ad.headline().replaceAll("'", "-") + "','"
				+ ad.description().replaceAll("'", "-") + "','" + ad.price() + "','" + ad.vendorId() + "','"
				+ ad.category().id() + "','" + ad.location() + "','"
				+ (ad.time().isRight() ? DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(ad.time().get())
						: DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now()))
				+ "')");
	}

	public static void insertFullAd(Ad ad, Connection conn) throws SQLException {
		insertVendor(ad, conn);
		insertAd(ad, conn);
		insertImages(ad, conn);
		conn.commit();
	}

	private static void insertImages(Ad ad, Connection conn) throws SQLException {
		Statement st = conn.createStatement();
		for (int i = 0; i < ad.images().length(); i++) {
			st.executeUpdate("INSERT INTO image VALUES ('" + ad.id() + "','" + i + "','" + ad.images().get(i) + "')");
		}
	}

	private static void insertVendor(Ad ad, Connection conn) throws SQLException {
		Statement st = conn.createStatement();
		st.executeUpdate("INSERT INTO vendor (id,name) VALUES ('" + ad.vendorId() + "','"
				+ ad.vendorName().replaceAll("'", "-") + "') ON DUPLICATE KEY UPDATE name=VALUES(name)");
	}

	public static void loadDriver() {
		try {
			Class.forName("com.mysql.cj.jdbc.Driver").newInstance();
			Class.forName("org.hsqldb.jdbc.JDBCDriver").newInstance();
		} catch (Exception e) {
			e.printStackTrace(System.out);
		}
	}

	static ArrayList<Ad> mapAds(ResultSet rs, Connection conn) {
		ArrayList<Ad> list = new ArrayList<Ad>();
		try {
			while (rs.next()) {
				ImmutableAd.Builder builder = ImmutableAd.builder();
				builder.id(rs.getLong("id"));
				builder.headline(rs.getString("headline"));
				builder.description(rs.getString("description"));
				builder.images(readImages(conn, rs.getLong("id")));
				builder.price(rs.getString("price"));
				builder.location(rs.getString("location"));
				builder.time(Either.right(ZonedDateTime.of(
						LocalDateTime.ofInstant(rs.getTimestamp("time").toInstant(), ZoneId.of("Europe/Berlin")),
						ZoneId.of("Europe/Berlin"))));
				list.add(builder.build());
			}
		} catch (SQLException e) {
			e.printStackTrace(System.out);
		}
		return list;
	}

	public static ArrayList<Ad> readAds(Connection conn, Long[] ids) throws SQLException {
		if (ids.length == 0) {
			return new ArrayList<Ad>();
		}
		StringBuilder builder = new StringBuilder();
		for (Long id : ids) {
			builder.append(id).append(",");
		}
		PreparedStatement statement = conn.prepareStatement(
				"SELECT * FROM ad WHERE id IN (" + builder.substring(0, builder.length() - 1) + ") ORDER BY id DESC");
		ResultSet rs = statement.executeQuery();
		return mapAds(rs, conn);
	}

	public static List<String> readImages(Connection conn, Long id) throws SQLException {
		ArrayList<String> list = new ArrayList<String>();
		PreparedStatement statement = conn.prepareStatement("select * from image where ad_id = " + id + " ORDER BY id");
		ResultSet rs = statement.executeQuery();
		while (rs.next()) {
			list.add(rs.getString("url"));
		}
		return List.ofAll(list);
	}
}
