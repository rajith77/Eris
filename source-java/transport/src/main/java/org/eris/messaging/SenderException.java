package org.eris.messaging;

@SuppressWarnings("serial")
public class SenderException extends Exception
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