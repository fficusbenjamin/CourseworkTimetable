package time_ontology;

import java.util.ArrayList;
import java.util.List;
import jade.content.Predicate;

///------------------------------------------------------------------------
///   Class:		Board (Class)
///   Description:	Class that, being a predicate of the ontology,holds
///					the list of unwanted tutorials to be swapped on a board.
///
///
///
///   Author:		Francesco Fico (40404272)     Date: 02/12/2020
///------------------------------------------------------------------------


public class Board implements Predicate {
	//creates a new list of tutorials for the board
	private List<Tutorial> board = new ArrayList<>();
	//get and set for the board
	public List<Tutorial> getBoard() {
		return board;
	}
	public void setBoard(List<Tutorial> board) {
		this.board = board;
	}
}
