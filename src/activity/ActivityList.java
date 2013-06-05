package activity;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import systemmanager.Consts;

import event.TimeStamp;

/**
 * Wrapper class for a linked list of activities. Each ActivityList has a uniform
 * priority. An ActivityList is created with default priority of 0.
 * 
 * @author ewah
 */
public class ActivityList implements Iterable<Activity> {

	private int priority;
	private TimeStamp time;
	private LinkedList<Activity> activityList;
	
	
	public ActivityList() {
		priority = Consts.DEFAULT_PRIORITY;
		activityList = new LinkedList<Activity>();
		time = new TimeStamp(-1);
	}
	
	public ActivityList(TimeStamp ts) {
		priority = Consts.DEFAULT_PRIORITY;
		activityList = new LinkedList<Activity>();
		time = ts;
	}
	
	public ActivityList(Activity a) {
		priority = Consts.DEFAULT_PRIORITY;
		activityList = new LinkedList<Activity>();
		activityList.add(a);
		time = a.getTime();
	}
	
	public ActivityList(LinkedList<Activity> acts) {
		priority = Consts.DEFAULT_PRIORITY;
		activityList = acts;
		time = acts.getFirst().getTime();
	}
	
	public ActivityList(int priority, Activity a) {
		this.priority = priority;
		activityList = new LinkedList<Activity>();
		activityList.add(a);
		time = a.getTime();
	}
	
	public ActivityList(int priority, LinkedList<Activity> acts) {
		this.priority = priority;
		activityList = acts;
		time = acts.getFirst().getTime();
	}

	/**
	 * Copy constructor.
	 * @param al
	 */
	public ActivityList(ActivityList al) {
		this.priority = al.priority;
		this.activityList = new LinkedList<Activity>(al.activityList);
		this.time = new TimeStamp(al.time);
	}
	
	/**
	 * Copy constructor that changes the priority of the input ActivityList.
	 * @param priority
	 * @param al
	 */
	public ActivityList(int priority, ActivityList al) {
		this.priority = priority;
		this.activityList = new LinkedList<Activity>();
		for (Iterator<Activity> it = al.iterator(); it.hasNext(); ) {
			this.activityList.add(it.next().deepCopy());
		}
		this.time = new TimeStamp(al.time);
	}
	
	/**
	 * Copy constructor that changes the time of the input ActivityList.
	 * @param ts
	 * @param al
	 */
	public ActivityList(TimeStamp ts, ActivityList al) {
		this.priority = al.priority;
		this.time = new TimeStamp(ts);
		this.activityList = new LinkedList<Activity>();
		for (Iterator<Activity> it = al.iterator(); it.hasNext(); ) {
			Activity a = it.next().deepCopy();
			a.setTime(ts);
			this.activityList.add(a);
		}
	}
	
	public boolean add(Activity act) {
		if (time.checkActivityTimeStamp(act)) {
			return activityList.add(act);
		} else {
			return false;
		}
	}
	
	public boolean addAll(ActivityList acts) {
		if (time.checkActivityTimeStamp(acts)) {
			return activityList.addAll(acts.activityList);
		} else {
			return false;
		}
	}
	
	public boolean addAll(int index, ActivityList acts) {
		if (time.checkActivityTimeStamp(acts)) {
			return activityList.addAll(index, acts.activityList);
		} else {
			return false;
		}
	}
	
	public void addLast(Activity act) {
		if (time.checkActivityTimeStamp(act)) {
			activityList.addLast(act);
		}
	}
	
	@Override
	public Iterator<Activity> iterator() {
		return activityList.iterator();
	}
	
	public int size() {
		return activityList.size();
	}
	
	public boolean isEmpty() {
		return activityList.isEmpty();
	}
	
	public void clear() {
		activityList.clear();
		priority = 0;
		time = null;
	}
	
	public boolean contains(Activity a) {
		return activityList.contains(a);
	}
	
	public Activity get(int index) {
		return activityList.get(index);
	}
	
	/**
	 * @return Integer of priority
	 */
	public Integer getPriority() {
		return new Integer(priority);
	}
	
	/**
	 * @return TimeStamp
	 */
	public TimeStamp getTime() {
		return time;
	}
	
	@Override
	public String toString() {
		return new String(time + "|" + "{" + priority + "}" +  activityList);
	}
	
	public List<Activity> getList() {
		return activityList;
	}
	
}
