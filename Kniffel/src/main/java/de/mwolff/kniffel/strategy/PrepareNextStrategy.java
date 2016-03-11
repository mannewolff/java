package de.mwolff.kniffel.strategy;

import de.mwolff.commons.command.Command;
import de.mwolff.commons.command.DefaultCommand;
import de.mwolff.kniffel.common.Wurf;
import de.mwolff.kniffel.context.KniffelContext;

public class PrepareNextStrategy extends DefaultCommand<KniffelContext> implements
		Command<KniffelContext> {

	@Override
	public void execute(KniffelContext context) throws Exception {

		Wurf wurf = context.ActWurf;

		prepareForNextWurf(context);

		if (context.AnzWurf == 4) {
			// Fertig !
			throw new Exception();
		}

	}

	/**
	 * Wird nur aufgerufen, wenn kein Kniffel und keine große Strasse bzw.
	 * beides schon weg ist.
	 * 
	 * @param context
	 */
	private void prepareForNextWurf(final KniffelContext context) {

		// Wenn 3. Wurf, dann muss entschieden werden
		if (context.AnzWurf == 3) {

			// 4er oder 4er Pasch
			
			
			
			// 3er oder 3er Pasch oder Full House
			
			// Kleine Strasse
			
			// Chance oder Streichen
			
		} else {

			// Ansonsten muss zurückgelegt werden
		}

		context.AnzWurf++;
	}

}
