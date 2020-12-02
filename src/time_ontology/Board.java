package time_ontology;

import java.util.ArrayList;
import java.util.List;
import jade.content.Predicate;


public class Board implements Predicate {

	private List<Tutorial> board = new ArrayList<>();
	
	public List<Tutorial> getBoard() {
		return board;
	}

	public void setBoard(List<Tutorial> board) {
		this.board = board;
	}




	
}
