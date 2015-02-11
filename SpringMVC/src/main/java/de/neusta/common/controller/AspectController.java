package de.neusta.common.controller;

import org.apache.log4j.Logger;

public class AspectController {

	private long time;
	private Long actTime;
	private Logger log;

	public void beginMethod(final Logger log, final String methodName) {
		this.log = log;
		this.time = System.currentTimeMillis();
		log.debug("Performing request mapping: " + methodName + ".");
	}

	public void endMethod() {
		actTime = Long.valueOf(System.currentTimeMillis() - time);
		log.debug("Operation took " + actTime.toString() + " milliseconds");
	}

}
