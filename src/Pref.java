
public class Pref {
	String Availability;
	String Day;
	int StartTime;
	int EndTime;
	
	public String getAvailability() {
		return Availability;
	}

	public void setAvailability(String availability) {
		Availability = availability;
	}

	public String getDay() {
		return Day;
	}

	public void setDay(String day) {
		Day = day;
	}

	public int getStartTime() {
		return StartTime;
	}

	public void setStartTime(int startTime) {
		StartTime = startTime;
	}

	public int getEndTime() {
		return EndTime;
	}

	public void setEndTime(int endTime) {
		EndTime = endTime;
	}

	public Pref(String availability, String day, int sTime, int eTime) {
		Availability = availability;
		Day = day;
		StartTime = sTime;
		EndTime = eTime;
	}
	public Pref() {
		
	}
	

}
