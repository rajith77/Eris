package org.eris.transport;

@SuppressWarnings("serial")
public class SenderClosedException extends TransportException
{
    public SenderClosedException(String msg)
    {
        super(msg);
    }

    public SenderClosedException(String msg, Throwable t)
    {
        super(msg, t);
    }
}
