package event;

import java.util.Comparator;

/**
 * Comparator for the EventQueue priority queue.
 * 
 * @author ewah 
 */
public class EventComparator implements Comparator<Event> {
	
	public int compare(Event e1, Event e2) {
		// assume neither event is null
		if ((e1 == null) || (e2 == null)) {
			// error handling
			System.err.println("null event");
		}
		return e1.getTime().compareTo(e2.getTime());
	}
}
