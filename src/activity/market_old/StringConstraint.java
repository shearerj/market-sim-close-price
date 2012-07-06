/*
 * The contents of this file are subject to the Locomotive Public License 
 * (LPL), a derivative of the Mozilla Public License, version 1.0. You 
 * may not use this file except in compliance with the License; You may 
 * obtain a copy of the License at http://www.locomotive.org/license/.
 * 
 * Software distributed under the License is distributed on an "AS IS" basis, 
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License 
 * for the specific language governing rights and limitations under the 
 * License. 
 *
 * The initial developers of this code under the LPL is Leverage Information 
 * Systems.  Portions created by Leverage are Copyright (C) 1998 Leverage 
 * Information Systems. All Rights reserved.
 */
package activity.market;

import java.util.*;

/**
 * This is a general, powerful way to constrain String values and generate
 * errors on specific criteria.
 * Here's an example of usage:
 * <pre>
 *     String username = (String) hd.form_hash.get("USERNAME");
 *     StringConstraint sc = new StringConstraint("USERNAME",
 *         "DEFINED TIDY NO_WHITESPACE ISALPHANUM " +
 *         "LENGTH_GREATERTHAN=1 MAXSIZE=" + MAX_USERNAME_LENGTH + 
 *         " WARNTRUNC");
 *     StringBuffer errors = new StringBuffer();
 *
 *     // this will append some error messages to errors, and also return a
 *     // new String which is a cleaned-up verison of the username.
 *     username = sc.check(username, errors);
 * </pre>
 * The constraints are performed or evaluated in order.  Here are all the
 * valid constraints, all specified as words within the String that is 
 * the second argument in the constructor:
 * <p>
 *  <B>Constraints which result in errors:</B><p>
 * <ol>
 * <li>DEFINED - hash variable with associated name must be defined, 
 *  ie, the string is not set to null <p>
 * <li>NO_WHITESPACE - requires string to be solid word <p>
 * <li>NOTNULL - require that the string is not the null string (""), 
 *  (must be defined in order for this check to be run)<p>
 * <li>LENGTH_GREATERTHAN=## - requires size to be larger than the integer
 *  argument included <p>
 * <li>WARNTRUNC - error if the MAXSIZE action resulted in resizing (chopping) 
 *  the string <p>
 * <li>ISINT -  requires the string to contain the string value of an int <p>
 * <li>ISINTLIST - requires that the string contain a comma-separated list
 *     of integers 
 * <li>ISFLOAT -  requires the string to contain a parseable float <p>
 * <li>ISDATE - requires the string to contain a date (MM-DD-YY)
 *  (use timestamp)<p>
 * <li>ISTIMESTAMP - requires the string to contain valid Timestamp
 *  which may have any of the following four forms:<p>
 *  (YY-MM-DD) (YY-MM-DD HH:MM:SS)
 *  (YYYY-MM-DD) (YYYY-MM-DD HH:MM:SS)<p>
 * <li>ISALPHANUM - requires the string to contain alphanumerics only <p>
 * <li>ISEMAIL - requires the string to contain an email address 
 *  (user@domain.type) <p><p>
 * </ul>
 *  <B>Constraints which modify the input string:</B><BR>
 * <ul>
 * <li>TIDY - chops off any white space on either side of string<p>
 * <li>MAXSIZE=##   - chops off the end of the string if to large<p>
 * </ul>
 *  <B>Constraints which affect total processing behavior:</B><BR>
 * <ul>
 * <li>IFNOTNULL - constraint application will continue only if string is  
 *  not the null string ("")<p>
 * <li>IFDEFINED - constraint application will continue only if string is  
 *  defined (ie, var itself is not the null pointer)<p>
 *</ul>
 * @version     %I%, %G%
 * @since       LAS 2.0
 */
public class StringConstraint
{
    private String proper_name = "";
    private int total = 0;
    private String order_array[];
    private Hashtable constraint_hash; 
    private Hashtable msg_hash;
    private boolean truncated = false;
    private StringBuffer error_sb;

