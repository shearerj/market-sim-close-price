package edu.umich.srg.learning;

import com.google.gson.JsonArray;

public interface Action {
	
	JsonArray getAction ();
	
	JsonArray getAction(String curr_state);
	
	double actionToPrice(double finalEstimate);
	
	int getActionSize ();

}