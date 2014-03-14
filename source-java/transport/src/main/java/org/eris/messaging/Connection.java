package org.eris.messaging;

public interface Connection
{
    public void connect() throws ConnectionException;
    
    public Session createSession() throws ConnectionException;
}