    /**
     * See the documentation at the top of this class for an example.
     * @param name the name of this field, to be used in an error message.
     * @param cons_list_string a string containing all the constraints.
     */
    public StringConstraint (String name, String cons_list_string)
    {
	total = 0;
	order_array = new String[20];
	proper_name = name;
	truncated = false;
	msg_hash = new Hashtable ();
	constraint_hash = new Hashtable();
	error_sb = new StringBuffer();

	initializeMessages();
	
	StringTokenizer st = new StringTokenizer (cons_list_string);
	while (st.hasMoreTokens())
	{
	    add ( st.nextToken ());	    
	}
    }

    /** Use the specified message instead of the default.  */
    public void overrideMessage(String constraint, String msg)
    {
	msg_hash.put (constraint, msg);
    }

    private void initializeMessages()
    {
	msg_hash.put("NO_WHITESPACE", 
		     proper_name + " must be single word or value<BR>\n");
	msg_hash.put("DEFINED", "No " + proper_name 
		     + " was specfied (notdef)<BR>\n");
	msg_hash.put("NOTNULL", "No " + proper_name 
		     + " was specfied (null)<BR>\n");
	msg_hash.put("LENGTH_GREATERTHAN", 
		      proper_name + " value is too short<BR>\n");
	msg_hash.put("WARNTRUNC", 
		     proper_name + " is too long and was truncated<BR>\n");
	msg_hash.put("ISINT", proper_name + " must be an integer<BR>\n");
	msg_hash.put("ISLONG",proper_name + " must be a long<BR> \n");
	msg_hash.put("ISINTLIST", proper_name + 
		     " must be an integer list <BR>\n");
	msg_hash.put("ISFLOAT",proper_name+" must be a parseable float<BR>\n");
	msg_hash.put("ISDATE", proper_name + " must be a valid date "
		     +"(MM-DD-YY)<BR>\n");
	msg_hash.put("ISTIMESTAMP", proper_name + " must be a valid timestamp:"
		     +" (YY-MM-DD), (YYYY-MM-DD), (YY-MM-DD HH:MM:SS) or " + 
		     "(YYYY-MM-DD HH:MM:SS) <BR>\n");
	msg_hash.put("ISALPHANUM", proper_name + " must contain alphanumerics "
		     +"only. (characters A-Z, a-z, or 0-9)<BR>\n");
	msg_hash.put("ISEMAIL", proper_name + " must be a valid email address "
		     +"( user@domain ) <BR>\n");
/*	msg_hash.put("INTEGERSETTEXT", proper_name + 
		     " (Integer Set Text) has errors in syntax: ");
	msg_hash.put("SIMPLEDATESETTEXT", proper_name + 
		     " (SimpleDateSetText) has errors in syntax: "); */
    }


    /** add a new constraint.  See the constructor for the list of constraints
     */
    public void add (String newconstraint)
    {
	StringTokenizer st = new StringTokenizer (newconstraint, "=");
	String key = null; 
	String value = "0";

	if (st.hasMoreTokens() )
	{
	    key = st.nextToken();
	}

	if (st.hasMoreTokens())
	{
	    value = st.nextToken();
	}

	if ((key != null) && (total < 20))
	{
	    order_array[total] = key;
	    constraint_hash.put (key, value);
	    total++;
	}
    }


    /** check all the values of a hash table against a hashtable containing
     * a list of constraints.
     * @param check_hash the hastable to check
     * @param constraints_hash a hastable containing StringConstraints,
     *        whose keys are the names of the keys in check_hash.
     */
    public static String checkHashWithConstraints (Hashtable check_hash, 
					   Hashtable constraints_hash)
    {
	StringBuffer sb_errors = new StringBuffer("");
	String c_key;

	Enumeration e = constraints_hash.keys();
	StringConstraint scons;
	String value_to_check;

	while (e.hasMoreElements())
	{
	    c_key = (String) e.nextElement();

	    scons = (StringConstraint) constraints_hash.get(c_key);
	    value_to_check = (String) check_hash.get (c_key);

	    value_to_check = scons.check (value_to_check, sb_errors);

	    // put constrained string (may have been modifed) 
	    // back in for later use
	    if (value_to_check != null)
	    {
		check_hash.put(c_key, value_to_check);
	    }
	}
	return (sb_errors.toString());
    }
    
