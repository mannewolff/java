package de.mwolff.kniffel.analyzer;

import de.mwolff.kniffel.common.Wurf;

/**
 * Marker interface for different Analyzers
 */
public interface Analyzer {
	boolean analyze(final Wurf wurf);
}
