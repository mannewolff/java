package de.mwolff.commons.command;

import static org.junit.Assert.*;

import org.junit.Test;

public class TestContext {

	private static final String INTEGER_VALUE = "IntegerValue";
	private static final String STRING_VALUE = "StringValue";

	private Context context = new DefaultContext();

	@Test
	public void testContextInterface() throws Exception {
		assertNotNull(context);
	}

	@Test
	public void testPutGetContext() throws Exception {
		context.put(STRING_VALUE, STRING_VALUE);
		String stringValue = (String) context.get(STRING_VALUE);
		assertEquals(STRING_VALUE, stringValue);
	}

	@Test
	public void testGet() throws Exception {
		context.put(INTEGER_VALUE, Integer.valueOf(42));
		Integer integerValue = (Integer) context.get(INTEGER_VALUE);
		assertEquals(Integer.valueOf(42), integerValue);
	}

	@Test
	public void testGetAsString() throws Exception {
		context.put(STRING_VALUE, STRING_VALUE);
		String stringValue = context.getAsString(STRING_VALUE);
		assertEquals(STRING_VALUE, stringValue);
	}

	@Test
	public void testNullContext() throws Exception {
		Context nullContext = DefaultContext.NULLCONTEXT;
		assertNull(nullContext);
	}

	@Test
	public void testNullValue() throws Exception {
		Object value = context.getAsString("null");
		assertNotNull(value);
	}
}
