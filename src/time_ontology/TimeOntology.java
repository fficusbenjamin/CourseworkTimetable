package time_ontology;

import jade.content.onto.BeanOntology;
import jade.content.onto.BeanOntologyException;
import jade.content.onto.Ontology;

public class TimeOntology extends BeanOntology {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static Ontology theInstance = new TimeOntology("my_ontology");
	
	public static Ontology getInstance(){
		return theInstance;
	}
	//singleton pattern
	private TimeOntology(String name) {
		super(name);
		try {
			
			add(Tutorial.class);
			add(Slot.class);
			add(Board.class);
			add(PropPredicate.class);

		} catch (BeanOntologyException e) {
			e.printStackTrace();
		}
	}
}
