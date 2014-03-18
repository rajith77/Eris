package org.eris.messaging;

@SuppressWarnings("serial")
public class SessionException extends Exception
{
    public SessionException(String msg)
    {
        super(msg);
    }

    public SessionException(String msg, Throwable t)
    {
        super(msg, t);
    }
}