    /** Check a string with the present set of constraints.  If there
     * are errors, they will be appended to the StringBuffer.
     * @return the string to be checked, which may have been modified by the 
     * constraints which were applied.
     */
    public String check (String stocheck, StringBuffer error_sb)
    {
	// check incoming string to see if it matches constraint list.
	// if it doesnot, it adds errors to the StringBuffer, and 
	// may or may not modify stocheck

	String constraint;

	for (int i = 0; i < total; i++)
	{
	    constraint = order_array[i];

	    if (constraint.equals("DEFINED"))
	    {
		error_sb.append (checkForDefined (stocheck));
	    }
	    else if (constraint.equals("IFDEFINED"))
	    {
		if (earlyExitIfNotDefined (stocheck))
		{
		    return (stocheck);
		}
	    }
	    else if (constraint.equals ("IFNOTNULL") && (stocheck != null))
	    {
		if (earlyExitIfNull (stocheck))
		{
		    return (stocheck);
		}
	    }
	    else if (constraint.equals ("NOTNULL") 
		     && (stocheck != null))
	    {
		error_sb.append (checkForNotNull (stocheck));
	    }
	    else if (constraint.equals ("TIDY")
		     && (stocheck != null))
	    {
		stocheck = stocheck.trim();
	    }
	    else if (constraint.equals ("NO_WHITESPACE") 
		     && (stocheck != null))
	    {
		error_sb.append (checkForWhitespace (stocheck));
	    }
	    else if (constraint.equals ("LENGTH_GREATERTHAN") 
		     && (stocheck != null))
	    {
		error_sb.append (checkForLengthGT (stocheck));
	    }
	    else if (constraint.equals ("MAXSIZE") 
		     && (stocheck != null))
	    {
		// chop end off if too long, but mark as truncated
		stocheck = checkForMaxSize (stocheck);
	    }
	    else if (constraint.equals ("ISFLOAT")
		     && (stocheck != null))
	    {
		error_sb.append (checkForIsFloat (stocheck) );
	    }
	    else if (constraint.equals ("ISLONG") 
		     && (stocheck != null))
	    {
		error_sb.append (checkForIsLong (stocheck) );
	    }	    
	    else if (constraint.equals ("ISINT") 
		     && (stocheck != null))
	    {
		error_sb.append (checkForIsInt (stocheck) );
	    }
	    else if (constraint.equals ("ISINTLIST") 
		     && (stocheck != null))
	    {
		error_sb.append (checkForIsIntList (stocheck) );
	    }
	    else if (constraint.equals ("ISDATE") 
		     && (stocheck != null))
	    {
		error_sb.append (checkForIsDate (stocheck) );
	    }
/*	    else if (constraint.equals ("ISTIMESTAMP") 
		     && (stocheck != null))
	    {
		error_sb.append (checkForIsTimestamp (stocheck) );
	    }  */
	    else if (constraint.equals ("ISALPHANUM") 
		     && (stocheck != null))
	    {
		error_sb.append (checkForIsAlphanum (stocheck));
	    }
	    else if (constraint.equals ("ISEMAIL") 
		     && (stocheck != null))
	    {
		error_sb.append (checkForIsEmail (stocheck) );
	    }
	    else if (constraint.equals ("WARNTRUNC") 
		     && (stocheck != null))
	    {
		// check for truncation from above "lessthan"
		error_sb.append (checkForTruncation () );
	    }
/*	    else if (constraint.equals ("INTEGERSETTEXT") && 
		     (stocheck != null))
	    {
		error_sb.append (checkForIntegerSetText (stocheck));
	    }
	    else if (constraint.equals ("SIMPLEDATESETTEXT") && 
		     (stocheck != null))
	    {
//		error_sb.append (checkForSimpleDateSetText (stocheck));
	    } */
	    else 
	    {
		error_sb.append ("StringConstraint: unimplemented type: " +
				    constraint);
	    }
	}

	return (stocheck);
    }

/*    private String checkForIntegerSetText (String s)
    {
	IntegerSetText ist = new IntegerSetText (s);
	
	if (! ist.isValid ())
	{
	    return ((String) msg_hash.get ("INTEGERSETTEXT") + 
		    ist.formatErrorVector() );
	}

	return ("");
    }
*/

/*    private String checkForSimpleDateSetText (String s)
    {
	SimpleDateSetText sdst = new SimpleDateSetText (s);
	
	if (! sdst.isValid ())
	{
	    return ((String) msg_hash.get ("SIMPLEDATESETTEXT") + 
		    sdst.formatErrorVector() );
	}

	return ("");
    }
    */

