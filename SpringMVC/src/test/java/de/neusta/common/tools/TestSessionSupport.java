package de.neusta.common.tools;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class TestSessionSupport {

	@Mock
	HttpSession session;

	@Mock
	HttpServletRequest request;

	@Test
	public void sessionHasLoginInformation() throws Exception {
		Mockito.when(this.request.getSession()).thenReturn(this.session);
		Mockito.when(this.session.getAttribute("Login")).thenReturn(
				new LoginSessionInformation());
		assertTrue(SessionSupport.validateSessionOnLogon(this.request));
	}

	@Test
	public void sessionHasNoLoginInformation() throws Exception {
		Mockito.when(this.request.getSession()).thenReturn(this.session);
		assertFalse(SessionSupport.validateSessionOnLogon(this.request));
	}

	@Test
	public void sessionIsNull() throws Exception {
		Mockito.when(this.request.getSession()).thenReturn(null);
		assertFalse(SessionSupport.validateSessionOnLogon(this.request));
	}

	@Test
	public void uglyTestBecauseOnlyStaticMehods() throws Exception {
		final SessionSupport sessionSupport = new SessionSupport();
		assertNotNull(sessionSupport);
	}

}
