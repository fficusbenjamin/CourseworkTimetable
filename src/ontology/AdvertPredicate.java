package ontology;


import jade.content.Predicate;
import jade.core.AID;

public class AdvertPredicate implements Predicate {
		/**
		 * 
		 */
		private AID owner;
		private AdvertBoard advertBoard;
		
		public AID getOwner() {
			return owner;
		}
		
		public void setOwner(AID owner) {
			this.owner = owner;
		}

		public AdvertBoard getAdvertBoard() {
			return advertBoard;
		}

		public void setAdvertBoard(AdvertBoard advertBoard) {
			this.advertBoard = advertBoard;
		}
}
