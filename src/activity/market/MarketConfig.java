/**
 * $Id: MarketConfig.java,v 1.37 2005/03/29 18:19:54 chengsf Exp $
 */
package activity.market;

import java.lang.reflect.Field;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;

/**
 * MarketConfig object takes a hashtable of param:value pairs and reads the
 * pairs into member variables after checking defined constraints on the vars
 * <p/>
 * Special case is buyerIDs and sellerIDs, where these are read into
 * the AuctionDirectory directory, from which permissions can be checked
 * <p/>
 * When adding new auction parameters, two entries must be added
 * <ol>
 * <li> define the member variable (under --PARAMS--), which must be public
 * <li> add an entry with constraints to the static array at --CONSTRAINTS--
 * </ol>
 */
public class MarketConfig {

	public static boolean DEBUG = false;

	/* implementation specific variables */
	public Hashtable raw_values = null;
	private static Hashtable constraintMap;
	public int auction_imp_bid_queue_length = 12;
	public int auction_imp_query_queue_length = 8;
	public int auction_imp_event_queue_length = 12;
	public int orderbook_imp_default_size = 120;

	//public AuctionDirectory directory; // TODO -- need to handle this

	//constraint definitions
	static String cst_for_string = "DEFINED TIDY NOTNULL NO_WHITESPACE MAXSIZE=9999";
	static String cst_for_opt_string = "IFDEFINED TIDY IFNOTNULL MAXSIZE=9999";
	static String cst_for_intlist = "DEFINED TIDY ISINTLIST";
	static String cst_for_int = "DEFINED TIDY ISINT";
	static String cst_for_opt_int = "IFDEFINED TIDY IFNOTNULL ISINT";
	static String cst_for_float = "DEFINED TIDY ISFLOAT";
	static String cst_for_opt_float = "IFDEFINED TIDY IFNOTNULL ISFLOAT";
	static String cst_for_long = "DEFINED TIDY ISLONG";
	static String cst_for_opt_long = "IFDEFINED TIDY ISLONG";

	//  --PARAMS--
	//  public String auction_bid_language = "pq";  //{"pq"}
	public String bid_dominance_sell = "none";  //{"ascending", "descending", "none"}
	public String bid_dominance_buy = "none";  //{"ascending", "descending", "none"}
	public int bid_btq = 0;                    //{0,1}
	public int bid_btq_strict = 0;             //{0,1}
	public int bid_btq_delta = 0;              //{any positive real number}
	public String matching_fn = "uniform";     //{"uniform", "earliest"}
	public float pricing_k = 0;                //0 = at bid, 1 = at ask
	public String sellerIDs;                   //{integers}, format = 0:1:2:...
	public String buyerIDs;                    //{integers}, format = 0:1:2:...
	public String name;                        //{any string}
	public String description;                 //{any string}
	public String xor_quote_type = "divisible";
	public int quote_non_anonymous = 0;
	public int xor_quote_quantity = 1;
	public int quote_include_hqw = 1;

	//--CONSTRAINTS--
	/**
	 * defines the constraints on parameters
	 */
	String constraints[] =
		{
			"name", cst_for_opt_string,
			"description", cst_for_opt_string,
			"auction_bid_language", cst_for_string,
			"bid_dominance_sell", cst_for_opt_string,
			"bid_dominance_buy", cst_for_opt_string,
			"xor_quote_type", cst_for_opt_string,
			"xor_quote_quantity", cst_for_opt_int,
			"sellerIDs", cst_for_string,
			"buyerIDs", cst_for_string,
			"bid_btq", cst_for_opt_int,
			"bid_btq_strict", cst_for_opt_int,
			"bid_btq_delta", cst_for_opt_int,
			"matching_fn", cst_for_string,
			"pricing_k", cst_for_opt_int,
			"quote_include_hqw", cst_for_opt_int,
			"quote_non_anonymous", cst_for_opt_int

		};

	/**
	 * Create a new MarketConfig.  Names and values
	 *
	 * @param configs A hashtable with names and String values
	 * @throws IllegalMarketParametersException
	 *          when the XML String s
	 *          contains parameters that violate constraints or are invalid
	 */
	public MarketConfig(Hashtable configs, Hashtable bidderAttributes)
			throws IllegalMarketParametersException {
		constraintMap = new Hashtable(constraints.length);
		for (int i = 0; i < constraints.length; i += 2)
			constraintMap.put(constraints[i], constraints[i + 1]);

		if (configs == null || configs.isEmpty()) {
			throw new IllegalMarketParametersException("null configs");
		}

		try {
			validateValues(configs);
			setupDirectory(bidderAttributes);
		} catch (NoSuchFieldException e) {
			throw new IllegalMarketParametersException("error validating configs\n" + e.toString());

		} catch (IllegalAccessException e) {
			throw new IllegalMarketParametersException("error validating configs\n" + e.toString());
		}
	}

	/**
	 * set up the directory of permissions based on buyerIDs and sellerIDs
	 *
	 * @throws IllegalMarketParametersException
	 *
	 */

