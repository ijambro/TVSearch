package com.tivo.demo.tvsearch;

import java.io.IOException;

import org.json.JSONArray;

public interface TVShowLoader {

	public JSONArray getShowListing(String url) throws IOException;
	
	public void loadShowsIntoIndex(JSONArray allShowsJson);
}
