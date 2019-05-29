package com.tivo.demo.tvsearch;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Paths;
import java.util.Arrays;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * TV Search application
 * 
 * It loads a Lucene search index based on a query from the listing of all upcoming TV shows from api.tvmaze.com.
 * Then it performs certain queries against that search index, using the Lucene syntax.
 *
 */
public class TVShowSearcher {
	
	//The simplest URL for a JSON list of TV shows coming up in the US, today
	//	Optional params: country, date
	//	Example: http://api.tvmaze.com/schedule?country=US&date=2014-12-01
	private final String TVSHOW_QUERY_URL = "http://api.tvmaze.com/schedule";
	
	//Keys for querying each JSONObject of the JSONArray response
	//top level
	private final String episodeNameKey = "name", episodeDescriptionKey = "summary", episodeNumberKey = "number"; 
	//top -> show level
	private final String showObjectKey = "show", showNameKey = "name", showTypeKey = "type", showDescriptionKey = "summary";
	//top -> show -> image level
	private final String imageObjectKey = "image", imageURLKey = "original"; 

	//Field Names in the Lucene Search Index
	//episode level
	private final String episodeNameFieldName = "episodeName", episodeDescriptionFieldName = "episodeSummary", episodeNumberFieldName = "episodeNumber"; 
	//show level
	private final String showNameFieldName = "name", showTypeFieldName = "type", showDescriptionFieldName = "summary";
	//image level
	private final String imageURLFieldName = "image"; 

	
	private final String [] QUERIES = {
			"Jeopardy",
			"name:Rachel",
			"name:Rachel -summary:Ray",
			"\"kids cartoon\"~15"
	};
	
	private final int hitsPerPage = 10;
	
	private StandardAnalyzer analyzer;
	private Directory index;
	
	/**
	 * Construct and initialize the TV Show Searcher
	 * @throws IOException 
	 */
	public TVShowSearcher() throws IOException {
		
		//Construct the Lucene search index
		analyzer = new StandardAnalyzer();
		index = new SimpleFSDirectory(Paths.get("/Users/palmerja/Lucene"));

		IndexWriterConfig config = new IndexWriterConfig(analyzer);

		IndexWriter w = new IndexWriter(index, config);
		
		//Query the show listing from an API
		JSONArray shows;
		try {
			shows = getShowListing(TVSHOW_QUERY_URL);
			
			w.deleteAll();
			w.commit();
			
			//Load the list of shows into a Lucene search index
			loadShowsIntoIndex(shows, w);

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			w.close();
		}
	}
	
	/**
	 * 
	 * @param url
	 * @throws IOException 
	 * 
	 */
	private JSONArray getShowListing(String url) throws IOException {
		JSONArray shows = null;
		
		System.out.println("Querying show listing URL: " + url);
        System.out.println();

        URL u = new URL(url);
        HttpURLConnection conn = (HttpURLConnection) u.openConnection();
        
        conn.setRequestMethod("GET");
        conn.setRequestProperty("content-type",	"application/json");
        
        conn.connect();
        
        System.out.println("Response code: " + conn.getResponseCode());
        System.out.println("Response message: " + conn.getResponseMessage());
        
        String jsonResponse = null;
        
        if (conn.getResponseCode() == 200) {
        	jsonResponse = extractResponse(conn);
        	
            System.out.println("Response content: " + jsonResponse);
            
            shows = new JSONArray(jsonResponse);
            
            System.out.println("JSON Array length: " + shows.length());
        }
        else {
            System.out.println("Request failed with response code: " + conn.getResponseCode());
        }
        
		return shows;
	}
	
	/**
	 * Method to extract the body of the response
	 * 
	 * @param con
	 * @return The body of the response
	 * @throws IOException
	 */
	private String extractResponse(URLConnection conn) throws IOException {
		StringBuffer response = new StringBuffer();
		
		//Use a try-with-resources to make sure the readers/streams get closed automatically, even if an exception is caught
		try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {	
			String inputLine;
			
			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
		}
		
		return response.toString();
	}
	
