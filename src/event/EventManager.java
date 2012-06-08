package event;

import activity.*;

import java.util.List;
import java.util.ListIterator;

/**
 * This class is responsible for updating and managing the Event priority queue.
 * This involves inserting new events as well as removing them when execution
 * is complete.
 * 
 * @author ewah
 */
public class EventManager {

	private EventQueue eventQueue;
	
	/**
	 * Constructor
	 */
	public EventManager() {
		eventQueue = new EventQueue();
	}
	
	/**
	 * Returns time (i.e. priority) of event at head of event queue
	 * @return
	 */
	public TimeStamp getCurrentTime() {
		return this.eventQueue.peek().getTime();
	}
	
	
	/**
	 * Adds an event to the event queue
	 * @param e		Event to be added
	 */
	public void addEvent(Event e) {
		this.eventQueue.add(e);
	}
	
	/**
	 * Checks if event queue is empty
	 * @return		boolean
	 */
	public boolean isEventQueueEmpty() {
		return this.eventQueue.isEmpty();
	}
	
	/**
	 * Executes event at head of Q. Removes from Q only when execution is complete.
	 * Adds new events to EventQ after event completion.
	 */
	public void executeCurrentEvent() {

		Event toExecute = this.eventQueue.peek();
		List<Event> eventsToAdd = toExecute.executeAll();
		System.out.println("event " + toExecute.getTime().toString() + " has been executed");
		
		// Check if event has completed execution
		if (toExecute.isCompleted()) {
			this.eventQueue.remove(toExecute);
		} else {
			// ERROR - TODO
			System.out.println("Some error has occurred.");
		}
		
		// then add the eventsToAdd to the EventQueue
		if (!eventsToAdd.isEmpty()) {
			// iterate through the List
			ListIterator<Event> itr = eventsToAdd.listIterator();
			while (itr.hasNext()) {
				Event e = itr.next();
				this.eventQueue.add(e);
			}
		}
		// then call function to reorganize the Q to make sure that only one Event of each priority exists
		
	}

}
