package org.eris.messaging;

public interface Session
{
    public Sender createSender(String address) throws SessionException;
    
    public Receiver createReceiver(String address) throws SessionException;

    public void close();
}
