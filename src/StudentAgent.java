import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import java.util.ArrayList;
import java.util.List;
import jade.content.ContentElement;
import jade.content.lang.Codec;
import jade.content.lang.Codec.CodecException;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.core.AID;
import time_ontology.*;

///-------------------------------------------------------------------
///   Class:		Student Agent (Class)
///   Description:	Student agent class is the class that holds the
///					the student agent and all the classes needed to
///					perform all the communication with the timetable
///					agent.
///
///   Author:		Francesco Fico (40404272)     Date: 02/12/2020
///-------------------------------------------------------------------

public class StudentAgent extends Agent {
	//initialise variables
	private final Codec codec = new SLCodec();
	private final Ontology timeOntology = TimeOntology.getInstance();
	List<Tutorial> timetable = new ArrayList<>();
	List<Pref> preferences = new ArrayList<>();
	private int availability = 2;

	//setup
	protected void setup() {
		Object[] args = getArguments();
		if (args != null && args.length > 0) {
			Pref preference = new Pref();
			preference = (Pref) args[0];
			preferences.add(preference);
		}
		getContentManager().registerLanguage(codec);
		getContentManager().registerOntology(timeOntology);
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("timetable setup");
		sd.setName("student");
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		} catch (FIPAException fe) {
			fe.printStackTrace();
		}
		//adds the ticker behaviour that performs the new tick method
		addBehaviour(new TickerBehaviour(this, 6000) {
			protected void onTick() {
			}
		});
		//all the remnant behaviours
		addBehaviour(new reqTimetableAdd());
		addBehaviour(new recTimetable());
		addBehaviour(new timetableListener());
		addBehaviour(new swapRequired());
		addBehaviour(new swapSlot());
	}

	//request timetable addition class
	private static class reqTimetableAdd extends Behaviour {
		public void action() {
			DFAgentDescription template = new DFAgentDescription();
			ServiceDescription desc = new ServiceDescription();
			ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
			//Gives the message a type for the timetable agent to interpret
			desc.setType("Timetable Agent");
			template.addServices(desc);
			try {
				DFAgentDescription[] result = DFService.search(myAgent, template);
				//if not zero the student exists
				if (result.length > 0) {
					cfp.addReceiver(result[0].getName());
				}
			} catch (FIPAException fe) {
				fe.printStackTrace();
			}
			//set the content of the message to addition request which calls the method inside the timetable agent to add students
			cfp.setContent("addition request");
			cfp.setConversationId("timetable setup");
			cfp.setReplyWith("cfp" + System.currentTimeMillis()); //unique value
			myAgent.send(cfp);
		}
		public boolean done() {
			return true;
		}
	}

	//clean-up operations
	protected void takeDown() {
		// Deregister from the yellow pages
		try {
			DFService.deregister(this);
		} catch (FIPAException fe) {
			fe.printStackTrace();
		}
	}
	//receive timetable class
	private class recTimetable extends CyclicBehaviour {
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) {
				try {
					ContentElement ce;
					ce = getContentManager().extractContent(msg);
					if (ce instanceof Slot) {
						Slot slot = (Slot) ce;
						Tutorial tut = slot.getSlot();
						AID slotOwner = slot.getSlotOwner();
						System.out.println("Module: " + tut.getModuleID() + " " + tut.getType() + tut.getDay() + " " + tut.getModuleName() + " " + "\nStudent:" + slotOwner);
						timetable.add(tut);
						DFAgentDescription template = new DFAgentDescription();
						ServiceDescription desc = new ServiceDescription();
						//confirmation timetable received
						ACLMessage conformMsg = new ACLMessage(ACLMessage.CONFIRM);
						desc.setType("Timetable Agent");
						template.addServices(desc);
						try {
							DFAgentDescription[] result = DFService.search(myAgent, template);
							if (result.length > 0) {
								conformMsg.addReceiver(result[0].getName());
								System.out.println("Message received from: " + result[0].getName());
								conformMsg.setConversationId("timetable setup");
								conformMsg.setContent("confirm");
								myAgent.send(conformMsg);
							}
						} catch (FIPAException fe) {
							fe.printStackTrace();
						}
						//creates a new utility tut
						int utility = utility(tut);
						availability = availability + utility;
						// if less or equal to a neutral availability
						if (utility <= 2) {
							ACLMessage swapMsg = new ACLMessage(ACLMessage.CFP);
							desc.setType("Timetable Agent");
							template.addServices(desc);
							try {
								DFAgentDescription[] dfdAvail = DFService.search(myAgent, template);
								if (dfdAvail.length > 0) {
									swapMsg.addReceiver(dfdAvail[0].getName());
									swapMsg.setLanguage(codec.getName());
									swapMsg.setOntology(timeOntology.getName());
									swapMsg.setConversationId("timetable setup");
									try {
										System.out.println("Message sent to the timetable agent");
										getContentManager().fillContent(swapMsg, slot);
										send(swapMsg);
									} catch (CodecException | OntologyException ce2) {
										ce2.printStackTrace();
									}
								}
							} catch (FIPAException fe) {
								fe.printStackTrace();
							}
						}
					}
				}
				catch (CodecException | OntologyException ce) {
					ce.printStackTrace();
				}
			}
		}
	}
	//class that handles messages
	private class timetableListener extends CyclicBehaviour {
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) {
				if (msg.getContent().equals("takedownRequest")) {
					myAgent.doDelete();
				} else if (msg.getContent().equals("newTick")) {
					boolean reqSwap = false;
					for (Tutorial tutorial : timetable) {
						int utility = utility(tutorial);
						availability = 2;
						availability = availability + utility;
						if (utility < 3) {
							reqSwap = true;
						}
					}
					if(reqSwap) {
						DFAgentDescription template = new DFAgentDescription();
						ServiceDescription desc = new ServiceDescription();
						ACLMessage reqMsg = new ACLMessage(ACLMessage.REQUEST);
						desc.setType("Timetable Agent");
						template.addServices(desc);
						try {
							DFAgentDescription[] availRecip = DFService.search(myAgent, template);
							if (availRecip.length > 0) {
								reqMsg.addReceiver(availRecip[0].getName());
							}
						} catch (FIPAException fe) {
							fe.printStackTrace();
						}
						reqMsg.setContent("reqTimetable");
						reqMsg.setConversationId("timetable setup");
						reqMsg.setReplyWith("cfp" + System.currentTimeMillis()); // Unique value
						myAgent.send(reqMsg);
					}
				}
			}
		}
	}
	// swap required class
	private class swapRequired extends CyclicBehaviour
	{
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.AGREE);
			ACLMessage msg = myAgent.receive(mt);
			if(msg != null){
				ContentElement ce;
				try {
					ce = getContentManager().extractContent(msg);
					if (ce instanceof Board) {
						Board board = (Board) ce;
						int availSlot = -1;
						for(int i = 0; i < board.getBoard().size(); i++) {
							for (Tutorial tutorial : timetable) {
								if (board.getBoard().get(i).getModuleName().equals(tutorial.getModuleName())) {
									int swapAvail = utility(board.getBoard().get(i));
									int currentAvail = utility(tutorial);
									System.out.println("Previous availability= " + swapAvail + " Current availability= " + currentAvail + " " + myAgent.getName());
									if (swapAvail > currentAvail) {
										availSlot = i;
									}
								}
							}
						}
						if(availSlot != -1) {
							Prop prop = new Prop();
							prop.setSlotOwner(board.getBoard().get(availSlot).getStudentOwner());
							prop.setProp(board.getBoard().get(availSlot));
							prop.setSlotRecipient(myAgent.getAID());
							ACLMessage swapAttempt = new ACLMessage(ACLMessage.PROPOSE);
							DFAgentDescription template = new DFAgentDescription();
							ServiceDescription desc = new ServiceDescription();
							desc.setType("Timetable Agent");
							template.addServices(desc);
							try {
								DFAgentDescription[] availSlotOwner = DFService.search(myAgent, template);
								if (availSlotOwner.length > 0) {
									swapAttempt.addReceiver(availSlotOwner[0].getName());
								}
							} catch (FIPAException fe) {
								fe.printStackTrace();
							}
							swapAttempt.setLanguage(codec.getName());
							swapAttempt.setOntology(timeOntology.getName());
							try {
								System.out.println("Swap requested");
								getContentManager().fillContent(swapAttempt, prop);
								send(swapAttempt);
								System.out.print("Swap sent");
							} catch (CodecException | OntologyException ce2) {
								ce2.printStackTrace();
							}
						}
					}
				} catch (CodecException | OntologyException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
	//swap slot class
	private class swapSlot extends CyclicBehaviour {
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) {
				ContentElement ce;
				try {
					ce = getContentManager().extractContent(msg);
					if (ce instanceof Slot) {
						Slot newSlot = (Slot) ce;
						for (int i = 0; i < timetable.size(); i++) {
							if (timetable.get(i).getModuleName().equals((newSlot.getSlot().getModuleName()))) {
								timetable.remove(i);
								timetable.add(newSlot.getSlot());
								int swap =0;
								swap++;
								System.out.println("Number of swap for this round is: "+ swap);
							}
						}
					}
				} catch (CodecException | OntologyException e) {
					e.printStackTrace();
				}
			}
		}

	}
	//utility function
	private int utility(Tutorial tutorial) {
		int scale = 2;
		for (Pref preference : preferences) {
			if ((preference.getStartTime() <= tutorial.getStartTime())) {
				if ((preference.getEndTime() >= tutorial.getEndTime())) {
					if (preference.getDay().equals(tutorial.getDay())) {
						switch (preference.getAvailability()) {
							case "Unavailable" -> scale = 0;
							case "Not Ideal" -> scale = 1;
							case "Fine" -> scale = 2;
							case "Ideal" -> scale = 3;
						}
					}
				}
			}
		}
		return scale;
	}
}
