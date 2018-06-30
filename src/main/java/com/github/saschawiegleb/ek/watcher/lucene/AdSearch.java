package com.github.saschawiegleb.ek.watcher.lucene;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import com.github.saschawiegleb.ek.watcher.Config;

import javaslang.collection.List;

/**
 * lightweight index for searching in headline and description
 */
public class AdSearch implements java.io.Serializable {
	static StandardAnalyzer analyzer = new StandardAnalyzer();

	private static final String DESCRIPTION = "description";
	private static final String HEADLINE = "headline";
	private static final String ID = "id";
	private static final String ID_INDEX = "id_index";
	private static final Logger logger = LogManager.getLogger(AdSearch.class);

	private static final long serialVersionUID = 1L;

	private Directory _index = null;

	public AdSearch() {
		try {
			_index = FSDirectory.open(new File(Config.INDEXFOLDER.get() + "/index").toPath());
		} catch (IOException e) {
			logger.error(e);
		}
	}

	private static void addDoc(IndexWriter w, Ad ad) throws IOException {
		Document doc = new Document();
		doc.add(new NumericDocValuesField(ID_INDEX, ad.id()));
		doc.add(new StoredField(ID, ad.id()));
		doc.add(new TextField(HEADLINE, ad.headline(), Store.YES));
		doc.add(new TextField(DESCRIPTION, ad.description(), Store.YES));
		w.addDocument(doc);
	}

	public void insertAdsLucene(List<Ad> ads) {
		IndexWriterConfig config = new IndexWriterConfig(analyzer);
		try (IndexWriter w = new IndexWriter(_index, config)) {
			for (Ad ad : ads) {
				addDoc(w, ad);
			}
		} catch (IOException e) {
			logger.error(e);
		}
	}

	public ArrayList<Long> luceneQuery(String searchString) throws IOException, ParseException {
		if (searchString == null || searchString.isEmpty()) {
			return new ArrayList<Long>();
		}
		// search the index
		DirectoryReader reader = DirectoryReader.open(_index);
		IndexSearcher searcher = new IndexSearcher(reader);
		// parse query
		MultiFieldQueryParser parser = new MultiFieldQueryParser(new String[] { HEADLINE, DESCRIPTION }, analyzer);
		Sort sort = new Sort(new SortField(ID_INDEX, SortField.Type.LONG, true));
		Query query = parser.parse(searchString);
		ScoreDoc[] hits = searcher.search(query, Config.RESULTS.getInt(), sort).scoreDocs;

		logger.info("Found " + hits.length + " results in " + reader.numDocs() + " indeced Documents");
		// iterate through the results:
		ArrayList<Long> adIds = new ArrayList<Long>();
		for (int i = 0; i < hits.length; i++) {
			Document hitDoc = searcher.doc(hits[i].doc);
			adIds.add(Long.parseLong(hitDoc.get(ID)));
		}

		reader.close();
		return adIds;
	}
}
