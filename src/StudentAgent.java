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
import time_ontology.Board;
import time_ontology.PropPredicate;
import time_ontology.Slot;
import time_ontology.TimeOntology;
import time_ontology.Tutorial;

import java.util.ArrayList;
import java.util.List;

import jade.content.ContentElement;
import jade.content.lang.Codec;
import jade.content.lang.Codec.CodecException;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.core.AID;

public class StudentAgent extends Agent {
	private Codec codec = new SLCodec();
	private Ontology timeOntology = TimeOntology.getInstance();

	List<Tutorial> timetable = new ArrayList<Tutorial>();
	
	List<Pref> preferences = new ArrayList<Pref>();

	private int availability = 0;


	public int getAvailability() {
		return availability;
	}
	
	protected void setup() {
		Object[] args = getArguments();
		if (args != null && args.length > 0) {
			Pref preference = new Pref();
			preference = (Pref) args[0];
			preferences.add(preference);
		}
		getContentManager().registerLanguage(codec);
		getContentManager().registerOntology(timeOntology);
		// System.out.println("Hello! Buyer-agent " + getAID().getName() + " is
		// ready.");
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("timetable-system");
		sd.setName("student");
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		} catch (FIPAException fe) {
			fe.printStackTrace();
		}
		addBehaviour(new TickerBehaviour(this, 6000) {
			protected void onTick() {

			}
		});
		addBehaviour(new recTimetable());
		addBehaviour(new reqTimetableAdd());
		addBehaviour(new timetableListener());
		addBehaviour(new swapRequired());
	}
	//request timetable addition
	private class reqTimetableAdd extends Behaviour {
		public void action() {
			DFAgentDescription template = new DFAgentDescription();
			ServiceDescription desc = new ServiceDescription();
			ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
			desc.setType("TimetableAgent");
			template.addServices(desc);
			try {
				DFAgentDescription[] result = DFService.search(myAgent, template);
				if (result.length > 0) {
					cfp.addReceiver(result[0].getName());
				}
			} catch (FIPAException fe) {
				fe.printStackTrace();
			}
			cfp.setContent("addition request");
			cfp.setConversationId("timetable-system");
			cfp.setReplyWith("cfp" + System.currentTimeMillis()); // Unique value
			myAgent.send(cfp);
		}

		public boolean done() {
			return true;
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
	}
	//receive timetable
	private class recTimetable extends CyclicBehaviour {
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) {
				try {
					ContentElement ce = null;
					ce = getContentManager().extractContent(msg);
					if (ce instanceof Slot) {
						Slot slot = (Slot) ce;
						Tutorial tut = slot.getTutorial();
						AID slotOwner = slot.getSlotOwner();
						System.out.println("Tutorial: " + tut.getDay() + tut.getModuleID() + tut.getModuleName() + "\n Student:" + slotOwner);
						timetable.add(tut);

						DFAgentDescription template = new DFAgentDescription();
						ServiceDescription desc = new ServiceDescription();

						//confirmation timetable received
						ACLMessage conformMsg = new ACLMessage(ACLMessage.CONFIRM);

						desc.setType("TimetableAgent");
						template.addServices(desc);
						try {
							DFAgentDescription[] result = DFService.search(myAgent, template);
							if (result.length > 0) {
								conformMsg.addReceiver(result[0].getName());
								System.out.println("result " + result[0].getName());

								conformMsg.setConversationId("timetable-system");

								// Let JADE convert from Java objects to string
								System.out.println("confirmation message sent");
								conformMsg.setContent("confirm");
								myAgent.send(conformMsg);
							}
						} catch (FIPAException fe) {
							fe.printStackTrace();
						}

						int availability = utility(tut);
						System.out.println("Availability is: " + availability);

						// Test with advertising neutral
						if (availability <= 2) {
							System.out.println("Availability <= 2");

							ACLMessage swapMsg = new ACLMessage(ACLMessage.CFP);
							desc.setType("TimetableAgent");
							template.addServices(desc);
							try {
								DFAgentDescription[] dfdAvail = DFService.search(myAgent, template);
								if (dfdAvail.length > 0) {
									swapMsg.addReceiver(dfdAvail[0].getName());
									System.out.println("Availability " + dfdAvail[0].getName());

									swapMsg.setLanguage(codec.getName());
									swapMsg.setOntology(timeOntology.getName());
									swapMsg.setConversationId("timetable-system");

									try {
										// Let JADE convert from Java objects to string
										System.out.println("Message sent to timetable agent");
										getContentManager().fillContent(swapMsg, slot);
										send(swapMsg);
									} catch (CodecException ce2) {
										ce2.printStackTrace();
									} catch (OntologyException oe) {
										oe.printStackTrace();
									}
								}
							} catch (FIPAException fe) {
								fe.printStackTrace();
							}

						}
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
	//method that handles messages
	private class timetableListener extends CyclicBehaviour {
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) {
				if (msg.getContent().equals("takedownRequest")) {
					System.out.println(myAgent.getAID().getName() + "is terminating");
					myAgent.doDelete();
				} else if (msg.getContent().equals("tickInform")) {
					//System.out.println("new student tick");
					boolean reqSwap = false;
					
					for(int i = 0; i < timetable.size(); i++) {
						int utility = utility(timetable.get(i));
						if(utility < 3) {
							reqSwap = true;
						}
					}
					
					if(reqSwap == true) {
						DFAgentDescription template = new DFAgentDescription();
						ServiceDescription desc = new ServiceDescription();
						ACLMessage cfp = new ACLMessage(ACLMessage.REQUEST);
						desc.setType("TimetableAgent");
						template.addServices(desc);
						try {
							DFAgentDescription[] availRecip = DFService.search(myAgent, template);
							if (availRecip.length > 0) {
								cfp.addReceiver(availRecip[0].getName());
							}
						} catch (FIPAException fe) {
							fe.printStackTrace();
						}
						cfp.setContent("requestTimetable");
						cfp.setConversationId("timetable-system");
						cfp.setReplyWith("cfp" + System.currentTimeMillis()); // Unique value
						myAgent.send(cfp);
					}
				}
			}
		}
	}

	private class swapRequired extends CyclicBehaviour
	{
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.AGREE);
			ACLMessage msg = myAgent.receive(mt);
			if(msg != null){
				ContentElement ce = null;
				try {
					ce = getContentManager().extractContent(msg);
					if (ce instanceof Board) {
						Board advert = (Board) ce;

						int availSlot = -1;
						for(int i = 0; i < advert.getBoard().size(); i++) {

							for(int j = 0; j < timetable.size(); j++) {

								if(advert.getBoard().get(i).getModuleName().equals(timetable.get(j).getModuleName())) {

									int swapAvail = utility(advert.getBoard().get(i));
									int currentAvail = utility(timetable.get(j));

									if(swapAvail > currentAvail) {
										availSlot = i;
									}
								}
							}
						}

						if(availSlot != -1) {
							PropPredicate prop = new PropPredicate();
							prop.setSlotOwner(advert.getBoard().get(availSlot).getStudentOwner());
							prop.setSlot(advert.getBoard().get(availSlot));
							prop.setSlotRecipient(myAgent.getAID());
							
							ACLMessage advertSwap = new ACLMessage(ACLMessage.PROPOSE);
							DFAgentDescription template = new DFAgentDescription();
							ServiceDescription desc = new ServiceDescription();
							
							desc.setType("TimetableAgent");
							template.addServices(desc);
							try {
								DFAgentDescription[] availSlotOwner = DFService.search(myAgent, template);
								if (availSlotOwner.length > 0) {
									advertSwap.addReceiver(availSlotOwner[0].getName());
								}
							} catch (FIPAException fe) {
								fe.printStackTrace();
							}
							advertSwap.setLanguage(codec.getName());
							advertSwap.setOntology(timeOntology.getName());
							
							try {
								// Let JADE convert from Java objects to string
								System.out.println("Swap requested");
								getContentManager().fillContent(advertSwap, prop);
								send(advertSwap);
								System.out.print("Swap sent");
							} catch (CodecException ce2) {
								ce2.printStackTrace();
							} catch (OntologyException oe) {
								oe.printStackTrace();
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

	private int utility(Tutorial tutorial) {
		int scale = 0;
		for (int i = 0; i < preferences.size(); i++) {
				if ((preferences.get(i).getStartTime() <= tutorial.getStartTime()) && (preferences.get(i).getEndTime() >= tutorial.getEndTime()) && preferences.get(i).getDay().equals(tutorial.getDay())) {
					switch (preferences.get(i).getAvailability()) {
						case "Unavailable":
							scale = 0;
							break;
						case "Not Ideal":
							scale = 1;
							break;
						case "Fine":
							scale = 2;
							break;
						case "Ideal":
							scale = 3;
							break;
					}
				}
			}
		availability = availability + scale;
		return scale;

	}
}
