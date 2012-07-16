/**
 *  $Id: Sequence.java,v 1.5 2003/11/06 19:49:55 omalley Exp $
 *  Copyright (c) 2002 University of Michigan. All rights reserved.
 */

package systemmanager;

/**
 * The Sequence class is used to hold sequences of unique values.
 */
public class Sequence {

	int m_value;
	static private int m_initialValue;

	/**
	 * Set the initial sequence value.
	 * @param n starting sequence value
	 */
	public Sequence(int n) {
		m_value = n;
		m_initialValue = n;
	}

	/**
	 * Reset the sequence.
	 */
	public synchronized void reset() {
		m_value = m_initialValue;
	}
	/**
	 * Add one to the sequence.
	 */
	public synchronized int increment() {
		return m_value++;
	}
	/**
	 * Subtract one from the sequence.
	 */
	public synchronized int decrement() {
		return m_value--;
	}
	/**
	 * Get the sequence value.
	 */
	public synchronized int get() {
		return m_value;
	}
}

