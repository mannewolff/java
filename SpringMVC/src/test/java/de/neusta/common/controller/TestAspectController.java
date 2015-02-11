package de.neusta.common.controller;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class TestAspectController {

	@Mock
	Logger log;
	

	@Test
	public void testBeginMethod() throws Exception {
		AspectController aspectController = new AspectController();
		aspectController.beginMethod(log, "myMethod");
		Logger log = (Logger) ReflectionTestUtils.getField(aspectController,"log");
		Assert.assertSame(log, this.log);
		long time = (long) ReflectionTestUtils.getField(aspectController,"time");
		// @TODO: Find a good test
		//Assert.assertTrue(time < 10000);
	}

	@Test
	public void testEndMethod() throws Exception {
		AspectController aspectController = new AspectController();
		aspectController.beginMethod(log, "myMethod");
		aspectController.endMethod();
		long time = (long) ReflectionTestUtils.getField(aspectController,"time");
		Long actTime = (Long) ReflectionTestUtils.getField(aspectController,"actTime");
		Assert.assertTrue(time > actTime.longValue());
	}
	
	
}