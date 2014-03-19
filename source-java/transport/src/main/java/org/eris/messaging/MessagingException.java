package org.eris.messaging;

@SuppressWarnings("serial")
public class MessagingException extends Exception
{
    public MessagingException(String msg)
    {
        super(msg);
    }

    public MessagingException(String msg, Throwable t)
    {
        super(msg, t);
    }
}