/** 
 * Copyright Information goes here
 */
package activity.market;

import java.util.*;

/**
 * An Exception thrown by the during the construction of a Market Entity
 */
public class IllegalMarketParametersException extends Exception 
{
	/**
	 * @param s  A string to be attached to the Exception
	 */
	public IllegalMarketParametersException (String s)
	{
		super (s);
	}

	public IllegalMarketParametersException ()
	{
		super ();
	}
}  
