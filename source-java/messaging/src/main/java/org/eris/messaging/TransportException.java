package org.eris.messaging;

/**
 * Thrown when the underlying network connection gets to an erroneous state.
 */
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