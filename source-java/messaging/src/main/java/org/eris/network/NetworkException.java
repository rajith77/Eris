package org.eris.network;

@SuppressWarnings("serial")
public class NetworkException extends Exception
{
    public NetworkException(String msg)
    {
        super(msg);
    }

    public NetworkException(String msg, Throwable t)
    {
        super(msg, t);
    }
}