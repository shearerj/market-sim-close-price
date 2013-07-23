//package systemmanager;
//
//import static logger.Logger.log;
//import static logger.Logger.Level.ERROR;
//
//import java.io.File;
//import java.io.FileInputStream;
//import java.io.IOException;
//import java.io.InputStreamReader;
//import java.util.ArrayList;
//import java.util.Collections;
//
//import org.json.simple.JSONObject;
//import org.json.simple.parser.JSONParser;
//import org.json.simple.parser.ParseException;
//
//import systemmanager.Consts.AgentType;
//import systemmanager.Consts.ModelType;
//import data.AgentPropsPair;
//import data.EntityProperties;
//import data.Keys;
////import data.SystemData;
//import event.TimeStamp;
//
//
///**
// * Stores list of parameters used in the simulation_spec.json file.
// * 
// * NOTE: All MarketModel types in the spec file must match the 
// * corresponding class name.
// * 
// * @author ewah
// */
//public class PlayerReference {
//
////	private SystemData data;
//	private JSONObject params;
//	private JSONObject assignments;
//	private JSONParser parser;
//	
//	public final static String ASSIGN_KEY = "assignment";
//	public final static String CONFIG_KEY = "configuration";
//	
//	// Parameters in spec file
//	public final static String SIMULATION_LENGTH = "sim_length";
//	public final static String TICK_SIZE = "tick_size";
//	public final static String LATENCY = "nbbo_latency";
//	public final static String ARRIVAL_RATE = "arrival_rate";
//	public final static String REENTRY_RATE = "reentry_rate";
//	public final static String FUNDAMENTAL_MEAN = "mean_value";
//	public final static String FUNDAMENTAL_KAPPA = "kappa";
//	public final static String FUNDAMENTAL_SHOCK_VAR = "shock_var";
//	public final static String PRIVATE_VALUE_VAR = "private_value_var";
//	public final static String PRIMARY_MODEL = "primary_model";
//	
//	/**
//	 * Constructor
//	 * @param file
//	 * @param l
//	 */
////	public PlayerReference(String file, SystemData d) {
////		data = d;
////		parser = new JSONParser();
////		loadFile(file);
////		readParams();
////	}
//	
//	/**
//	 * Load the simulation specification file.
//	 * @param specFile
//	 */
//	public void loadFile(String specFile) {
//		try {
//			FileInputStream is = new FileInputStream(new File(specFile));
//			InputStreamReader isr = new InputStreamReader(is);
//			Object obj = parser.parse(isr);
//			JSONObject array = (JSONObject) obj;
//			assignments = (JSONObject) array.get(ASSIGN_KEY);
//			params = (JSONObject) array.get(CONFIG_KEY);
//			isr.close();
//			is.close();
//		} catch (IOException e) {
//			System.err.println(this.getClass().getSimpleName() + 
//					"::loadFile(String): error opening/processing spec file: " +
//					specFile);
//			e.printStackTrace();
//		} catch (ParseException e) {
//			System.err.println(this.getClass().getSimpleName() + 
//					"::loadFile(String): JSON parsing error");
//		}
//	}
//	
//	
//	/**
//	 * Parses the spec file for config parameters. Overrides settings 
//	 * in environment properties file & agent properties config.
//	 */
//	public void readParams() {
//		
////		data.simLength = new TimeStamp(Integer.parseInt(getValue(SIMULATION_LENGTH)));
////		data.tickSize = Integer.parseInt(getValue(TICK_SIZE));	
////		data.nbboLatency = new TimeStamp(Integer.parseInt(getValue(LATENCY)));
////		data.arrivalRate = Double.parseDouble(getValue(ARRIVAL_RATE));
////		data.reentryRate = Double.parseDouble(getValue(REENTRY_RATE));
////		data.meanValue = Integer.parseInt(getValue(FUNDAMENTAL_MEAN));
////		data.kappa = Double.parseDouble(getValue(FUNDAMENTAL_KAPPA));
////		data.shockVar = Double.parseDouble(getValue(FUNDAMENTAL_SHOCK_VAR));
////		data.pvVar = Double.parseDouble(getValue(PRIVATE_VALUE_VAR));
////		data.primaryModelDesc = getValue(PRIMARY_MODEL);
//		
//		/*******************
//		 * MARKET MODELS
//		 *******************/
//		for (ModelType modelType : ModelType.values()) {
//			// models here is a comma-separated list
//			String models = getValue(modelType);
//			if (models != null) {
//				if (!models.isEmpty()) {
//					if (models.endsWith(",")) {
//						// remove any extra appended commas
//						models = models.substring(0, models.length() - 1);
//					}
//					String[] configs = models.split("[,]+");
//					
//					if (configs.length > 1) {
//						// if > 1, # model type = # of items in the list
//						// check if there are NONE or 0 of this model
////						data.numModelType.put(modelType, configs.length);
////					} else if (!models.equals(Consts.MODEL_CONFIG_NONE) && 
////							!models.equals("0")) {
////						data.numModelType.put(modelType, configs.length);
//					} else {
////						data.numModelType.put(modelType, 0);
//					}
//				}
//			}
//		}
//		
//		/*******************
//		 * CONFIGURATION - add environment agents
//		 *******************/
//		for (AgentType agentType : Consts.AgentType.values()) {
//			String num = getValue(agentType);
//			String setup = getValue(agentType + Consts.SETUP_SUFFIX);
//			if (num != null) {
//				int n = Integer.parseInt(num);
//				EntityProperties op = getStrategyParameters(agentType, setup);
//				AgentPropsPair a = new AgentPropsPair(agentType, op);
////				data.addEnvAgentNumber(a, n);
//			}
//		}
//
//		/*******************
//		 * ASSIGNMENT - add players
//		 *******************/
//		for (String role : Collections.singleton("EX_ROLE")/*Consts.roles*/) {
//			Object strats = assignments.get(role);
//			if (strats != null) {			
//				@SuppressWarnings("unchecked")
//				ArrayList<String> strategies = (ArrayList<String>) strats;
//				for (String strat : strategies) {
//					if (!strat.equals("")) {
//						String[] as = strat.split("[:]+");	// split on colon
//						if (as.length != 2) {
//							log(ERROR, this.getClass().getSimpleName() + 
//									"::setRolePlayers: " + "incorrect strategy string");
//						} else {
//							// first elt is agent type, second elt is strategy
//							AgentType type = AgentType.valueOf(as[0]);
//							EntityProperties op = getStrategyParameters(type, as[1]);
////							data.addPlayerProperties(new AgentPropsPair(type, op));
//						}
//					}
//				}
//			}
//		}
//	}
//	
//	/**
//	 * Gets the value associated with a given key in the JSONObject.
//	 * 
//	 * @param key
//	 * @return
//	 */
//	public String getValue(String key) {
//		if (params.containsKey(key)) {
//			return (String) params.get(key);
//		} else {
//			return null;
//		}
//	}
//	
//	public String getValue(ModelType key) {
//		return getValue(key.toString());
//	}
//	
//	public String getValue(AgentType key) {
//		return getValue(key.toString());
//	}
//
//	/**
//	 * Wrapper method because log is not static.
//	 * 
//	 * @param type
//	 * @param strategy
//	 * @return
//	 */
//	private EntityProperties getStrategyParameters(AgentType type, String strategy) {
//		EntityProperties op = PlayerReference.getAgentProperties(type, strategy);
//		
//		if (op == null) {
//			log(ERROR, this.getClass().getSimpleName() + 
//					"::getStrategyParameters: error parsing " + strategy.split("[_]+"));
//		}
//		return op;
//	}
//	
//	
//	/**
//	 * Gets properties for an entity. Will overwrite default ObjectProperties set in
//	 * Consts. If the entity type indicates that the entity is a player in a role, 
//	 * this method parses the strategy, if any, in the simulation spec file.
//	 *
//	 * @param type
//	 * @param strategy
//	 * @return ObjectProperties
//	 */
//	public static EntityProperties getAgentProperties(AgentType type, String strategy) {
//		EntityProperties p = null; // new ObjectProperties(Consts.getProperties(type));
////		p.put(Keys.STRATEGY, strategy);
//		
//		if (strategy == null) return p;
//		
//		// Check that strategy is not blank
//		if (!strategy.equals("") /*&& !type.equals(Consts.AgentType.DUMMY)*/) {
//			String[] stratParams = strategy.split("[_]+");
//			if (stratParams.length % 2 != 0) {
//				return null;
//			}
//			for (int j = 0; j < stratParams.length; j += 2) {
//				p.put(stratParams[j], stratParams[j+1]);
//			}
//		}
//		return p;
//	}
//	
//	public static EntityProperties getAgentProperties(String type, String strategy) {
//		return getAgentProperties(AgentType.valueOf(type), strategy);
//	}
//}
