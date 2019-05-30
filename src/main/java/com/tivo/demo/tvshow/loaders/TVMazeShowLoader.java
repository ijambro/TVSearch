package com.tivo.demo.tvshow.loaders;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.Directory;
import org.json.JSONArray;
import org.json.JSONObject;

import com.tivo.demo.tvsearch.TVShowSearcher;

public class TVMazeShowLoader extends AbstractShowLoader {

	//The simplest URL for a JSON list of TV shows coming up in the US, today
	//	Optional params: country, date
	//	Example: http://api.tvmaze.com/schedule?country=US&date=2014-12-01
	private final String TVMAZE_QUERY_URL = "http://api.tvmaze.com/schedule";
	
	//Keys for querying each JSONObject of the JSONArray response
	//top level
	private final String episodeNameKey = "name", episodeDescriptionKey = "summary", episodeNumberKey = "number"; 
	//top -> show level
	private final String showObjectKey = "show", showNameKey = "name", showTypeKey = "type", showDescriptionKey = "summary";
	//top -> show -> image level
	private final String imageObjectKey = "image", imageURLKey = "original"; 

		
	
	public TVMazeShowLoader() throws IOException {
		
		//Construct the Lucene search analyzer and index via the superclass default constructor
		//This will then trigger calls to getShowListing and loadShowsIntoIndex
		super();
	}
	

	/**
	 * 
	 * @param url
	 * @throws IOException 
	 */
	@Override
	public JSONArray getShowListing() throws IOException {
		JSONArray shows = null;
		
		System.out.println("Querying TVMaze show listing URL: " + TVMAZE_QUERY_URL);
        System.out.println();

        URL u = new URL(TVMAZE_QUERY_URL);
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
	


	@Override
	public void loadShowsIntoIndex(JSONArray allShowsJson, IndexWriter indexWriter) throws IOException {
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
        	doc.add(new TextField(TVShowSearcher.episodeNameFieldName, episodeName, Field.Store.YES));
        if (episodeDescription != null)
        	doc.add(new TextField(TVShowSearcher.episodeDescriptionFieldName, episodeDescription, Field.Store.YES));
        if (showName != null)
        	doc.add(new TextField(TVShowSearcher.showNameFieldName, showName, Field.Store.YES));
        if (showType != null)
        	doc.add(new TextField(TVShowSearcher.showTypeFieldName, showType, Field.Store.YES));
        if (showDescription != null)
        	doc.add(new TextField(TVShowSearcher.showDescriptionFieldName, showDescription, Field.Store.YES));
        
        //Searchable as a single token
        doc.add(new IntPoint(TVShowSearcher.episodeNumberFieldName, episodeNumber)); //For range queries, but doesn't get included in result
        doc.add(new StoredField(TVShowSearcher.episodeNumberFieldName, episodeNumber)); //To be included in the result
        
        //Non-Searchable, only shown in result
        if (showImageURL != null)
        	doc.add(new StoredField(TVShowSearcher.imageURLFieldName, showImageURL));

        indexWriter.addDocument(doc);
	}


	public StandardAnalyzer getAnalyzer() {
		return analyzer;
	}


	public Directory getIndex() {
		return index;
	}
	
}
