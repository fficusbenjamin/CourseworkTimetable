package ontology;


import jade.content.onto.BeanOntology;
import jade.content.onto.BeanOntologyException;
import jade.content.onto.Ontology;

public class TimetableOntology extends BeanOntology{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static Ontology theInstance = new TimetableOntology("my_ontology");
	
	public static Ontology getInstance(){
		return theInstance;
	}
	//singleton pattern
	private TimetableOntology(String name) {
		super(name);
		try {
			//add("timetable_ontology_elements");
			//add(Preferences.class); 
			
			add(Tutorial.class);
			add(StudPredicate.class);
			add(AdvertBoard.class);
			add(AdvertPredicate.class);
			add(PropPredicate.class);

		} catch (BeanOntologyException e) {
			e.printStackTrace();
		}
	}
}
