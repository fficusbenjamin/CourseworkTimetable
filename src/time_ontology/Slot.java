package time_ontology;

import jade.content.Predicate;
import jade.core.AID;

public class Slot implements Predicate {

	private static final long serialVersionUID = 1L;
	private AID slotOwner;
	private Tutorial tutorial;
	
	public AID getSlotOwner() {
		return slotOwner;
	}
	
	public void setSlotOwner(AID slotOwner) {
		this.slotOwner = slotOwner;
	}
	
	public Tutorial getTutorial() {
		return tutorial;
	}
	
	public void setTutorial(Tutorial tutorial) {
		this.tutorial = tutorial;
	}
}