	public void setupDirectory(Hashtable bidderAttributes) throws IllegalMarketParametersException {
//		directory = new AuctionDirectory();  // TODO -- need to deal with the seller/bidder IDs properly
		StringTokenizer st = null;
		int cnt = 0;
		// Get and set sellers.

		if (sellerIDs != null) {
			st = new StringTokenizer(sellerIDs, ":");
			while (st.hasMoreTokens()) {
				int n = 0;
				try {
					n = Integer.parseInt(st.nextToken());
				} catch (NumberFormatException e) {
					throw new IllegalMarketParametersException("seller ids nan: " + e);
				}
//				directory.letSell(n);
				cnt++;
			}
			if (cnt == 0) {
				throw new IllegalMarketParametersException("no seller ids in auction parameters");
			}
		}
		cnt = 0;
		// Get and set buyers.
		if (buyerIDs != null) {
			st = new StringTokenizer(buyerIDs, ":");
			while (st.hasMoreTokens()) {
				int n = 0;
				try {
					n = Integer.parseInt(st.nextToken());
				} catch (NumberFormatException e) {
					throw new IllegalMarketParametersException("buyer ids nan: " + e);
				}
//				directory.letBuy(n);
				cnt++;
			}
			if (cnt == 0) {
				throw new IllegalMarketParametersException("no buyer ids in auction parameters");
			}
		}
		if (bidderAttributes != null)
			for (Enumeration e = bidderAttributes.keys(); e.hasMoreElements();) {
				String key = (String) e.nextElement();
//				directory.addAttribute(key,
//						new Integer((String) bidderAttributes.get(key)).intValue());
			}
	}

	/**
	 * validate the parameters based on the static constraints defined above
	 *
	 * @param newvalues hashtable of unprocessed param:value pairs
	 * @throws IllegalMarketParametersException
	 *                                if value is not valid
	 * @throws NoSuchFieldException   if parameter doesn't exist
	 * @throws IllegalAccessException if we access a private member by accident
	 */
	public void validateValues(Hashtable newvalues)
			throws IllegalMarketParametersException,
			NoSuchFieldException, IllegalAccessException {
		if (raw_values == null)
			raw_values = new Hashtable();

		StringBuffer errors = new StringBuffer();
		String param, val, constraint;
		StringConstraint sc;
		Field field;

		/* validate each parameter.  Errors go in the string buffer,
    altered values clobber original in raw_values */
		Enumeration Enum = newvalues.keys();
		while (Enum.hasMoreElements()) {
			param = (String) Enum.nextElement();
			val = (String) newvalues.get(param);
			constraint = (String) constraintMap.get(param);
			if (constraint == null)
				throw new IllegalMarketParametersException("unknown parameter: " + param);
			debug("param/val/constraint:  " + param + "/" + val + "/" + constraint);
			sc = new StringConstraint(param, constraint);
			raw_values.put(param, sc.check(val, errors));
		}
		if (errors.length() > 0) {
			throw new IllegalMarketParametersException(errors.toString());
		}

		Enumeration en = raw_values.keys();
		while (en.hasMoreElements()) {
			param = (String) en.nextElement();
			val = (String) raw_values.get(param);
			constraint = (String) constraintMap.get(param);

			if (constraint == null)
				throw new IllegalMarketParametersException("constraint not defined for " + param);

			field = this.getClass().getField(param);

			//don't set if val is null, or empty string and we're not setting a string
			if ((val == null) || ((val == "") && (constraint != cst_for_string)))
				continue;

			//set the member field
			if ((cst_for_opt_string == constraint) || (cst_for_string == constraint))
				field.set(this, val);
			else if ((cst_for_int == constraint) || (cst_for_opt_int == constraint))
				field.setInt(this, Integer.parseInt(val));
			else if ((cst_for_float == constraint) || (cst_for_opt_float == constraint))
				field.setFloat(this, Float.parseFloat(val));
			else if ((cst_for_long == constraint) || (cst_for_opt_long == constraint))
				field.setLong(this, Long.parseLong(val));
			else if (cst_for_intlist == constraint)
				field.set(this, StringConstraint.parseIntList(val));
		}
	}

	public boolean validateValue(String param, String val) {
		param = param.trim();
		if (DEBUG) System.out.println("validating: " + param + "," + val);
		if (param == null) System.out.println("null param " + param + val);
		if (constraintMap == null) System.out.println("null constraintmap");
		String constraint = (String) constraintMap.get(param);

		if (constraint == null) {
			System.out.println("null constraint");
			return false;
		}

		StringBuffer errors = new StringBuffer();

		try {
			Field field = MarketConfig.class.getField(param);
		} catch (Exception e) {
			System.out.println("error getting field");
			e.printStackTrace();  //To change body of catch statement use Options | File Templates.
			return false;
		}
		StringConstraint sc = new StringConstraint(param, constraint);
		sc.check(val, errors);

		if (errors.length() > 0) {
			System.out.println("error: " + errors.toString());
			return false;
		}
		return true;
	}


	private void debug(String s) {
		if (DEBUG)
			System.out.println(s);
	}


}

