package de.mwolff.commons.command;

//@formatter:off
import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;
//@formatter:on

import org.junit.Test;

public class CommandExceptionTest {
    
    @Test
    public void commandExceptionDefaultConstructorTest() throws Exception {
        assertThat(new CommandException(), notNullValue());
    }
    
    @Test
    public void commandExceptionWithMessageTest() throws Exception {
        CommandException commandException = new CommandException("message");
        assertThat(commandException.getMessage(), is("message"));
    }
    
    @Test
    public void commandExceptionWithMessageAndThrowableTest() throws Exception {
        CommandException commandException = new CommandException("message", null);
        assertThat(commandException.getMessage(), is("message"));
        assertThat(commandException.getCause(), nullValue());
    }

    @Test
    public void commandExceptionWithThrowableTest() throws Exception {
        Exception exception = new Exception();
        CommandException commandException = new CommandException(exception);
        assertThat(commandException.getCause(), is(exception));
    }

}
