import jade.core.*;
import jade.core.Runtime;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;


///-------------------------------------------------------------------
///   Class:		App (Class)
///   Description:	Main class that initialise agents and their
///					ontologies.
///					Student agents have four kinds of availability:
/// 							  Unavailable (0)
///								  Not Ideal   (1)
///								  Fine		  (2)
///								  Ideal		  (3)
///
///   Author:		Francesco Fico (40404272)     Date: 02/12/2020
///-------------------------------------------------------------------


public class App {
	public static void main(String[] args) {
		//Types of agent "Unavailable", "Not Ideal", "Fine", "Ideal"
		// creates the profile, runtime and container
		Profile myProfile = new ProfileImpl();
		Runtime myRuntime = Runtime.instance();
		ContainerController myContainer = myRuntime.createMainContainer(myProfile);	
		try{
			//initialise the agent controller
			AgentController rma = myContainer.createNewAgent("rma", "jade.tools.rma.rma", null);
			rma.start();
			
			Pref prefA = new Pref("Unavailable", "Tuesday", 1200, 1700);
			Pref[] preferencesA = {prefA};
			
			Pref prefB = new Pref("Unavailable", "Friday", 1100, 1700);
			Pref[] preferencesB = {prefB};
			
			AgentController TimetableAgent = myContainer.createNewAgent("Timetabler", TimetableAgent.class.getCanonicalName(), null);
			TimetableAgent.start();
			
			AgentController StudentA = myContainer.createNewAgent("StudentA", StudentAgent.class.getCanonicalName(), preferencesA);
			StudentA.start();
			
			AgentController StudentB = myContainer.createNewAgent("StudentB", StudentAgent.class.getCanonicalName(), preferencesB);
			StudentB.start();
		}
		catch(Exception e){
			System.out.println("impossible to initialise agents " + e.toString());
		}
	}
}
