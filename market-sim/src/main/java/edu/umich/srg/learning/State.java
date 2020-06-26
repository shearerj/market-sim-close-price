package edu.umich.srg.learning;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import edu.umich.srg.marketsim.privatevalue.PrivateValue;

public interface State{
		
	JsonArray getState (double finalEstimate, int side, PrivateValue privateValue);
	
	JsonObject getStateDict (double finalEstimate, int side, PrivateValue privateValue);
	
	JsonArray getNormState(JsonArray state);
	
	int getStateSize ();
	
}