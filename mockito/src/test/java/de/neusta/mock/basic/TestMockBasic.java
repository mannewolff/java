package de.neusta.mock.basic;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

import org.junit.Test;

public class TestMockBasic {

	@Test
	public void myClass() throws Exception {
		MyClass test = new MyClass();
		assertThat("Die Antwort ist 42.", is(test.getAnswerOfAllQuestions()));
	}

	@Test
	public void myClassWithMock() throws Exception {
		MyClass test = mock(MyClass.class);

		// preparation
		when(test.getAnswerOfAllQuestions()).thenReturn("Die Antwort ist 45.");

		//execution
		String result = test.getAnswerOfAllQuestions();

		// verification
		
		// more or less redundant because result is only set if method is
		// called.
		verify(test, times(1)).getAnswerOfAllQuestions();
		assertThat("Die Antwort ist 45.", is(result));
	}

}
