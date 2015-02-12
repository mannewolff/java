package de.neusta.common.controller;

import org.apache.log4j.Logger;

public class AspectController {

	private long time;
	private long actTime;
	private Logger log;

	public void beginMethod(final Logger log, final String methodName) {
		this.log = log;
		this.time = System.currentTimeMillis();
		log.debug("time = " + time);
		log.debug("Performing request mapping: " + methodName + ".");
	}

	public void endMethod() {
		actTime = System.currentTimeMillis();
		log.debug("actTime = " + actTime);
		Long dif = Long.valueOf(actTime - time);
		log.debug("dif = " + dif);
		log.debug("Operation took " + dif.toString()  + " milliseconds");
	}

}
