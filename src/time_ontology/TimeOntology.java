package time_ontology;

import jade.content.onto.BeanOntology;
import jade.content.onto.BeanOntologyException;
import jade.content.onto.Ontology;

import java.io.Serial;

///------------------------------------------------------------------------
///   Class:		TimeOntology (Class)
///   Description:	Tutorial class that states all its properties
///
///
///
///
///   Author:		Francesco Fico (40404272)     Date: 02/12/2020
///------------------------------------------------------------------------

public class TimeOntology extends BeanOntology {

	@Serial
	private static final long serialVersionUID = 1L;
	//initialisation of the ontology instance
	private static final Ontology theInstance = new TimeOntology("ontology");
	//getter for the ontology instance
	public static Ontology getInstance(){
		return theInstance;
	}
	//singleton pattern for the TimeOntology instance
	private TimeOntology(String name) {
		super(name);
		try {
			//add the tutorial and all the predicates to the ontology instance
			add(Tutorial.class);
			add(Slot.class);
			add(Board.class);
			add(Prop.class);

		} catch (BeanOntologyException e) {
			e.printStackTrace();
		}
	}
}
