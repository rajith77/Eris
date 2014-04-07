package org.eris.transport;

@SuppressWarnings("serial")
public class TransportException extends Exception
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