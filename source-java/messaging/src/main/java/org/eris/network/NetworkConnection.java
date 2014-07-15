package org.eris.network;

public interface NetworkConnection<T>
{
    public void setReceiver(Receiver<T> recv);

    public Sender<T> getSender();

    public void start() throws NetworkException;

    public void close() throws NetworkException;
}
