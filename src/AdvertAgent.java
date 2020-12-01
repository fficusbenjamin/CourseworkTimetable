//import java.util.ArrayList;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import jade.content.ContentElement;
import jade.content.abs.AbsContentElement;
import jade.content.lang.Codec;
import jade.content.lang.Codec.CodecException;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import ontology.*;

public class AdvertAgent extends Agent {
	private Codec codec = new SLCodec();
	private Ontology ontology = TimetableOntology.getInstance();

	AdvertBoard advertboard = new AdvertBoard();
	ArrayList<PropPredicate> proposals = new ArrayList<PropPredicate>();
	
	int tickCount = 0;

	protected void setup() {
		getContentManager().registerLanguage(codec);
		getContentManager().registerOntology(ontology);
		// System.out.println("Hello! Buyer-agent " + getAID().getName() + " is
		// ready.");
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("AdvertiserAgent");
		sd.setName("AdvertiserAgent");
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		} catch (FIPAException fe) {
			fe.printStackTrace();
		}
		addBehaviour(new TickerBehaviour(this, 6000) {
			protected void onTick() {
				tick();
			}
		});
		addBehaviour(new RecieveSlot());
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
			cfp.setConversationId("timetable-system");
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
			cfp.setConversationId("timetable-system");
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
		System.out.println("Advertiser agent terminating.");
	}
	
	private void handleSwaps() {
		for(int i = 0; i < proposals.size(); i++) {
			for(int j = 0; j < proposals.size(); j++) {
				if(proposals.get(i).getOwner().equals(proposals.get(j).getProposee())) {
					
					
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
			
			if(msg != null) {
				try {
					ContentElement ce = null;
					ce = getContentManager().extractContent(msg);
					if(ce instanceof PropPredicate) {
						PropPredicate owns = (PropPredicate) ce;
						proposals.add(owns);
						System.out.println("Recieved: "+ owns.getSlot().getName());
						System.out.println("From: "+ owns.getOwner().getName());
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

	private class RecieveSlot extends CyclicBehaviour {
		public void action() {
			// System.out.println("Advertiser agent action ran");
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) {
				try {
					ContentElement ce = null;
					ce = getContentManager().extractContent(msg);
					if (ce instanceof StudPredicate) {
						StudPredicate owns = (StudPredicate) ce;
						advertboard.getSlots().add(owns.getTutorial());
						advertboard.getOwners().add(owns.getOwner());
						System.out.print("Advertiser Agent added slot: " + owns.getTutorial().getName());
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
						AdvertPredicate advpredicate = new AdvertPredicate();
						advpredicate.setAdvertBoard(advertboard);
						advpredicate.setOwner(myAgent.getAID());
						getContentManager().fillContent(reply, advpredicate);
						//getContentManager().fillContent(reply, (ContentElement) advertBoard);
						send(reply);
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
}
