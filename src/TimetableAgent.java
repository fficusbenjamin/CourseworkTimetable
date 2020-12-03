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

///--------------------------------------------------------------------
///   Class:		Timetable Agent (Class)
///   Description:	Timetable agent class is the class that holds the
///					the timetable agent and all the methods needed to
///					perform all the communication with the student
///					agent, the creation of a timetable and the ontology
///					instance.
///
///   Author:		Francesco Fico (40404272)     Date: 02/12/2020
///--------------------------------------------------------------------


public class TimetableAgent extends Agent{
	private final Codec codec = new SLCodec();
	private final Ontology timeOntology = TimeOntology.getInstance();

	List<AID> students = new ArrayList<>();

	Board board = new Board();
	ArrayList<Prop> props = new ArrayList<>();

	int nmbTicks = 0;

	protected void setup() {
		getContentManager().registerLanguage(codec);
		getContentManager().registerOntology(timeOntology);

		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("Timetable Agent");
		sd.setName("Timetable Agent");
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		} catch (FIPAException fe) {
			fe.printStackTrace();
		}
		addBehaviour(new WakerBehaviour(this, 10000) {
			protected void onWake() {
				System.out.println("The timetable has " + students.size() + " Student Agents." );
				for (int i = 0; i < students.size(); i++) {
					// Prepare the Query-IF message
					ACLMessage msg = new ACLMessage(ACLMessage.CFP);
					msg.addReceiver(students.get(i));
					msg.setLanguage(codec.getName());
					msg.setOntology(timeOntology.getName());
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
					} else {
						sem.setStudentOwner(students.get(i));
						sem.setDay("Friday");
						sem.setModuleName("SEM");
						sem.setModuleID("SET1010");
						sem.setRoom("A17");
						sem.setType("Tutorial");
						sem.setStartTime(1200);
						sem.setEndTime(1300);
					}

					Slot slot = new Slot();
					slot.setSlotOwner(students.get(i));
					slot.setSlot(sem);
					try {
						getContentManager().fillContent(msg, slot);
						send(msg);
					} catch (CodecException | OntologyException ce) {
						ce.printStackTrace();
					}
				}
			}
		});
		addBehaviour(new TickerBehaviour(this, 6000) {
			protected void onTick() {
				newTick();
			}
		});
		this.addBehaviour(new addStudents());
		this.addBehaviour(new shutdown());
		addBehaviour(new assignedTimetable());
		addBehaviour(new recTimetableRequest());
		addBehaviour(new recProposal());
	}
	private void newTick() {
		if (nmbTicks < 5) {
			System.out.println("Round " + (nmbTicks+1));
			recSwap();

			DFAgentDescription template = new DFAgentDescription();
			ServiceDescription desc = new ServiceDescription();
			ACLMessage cfp = new ACLMessage(ACLMessage.INFORM);
			desc.setType("Timetable Agent");
			template.addServices(desc);
			try {
				DFAgentDescription[] result = DFService.search(this, template);
				if (result.length > 0) {
					cfp.addReceiver(result[0].getName());
				}
			} catch (FIPAException fe) {
				fe.printStackTrace();
			}
			cfp.setContent("newTick");
			cfp.setConversationId("timetable setup");
			this.send(cfp);

			nmbTicks++;
		} else {
			DFAgentDescription template = new DFAgentDescription();
			ServiceDescription desc = new ServiceDescription();
			ACLMessage cfp = new ACLMessage(ACLMessage.INFORM);
			desc.setType("Timetable Agent");
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
		System.out.println("Shutting down agents.");
	}

	//CHANGE ORDER
	private void recSwap() {
		for(int i = 0; i < props.size(); i++) {
			for (Prop prop : props) {
				if (props.get(i).getSlotOwner().equals(prop.getSlotRecipient())) {
					ACLMessage msg = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
					msg.addReceiver(props.get(i).getSlotRecipient());
					msg.setLanguage(codec.getName());
					msg.setOntology(timeOntology.getName());
					// Prepare the content.

					Slot slot = new Slot();
					slot.setSlotOwner(props.get(i).getSlotOwner());
					slot.setSlot(props.get(i).getProp());
					try {
						// Let JADE convert from Java objects to string
						getContentManager().fillContent(msg, slot);
						send(msg);
					} catch (CodecException | OntologyException ce) {
						ce.printStackTrace();
					}
				}
			}
		}
		props.removeAll(props);
	}

	private class recProposal extends CyclicBehaviour{
		public void action() {
			// TODO Auto-generated method stub
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.PROPOSE);
			ACLMessage msg = myAgent.receive(mt);
			if(msg != null) {
				try {
					ContentElement ce;
					ce = getContentManager().extractContent(msg);
					if(ce instanceof Prop) {
						Prop prop = (Prop) ce;
						props.add(prop);
						System.out.println("Received: (Module Name)"+ prop.getProp().getModuleName());
						System.out.println("From: (Student Name)"+ prop.getSlotOwner().getName());
						System.out.println(" ");
					}
				}
				catch (CodecException | OntologyException ce) {
					ce.printStackTrace();
				}
			}
		}
	}

	private class recTimetableRequest extends CyclicBehaviour{
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
			ACLMessage msg = myAgent.receive(mt);

			if(msg != null) {
				if(msg.getContent().equals("reqTimetable")) {
					ACLMessage timetableReply = new ACLMessage(ACLMessage.AGREE);
					timetableReply.setLanguage(codec.getName());
					timetableReply.setOntology(timeOntology.getName());
					timetableReply.addReceiver(msg.getSender());

					try {
						// Let JADE convert from Java objects to string
						Board board = new Board();
						board.setBoard(TimetableAgent.this.board.getBoard());
						getContentManager().fillContent(timetableReply, board);
						send(timetableReply);
					} catch (CodecException | OntologyException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
	}

	private class addStudents extends CyclicBehaviour {
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) {
				if (msg.getContent().equals("addition request")) {
					students.add(msg.getSender());
					new assignedTimetable();
				}
				else {
					try {
						ContentElement ce;
						ce = getContentManager().extractContent(msg);
						if (ce instanceof Slot) {
							Slot owns = (Slot) ce;
							board.getBoard().add(owns.getSlot());
							System.out.print("Timetable Agent has: " + owns.getSlot().getModuleName());
							System.out.println(" ");

						}
					}
					catch (CodecException | OntologyException ce) {
						ce.printStackTrace();
					}
				}
			}
		}
	}
	
	private static class assignedTimetable extends CyclicBehaviour {
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CONFIRM);
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) {
				System.out.println(msg.getSender().getName() + " has been assigned a timetable");
			}
		}
	}

	private class shutdown extends CyclicBehaviour {
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
			ACLMessage msg = myAgent.receive(mt);

			if (msg != null) {
				if (msg.getContent().equals("takedownRequest")) {
					for (AID student : students) {
						DFAgentDescription template = new DFAgentDescription();
						ServiceDescription desc = new ServiceDescription();
						ACLMessage inform = new ACLMessage(ACLMessage.INFORM);
						template.addServices(desc);
						inform.addReceiver(student);
						inform.setContent("takedownRequest");
						myAgent.send(inform);
					}
					myAgent.doDelete();
				} else if (msg.getContent().equals("newTick")) {
					for (AID student : students) {
						DFAgentDescription template = new DFAgentDescription();
						ServiceDescription desc = new ServiceDescription();
						ACLMessage inform = new ACLMessage(ACLMessage.INFORM);
						template.addServices(desc);
						inform.addReceiver(student);
						inform.setContent("newTick");
						myAgent.send(inform);
					}
				}
			}
		}
	}
}