    private String checkForTruncation ()
    {
	if (truncated)
	{
	    return ((String) msg_hash.get("WARNTRUNC"));
	}
	return ("");
    }

    private String checkForWhitespace(String s)
    {
	char chars[] = s.toCharArray();
	
	for (int i = 0; i < chars.length; i++)
	{
	    if (chars[i] <= ' ')
	    {
		return ((String) msg_hash.get("NO_WHITESPACE"));
	    }
	}
	
	return ("");
    }

    private String checkForLengthGT(String s)
    {

	int lenval = Integer.parseInt ((String) 
			       constraint_hash.get("LENGTH_GREATERTHAN"));

	if (s.length() <= lenval)
	{
	    return ((String) msg_hash.get("LENGTH_GREATERTHAN"));
	}
	return ("");
    }

    private String checkForMaxSize(String s)
    {
	int lenval = Integer.parseInt ((String) 
			       constraint_hash.get("MAXSIZE"));

	if (s.length() > lenval)
	{
	    truncated = true;
	    return (s.substring(0, lenval));
	}
	return (s);
	
    }

    private String checkForIsFloat(String s)
    {
	try 
	{
	   Float.parseFloat (s);
	}
	catch (NumberFormatException nfe)
	{
	    return ((String) msg_hash.get("ISFLOAT"));
	}
	return ("");	
    }

    private String checkForIsInt(String s)
    {
	try 
	{
	    if (! s.equals (String.valueOf(Integer.parseInt(s))))
	    {
		return ((String) msg_hash.get("ISINT"));
	    }
	}
	catch (NumberFormatException nfe)
	{
	    return ((String) msg_hash.get("ISINT"));
	}
	return ("");	
    }
    private String checkForIsLong(String s)
    {
	try 
	{
	    if (! s.equals (String.valueOf(Long.parseLong(s))))
	    {
		return ((String) msg_hash.get("ISLONG"));
	    }
	}
	catch (NumberFormatException nfe)
	{
	    return ((String) msg_hash.get("ISLONG"));
	}
	return ("");	
    }
    private String checkForIsIntList(String s)
    {
	int array[] = parseIntList (s);  // null if bogus
	if (array == null)
	{
	    return ((String) msg_hash.get ("ISINTLIST"));
	}

	return ("");
    }

    private String checkForIsAlphanum (String s)
    {
	// note it may be faster to grab the whole char array, rather than 
	// use s.charAt()
	// Also, Java has many Character class routines which might 
	// help, but we in the interest of speed and simplicity we'll only
	// deal with ASCII now
	// we might want to revisit this later
	
	char c;
	for (int i = 0; i < s.length(); i++)
	{
	    c = s.charAt (i);

	    if (! (((c >= 'A') && (c <= 'Z')) ||
		((c >= 'a') && (c <= 'z')) ||
		((c >= '0') && (c <= '9'))))
	    {
		return ((String) msg_hash.get ("ISALPHANUM"));
	    }
	}

	return ("");
    }

    private String checkForIsEmail (String s)
    {
	// start real basic here with "username@domain.tld"
	// this may need to get squirrelly

	// check characters first
	char c;
	for (int i = 0; i < s.length(); i++)
	{
	    c = s.charAt(i);
	    
	    if (! ( ((c >= 'a') && (c <= 'z')) ||
		    ((c >= 'A') && (c <= 'Z')) ||
		    ((c >= '0') && (c <= '9')) || 
		    (c == '+') || (c == '@') || (c == '.') || (c == '\'') ||
		    (c == '%') || (c == '-') || (c == '_') || (c == '!') ))
	    {
		return ( (String) msg_hash.get ("ISEMAIL"));
	    }
	}

	// make sure we have userpart@hostpart, there can be only one "@"
	StringTokenizer st = new StringTokenizer (s, "@");

	if (st.countTokens() != 2)
	{
	    return ( (String) msg_hash.get ("ISEMAIL"));
	}

	// domainname should have at least one period
	st.nextToken();
	String domainname = st.nextToken();

	StringTokenizer ost = new StringTokenizer (domainname, ".");

	if (ost.countTokens() < 2)
	{
	    return ( (String) msg_hash.get ("ISEMAIL"));
	}
	return ("");
    }

/*    private String checkForIsTimestamp (String s)
    {
	try 
	{
	    SQL.stringToTimestamp (s);
	}
	catch (IllegalArgumentException iae)
	{
	    return ((String) msg_hash.get("ISTIMESTAMP"));		
	}

	return ("");
    }   */

