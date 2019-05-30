package com.tivo.demo.tvshow.loaders;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLConnection;
import java.nio.file.Paths;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.json.JSONArray;

public abstract class AbstractShowLoader {

	protected StandardAnalyzer analyzer;
	protected Directory index;
	
	/**
	 * Construct the Lucene search index, query the show listing from an API, and load shows into index
	 * @throws IOException
	 */
	public AbstractShowLoader() throws IOException {
		
		//Construct the Lucene search index
		analyzer = new StandardAnalyzer();
		index = new SimpleFSDirectory(Paths.get("/Users/palmerja/Lucene"));
		
		//Construct the IndexWriter
		IndexWriterConfig config = new IndexWriterConfig(analyzer);
		IndexWriter indexWriter = new IndexWriter(index, config);
		
		//Query the show listing from an API, using the methods that any subclass will override
		JSONArray shows;
		try {
			shows = getShowListing();
			
			indexWriter.deleteAll();
			indexWriter.commit();
			
			//Load the list of shows into a Lucene search index
			loadShowsIntoIndex(shows, indexWriter);

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			indexWriter.close();
		}
	}
	
	
	/**
	 * Subclasses must implement to query the target TV Show API
	 * @param url
	 * @return
	 * @throws IOException
	 */
	public abstract JSONArray getShowListing() throws IOException;
	
	
	/**
	 * Subclasses must implement to add each TV Show into the Lucene Index
	 * @param allShowsJson
	 * @param indexWriter
	 * @throws IOException
	 */
	public abstract void loadShowsIntoIndex(JSONArray allShowsJson, IndexWriter indexWriter) throws IOException;
	
	
	public StandardAnalyzer getAnalyzer() {
		return analyzer;
	}

	public Directory getIndex() {
		return index;
	}
	
	
	/**
	 * Helper Method to extract the body of the response
	 * 
	 * @param con
	 * @return The body of the response
	 * @throws IOException
	 */
	protected String extractResponse(URLConnection conn) throws IOException {
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
	
}
