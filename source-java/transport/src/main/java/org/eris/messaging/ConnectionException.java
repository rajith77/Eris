package org.eris.messaging;

@SuppressWarnings("serial")
public class ConnectionException extends Exception
{
    public ConnectionException(String msg)
    {
        super(msg);
    }

    public ConnectionException(String msg, Throwable t)
    {
        super(msg, t);
    }
}