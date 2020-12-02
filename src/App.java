import jade.core.*;
import jade.core.Runtime;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;


public class App {
	public static void main(String[] args) {
		///Types of agent "Unavailable", "Not Ideal", "Fine", "Ideal"
		Profile myProfile = new ProfileImpl();
		Runtime myRuntime = Runtime.instance();
		ContainerController myContainer = myRuntime.createMainContainer(myProfile);	
		try{
			AgentController rma = myContainer.createNewAgent("rma", "jade.tools.rma.rma", null);
			rma.start();
			
			//Tutorial tut = new Tutorial("SEM", "Tuesday", 1300, 1400);
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
			System.out.println("Exception starting agent: " + e.toString());
		}
	}

}
