package org.eris.messaging;

@SuppressWarnings("serial")
public class ReceiverException extends MessagingException
{
    public ReceiverException(String msg)
    {
        super(msg);
    }

    public ReceiverException(String msg, Throwable t)
    {
        super(msg, t);
    }
}