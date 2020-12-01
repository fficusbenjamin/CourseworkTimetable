import java.util.ArrayList;
import java.util.List;

import jade.content.lang.Codec;
import jade.content.lang.Codec.CodecException;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.content.onto.basic.Action;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import ontology.*;

public class TimetableAgent extends Agent {
	private Codec codec = new SLCodec();
	private Ontology ontology = TimetableOntology.getInstance();

	List<AID> students = new ArrayList<AID>();

	protected void setup() {
		getContentManager().registerLanguage(codec);
		getContentManager().registerOntology(ontology);

		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("TimetableAgent");
		sd.setName("TimetableAgent");
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		} catch (FIPAException fe) {
			fe.printStackTrace();
		}
		addBehaviour(new WakerBehaviour(this, 10000) {
			protected void onWake() {
				System.out.println("Student Agents " + students.size());
				for (int i = 0; i < students.size(); i++) {
					// Prepare the Query-IF message
					ACLMessage msg = new ACLMessage(ACLMessage.CFP);
					msg.addReceiver(students.get(i));
					msg.setLanguage(codec.getName());
					msg.setOntology(ontology.getName());
					// Prepare the content.
					Tutorial sem = new Tutorial();
					if (i == 0) {
						sem.setDay("Tuesday");
						sem.setName("SEM");
						sem.setStartTime(1500);
						sem.setEndTime(1600);
					} else if (i > 0) {
						sem.setDay("Friday");
						sem.setName("SEM");
						sem.setStartTime(1200);
						sem.setEndTime(1300);
					}

					StudPredicate owns = new StudPredicate();
					owns.setOwner(students.get(i));
					owns.setTutorial(sem);
					try {
						// Let JADE convert from Java objects to string
						getContentManager().fillContent(msg, owns);
						send(msg);
					} catch (CodecException ce) {
						ce.printStackTrace();
					} catch (OntologyException oe) {
						oe.printStackTrace();
					}
				}
			}
		});

		this.addBehaviour(new StudentRegister());
		this.addBehaviour(new DeconstructStudents());
		this.addBehaviour(new ConfirmTimetable());
	}

	// Put agent clean-up operations here
	protected void takeDown() {
		// Deregister from the yellow pages
		try {
			DFService.deregister(this);
		} catch (FIPAException fe) {
			fe.printStackTrace();
		}

		System.out.println("Timetable agent terminating.");
	}

	private class StudentRegister extends CyclicBehaviour {
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) {
				students.add(msg.getSender());
			}
		}
	}
	
	private class ConfirmTimetable extends CyclicBehaviour {
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CONFIRM);
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) {
				System.out.println(msg.getSender().getName() + " confirmed timetable");
			}
		}
	}

	private class DeconstructStudents extends CyclicBehaviour {
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
			ACLMessage msg = myAgent.receive(mt);

			if (msg != null) {
				if (msg.getContent().equals("takedownRequest")) {
					for (int i = 0; i < students.size(); i++) {
						DFAgentDescription template = new DFAgentDescription();
						ServiceDescription desc = new ServiceDescription();
						ACLMessage inform = new ACLMessage(ACLMessage.INFORM);
						template.addServices(desc);
						inform.addReceiver(students.get(i));
						inform.setContent("takedownRequest");
						myAgent.send(inform);
					}
					System.out.println("Agent called");
					myAgent.doDelete();
				} else if (msg.getContent().equals("tickInform")) {
					System.out.println("new advertiser tick");
					for (int i = 0; i < students.size(); i++) {
						DFAgentDescription template = new DFAgentDescription();
						ServiceDescription desc = new ServiceDescription();
						ACLMessage inform = new ACLMessage(ACLMessage.INFORM);
						template.addServices(desc);
						inform.addReceiver(students.get(i));
						inform.setContent("tickInform");
						myAgent.send(inform);
					}
				}
			}
		}
	}
}