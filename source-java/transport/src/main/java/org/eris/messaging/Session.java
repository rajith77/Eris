package org.eris.messaging;

public interface Session
{
    public Sender createSender(String address, SenderMode mode) throws TransportException, SessionException, TimeoutException;
    
    public Receiver createReceiver(String address) throws TransportException, SessionException, TimeoutException;

    public void close() throws org.eris.messaging.TransportException;
}
