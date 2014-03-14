package org.eris.messaging;

public interface Session
{
    public Sender createSender(String address);
    
    public Receiver createReceiver(String address);
}
