package time_ontology;

import jade.content.Predicate;
import jade.core.AID;

///------------------------------------------------------------------------
///   Class:		Prop (Class)
///   Description:	Class that , being a predicate of the ontology, holds
///					the unwanted tutorials to be swapped on a board.
///
///
///
///   Author:		Francesco Fico (40404272)     Date: 02/12/2020
///------------------------------------------------------------------------


public class Prop implements Predicate {
	//initialise the class variables
	private AID slotOwner;
	private AID slotRecipient;
	private Tutorial slot;
	//getters and setters for the variables
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
