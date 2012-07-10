package event;

import java.util.PriorityQueue;
import java.util.Comparator;

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
	
}
