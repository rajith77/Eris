package org.eris.messaging;

public interface Connection
{
    public void connect() throws TransportException, ConnectionException, TimeoutException;
    
    public Session createSession() throws TransportException, ConnectionException, TimeoutException;
}
