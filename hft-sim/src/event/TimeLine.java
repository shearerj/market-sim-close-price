package event;

public interface TimeLine {

	public void scheduleActivityIn(TimeStamp delay, Activity act);
	
	public TimeStamp getCurrentTime();
	
}
