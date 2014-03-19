package org.eris.messaging;

@SuppressWarnings("serial")
public class TransportException extends MessagingException
{
    public TransportException(String msg)
    {
        super(msg);
    }

    public TransportException(String msg, Throwable t)
    {
        super(msg, t);
    }
}