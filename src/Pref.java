///---------------------------------------------------------------------
///   Class:		Pref (Class)
///   Description:	Class that sets the preference for the student agent
///
///
///
///   Author:		Francesco Fico (40404272)     Date: 02/12/2020
///---------------------------------------------------------------------



public class Pref {
	//initialise the properties
	String Availability;
	String Day;
	int StartTime;
	int EndTime;

	//getters and setters for the properties
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

	//method that assign the properties
	public Pref(String availability, String day, int sTime, int eTime) {
		Availability = availability;
		Day = day;
		StartTime = sTime;
		EndTime = eTime;
	}
	//constructor
	public Pref() {
	}
}
