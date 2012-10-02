package activity;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.PriorityQueue;

import event.TimeStamp;

/**
 * Class designed to hold ActivityLists in a way that orders them by priority
 * while ensuring that only a single ActivityList exists for each priority. This
 * class assumes that the ActivityList priority is fixed and will not update it
 * under any circumstances.
 * 
 * The combination of the priority queue and the hash map is a work-around to 
 * avoid the tie-breaking issue for PriorityQueues.
 * 
 * @author ewah
 */
public class PriorityActivityList {

	private TimeStamp time;
	private PriorityQueue<Integer> priorityQueue;
	private HashMap<Integer, ActivityList> activityListMap;
	
	/**
	 * Constructor, initialized at capacity 1.
	 */
	public PriorityActivityList() {
		priorityQueue = new PriorityQueue<Integer>(1);
		activityListMap = new HashMap<Integer, ActivityList>();
		time = new TimeStamp(0);
	}
	
	/**
	 * Constructor with a given capacity.
	 * @param capacity
	 */
	public PriorityActivityList(int capacity) {
		priorityQueue = new PriorityQueue<Integer>(capacity);
		activityListMap = new HashMap<Integer, ActivityList>();
		time = new TimeStamp(0);
	}
	
	public PriorityActivityList(ActivityList al) {
		priorityQueue = new PriorityQueue<Integer>(1);
		activityListMap = new HashMap<Integer, ActivityList>();
		activityListMap.put(al.getPriority(), al);
		time = al.getTime();
	}
	
	/**
	 * Copy constructor.
	 * @param a
	 */
	public PriorityActivityList(PriorityActivityList pal) {
		this.priorityQueue = new PriorityQueue<Integer>(pal.priorityQueue);
		this.activityListMap = new HashMap<Integer, ActivityList>();
		for(Map.Entry<Integer, ActivityList> entry : pal.activityListMap.entrySet()) {
			this.activityListMap.put(entry.getKey(), new ActivityList(entry.getValue()));
		}
		this.time = new TimeStamp(pal.time);
	}
	
	/**
	 * Copy constructor that changes the times for all activities in the priority
	 * activity list.
	 * @param ts
	 * @param pal
	 */
	public PriorityActivityList(TimeStamp ts, PriorityActivityList pal) {
		this.priorityQueue = new PriorityQueue<Integer>(pal.priorityQueue);
		this.activityListMap = new HashMap<Integer, ActivityList>();
		for(Map.Entry<Integer, ActivityList> entry : pal.activityListMap.entrySet()) {
			this.activityListMap.put(entry.getKey(), new ActivityList(ts, entry.getValue()));
		}
		this.time = ts;
	}
	
	
	public boolean isEmpty() {
		return priorityQueue.isEmpty();
	}
	
	public void clear() {
		priorityQueue.clear();
		activityListMap.clear();
		time = null;
	}
	
	public TimeStamp getTime() {
		return time;
	}
	
	public void add(ActivityList al) {
		if (!priorityQueue.contains(al.getPriority())) {
			// insert new priority
			priorityQueue.add(al.getPriority());
			activityListMap.put(al.getPriority(), al);
		} else {
			// append to ActivityList at the existing priority
			activityListMap.get(al.getPriority()).addAll(al);
		}	
	}
	
	public void add(Activity a) {
		ActivityList al = new ActivityList(a);
		this.add(al);
	}

	public void add(int priority, Activity a) {
		ActivityList al = new ActivityList(a);
		this.add(al);
	}
	
	public void add(PriorityActivityList pal) {
		// iterate over all items in the object
		PriorityQueue<Integer> copy = new PriorityQueue<Integer>(pal.priorityQueue);
		
		while (!copy.isEmpty()) {
			// access the activity list queue by priority
			Integer priority = copy.poll();
			if (priority != null) {
				this.add(pal.activityListMap.get(priority));
			}
		}
	}
	
	/**
	 * @return iterator over entire internal activity list.
	 */
	public Iterator<Activity> iterator() {
		return this.getActivities().iterator();
	}
	
	/**
	 * Get all activities in the object, sorted by priority.
	 * 
	 * @return ActivityList linked list of activities in this object.
	 */
	public ActivityList getActivities() {
		ActivityList acts = new ActivityList(time);
		PriorityQueue<Integer> copy = new PriorityQueue<Integer>(priorityQueue);

		while (!copy.isEmpty()) {
			// access the activity list queue by priority
			Integer priority = copy.poll();
			if (priority != null) {
				acts.addAll(activityListMap.get(priority));
			}
		}
		return acts;
	}
	
	public String toString() {
		return new String(time + "|" + activityListMap);
	}
	
}
