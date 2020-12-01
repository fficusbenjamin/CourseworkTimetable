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
import jade.lang.acl.UnreadableException;
import ontology.*;

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
	private Ontology ontology = TimetableOntology.getInstance();

	List<Tutorial> timetable = new ArrayList<Tutorial>();
	
	List<Pref> preferences = new ArrayList<Pref>();

	private int happiness = 0;

	public int getHappiness() {
		return happiness;
	}
	
	protected void setup() {
		Object[] args = getArguments();
		if (args != null && args.length > 0) {
			Pref preference = new Pref();
			preference = (Pref) args[0];
			preferences.add(preference);
		}
		getContentManager().registerLanguage(codec);
		getContentManager().registerOntology(ontology);
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
		addBehaviour(new recieveTimetable());
		addBehaviour(new requestPerformer());
		addBehaviour(new handleInform());
		addBehaviour(new handleAdvertConfirm());
	}

	private class requestPerformer extends Behaviour {
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
			cfp.setContent("request");
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
		//System.out.println(getAID().getName() + "happiness: " + happiness);
	}

	private class recieveTimetable extends CyclicBehaviour {
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) {
				try {
					ContentElement ce = null;
					ce = getContentManager().extractContent(msg);
					if (ce instanceof StudPredicate) {
						StudPredicate owns = (StudPredicate) ce;
						Tutorial tut = owns.getTutorial();
						AID owner = owns.getOwner();
						System.out.println("Tutorial: " + tut.getDay() + "\n Owner:" + owner);
						timetable.add(tut);

						DFAgentDescription template = new DFAgentDescription();
						ServiceDescription desc = new ServiceDescription();

						ACLMessage confirmation = new ACLMessage(ACLMessage.CONFIRM);
						desc.setType("TimetableAgent");
						template.addServices(desc);
						try {
							DFAgentDescription[] result = DFService.search(myAgent, template);
							// System.out.println("Search?");§§
							if (result.length > 0) {
								confirmation.addReceiver(result[0].getName());
								System.out.println("result " + result[0].getName());

								confirmation.setConversationId("timetable-system");

								// Let JADE convert from Java objects to string
								System.out.println("confirmation message sent");
								confirmation.setContent("confirm");
								myAgent.send(confirmation);
							}
						} catch (FIPAException fe) {
							fe.printStackTrace();
						}

						int util = utility(tut);
						System.out.println("Utility is: " + util);

						// Test with advertising neutral
						if (util <= 0) {
							System.out.println("Util < 0");

							ACLMessage swapmsg = new ACLMessage(ACLMessage.CFP);
							desc.setType("AdvertiserAgent");
							template.addServices(desc);
							try {
								DFAgentDescription[] result = DFService.search(myAgent, template);
								// System.out.println("Search?");
								if (result.length > 0) {
									swapmsg.addReceiver(result[0].getName());
									System.out.println("result " + result[0].getName());

									swapmsg.setLanguage(codec.getName());
									swapmsg.setOntology(ontology.getName());
									swapmsg.setConversationId("timetable-system");

									try {
										// Let JADE convert from Java objects to string
										System.out.println("Advertiser agent message sent");
										getContentManager().fillContent(swapmsg, owns);
										send(swapmsg);
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

	private class handleInform extends CyclicBehaviour {
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) {
				if (msg.getContent().equals("takedownRequest")) {
					System.out.println(myAgent.getAID().getName() + "is terminating");
					myAgent.doDelete();
				} else if (msg.getContent().equals("tickInform")) {
					//System.out.println("new student tick");
					boolean advertNeeded = false;
					
					for(int i = 0; i < timetable.size(); i++) {
						int util = utility(timetable.get(i));
						if(util < 0) {
							advertNeeded = true;
						}
					}
					
					if(advertNeeded == true) {
						DFAgentDescription template = new DFAgentDescription();
						ServiceDescription desc = new ServiceDescription();
						ACLMessage cfp = new ACLMessage(ACLMessage.REQUEST);
						desc.setType("AdvertiserAgent");
						template.addServices(desc);
						try {
							DFAgentDescription[] result = DFService.search(myAgent, template);
							if (result.length > 0) {
								cfp.addReceiver(result[0].getName());
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

	private class handleAdvertConfirm extends CyclicBehaviour
	{
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.AGREE);
			ACLMessage msg = myAgent.receive(mt);
			if(msg != null){
				ContentElement ce = null;
				try {
					ce = getContentManager().extractContent(msg);
					if (ce instanceof AdvertPredicate) {
						AdvertPredicate advert = (AdvertPredicate) ce;
						int desiredSlot = maximum(advert.getAdvertBoard());
						System.out.println("Index of desired slot " + desiredSlot);
						
						if(desiredSlot != -1) {
							PropPredicate proposal = new PropPredicate();
							proposal.setOwner(advert.getAdvertBoard().getOwners().get(desiredSlot));
							proposal.setSlot(advert.getAdvertBoard().getSlots().get(desiredSlot));
							proposal.setProposee(myAgent.getAID());
							
							ACLMessage advertSwap = new ACLMessage(ACLMessage.PROPOSE);
							DFAgentDescription template = new DFAgentDescription();
							ServiceDescription desc = new ServiceDescription();
							
							desc.setType("AdvertiserAgent");
							template.addServices(desc);
							try {
								DFAgentDescription[] result = DFService.search(myAgent, template);
								if (result.length > 0) {
									advertSwap.addReceiver(result[0].getName());
								}
							} catch (FIPAException fe) {
								fe.printStackTrace();
							}
							advertSwap.setLanguage(codec.getName());
							advertSwap.setOntology(ontology.getName());
							
							try {
								// Let JADE convert from Java objects to string
								System.out.println("Advertiser agent message sent");
								getContentManager().fillContent(advertSwap, proposal);
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
	
	private int maximum(AdvertBoard adverts) {
		int maximum = -1;
		for(int i=0; i < adverts.getSlots().size(); i++) {
			
			for(int j = 0; j < timetable.size(); j++) {
				
				if(adverts.getSlots().get(i).getName().equals(timetable.get(j).getName())) {
					//System.out.println("slots are similar");
					
					int advertUtil = utility(adverts.getSlots().get(i));
					int currentUtil = utility(timetable.get(j));
					
					if(advertUtil > currentUtil) {
						maximum = i;
					}
				}
				
			}
		}
		return maximum;
	}
	private int utility(Tutorial tutty) {
		int score = 0;
		for (int i = 0; i < preferences.size(); i++) {
			if (preferences.get(i).getDay().equals(tutty.getDay())){
				if ((preferences.get(i).getStartTime() <= tutty.getStartTime()) && (preferences.get(i).getEndTime() >= tutty.getEndTime())) {
					switch (preferences.get(i).getType()) {
					case "Unable":
						score = -10;
						break;
					case "Prefer Not":
						score = -5;
						break;
					case "Neutral":
						score = 0;
						break;
					case "Would Like":
						score = 10;
						break;
					}
				}
			}
		}
		happiness = happiness + score;
		return score;

	}
}
