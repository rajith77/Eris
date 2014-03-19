package org.eris.messaging;

@SuppressWarnings("serial")
public class SenderException extends MessagingException
{
    public SenderException(String msg)
    {
        super(msg);
    }

    public SenderException(String msg, Throwable t)
    {
        super(msg, t);
    }
}