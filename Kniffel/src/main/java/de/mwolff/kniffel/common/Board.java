package de.mwolff.kniffel.common;

import java.util.HashMap;
import java.util.Map;

import de.mwolff.kniffel.context.KniffelContext;

public class Board implements Constants {

	private Map<Integer, Integer[]> boardStructure = new HashMap<Integer, Integer[]>();
	{
		boardStructure.put(EINS, new Integer[6]);
		boardStructure.put(ZWEI, new Integer[6]);
		boardStructure.put(DREI, new Integer[6]);
		boardStructure.put(VIER, new Integer[6]);
		boardStructure.put(FUENF, new Integer[6]);
		boardStructure.put(SECHS, new Integer[6]);
		boardStructure.put(DREIER, new Integer[6]);
		boardStructure.put(VIERER, new Integer[6]);
		boardStructure.put(LITTLE, new Integer[6]);
		boardStructure.put(BIG, new Integer[6]);
		boardStructure.put(FULLHOUSE, new Integer[6]);
		boardStructure.put(KNIFFEL, new Integer[6]);
		boardStructure.put(GESAMTOBEN, new Integer[6]);
		boardStructure.put(GESAMTUNTEN, new Integer[6]);
		boardStructure.put(CHANCE, new Integer[6]);
		boardStructure.put(TENDENZ, new Integer[6]);

	}

	public int isFree(final Integer column) {

		Integer[] boardcolumn = boardStructure.get(column);
		int position = 0;

		for (Integer integer : boardcolumn) {
			if (integer == null) {
				break;
			}
			position++;
		}
		return position;
	}

	public int get(final Integer column, final int position) {
		Integer[] boardcolumn = boardStructure.get(column);
		Integer value = boardcolumn[position];

		int val;
		if (value == null) {
			val = 0;
		} else
			val = value.intValue();
		return val;
	}

	private int set(final Integer column, final Integer value,
			final int position, final boolean next) {
		Integer[] boardcolumn = boardStructure.get(column);
		int aposition = position;
		if (next) {
			aposition = isFree(column);
		}

		boardcolumn[aposition] = Integer.valueOf(value);
		boardStructure.put(column, boardcolumn);
		return aposition;
	}

	public void setOben(final Integer column, final Integer value,
			final int position, final boolean next) {

		int aposition = set(column, value, position, next);
		Integer[] gesamtOben = boardStructure.get(GESAMTOBEN);
		if (gesamtOben[aposition] == null) {
			gesamtOben[aposition] = Integer.valueOf(0);
		}

		gesamtOben[aposition] = Integer.valueOf(gesamtOben[aposition]
				.intValue() + value);
		boardStructure.put(GESAMTOBEN, gesamtOben);

		Integer[] tendenz = boardStructure.get(TENDENZ);
		if (tendenz[aposition] == null) {
			tendenz[aposition] = 0;
		}
		int ausgewogen = 3 * column.intValue();
		int atendenz = value - ausgewogen;
		tendenz[aposition] += atendenz;
		boardStructure.put(TENDENZ, tendenz);
	}

	public void setNextUnten(final KniffelContext context,
			final Integer column, final int value) {
		Board board = context.ActBoard;
		int actposition = board.set(column, value, 0, true);
		int actvalue = board.get(Constants.GESAMTUNTEN, actposition);
		actvalue += value;
		board.set(Constants.GESAMTUNTEN, actvalue, actposition, false);
	}

	public int getCountOf(Integer column) {
		Integer[] boardcolumn = boardStructure.get(column);
		int count = 0;
		for (Integer integer : boardcolumn) {
			if (integer == null)
				count++;
		}
		return count;
	}

	public void printBoard() {

		System.out.println("-----------------------------------------------");
		for (int i = 1; i < 7; i++) {
			Integer[] boardcolumn = boardStructure.get(Integer.valueOf(i));
			System.out.print(" " + i + "          ");
			for (Integer integer : boardcolumn) {
				if (integer == null)
					integer = 0;
				String result = format(integer.toString());
				System.out.print(result);
			}
			System.out.println("");
		}
		System.out.println("-----------------------------------------------");
		System.out.print(" Gesamt     ");
		Integer[] boardcolumn = boardStructure.get(GESAMTOBEN);
		for (Integer integer : boardcolumn) {
			if (integer == null)
				integer = 0;
			String result = format(integer.toString());
			System.out.print(result);
		}
		System.out.println("");
		System.out.println("===============================================");

		String[] strings = { "Dreier   ", "Vierer   ", "FullHouse",
				"Kleine   ", "Grosse   ", "Kniffel  ", "Chance   " };
		for (int i = 100; i <= 106; i++) {
			boardcolumn = boardStructure.get(Integer.valueOf(i));
			System.out.print(" " + strings[i - 100] + "  ");
			for (Integer integer : boardcolumn) {
				if (integer == null)
					integer = 0;
				String result = format(integer.toString());
				System.out.print(result);
			}
			System.out.println("");
		}
		System.out.println("-----------------------------------------------");
		System.out.print(" Gesamtunten");
		boardcolumn = boardStructure.get(GESAMTUNTEN);
		for (Integer integer : boardcolumn) {
			if (integer == null)
				integer = 0;
			String result = format(integer.toString());
			System.out.print(result);
		}
		System.out.println("");
		System.out.print(" Gesamtoben ");
		boardcolumn = boardStructure.get(GESAMTOBEN);
		for (Integer integer : boardcolumn) {
			if (integer == null)
				integer = 0;
			String result = format(integer.toString());
			System.out.print(result);
		}
		System.out.println("");
		System.out.println("===============================================");

	}

	private String format(final String string) {

		String result = string;
		while (result.length() < 5) {
			result = " " + result;
		}
		return result;
	}

	public int isBonusNeed(int position) {
		int tendenz = get(TENDENZ, position);
		return tendenz;
	}

	public int[] getRowsForBonus() {

		int[] bonuses = new int[6];
		for (int i = 0; i < 6; i++) {
			bonuses[i] = isBonusNeed(i);
		}
		return bonuses;
	}

}