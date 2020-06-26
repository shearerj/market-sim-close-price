package edu.umich.srg.learning;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public interface Action {
	
	JsonArray getAction ();
	JsonObject getActionDict(double finalEstimate);
	
	JsonArray getAction(JsonObject state);
	JsonObject getActionDict(JsonObject state,double finalEstimate);
	
	double actionToPrice(double finalEstimate);
	
	int getActionSize ();

}