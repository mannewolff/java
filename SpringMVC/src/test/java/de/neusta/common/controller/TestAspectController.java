package de.neusta.common.controller;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class TestAspectController {

	@Mock
	Logger log;
	

	@Test
	public void testBeginMethod() throws Exception {
		AspectController aspectController = new AspectController();
		aspectController.beginMethod(log, "myMethod");
	}

	@Test
	public void testEndMethod() throws Exception {
		AspectController aspectController = new AspectController();
		aspectController.beginMethod(log, "myMethod");
		aspectController.endMethod();
	}
	
	
}
