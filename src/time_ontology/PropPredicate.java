package time_ontology;

import jade.content.Predicate;
import jade.core.AID;

public class PropPredicate implements Predicate {

	private AID slotOwner;
	private AID slotRecipient;
	private Tutorial slot;
	public AID getSlotOwner() {
		return slotOwner;
	}
	public void setSlotOwner(AID slotOwner) {
		this.slotOwner = slotOwner;
	}
	public AID getSlotRecipient() {
		return slotRecipient;
	}
	public void setSlotRecipient(AID slotRecipient) {
		this.slotRecipient = slotRecipient;
	}
	public Tutorial getSlot() {
		return slot;
	}
	public void setSlot(Tutorial slot) {
		this.slot = slot;
	}
	
	
	
}
