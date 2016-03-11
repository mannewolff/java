package de.mwolff.kniffel.common;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import de.mwolff.kniffel.context.KniffelContext;


public class TestBoard {

	private Board board;
	
	@Before
	public void setUp() {
		board = new Board();
	}

	@Test
	public void testGetValueInRow() throws Exception {
		board.setOben(Constants.EINS, 3, 0, false);
		int value = board.get(Constants.EINS, 0);
		assertEquals(3, value);
	}
	
	@Test
		public void testSetObenValueInRow() throws Exception {
			board.setOben(Constants.EINS, 3, 0, false);
			int value = board.isFree(Constants.EINS);
			assertEquals(1, value);
			value = board.isFree(Constants.GESAMTOBEN);
			assertEquals(1, value);
			value = board.get(Constants.GESAMTOBEN, 0);
			assertEquals(3, value);
		}
	
	@Test
	public void testTendenz() throws Exception {
		board.setOben(Constants.EINS, 3, 0, false);
		// Tendenz must be 0
		int tendenz = board.get(Constants.TENDENZ, 0);
		assertEquals(0, tendenz);
		
		board.setOben(Constants.ZWEI, 4, 0,  false);
		// Tendenz must be -2
		tendenz = board.get(Constants.TENDENZ, 0);
		assertEquals(-2, tendenz);
		
		board.setOben(Constants.DREI, 12, 0,  false);
		// Tendenz must be +1
		tendenz = board.get(Constants.TENDENZ, 0);
		assertEquals(1, tendenz);
	}
		
	@Test
	public void testIsFree() throws Exception {
		int value = board.isFree(Constants.EINS);
		assertEquals(0, value);
	}

	@Test
		public void testSetObenSix() throws Exception {
			int position = board.isFree(Constants.EINS);
			assertEquals(0, position);
			board.setOben(Constants.EINS, 3, position, false);
			position = board.isFree(Constants.EINS);
			assertEquals(1, position);
			board.setOben(Constants.EINS, 3, position, false);
			position = board.isFree(Constants.EINS);
			assertEquals(2, position);
			board.setOben(Constants.EINS, 3, position, false);
			position = board.isFree(Constants.EINS);
			assertEquals(3, position);
			board.setOben(Constants.EINS, 3, position, false);
			position = board.isFree(Constants.EINS);
			assertEquals(4, position);
			board.setOben(Constants.EINS, 3, position, false);
			position = board.isFree(Constants.EINS);
			assertEquals(5, position);
			board.setOben(Constants.EINS, 3, position, false);
			int value = board.isFree(Constants.EINS);
			assertEquals(Constants.FULL, value);
		}	

	@Test
		public void testSetObenSixAutomatic() throws Exception {
			board.setOben(Constants.EINS, 3, 0, true);
			int position = board.isFree(Constants.EINS);
			assertEquals(1, position);
			board.setOben(Constants.EINS, 3, 0, true);
			position = board.isFree(Constants.EINS);
			assertEquals(2, position);
			board.setOben(Constants.EINS, 3, 0, true);
			position = board.isFree(Constants.EINS);
			assertEquals(3, position);
			board.setOben(Constants.EINS, 3, 0, true);
			position = board.isFree(Constants.EINS);
			assertEquals(4, position);
			board.setOben(Constants.EINS, 3, 0, true);
			position = board.isFree(Constants.EINS);
			assertEquals(5, position);
			board.setOben(Constants.EINS, 3, 0, true);
			int value = board.isFree(Constants.EINS);
			assertEquals(Constants.FULL, value);
		}	

    @Test
	public void testGetCountOf() throws Exception {
    	board.setOben(Constants.EINS, 1, 0, true);
    	board.setOben(Constants.EINS, 1, 0, true);
    	int value = board.getCountOf(Constants.EINS);
    	assertEquals(4, value);
	}
    
    @Test
	public void testIsBonusNeed() throws Exception {
    	KniffelContext context = new KniffelContext();
    	board.setOben(Constants.EINS, 2, 0, true);
    	board.setOben(Constants.ZWEI, 4, 0, true);
    	context.ActBoard = board;
    	board.setNextUnten(context, Constants.BIG, 40);
    	int value = board.isBonusNeed(0);
		board.printBoard();
    	assertEquals(-3, value);
	}

    @Test
	public void testGetRowsForBonus() throws Exception {

    	KniffelContext context = new KniffelContext();
    	
    	board.setOben(Constants.VIER, 8, 0, false);
    	board.setOben(Constants.FUENF, 10, 1, false);

    	context.ActBoard = board;
    	int[] bonuses = board.getRowsForBonus();
    	
    	assertEquals(-4, bonuses[0]);
    	assertEquals(-5, bonuses[1]);
	}

    @Test
	public void testPrintBoard() throws Exception {
		board.printBoard();
	}
}
