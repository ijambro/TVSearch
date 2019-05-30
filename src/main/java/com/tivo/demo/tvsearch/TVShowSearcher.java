package com.tivo.demo.tvsearch;

import java.io.IOException;
import java.util.Arrays;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;

import com.tivo.demo.tvshow.loaders.AbstractShowLoader;
import com.tivo.demo.tvshow.loaders.TivoShowLoader;

/**
 * TV Search application
 * 
 * It loads a Lucene search index based on a query from the listing of all upcoming TV shows from api.tvmaze.com.
 * Then it performs certain queries against that search index, using the Lucene syntax.
 *
 */
public class TVShowSearcher {
	
	//Field Names in the Lucene Search Index
	//episode level
	public static final String episodeNameFieldName = "episodeName", episodeDescriptionFieldName = "episodeSummary", episodeNumberFieldName = "episodeNumber"; 
	//show level
	public static final String showNameFieldName = "name", showTypeFieldName = "type", showDescriptionFieldName = "summary";
	//image level
	public static final String imageURLFieldName = "image"; 

	
	private final String [] QUERIES = {
			"Jeopardy",
			"name:Rachel",
			"name:Rachel -summary:Ray",
			"\"kids cartoon\"~15"
	};
	
	private final int hitsPerPage = 10;
	
	private Directory index;
	private StandardAnalyzer analyzer;

	
	/**
	 * Construct the TV Show Searcher with the index and analyzer used by the TV Show Loader
	 */
	public TVShowSearcher(Directory index, StandardAnalyzer analyzer) {
		this.index = index;
		this.analyzer = analyzer;
	}

	
	/**
	 * Perform several search queries
	 * @throws IOException If DirectoryReader.open fails to open the Index
	 */
	public void performQueries() throws IOException {
		
		IndexReader reader = DirectoryReader.open(index);
		IndexSearcher searcher = new IndexSearcher(reader);
		
		for (String q : QUERIES) {
			try {
				performQuery(q, searcher);
			} catch (QueryNodeException e) {
				System.err.println("Exception caught performing query '" + q + "': " + e);
				e.printStackTrace();
			}
		}
		
		reader.close();
	}
	
	/**
	 * 
	 * @param q
	 * @param searcher
	 * @throws QueryNodeException
	 * @throws IOException 
	 */
	private void performQuery(String q, IndexSearcher searcher) throws QueryNodeException, IOException {
		System.out.println("Performing Lucene Query: " + q);
        System.out.println();
        
        Query query = new StandardQueryParser(analyzer).parse(q, showNameFieldName);
        
        TopDocs docs = searcher.search(query, hitsPerPage);
        ScoreDoc[] hits = docs.scoreDocs;
        
        for (ScoreDoc scoredDoc : hits) {
        	int docId = scoredDoc.doc;
        	Document doc = searcher.doc(docId);

            System.out.println("Lucene Query Result: ");
            System.out.println("Show Name: " + doc.get(showNameFieldName) + ", Show Type: " + doc.get(showTypeFieldName) + ", Episode Name: " + doc.get(episodeNameFieldName) + " (" + doc.get(episodeNumberFieldName) + "), Episode Description: " + doc.get(episodeDescriptionFieldName));
            System.out.println();
        }
	}
	
	
    public static void main(String [] args) throws IOException {
        System.out.println("Welcome to the Lucene TV Show Searcher!");
        System.out.println();
        System.out.println("Input Params: " + Arrays.asList(args));
        System.out.println();
        
        //Construct the Lucene search index, query the show listing from an API, and load shows into index
        AbstractShowLoader loader = new TivoShowLoader();

        //Construct the Searcher and perform Lucene queries against the index
        TVShowSearcher searcher = new TVShowSearcher(loader.getIndex(), loader.getAnalyzer());
		searcher.performQueries();			
    }
}
