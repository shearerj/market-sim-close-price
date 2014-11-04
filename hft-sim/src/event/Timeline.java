package event;

public interface Timeline {

	public void scheduleActivityIn(TimeStamp delay, Activity act);
	
	public TimeStamp getCurrentTime();
	
}
