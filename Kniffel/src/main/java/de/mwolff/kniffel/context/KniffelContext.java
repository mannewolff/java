package de.mwolff.kniffel.context;

import de.mwolff.commons.command.Context;
import de.mwolff.commons.command.DefaultContext;
import de.mwolff.kniffel.common.Board;
import de.mwolff.kniffel.common.Wurf;

public class KniffelContext extends DefaultContext implements Context{

	public Wurf ActWurf;
	public int AnzWurf; // 1..3
	public boolean isKniffel;
	public boolean isBigStreet;
	public boolean isLittleStreet;
	public boolean isKuemmerChance;
	public boolean isFullHouse;
	public boolean onceMore;
	public boolean isZweier;
	public boolean isDreier;
	public boolean isVierer;
	public Board ActBoard;
}
