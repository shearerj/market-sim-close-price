package activity;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;

import event.TimeStamp;

/**
 * Class designed to hold ActivityLists in a way that orders them by priority
 * while ensuring that only a single ActivityList exists for each priority. This
 * class assumes that the ActivityList priority is fixed and will not update it
 * under any circumstances.
 * 
 * The combination of the priority management and the HashMap is a work-around to 
 * avoid the tie-breaking issue for ActivityLists with identical priorities.
 * 
 * NOTE: Priorities are ordered LOW to HIGH via a TreeSet, which ensures that
 * there are no duplicate priorities stored in the PriorityActivityList.
 * 
 * @author ewah
 */
public class PriorityActivityList {

	private TreeSet<Integer> priorityQueue;
	private HashMap<Integer, ActivityList> activityListMap;
	
	/**
	 * Constructor.
	 */
	public PriorityActivityList() {
		priorityQueue = new TreeSet<Integer>();
		activityListMap = new HashMap<Integer, ActivityList>();
	}
	
	/**
	 * Constructor.
	 * @param al ActivityList
	 */
	public PriorityActivityList(ActivityList al) {
		priorityQueue = new TreeSet<Integer>();
		activityListMap = new HashMap<Integer, ActivityList>();
		activityListMap.put(al.getPriority(), al);
	}
	
	
	/**
	 * Private constructor.
	 * @param pq
	 * @param acts
	 */
	private PriorityActivityList(TreeSet<Integer> pq, HashMap<Integer, ActivityList> acts) {
		priorityQueue = pq;
		activityListMap = acts;
	}
	
	/**
	 * Copy constructor.
	 * @param a
	 */
	public PriorityActivityList(PriorityActivityList pal) {
		this.priorityQueue = new TreeSet<Integer>(pal.priorityQueue);
		this.activityListMap = new HashMap<Integer, ActivityList>();
		for (Map.Entry<Integer, ActivityList> entry : pal.activityListMap.entrySet()) {
			this.activityListMap.put(entry.getKey(), new ActivityList(entry.getValue()));
		}
	}
	
	/**
	 * Copy constructor that changes the times for all activities in the priority
	 * activity list.
	 * @param ts
	 * @param pal
	 */
	public PriorityActivityList(TimeStamp ts, PriorityActivityList pal) {
		this.priorityQueue = new TreeSet<Integer>(pal.priorityQueue);
		this.activityListMap = new HashMap<Integer, ActivityList>();
		for (Map.Entry<Integer, ActivityList> entry : pal.activityListMap.entrySet()) {
			this.activityListMap.put(entry.getKey(), new ActivityList(ts, entry.getValue()));
		}
	}
	
	/**
	 * @return true if empty.
	 */
	public boolean isEmpty() {
		return priorityQueue.isEmpty();
	}
	
	/**
	 * Clear all structures.
	 */
	public void clear() {
		priorityQueue.clear();
		activityListMap.clear();
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return new String(activityListMap.toString());
	}	
	
	/**
	 * @param al ActivityList to add
	 */
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
	
	/**
	 * @param a	Activity to add
	 */
	public void add(Activity a) {
		ActivityList al = new ActivityList(a);
		this.add(al);
	}

	/**
	 * @param priority
	 * @param a			Activity to add
	 */
	public void add(int priority, Activity a) {
		ActivityList al = new ActivityList(priority, a);
		this.add(al);
	}
	
	/**
	 * @param pal	PriorityActivityList to add
	 */
	public void add(PriorityActivityList pal) {
		// iterate over all items in the object
		TreeSet<Integer> copy = new TreeSet<Integer>(pal.priorityQueue);
		
		while (!copy.isEmpty()) {
			// access the activity list queue by priority
			Integer priority = copy.pollFirst();
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
	 * @return last priority
	 */
	public int getLastPriority() {
		return priorityQueue.last();
	}
	
	/**
	 * @param priority
	 * @return
	 */
	public ActivityList getActivityAtPriority(int priority) {
		return activityListMap.get(priority);
	}
	
	/**
	 * Get all activities in the object, sorted by priority.
	 * 
	 * @return ActivityList linked list of activities in this object.
	 */
	public ActivityList getActivities() {
		if (!isEmpty()) {
			ActivityList acts = new ActivityList(activityListMap.get(priorityQueue.first()).getTime());
			TreeSet<Integer> copy = new TreeSet<Integer>(priorityQueue);
	
			while (!copy.isEmpty()) {
				// access the activity list queue by priority
				Integer priority = copy.pollFirst();
				if (priority != null) {
					acts.addAll(activityListMap.get(priority));
				}
			}
			return acts;
		} else {
			return null;
		}
	}
	
	/**
	 * Returns the portion PriorityActivityList that only includes priority
	 * ActivityLists above the given threshold (inclusive). Modifies the calling Object.
	 * 
	 * @param threshold
	 * @return
	 */
	public PriorityActivityList getPriorityListGreaterThan(int threshold) {
		TreeSet<Integer> copyPriorityQueue = new TreeSet<Integer>();
		HashMap<Integer, ActivityList> copyActivityListMap = new HashMap<Integer, ActivityList>();
		
		while (!this.isEmpty() && priorityQueue.first() >= threshold) {
			int priority = priorityQueue.pollFirst();
			copyPriorityQueue.add(priority);
			copyActivityListMap.put(priority, activityListMap.get(priority));
			activityListMap.remove(priority);
		}
		return new PriorityActivityList(copyPriorityQueue, copyActivityListMap);
	}
	
	/**
	 * Returns the portion PriorityActivityList that only includes priority
	 * ActivityLists below the given threshold (inclusive). Modifies the calling Object.
	 * 
	 * @param threshold
	 * @return
	 */
	public PriorityActivityList getPriorityListLessThan(int threshold) {
		TreeSet<Integer> copyPriorityQueue = new TreeSet<Integer>();
		HashMap<Integer, ActivityList> copyActivityListMap = new HashMap<Integer, ActivityList>();
		
		while (!this.isEmpty() && priorityQueue.first() <= threshold) {
			int priority = priorityQueue.pollFirst();
			copyPriorityQueue.add(priority);
			copyActivityListMap.put(priority, this.activityListMap.get(priority));
			activityListMap.remove(priority);
		}
		return new PriorityActivityList(copyPriorityQueue, copyActivityListMap);
	}
}
