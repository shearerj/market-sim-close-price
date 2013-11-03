package data;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.Map.Entry;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class MultiSimulationObservation {

	protected static final Gson gson = new Gson();
	JsonElement observations;
	int n;
	
	public MultiSimulationObservation() {
		observations = null;
		n = 0;
	}
	
	public void addObservation(Observations obs) {
		n++;
		if (observations == null)
			observations = obs.observations;
		else
			observations = mergeJson(observations, obs.observations);
	}
	
	// Recursively merge json objects. This is super fragile! But will work if everything stays the same.	
	protected JsonElement mergeJson(JsonElement agg, JsonElement add) {
		if (agg.isJsonObject()) {
			JsonObject obj = new JsonObject();
			for (Entry<String, JsonElement> e : agg.getAsJsonObject().entrySet())
				obj.add(e.getKey(), mergeJson(e.getValue(), add.getAsJsonObject().get(e.getKey())));
			return obj;
		} else if (agg.isJsonArray()) {
			JsonArray arr = new JsonArray();
			for (Iterator<JsonElement> itg = agg.getAsJsonArray().iterator(), itd = add.getAsJsonArray().iterator(); itg.hasNext();)
				arr.add(mergeJson(itg.next(), itd.next()));
			return arr;
		} else if (agg.isJsonPrimitive()) {
			JsonPrimitive aggp = agg.getAsJsonPrimitive();
			if (aggp.isNumber()) {
				return new JsonPrimitive((aggp.getAsDouble() * (n - 1) + add.getAsDouble())/ n);
			} else if (aggp.isString()) {
				return aggp; // FIXME Ignores strings beyond the first!
			} else {
				throw new IllegalArgumentException();
			}
		} else {
			throw new IllegalArgumentException();
		}
	}
	
	public void writeToFile(File observationsFile) throws IOException {
		Writer writer = null;
		try {
			writer = new FileWriter(observationsFile);
			gson.toJson(observations, writer);
		} finally {
			if (writer != null) writer.close();
		}
	}
	
}
