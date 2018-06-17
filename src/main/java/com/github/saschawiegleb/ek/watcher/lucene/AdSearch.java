package com.github.saschawiegleb.ek.watcher.lucene;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import com.github.saschawiegleb.ek.entity.Ad;
import com.github.saschawiegleb.ek.watcher.sql.AdStorage;

import javaslang.collection.List;

/**
 * lightweight index for searching in headline and description
 */
public class AdSearch implements java.io.Serializable {
	static StandardAnalyzer analyzer = new StandardAnalyzer();

	static Directory index = null;
	private static final long serialVersionUID = 1L;
	static {
		try {
			index = FSDirectory.open(new File("/tmp/testindex").toPath());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void addDoc(IndexWriter w, Ad ad) throws IOException {
		Document doc = new Document();
		doc.add(new NumericDocValuesField("id_index", ad.id()));
		doc.add(new StoredField("id", ad.id()));
		doc.add(new TextField("headline", ad.headline(), Store.YES));
		doc.add(new TextField("description", ad.description(), Store.YES));
		w.addDocument(doc);
	}

	public static void insertAdsLucene(List<Ad> ads) {
		IndexWriterConfig config = new IndexWriterConfig(analyzer);
		try (IndexWriter w = new IndexWriter(index, config)) {
			for (Ad ad : ads) {
				addDoc(w, ad);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static long[] toPrimitives(ArrayList<Long> list) {
		long[] primitives = new long[list.size()];
		for (int i = 0; i < list.size(); i++)
			primitives[i] = list.get(i);
		return primitives;
	}

	public ArrayList<Ad> luceneQuery(String searchString) throws IOException, ParseException {
		if (searchString == null || searchString.isEmpty()) {
			return new ArrayList<Ad>();
		}
		// Now search the index:
		DirectoryReader ireader = DirectoryReader.open(index);
		IndexSearcher isearcher = new IndexSearcher(ireader);
		// Parse a simple query that searches for "text":
		MultiFieldQueryParser parser = new MultiFieldQueryParser(new String[] { "headline", "description" }, analyzer);
		// QueryParser parser = new QueryParser("description", analyzer);
		Sort sort = new Sort(new SortField("id_index", SortField.Type.LONG, true));
		Query query = parser.parse(searchString);
		ScoreDoc[] hits = isearcher.search(query, 50, sort).scoreDocs;

		System.out.println("Found " + hits.length + " results in " + ireader.numDocs() + " indeced Documents");
		// Iterate through the results:
		ArrayList<Long> list = new ArrayList<Long>();
		for (int i = 0; i < hits.length; i++) {
			Document hitDoc = isearcher.doc(hits[i].doc);
			// System.out.println("This is the text to be indexed: " +
			// hitDoc.get("id") + " " + hitDoc.get("headline"));
			list.add(Long.parseLong(hitDoc.get("id")));
		}

		try {
			Connection connection = AdStorage.getConnection(AdStorage.defaultConnectionType);
			ArrayList<Ad> readAds = AdStorage.readAds(connection, list.toArray(new Long[list.size()]));
			return readAds;
		} catch (SQLException e) {
			e.printStackTrace();
		}

		ireader.close();
		return new ArrayList<Ad>();
	}
}
