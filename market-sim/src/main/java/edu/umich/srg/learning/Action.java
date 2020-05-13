package edu.umich.srg.learning;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public interface Action {
	
	JsonArray getAction ();
	
	JsonArray getAction(JsonObject state);
	
	double actionToPrice(double finalEstimate);
	
	int getActionSize ();

}