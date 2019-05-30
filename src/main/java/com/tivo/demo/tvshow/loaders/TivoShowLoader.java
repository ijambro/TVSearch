package com.tivo.demo.tvshow.loaders;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import org.apache.lucene.index.IndexWriter;
import org.json.JSONArray;

import com.tivo.demo.tvshow.loaders.oauth.OAuthSignatureGenerator;

public class TivoShowLoader extends AbstractShowLoader {

	private final String TIVO_DATA_SERVICE_ID_USEAST = "4068858495";
	private final String TIVO_DATA_SERVICE_CHANNELS_SCREEN = "https://developers.tivo.com/api/v1/resolve/3/data_service_channels/screen?id=";
	
	public TivoShowLoader() throws IOException {
		
	}

	
	@Override
	public JSONArray getShowListing() throws IOException {
		JSONArray shows = null;
		
		String url = TIVO_DATA_SERVICE_CHANNELS_SCREEN + TIVO_DATA_SERVICE_ID_USEAST;
		System.out.println("Querying Tivo show listing URL: " + url);
        System.out.println();

        OAuthSignatureGenerator oauth = new OAuthSignatureGenerator();
        String consumerKey = "";
        String consumerSecret = "";
        
        String oauthHeader = null;
        try {
        	String signature = oauth.generateSignature("GET", url, consumerKey, consumerSecret);
        	oauthHeader = oauth.buildAuthorizationHeader(consumerKey, signature);
        	
        	System.out.println("OAuth header: " + oauthHeader);
            System.out.println();
        } catch (UnsupportedEncodingException | NoSuchAlgorithmException | InvalidKeyException e) {
        	System.err.println("Exception caught generating OAuth signature: " + e);
        	e.printStackTrace();
        	throw new IOException(e);
        }
        
        URL u = new URL(url);
        HttpURLConnection conn = (HttpURLConnection) u.openConnection();
        
        conn.setRequestMethod("GET");
        conn.setRequestProperty("content-type",	"application/json");
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("Authorization", oauthHeader);
        
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
		

	}

}
