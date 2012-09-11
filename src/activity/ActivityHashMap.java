package activity;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.Set;
import java.util.Map;

import event.TimeStamp;

/**
 * Wrapper class for HashMap of Activities. The data structure for any
 * additional Activities that are generated by an Activity, which is
 * hashed by TimeStamp.
 * 
 * @author ewah
 */
public class ActivityHashMap {

	private HashMap<TimeStamp, LinkedList<Activity>> acts;

	
	public ActivityHashMap() {
		acts = new HashMap<TimeStamp, LinkedList<Activity>>();
	}
	
	/**
	 * Clear the HashMap structure.
	 */
	public void clear() {
		acts.clear();
	}
	
	/**
	 * Creates new entry hashed at the activity's TimeStamp, with a single
	 * element LinkedList containing the given Activity.
	 * 
	 * If key already exists, appends the given Activity at the end of the
	 * Activity LinkedList.
	 * 
	 * @param act
	 * @return true if inserted correctly, false otherwise
	 */
	public boolean insertActivity(Activity act) {
		if (act == null)
			return false;
		
		boolean ret = false;
		TimeStamp ts = act.getTime();
		if (!ts.checkActivityTimeStamp(act))
			return false;
		
		if (acts.containsKey(ts)) {
			// append Activity to LinkedList
			ret = acts.get(ts).add(act);
		} else {
			// create new key-value mapping
			LinkedList<Activity> vec = new LinkedList<Activity>();
			ret = vec.add(act);
			acts.put(ts, vec);
		}
		return ret;
	}
	
	/**
	 * Creates new entry hashed at TimeStamp of activities, with a LinkedList
	 * containing the specified list of activities.
	 * 
	 * If key already exists, appends the given Activity LinkedList at the end
	 * of the existing LinkedList.
	 * 
	 * @param av
	 * @return true if inserted correctly, false otherwise
	 */
	public boolean insertActivity(LinkedList<Activity> av) {
		if (av == null) return false;
		
		boolean ret = false;
		TimeStamp ts = av.get(0).getTime();
		if (!ts.checkActivityTimeStamp(av)) {
			System.out.println("timestamps do not match");
			return false;
		}
		
		if (acts.containsKey(ts)) {
			// append Activity Vector to LinkedList
			ret = acts.get(ts).addAll(av);
		} else {
			// create new key-value mapping
			acts.put(ts, av);
		}
		return ret;
	}
	
	/**
	 * Appends two ActivityHashMaps together. Modifies the calling Object's HashMap.
	 * 
	 * @param ahm
	 * @return
	 */
	public boolean appendActivityHashMap(ActivityHashMap ahm) {
		boolean ret = true;
		for (Map.Entry<TimeStamp,LinkedList<Activity>> entry : ahm.acts.entrySet()) {
			ret = ret && insertActivity(entry.getValue());
		}
		return ret;
	}
	
	/**
	 * @return true if contains no key-value pairings
	 */
	public boolean isEmpty() {
		return acts.isEmpty();
	}
	
	/**
	 * @return Set of TimeStamps (keys)
	 */
	public Set<TimeStamp> keys() {
		return acts.keySet();
	}
	
	/**
	 * @return Set view of mappings in this ActivityHashMap
	 */
	public Set<Map.Entry<TimeStamp,LinkedList<Activity>>> entrySet() {
		return acts.entrySet();
	}
	
	
	/**
	 * @param list
	 * @return true if hashmap contains the list, otherwise false.
	 */
	public boolean contains(LinkedList<Activity> list) {
		
		// Check if directly contained within the hashmap
		boolean contained = acts.containsValue(list);
		
		// Check if is a sublist of any linked list within the hashmap
		for (Map.Entry<TimeStamp,LinkedList<Activity>> entry : acts.entrySet()) {
			LinkedList<Activity> test = entry.getValue();
			if (test.equals(list)) {
				contained = true;
				break;
			}
			
			if (list.hashCode() == test.hashCode()) {
				contained = true;
				break;
			}
		}
		return contained;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		String s = "";
		for (Map.Entry<TimeStamp,LinkedList<Activity>> entry : acts.entrySet()) {
			s += entry.getKey().toString() + ": ";
			for (Iterator<Activity> it = entry.getValue().iterator(); it.hasNext(); ) {
				s += it.next().toString() + "...";
			}
		}
		return s;
	}

}
