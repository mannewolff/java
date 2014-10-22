package de.neusta.common.tools;

import static org.junit.Assert.*;

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
	public void uglyTestBecauseOnlyStaticMehods() throws Exception {
		SessionSupport sessionSupport = new SessionSupport();
		assertNotNull(sessionSupport);
	}
	
	@Test
	public void sessionIsNull() throws Exception {
		Mockito.when(request.getSession()).thenReturn(null);
		assertFalse(SessionSupport.validateSessionOnLogon(request));
	}

	@Test
	public void sessionHasNoLoginInformation() throws Exception {
		Mockito.when(request.getSession()).thenReturn(session);
		assertFalse(SessionSupport.validateSessionOnLogon(request));
	}
	
	@Test
	public void sessionHasLoginInformation() throws Exception {
		Mockito.when(request.getSession()).thenReturn(session);
		Mockito.when(session.getAttribute("Login")).thenReturn(new LoginSessionInformation());
		assertTrue(SessionSupport.validateSessionOnLogon(request));
	}

	
	
}
