package org.eris.network;

@SuppressWarnings("serial")
public class SenderException extends NetworkException
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
