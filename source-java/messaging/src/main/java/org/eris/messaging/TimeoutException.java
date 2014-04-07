package org.eris.messaging;

@SuppressWarnings("serial")
public class TimeoutException extends MessagingException
{
    public TimeoutException(String msg)
    {
        super(msg);
    }

    public TimeoutException(String msg, Throwable t)
    {
        super(msg, t);
    }
}