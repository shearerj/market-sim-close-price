package event;

import java.util.ArrayList;
import java.util.PriorityQueue;
import java.util.Comparator;
import java.util.Iterator;

/**
 * @author ewah
 *
 * This is essentially a wrapper class for PriorityQueue, where priority 
 * is the TimeStamp at which an event occurs.
 */
public class EventQueue {

	private PriorityQueue<Event> eventQueue;
	private Comparator<Event> eventComparator;
	
	/**
	 * Constructor for the EventQueue, initialized at capacity 1.
	 */
	public EventQueue() {
		eventComparator = new EventComparator();
		eventQueue = new PriorityQueue<Event>(1, eventComparator);
	}
	
	/**
	 * Constructor for the EventQueue where capacity is specified by a parameter. 
	 * @param capacity
	 */
	public EventQueue(int capacity) {
		eventComparator = new EventComparator();
		eventQueue = new PriorityQueue<Event>(capacity, eventComparator);
	}

	/**
	 * Function to add Event to the Q.
	 * @param e
	 * @return
	 */
	public boolean add(Event e) {
		return this.eventQueue.add(e); 
	}
	
	/**
	 * Function to remove (and return) Event from the Q.
	 * @param e
	 * @return
	 */
	public boolean remove(Event e) {
		return this.eventQueue.remove(e);
	}
	
	/**
	 * @return
	 */
	public Event poll() {
		return this.eventQueue.poll();
	}
	
	/**
	 * @return
	 */
	public Event peek() {
		return this.eventQueue.peek();
	}
	
	/**
	 * @return size of the event queue
	 */
	public int size() {
		return this.eventQueue.size();
	}
	
	/**
	 * @return boolean
	 */
	public boolean isEmpty() {
		return this.eventQueue.isEmpty();
	}
	
	/**
	 * Checks if Q contains an Event with the specified TimeStamp already.
	 * 
	 * @param ts
	 * @return Event that matches, otherwise return null 
	 */
	public Event contains(TimeStamp ts) {
		ArrayList<Event> events = new ArrayList<Event>(eventQueue);
		for (Iterator<Event> it = events.iterator(); it.hasNext(); ) {
			Event e = it.next();
			if (e.getTime().getTimeStamp().equals(ts.getTimeStamp())) {
				return e;
			}
		}
		return null;
	}
	
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		String s  = "Q: ";
		PriorityQueue<Event> copy = new PriorityQueue<Event>(this.eventQueue);
		int numEvents = copy.size();
		for (int i = 0; i < numEvents; i++) {
			s += copy.poll().toString() + "// ";
		}
		return s;
	}
}
