package de.neusta.mock.basic;

import java.util.LinkedList;
import java.util.List;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;


import org.junit.Test;

public class TestSpy {

	@Test
	public void simpleSpy() throws Exception {

		List<String> list = new LinkedList<String>();
		List<String> spy = spy(list);

		// optionally, you can stub out some methods:
		when(spy.size()).thenReturn(100);

		// using the spy calls real methods
		spy.add("one");
		spy.add("two");

		// prints "one" - the first element of a list
		assertThat("one", is(spy.get(0)));
		assertThat("two", is(spy.get(1)));

		// size() method was stubbed - 100 is printed
		assertThat(100, is(spy.size()));

		// optionally, you can verify
		verify(spy).add("one");
		verify(spy).add("two");
	}

}
