package time_ontology;

import jade.content.Predicate;
import jade.core.AID;

import java.io.Serial;

///------------------------------------------------------------------------
///   Class:		Slot (Class)
///   Description:	Class that , being a predicate of the ontology, holds
///					the actual tutorial to be stored on the board.
///
///
///
///   Author:		Francesco Fico (40404272)     Date: 02/12/2020
///------------------------------------------------------------------------

public class Slot implements Predicate {

	@Serial
	private static final long serialVersionUID = 1L;
	//initialise the class variables
	private AID slotOwner;
	private Tutorial slot;

	//getters and setters for the variables
	public AID getSlotOwner() {
		return slotOwner;
	}
	
	public void setSlotOwner(AID slotOwner) {
		this.slotOwner = slotOwner;
	}
	
	public Tutorial getSlot() {
		return slot;
	}
	
	public void setSlot(Tutorial slot) {
		this.slot = slot;
	}
}