    // use Timestamp instead
    private String checkForIsDate(String s)
    {
	// makes sure the date is in the format MM-DD-YY

	// check length
	if (s.length() != 8)
	{
	    return ((String) msg_hash.get("ISDATE"));
	}

	// make sure we have three hyphen separated fields
	StringTokenizer st = new StringTokenizer (s, "-");
	if (st.countTokens() != 3)
	{
	    return ((String) msg_hash.get("ISDATE"));
	}

	String tokens[] = new String [3];
	int values[] = new int [3];

	// make sure each field contains a number and fill the number array
	for (int i = 0; i < 3; i++)
	{
	    tokens[i] = st.nextToken();
	    if ((tokens[i].charAt(0) == 48) && (tokens[i].length() > 1))
	    {
		tokens[i] = tokens[i].substring(1);
	    }

	    try 
	    {
		values[i] = Integer.parseInt(tokens[i]);
	    }
	    catch (NumberFormatException nfe)
	    {

	    }
	    
	    if (! tokens[i].equals (String.valueOf (values[i])))
	    {
		return ((String) msg_hash.get("ISDATE"));
	    }
	}
	
	// check for valid months
	if ((values[0] > 13) || (values[0] < 1))
	{
	    return ((String) msg_hash.get("ISDATE"));
	}

	// check days
	if ((values[1] > 31) || (values[1] < 1))
	{
	    return ((String) msg_hash.get("ISDATE"));
	}

	// check years
	if ((values[2] > 99) || (values[2] < 0))
	{
	    return ((String) msg_hash.get("ISDATE"));
	}

	return ("");	
    }

    private boolean earlyExitIfNull (String s)
    {
	if (s.equals(""))
	{
	    return (true);
	}
	return (false);
    }

    private boolean earlyExitIfNotDefined (String s)
    {
	if (s == null)
	{
	    return (true);
	}
	return (false);
    }

    private String checkForDefined(String s)
    {
	if (s == null)
	{
	    return ((String) msg_hash.get("DEFINED"));
	}

	return ("");
    }

    private String checkForNotNull (String s)
    {
	if (s.equals(""))
	{
	    return ((String) msg_hash.get("NOTNULL"));
	}

	return ("");
    }

    public String getProperName()
    {
	return(proper_name);
    }

    /**
     * parses an integer list, which is defined as a String containing
     * one or more parseable Integers, separated by a comma and zero or more 
     * whitespace characters (space or tab), but no newlines. 
     * @returns a new int array or null if the input string is malformed
     * @param s  a String containing the integer list
     */
    public static int[] parseIntList (String s)
    {
	if (s == null || s.equals(""))
	{
	    return (null);
	}

	Vector v = new Vector();
	StringTokenizer st = new StringTokenizer (s, ",");

	// split string on commas
	// try to parse as int
	// if it works, add it to the vector
	// if it fails, return null	
	Integer i; 
	while (st.hasMoreTokens())
	{
	    String tok = st.nextToken();

	    try
	    {
		i = new Integer (tok);
	    }
	    catch (NumberFormatException nfe)
	    {
		return (null);
	    }

	    v.add (i);
	}
	
	// if we've gotten through the whole list of tokens, 
	// we should have a vector with a heap of integers
	// size the vector, create an array of ints, 
	// then for each object in the vector, put the int value into the array
	// of ints

	int size = v.size();
	int array[] = new int[size];

	for (int index = 0; index < size; index++)
	{
	    array[index] = ((Integer) v.get (index)).intValue();
	}

	return (array);
    }
}

