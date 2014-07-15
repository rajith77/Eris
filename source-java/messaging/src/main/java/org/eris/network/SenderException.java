package org.eris.network;

@SuppressWarnings("serial")
public class SenderException extends TransportException
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
