package edu.umich.srg.egtaonline;

import com.google.gson.JsonObject;

public interface Observation {

  Iterable<? extends Player> getPlayers();

  JsonObject getFeatures();

  public static interface Player {

    String getRole();

    String getStrategy();

    double getPayoff();

    JsonObject getFeatures();

  }

}
