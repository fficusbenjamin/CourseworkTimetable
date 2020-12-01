package ontology;


import jade.content.Concept;
import jade.content.Predicate;
import jade.content.onto.annotations.Slot;

public class Tutorial implements Concept {
	String name;
	String day;
	int startTime;
	int endTime;

	public Tutorial() {

	}
	
	
	@Slot (mandatory = true)
	public String getDay() {
		return day;
	}



	public void setDay(String day) {
		this.day = day;
	}


	@Slot (mandatory = true)
	public int getStartTime() {
		return startTime;
	}



	public void setStartTime(int startTime) {
		this.startTime = startTime;
	}


	@Slot (mandatory = true)
	public int getEndTime() {
		return endTime;
	}



	public void setEndTime(int endTime) {
		this.endTime = endTime;
	}



	public void setName(String name) {
		this.name = name;
	}


	@Slot (mandatory = true)
	public String getName() {
		return name;
	}
}

