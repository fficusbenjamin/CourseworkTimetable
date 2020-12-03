import java.util.ArrayList;
import java.util.List;

import jade.content.ContentElement;
import jade.content.lang.Codec;
import jade.content.lang.Codec.CodecException;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import time_ontology.*;


//REFACTOR CLASS AND VARIABLE NAMES, MAKE SURE ALL THE PRINTED STRINGS ARE CHANGED EVERYWHERE


public class TimetableAgent extends Agent{
	private Codec codec = new SLCodec();
	private Ontology ontology = TimeOntology.getInstance();

	List<AID> students = new ArrayList<AID>();

	Board advertboard = new Board();
	ArrayList<Prop> proposals = new ArrayList<Prop>();

	int tickCount = 0;

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
						sem.setStudentOwner(students.get(i));
						sem.setDay("Tuesday");
						sem.setModuleName("SEM");
						sem.setModuleID("SET1010");
						sem.setRoom("D2");
						sem.setType("Lecture");
						sem.setStartTime(1500);
						sem.setEndTime(1600);
					} else if (i > 0) {
						sem.setStudentOwner(students.get(i));
						sem.setDay("Friday");
						sem.setModuleName("SEM");
						sem.setModuleID("SET1010");
						sem.setRoom("A17");
						sem.setType("Tutorial");
						sem.setStartTime(1200);
						sem.setEndTime(1300);
					}

					Slot owns = new Slot();
					owns.setSlotOwner(students.get(i));
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
		addBehaviour(new TickerBehaviour(this, 6000) {
			protected void onTick() {
				tick();
			}
		});
		this.addBehaviour(new StudentRegister());
		this.addBehaviour(new DeconstructStudents());
		this.addBehaviour(new ConfirmTimetable());
		addBehaviour(new handleRequest());
		addBehaviour(new handleProposal());
	}
	private void tick() {
		if (tickCount < 10) {
			System.out.println("tick");
			handleSwaps();

			DFAgentDescription template = new DFAgentDescription();
			ServiceDescription desc = new ServiceDescription();
			ACLMessage cfp = new ACLMessage(ACLMessage.INFORM);
			desc.setType("TimetableAgent");
			template.addServices(desc);
			try {
				DFAgentDescription[] result = DFService.search(this, template);
				if (result.length > 0) {
					cfp.addReceiver(result[0].getName());
				}
			} catch (FIPAException fe) {
				fe.printStackTrace();
			}
			cfp.setContent("tickInform");
			cfp.setConversationId("timetable setup");
			this.send(cfp);

			tickCount++;

		} else {
			// shut down
			System.out.println("Decontstruction started");
			DFAgentDescription template = new DFAgentDescription();
			ServiceDescription desc = new ServiceDescription();
			ACLMessage cfp = new ACLMessage(ACLMessage.INFORM);
			desc.setType("TimetableAgent");
			template.addServices(desc);
			try {
				DFAgentDescription[] result = DFService.search(this, template);
				if (result.length > 0) {
					cfp.addReceiver(result[0].getName());
				}
			} catch (FIPAException fe) {
				fe.printStackTrace();
			}
			cfp.setContent("takedownRequest");
			cfp.setConversationId("timetable setup");
			this.send(cfp);

			this.doDelete();
		}
	}

	// Put agent clean-up operations here
	protected void takeDown() {
		// Deregister from the yellow pages
		try {
			DFService.deregister(this);
		} catch (FIPAException fe) {
			fe.printStackTrace();
		}
		System.out.println("Timetabler agent terminating.");
	}

	//CHANGE ORDER
	private void handleSwaps() {
		for(int i = 0; i < proposals.size(); i++) {
			for(int j = 0; j < proposals.size(); j++) {
				if(proposals.get(i).getSlotOwner().equals(proposals.get(j).getSlotRecipient())) {
					ACLMessage msg = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
					msg.addReceiver(proposals.get(i).getSlotRecipient());
					msg.setLanguage(codec.getName());
					msg.setOntology(ontology.getName());
					// Prepare the content.

					Slot owns = new Slot();
					owns.setSlotOwner(proposals.get(i).getSlotOwner());
					owns.setTutorial(proposals.get(i).getSlot());
					try {
						// Let JADE convert from Java objects to string
						getContentManager().fillContent(msg, owns);
						System.out.println("sent");
						send(msg);
					} catch (CodecException ce) {
						ce.printStackTrace();
					} catch (OntologyException oe) {
						oe.printStackTrace();
					}

				}
			}
		}
		proposals.removeAll(proposals);
	}

	private class handleProposal extends CyclicBehaviour{
		public void action() {
			// TODO Auto-generated method stub
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.PROPOSE);
			ACLMessage msg = myAgent.receive(mt);
			//System.out.println("msg not null");

			if(msg != null) {
				try {
					ContentElement ce = null;
					ce = getContentManager().extractContent(msg);
					if(ce instanceof Prop) {
						Prop owns = (Prop) ce;
						proposals.add(owns);
						System.out.println("Received: "+ owns.getSlot().getModuleName());
						System.out.println("From: "+ owns.getSlotOwner().getName());
						System.out.println(" ");
					}
				}
				catch (CodecException ce) {
					ce.printStackTrace();
				} catch (OntologyException oe) {
					oe.printStackTrace();
				}
			}
		}

	}

	private class handleRequest extends CyclicBehaviour{
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
			ACLMessage msg = myAgent.receive(mt);

			if(msg != null) {
				if(msg.getContent().equals("requestTimetable")) {
					System.out.println("timetable reply");
					ACLMessage reply = new ACLMessage(ACLMessage.AGREE);
					reply.setLanguage(codec.getName());
					reply.setOntology(ontology.getName());
					reply.addReceiver(msg.getSender());

					try {
						// Let JADE convert from Java objects to string
						Board advpredicate = new Board();
						advpredicate.setBoard(advertboard.getBoard());
						getContentManager().fillContent(reply, advpredicate);
						//getContentManager().fillContent(reply, (ContentElement) advertBoard);
						send(reply);
						//System.out.println(msg.getSender());
					} catch (CodecException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (OntologyException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
	}

	private class StudentRegister extends CyclicBehaviour {
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) {

				if (msg.getContent().equals("addition request")) {
					students.add(msg.getSender());
					System.out.println("Student added.");
				}
				else {
					try {
						ContentElement ce = null;
						ce = getContentManager().extractContent(msg);
						if (ce instanceof Slot) {
							Slot owns = (Slot) ce;
							advertboard.getBoard().add(owns.getTutorial());
							System.out.print("Timetabler Agent added slot: " + owns.getTutorial().getModuleName());
						}
					}

					catch (CodecException ce) {
						ce.printStackTrace();
					} catch (OntologyException oe) {
						oe.printStackTrace();
					}
				}
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
