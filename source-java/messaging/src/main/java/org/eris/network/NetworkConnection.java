package org.eris.transport;

public interface NetworkConnection<T>
{
    public void setReceiver(Receiver<T> recv);

    public Sender<T> getSender();

    public void start() throws TransportException;

    public void close() throws TransportException;
}
