package event;

import java.util.Comparator;

/**
 * @author elaine
 *
 * Comparator for the EventQueue priority queue.
 */
public class EventComparator implements Comparator<Event>{
	
	@Override
	public int compare(Event e1, Event e2) {
		
		// assume neither event is null?
		if ((e1 == null) || (e2 == null)) {
			// error handling
		}
		return e1.getTime().compareTo(e2.getTime());
	}
}
