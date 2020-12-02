package time_ontology;

import jade.content.Concept;
import jade.core.AID;

public class Tutorial implements Concept {

	AID studentOwner;
	String moduleName;
	String moduleID;
	String type;
	String room;
	String day;
	int startTime;
	int endTime;

	public Tutorial() {

	}

	public AID getStudentOwner() {
		return studentOwner;
	}

	public void setStudentOwner(AID studentOwner) {
		this.studentOwner = studentOwner;
	}

	public String getModuleName() {
		return moduleName;
	}

	public void setModuleName(String moduleName) {
		this.moduleName = moduleName;
	}

	public String getModuleID() {
		return moduleID;
	}

	public void setModuleID(String moduleID) {
		this.moduleID = moduleID;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getRoom() {
		return room;
	}

	public void setRoom(String room) {
		this.room = room;
	}

	public String getDay() {
		return day;
	}

	public void setDay(String day) {
		this.day = day;
	}

	public int getStartTime() {
		return startTime;
	}

	public void setStartTime(int startTime) {
		this.startTime = startTime;
	}

	public int getEndTime() {
		return endTime;
	}

	public void setEndTime(int endTime) {
		this.endTime = endTime;
	}
}