	private void loadShowsIntoIndex(JSONArray allShowsJson, IndexWriter indexWriter) throws IOException {
		//DEBUG
		if (allShowsJson == null || allShowsJson.length() == 0) {
			System.out.println("Show Listing is null.  Loading sample show...");
			addShowToIndex(indexWriter, "Wheel of Fortune", "Game show", "Game show where ordinary people pick letters to fill in blanks in a puzzle", null, "Today's Episode", 12345, "Contestants are Jake, Sarah and Leon Palmer");
		}
		
		JSONObject episodeJson, showJson, imageJson;
		
		//TODO: Create a new class TVShowEpisode - set all fields to null when constructing
		String showName, showType, showDescription, showImageURL, episodeName;
		String episodeDescription;
		int episodeNumber;
		
		for (int i = 0; i < allShowsJson.length(); i++) {
			episodeJson = allShowsJson.getJSONObject(i);
			
			if (episodeJson != null) {
				episodeName = episodeJson.optString(episodeNameKey);
				episodeNumber = episodeJson.optInt(episodeNumberKey);
				episodeDescription = episodeJson.optString(episodeDescriptionKey);
				
				//TODO: Move this into TVShowEpisode class
				//Have to init fields to null which might not be found
				showName = null; showType = null; showDescription = null; showImageURL = null;
				
				showJson = episodeJson.optJSONObject(showObjectKey);
				if (showJson != null) {
					showName = showJson.optString(showNameKey);
					showType = showJson.optString(showTypeKey);
					showDescription = showJson.optString(showDescriptionKey);
					
					imageJson = showJson.optJSONObject(imageObjectKey);
					if (imageJson != null) {
						showImageURL = imageJson.optString(imageURLKey);
					}
				}
				
				addShowToIndex(indexWriter, showName, showType, showDescription, showImageURL, episodeName, episodeNumber, episodeDescription == null ? "Oh no I'm null!" : episodeDescription.toString());
			}
		}
	}
	
	/**
	 * 
	 * @param indexWriter
	 * @param showName
	 * @param showType
	 * @param showDescription
	 * @param showImageURL
	 * @param episodeName
	 * @param episodeNumber
	 * @param episodeDescription
	 * @throws IOException
	 */
	private void addShowToIndex(IndexWriter indexWriter, String showName, String showType, String showDescription, String showImageURL, String episodeName, int episodeNumber, String episodeDescription) throws IOException {
		System.out.println("Adding show to Lucene index: " + showName + ", episode: " + episodeName + " (" + episodeNumber + ")");
        System.out.println();
        
        Document doc = new Document();
        //Searchable as full-text
        if (episodeName != null)
        	doc.add(new TextField(episodeNameFieldName, episodeName, Field.Store.YES));
        if (episodeDescription != null)
        	doc.add(new TextField(episodeDescriptionFieldName, episodeDescription, Field.Store.YES));
        if (showName != null)
        	doc.add(new TextField(showNameFieldName, showName, Field.Store.YES));
        if (showType != null)
        	doc.add(new TextField(showTypeFieldName, showType, Field.Store.YES));
        if (showDescription != null)
        	doc.add(new TextField(showDescriptionFieldName, showDescription, Field.Store.YES));
        
        //Searchable as a single token
        doc.add(new IntPoint(episodeNumberFieldName, episodeNumber)); //For range queries, but doesn't get included in result
        doc.add(new StoredField(episodeNumberFieldName, episodeNumber)); //To be included in the result
        
        //Non-Searchable, only shown in result
        if (showImageURL != null)
        	doc.add(new StoredField(imageURLFieldName, showImageURL));

        indexWriter.addDocument(doc);
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
	
	
    public static void main(String [] args) {
        System.out.println("Welcome to the Lucene TV Show Searcher!");
        System.out.println();
        System.out.println("Input Params: " + Arrays.asList(args));
        System.out.println();
        
        TVShowSearcher searcher;
		try {
			searcher = new TVShowSearcher();
			searcher.performQueries();
		} catch (IOException e) {
			System.err.println("Exception caught constructing TVShowSearcher: " + e);
			e.printStackTrace();
		}
    }
}
